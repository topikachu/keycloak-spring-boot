package com.github.topikachu.keycloak.spring.support.provider;

import com.google.auto.service.AutoService;
import org.keycloak.executors.ExecutorsProvider;
import org.keycloak.executors.ExecutorsProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.ExecutorService;

@AutoService(ExecutorsProviderFactory.class)
public class SpringExecutorsProviderFactory extends SpringSupportProviderFactory implements ExecutorsProviderFactory {

    @Autowired
    ExecutorService keycloakExecutorService;

    @Override
    public ExecutorsProvider create(KeycloakSession session) {
        return new ExecutorsProvider(){

            @Override
            public void close() {
            }

            @Override
            public ExecutorService getExecutor(String taskType) {
                return keycloakExecutorService;
            }
        };
    }



    @Override
    public String getId() {
        return "spring";
    }
}
