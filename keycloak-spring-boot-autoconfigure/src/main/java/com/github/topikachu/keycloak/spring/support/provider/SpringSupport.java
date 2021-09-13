package com.github.topikachu.keycloak.spring.support.provider;

import org.keycloak.platform.Platform;
import org.springframework.context.ConfigurableApplicationContext;

abstract public class SpringSupport {
    protected ConfigurableApplicationContext getApplicationContext() {
        return SpringPlatform.class.cast(Platform.getPlatform())
                .getApplicationContext();
    }

    protected void autowireSelf() {
        getApplicationContext().getAutowireCapableBeanFactory().autowireBean(this);
    }

}
