package com.github.topikachu.keycloak.spring.support.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;

@Getter
@Setter
@ConfigurationProperties(prefix = "keycloak")
public class KeycloakProperties extends HashMap<String, Object> {

}
