package com.github.topikachu.keycloak.spring.support.security;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class KeycloakSecurityObject {
    private String realm;
    private String resource;

    public static class KeycloakSecurityObjectBuilderBean {
        public KeycloakSecurityObject build(String realm, String resource) {
            return KeycloakSecurityObject.builder()
                    .realm(realm)
                    .resource(resource)
                    .build();
        }
    }
}
