package com.github.topikachu.keycloak.spring.support.provider;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSessionFactory;

abstract public class SpringSupportProviderFactory extends SpringSupport {
    protected Config.Scope config;

    public void init(Config.Scope config) {
        this.config = config;
        autowireSelf();
    }

    public void postInit(KeycloakSessionFactory factory) {
    }

    public void close() {
    }
}
