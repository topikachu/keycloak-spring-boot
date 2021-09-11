package com.github.topikachu.keycloak.spring.support.processor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Slf4j
public class KeycloakEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private final YamlPropertySourceLoader loader = new YamlPropertySourceLoader();

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment,
                                       SpringApplication application) {
        Resource path = new ClassPathResource("keycloak-defaults.yml");
        loadYaml(path)
                .forEach(propertySource -> environment.getPropertySources().addLast(propertySource));
    }


    private List<PropertySource<?>> loadYaml(Resource path) {
        try {
            return this.loader.load("keycloak-defaults", path);
        } catch (IOException e) {
            log.warn("Can't load keycloak-defaults.yml from class path: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

}
