package com.github.topikachu.keycloak.spring.support.provider;

import com.google.auto.service.AutoService;
import lombok.Getter;
import org.keycloak.platform.PlatformProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.system.ApplicationTemp;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.SmartApplicationListener;

import java.io.File;

@AutoService(PlatformProvider.class)
public class SpringPlatform extends SpringSupport implements PlatformProvider, SmartApplicationListener {


    @Getter
    private ConfigurableApplicationContext applicationContext;

    public void install(ConfigurableApplicationContext context) {
        this.applicationContext = context;
        context.addApplicationListener(this);
    }

    ApplicationTemp temp = new ApplicationTemp();
    @Getter
    private Runnable startup;
    @Getter
    private Runnable shutdown;

    @Override
    public void onStartup(Runnable startup) {
        this.startup = startup;
    }

    @Override
    public void onShutdown(Runnable shutdown) {
        this.shutdown = shutdown;
    }

    @Override
    public void exit(Throwable cause) {
        SpringApplication.exit(applicationContext, () -> -1);
    }

    @Override
    public File getTmpDirectory() {
        return temp.getDir();
    }

    @Override
    public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
        return eventType.isAssignableFrom(ApplicationReadyEvent.class) || eventType.isAssignableFrom(ContextClosedEvent.class);
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ApplicationReadyEvent) {
            startup.run();
        } else if (event instanceof ContextClosedEvent) {
            shutdown.run();
        }
    }
}
