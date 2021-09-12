package com.github.topikachu.keycloak.spring.support.provider;

import com.google.auto.service.AutoService;
import org.infinispan.manager.EmbeddedCacheManager;
import org.keycloak.Config;
import org.keycloak.cluster.ManagedCacheManagerProvider;
import org.springframework.beans.factory.annotation.Autowired;

@AutoService(ManagedCacheManagerProvider.class)
public class SpringCacheManagerProvider extends SpringSupport implements ManagedCacheManagerProvider {


    @Autowired
    private EmbeddedCacheManager managedCacheManager;


    @Override
    public Object getCacheManager(Config.Scope config) {
        if (managedCacheManager == null) {
            synchronized (this) {
                if (managedCacheManager == null) {
                    autowireSelf();
                }
            }
        }
        return managedCacheManager;

    }


}
