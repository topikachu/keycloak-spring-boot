package com.github.topikachu.keycloak.spring.support.provider;

import com.google.auto.service.AutoService;
import org.jboss.resteasy.core.ResteasyContext;

@AutoService(org.keycloak.common.util.ResteasyProvider.class)
public class ResteasyProvider implements org.keycloak.common.util.ResteasyProvider {

    @Override
    public <R> R getContextData(Class<R> type) {
        return ResteasyContext.getContextData(type);
    }

    @Override
    public void pushDefaultContextObject(Class type, Object instance) {
        ResteasyContext.getContextData(org.jboss.resteasy.spi.Dispatcher.class).getDefaultContextObjects()
                .put(type, instance);
    }

    @Override
    public void pushContext(Class type, Object instance) {
        ResteasyContext.pushContext(type, instance);
    }

    @Override
    public void clearContextData() {
        ResteasyContext.clearContextData();
    }

}
