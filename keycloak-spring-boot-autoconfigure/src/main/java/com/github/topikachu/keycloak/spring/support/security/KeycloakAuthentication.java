package com.github.topikachu.keycloak.spring.support.security;

import lombok.Getter;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.resources.admin.AdminAuth;
import org.springframework.security.authentication.AbstractAuthenticationToken;

public class KeycloakAuthentication extends AbstractAuthenticationToken {


    @Getter
    private KeycloakSession session;
    @Getter
    private AuthenticationManager.AuthResult authResult;

    @Getter
    private final AdminAuth adminAuth;

    public KeycloakAuthentication(AuthenticationManager.AuthResult authResult, KeycloakSession session, AdminAuth adminAuth) {
        super(null);
        this.authResult = authResult;
        this.session = session;
        this.adminAuth = adminAuth;
        ;
    }


    @Override
    public String getName() {
        return authResult.getUser().getUsername();
    }

    @Override
    public UserModel getDetails() {
        return authResult.getUser();
    }

    @Override
    public AccessToken getCredentials() {
        return authResult.getToken();
    }

    @Override
    public UserModel getPrincipal() {
        return getDetails();
    }
}
