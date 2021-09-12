package com.github.topikachu.keycloak.spring.support.security;

import lombok.Getter;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.springframework.security.authentication.AbstractAuthenticationToken;

public class KeycloakAuthentication extends AbstractAuthenticationToken {

    @Getter
    private  AdminPermissionEvaluator evaluator;
    @Getter
    private  KeycloakSession session;
    @Getter
    private  AuthenticationManager.AuthResult authResult;

    /**
     * Creates a token with the supplied array of authResult.
     *
     * @param authResult the collection of <tt>GrantedAuthority</tt>s for the principal
     *                    represented by this authentication object.
     * @param evaluator
     */
    public KeycloakAuthentication(AuthenticationManager.AuthResult authResult, KeycloakSession session, AdminPermissionEvaluator evaluator) {
        super(null);
        this.authResult =authResult;
        this.session=session;
        this.evaluator=evaluator;
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
