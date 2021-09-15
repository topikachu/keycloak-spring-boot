package com.github.topikachu.keycloak.spring.support.event;

import lombok.Getter;
import org.keycloak.models.KeycloakSessionFactory;
import org.springframework.context.ApplicationEvent;

@Getter
public class KeycloakReadyEvent extends ApplicationEvent {
    private KeycloakSessionFactory keycloakSessionFactory;

    public KeycloakReadyEvent(Object source, KeycloakSessionFactory keycloakSessionFactory) {
        super(source);
        this.keycloakSessionFactory = keycloakSessionFactory;
    }
}
