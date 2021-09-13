package com.github.topikachu.keycloak.spring.support.security;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.jboss.resteasy.plugins.server.servlet.ServletUtil;
import org.jboss.resteasy.specimpl.ResteasyHttpHeaders;
import org.keycloak.common.ClientConnection;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.admin.AdminAuth;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;

@RequiredArgsConstructor
public class KeycloakAdminCredentialFilter extends OncePerRequestFilter {


    @NonNull
    private String keycloakServletPrefix;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        extractAuthentication(request);
        filterChain.doFilter(request, response);
    }

    private void extractAuthentication(HttpServletRequest request) {
        KeycloakSession session = (KeycloakSession) request.getAttribute("KEYCLOAK_SESSION");
        ClientConnection clientConnection = (ClientConnection) request.getAttribute("KEYCLOAK_CLIENT_CONNECTION");
        if (session == null) {
            return;
        }
        ResteasyHttpHeaders headers = ServletUtil.extractHttpHeaders(request);
        String tokenString = AppAuthManager.extractAuthorizationHeaderToken(headers);
        if (tokenString == null) {
            return;
        }
        AccessToken token;
        try {
            JWSInput input = new JWSInput(tokenString);
            token = input.readJsonContent(AccessToken.class);
        } catch (JWSInputException e) {
            logger.warn("Bearer token format error");
            return;
        }
        String realmName = token.getIssuer().substring(token.getIssuer().lastIndexOf('/') + 1);
        RealmManager realmManager = new RealmManager(session);
        RealmModel realm = realmManager.getRealmByName(realmName);
        if (realm == null) {
            logger.warn("Unknown realm in token");
            return;
        }
        session.getContext().setRealm(realm);
        UriInfo uriInfo = ServletUtil.extractUriInfo(request, keycloakServletPrefix);
        AuthenticationManager.AuthResult authResult = new AppAuthManager.BearerTokenAuthenticator(session)
                .setRealm(realm)
                .setUriInfo(uriInfo)
                .setConnection(clientConnection)
                .setHeaders(headers)
                .authenticate();
        if (authResult == null) {
            logger.debug("Token not valid");
            return;
        }
        ClientModel client = realm.getClientByClientId(token.getIssuedFor());
        if (client == null) {
            logger.debug("Could not find client for authorization");
            return;

        }
        AdminAuth adminAuth = new AdminAuth(realm, authResult.getToken(), authResult.getUser(), client);
//
        KeycloakAuthentication authentication = new KeycloakAuthentication(authResult, session, adminAuth);
        authentication.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }


}
