package com.github.topikachu.keycloak.spring.support.healthh;

import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resources.KeycloakApplication;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;


public class KeycloakHealthIndicator implements HealthIndicator {


    @Override
    public Health health() {
        KeycloakSessionFactory sessionFactory = KeycloakApplication.getSessionFactory();
        if (sessionFactory != null) {
            return Health.up().build();
        } else {
            return Health.down().build();
        }
    }


}
