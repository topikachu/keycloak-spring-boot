package com.github.topikachu.keycloak.spring.support.security;

import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.permissions.AdminPermissions;
import org.keycloak.services.resources.admin.permissions.ClientPermissionEvaluator;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;

import java.io.Serializable;

public class KeycloakPermissionEvaluator implements PermissionEvaluator {

    @Override
    public boolean hasPermission(Authentication authentication, Object target, Object permission) {
        if (authentication instanceof KeycloakAuthentication){
            KeycloakAuthentication keycloakAuthentication= (KeycloakAuthentication) authentication;
            AdminPermissionEvaluator evaluator = keycloakAuthentication.getEvaluator();
            ExpressionParser parser = new SpelExpressionParser();
            Expression expression = parser.parseExpression("");
            if ("clients".equals(target)){
                ClientPermissionEvaluator clientsEvaluator = evaluator.clients();
                if ("list".equals(permission)){
                    return clientsEvaluator.canList();
                }
            }
        }
        return false;
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
//        if (authentication instanceof KeycloakAuthentication){
//            KeycloakAuthentication keycloakAuthentication= (KeycloakAuthentication) authentication;
//            keycloakAuthentication.getEvaluator().clients().canList();
//        }
        return false;
    }
}
