package com.example.keycloaksampleapp;

import com.github.topikachu.keycloak.spring.support.config.KeycloakCustomProperties;
import com.github.topikachu.keycloak.spring.support.security.KeycloakAdminCredentialFilter;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@Configuration
public class SecurityConfig {


    @Bean
    public SecurityFilterChain actuateSecurityFilterChain(HttpSecurity http) throws Exception {
        http.requestMatcher(EndpointRequest.toAnyEndpoint())
                .authorizeRequests()
                .anyRequest().permitAll();

        return http.build();

    }


    @Bean
    public SecurityFilterChain keyCloakSecurityFilterChain(HttpSecurity http, KeycloakCustomProperties customProperties) throws Exception {
        return http
                .formLogin().disable()
                .sessionManagement()
                .sessionCreationPolicy(STATELESS)
                .and()
                .requestMatchers()
                .antMatchers("/admin/**")
                .and()
                .authorizeRequests()
                .antMatchers(GET, "/admin/realms/{realm}/{resource}").access("hasPermission(@securityObjectBuilder.build(#realm,#resource),'canList')")
                .and()
                .addFilterAt(new KeycloakAdminCredentialFilter(customProperties.getServer().getKeycloakPath()),
                        UsernamePasswordAuthenticationFilter.class)
                .build();
    }


}
