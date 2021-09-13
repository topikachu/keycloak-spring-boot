package com.github.topikachu.keycloak.spring.support.security;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.permissions.AdminPermissions;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;

import java.io.Serializable;

public class KeycloakPermissionEvaluator implements PermissionEvaluator {

    @Override
    public boolean hasPermission(Authentication authentication, Object target, Object permission) {
        if (authentication instanceof KeycloakAuthentication && target instanceof KeycloakSecurityObject) {
            KeycloakAuthentication keycloakAuthentication = (KeycloakAuthentication) authentication;
            KeycloakSession session = keycloakAuthentication.getSession();
            KeycloakSecurityObject keycloakSecurityObject = (KeycloakSecurityObject) target;
            RealmManager realmManager = new RealmManager(session);
            RealmModel realm = realmManager.getRealmByName(keycloakSecurityObject.getRealm());
            AdminPermissionEvaluator evaluator = AdminPermissions.evaluator(session, realm, keycloakAuthentication.getAdminAuth());
            ExpressionParser parser = new SpelExpressionParser();
            String resource = keycloakSecurityObject.getResource();
            String expressionString = KeycloakPermission.from(resource).toPermissionExpression(String.valueOf(permission));
            Expression expression = parser.parseExpression(expressionString);
            return expression.getValue(evaluator, Boolean.class);
        }
        return false;
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        return false;
    }
}
