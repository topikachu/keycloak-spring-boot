package com.github.topikachu.keycloak.spring.support.security;

import org.jboss.resteasy.core.Headers;
import org.jboss.resteasy.specimpl.ResteasyHttpHeaders;
import org.keycloak.common.ClientConnection;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.admin.AdminAuth;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.permissions.AdminPermissions;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.util.Enumeration;
import java.util.List;

public class KeycloakAdminSecurityCustomizer implements WebSecurityCustomizer {
    @Override
    public void customize(WebSecurity web) {
        web.addSecurityFilterChainBuilder(() -> {
            return new SecurityFilterChain() {

                @Override
                public boolean matches(HttpServletRequest request) {
                    return true;
                }

                @Override
                public List<Filter> getFilters() {
                    return List.of(new KeycloakAdminCredentialFilter());
                }
            };
        });
    }

    static class KeycloakAdminCredentialFilter extends OncePerRequestFilter {

        private TokenManager tokenManager = new TokenManager();



        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
            extractAuthentication(request);
            filterChain.doFilter(request,response);
        }

        private void extractAuthentication(HttpServletRequest request) {
            KeycloakSession session = (KeycloakSession) request.getAttribute("KEYCLOAK_SESSION");
            ClientConnection clientConnection = (ClientConnection) request.getAttribute("KEYCLOAK_CLIENT_CONNECTION");
            if (session == null) {
                return;
            }
            ResteasyHttpHeaders headers = new ResteasyHttpHeaders(extractRequestHeaders(request));
            String tokenString = AppAuthManager.extractAuthorizationHeaderToken(headers);
            if (tokenString == null)  {
                return;
            }
            AccessToken token;
            try {
                JWSInput input = new JWSInput(tokenString);
                token = input.readJsonContent(AccessToken.class);
            } catch (JWSInputException e) {
                throw new BadCredentialsException("Bearer token format error");
            }
            String realmName = token.getIssuer().substring(token.getIssuer().lastIndexOf('/') + 1);
            RealmManager realmManager = new RealmManager(session);
            RealmModel realm = realmManager.getRealmByName(realmName);
            if (realm == null) {
                throw new BadCredentialsException("Unknown realm in token");
            }
            session.getContext().setRealm(realm);
            AuthenticationManager.AuthResult authResult = new AppAuthManager.BearerTokenAuthenticator(session)
                    .setRealm(realm)
                    .setConnection(clientConnection)
                    .setHeaders(headers)
                    .authenticate();
            if (authResult == null) {
                logger.debug("Token not valid");
                throw new BadCredentialsException("Bearer");
            }
            ClientModel client = realm.getClientByClientId(token.getIssuedFor());
            if (client == null) {
                throw new BadCredentialsException("Could not find client for authorization");

            }
            AdminAuth adminAuth = new AdminAuth(realm, authResult.getToken(), authResult.getUser(), client);
            AdminPermissionEvaluator evaluator=AdminPermissions.evaluator(session,realm,adminAuth );
            KeycloakAuthentication authentication= new KeycloakAuthentication(authResult, session,evaluator);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }


    }

    public static MultivaluedMap<String, String> extractRequestHeaders(HttpServletRequest request) {
        Headers<String> requestHeaders = new Headers<String>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            Enumeration<String> headerValues = request.getHeaders(headerName);
            while (headerValues.hasMoreElements()) {
                String headerValue = headerValues.nextElement();
                requestHeaders.add(headerName, headerValue);
            }
        }
        return requestHeaders;
    }
}
