package com.example.keycloaksampleapp;

import com.github.topikachu.keycloak.spring.support.config.KeycloakCustomProperties;
import com.github.topikachu.keycloak.spring.support.security.KeycloakAdminCredentialFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    @Autowired
    KeycloakCustomProperties customProperties;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
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
                        UsernamePasswordAuthenticationFilter.class);

    }
}
