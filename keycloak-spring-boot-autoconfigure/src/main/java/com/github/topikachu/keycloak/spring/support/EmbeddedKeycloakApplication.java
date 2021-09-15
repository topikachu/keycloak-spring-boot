package com.github.topikachu.keycloak.spring.support;


import com.github.topikachu.keycloak.spring.support.config.KeycloakCustomProperties;
import com.github.topikachu.keycloak.spring.support.config.KeycloakProperties;
import com.github.topikachu.keycloak.spring.support.event.KeycloakReadyEvent;
import com.github.topikachu.keycloak.spring.support.provider.SpringBootConfigProvider;
import com.github.topikachu.keycloak.spring.support.provider.SpringPlatform;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.Config;
import org.keycloak.exportimport.ExportImportConfig;
import org.keycloak.exportimport.ExportImportManager;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakTransactionManager;
import org.keycloak.platform.Platform;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.managers.ApplianceBootstrap;
import org.keycloak.services.resources.KeycloakApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

import javax.ws.rs.ApplicationPath;
import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Slf4j
@ApplicationPath("/")
public class EmbeddedKeycloakApplication extends KeycloakApplication {

    @NonNull
    @Autowired
    private KeycloakProperties keycloakProperties;
    @NonNull
    @Autowired
    private KeycloakCustomProperties customProperties;


    @Override
    protected ExportImportManager bootstrap() {
        ExportImportManager exportImportManager = super.bootstrap();
        tryCreateMasterRealmAdminUser();
        tryImportRealm();
        return exportImportManager;
    }

    protected void loadConfig() {
        ConfigurableApplicationContext applicationContext = getApplicationContext();
        applicationContext.getAutowireCapableBeanFactory().autowireBean(this);
        SpringBootConfigProvider springBootConfigProvider = new SpringBootConfigProvider(keycloakProperties);
        Config.init(springBootConfigProvider);
    }

    private ConfigurableApplicationContext getApplicationContext() {
        return SpringPlatform.class.cast(Platform.getPlatform()).getApplicationContext();
    }

    protected void tryCreateMasterRealmAdminUser() {
        if (!customProperties.getAdminUser().isCreateAdminUserEnabled()) {
            log.warn("Skipping creation of keycloak master adminUser.");
            return;
        }
        KeycloakCustomProperties.AdminUser adminUser = customProperties.getAdminUser();
        String username = adminUser.getUsername();
        if (!(StringUtils.hasLength(username) || StringUtils.hasText(username))) {
            return;
        }
        KeycloakSession session = getSessionFactory().create();
        KeycloakTransactionManager transaction = session.getTransactionManager();
        try {
            transaction.begin();
            boolean randomPassword = false;
            String password = adminUser.getPassword();
            if (StringUtils.isEmpty(adminUser.getPassword())) {
                password = UUID.randomUUID().toString();
                randomPassword = true;
            }
            new ApplianceBootstrap(session).createMasterRealmUser(username, password);
            if (randomPassword) {
                log.info("Generated admin password: {}", password);
            }
            ServicesLogger.LOGGER.addUserSuccess(username, Config.getAdminRealm());
            transaction.commit();
        } catch (IllegalStateException e) {
            transaction.rollback();
            ServicesLogger.LOGGER.addUserFailedUserExists(username, Config.getAdminRealm());
        } catch (Throwable t) {
            transaction.rollback();
            ServicesLogger.LOGGER.addUserFailed(t, username, Config.getAdminRealm());
        } finally {
            session.close();
        }
    }

    protected void tryImportRealm() {
        KeycloakCustomProperties.Migration imex = customProperties.getMigration();
        Resource importLocation = imex.getImportLocation();
        if (!importLocation.exists()) {
            log.info("Could not find keycloak import file {}", importLocation);
            return;
        }
        File file;
        try {
            file = importLocation.getFile();
        } catch (IOException e) {
            log.error("Could not read keycloak import file {}", importLocation, e);
            return;
        }
        log.info("Starting Keycloak realm configuration import from location: {}", importLocation);
        KeycloakSession session = getSessionFactory().create();
        ExportImportConfig.setAction("import");
        ExportImportConfig.setProvider(imex.getImportProvider());
        ExportImportConfig.setFile(file.getAbsolutePath());
        ExportImportManager manager = new ExportImportManager(session);
        manager.runImport();
        session.close();
        log.info("Keycloak realm configuration import finished.");
    }

    @Override
    protected void startup() {
        super.startup();
        KeycloakSessionFactory sessionFactory = getSessionFactory();
        if (sessionFactory != null) {
            sessionFactory.register(event -> getApplicationContext().publishEvent(event));
            getApplicationContext().publishEvent(new KeycloakReadyEvent(this, sessionFactory));
        }

    }
}
