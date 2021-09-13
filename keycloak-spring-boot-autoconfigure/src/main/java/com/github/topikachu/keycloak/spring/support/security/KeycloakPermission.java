package com.github.topikachu.keycloak.spring.support.security;

import java.util.Arrays;
import java.util.Objects;

public enum KeycloakPermission {


    CLIENTS;


    static KeycloakPermission from(String name) {
        return Arrays.stream(values())
                .filter(r -> Objects.equals(r.name().toLowerCase(), name.toLowerCase()))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException("no such resource: " + name)
                );
    }

    public String toPermissionExpression(String permission) {
        return name().toLowerCase() + "()." + permission + "()";
    }
}
