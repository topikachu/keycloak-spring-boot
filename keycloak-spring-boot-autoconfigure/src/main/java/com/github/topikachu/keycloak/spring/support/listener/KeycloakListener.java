package com.github.topikachu.keycloak.spring.support.listener;

import com.github.topikachu.keycloak.spring.support.provider.SpringPlatform;
import org.keycloak.common.Profile;
import org.keycloak.platform.Platform;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

public class KeycloakListener implements ApplicationListener<ApplicationPreparedEvent> {
    @Override
    public void onApplicationEvent(ApplicationPreparedEvent event) {
        ConfigurableApplicationContext context = event.getApplicationContext();
        SpringPlatform.class.cast(Platform.getPlatform())
                .startWithSpring(context);
        ConfigurableEnvironment environment = context.getEnvironment();
        Profile profile=new Profile(feature-> environment.getProperty(feature));
        Profile.setInstance(profile);
    }
}
