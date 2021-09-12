package com.github.topikachu.keycloak.spring.support.provider;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.auto.service.AutoService;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.Config;
import org.keycloak.ServerStartupError;
import org.keycloak.common.Version;
import org.keycloak.connections.jpa.DefaultJpaConnectionProvider;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.connections.jpa.JpaConnectionProviderFactory;
import org.keycloak.connections.jpa.PersistenceExceptionConverter;
import org.keycloak.connections.jpa.updater.JpaUpdaterProvider;
import org.keycloak.exportimport.ExportImportManager;
import org.keycloak.migration.MigrationModelManager;
import org.keycloak.migration.ModelVersion;
import org.keycloak.models.*;
import org.keycloak.models.dblock.DBLockManager;
import org.keycloak.models.dblock.DBLockProvider;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.provider.ServerInfoAwareProviderFactory;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.managers.ApplianceBootstrap;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.util.JsonSerialization;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import static org.keycloak.models.utils.KeycloakModelUtils.runJobInTransaction;

@Slf4j
@AutoService(JpaConnectionProviderFactory.class)
public class SpringJpaConnectionProviderFactory extends SpringSupportProviderFactory implements JpaConnectionProviderFactory, ServerInfoAwareProviderFactory {

    @NonNull
    @Autowired
    private DataSource dataSource;
    @NonNull
    @Autowired
    private EntityManagerFactory emf;

    @NonNull
    @Autowired
    private PlatformTransactionManager transactionManager;

    @NonNull
    @Autowired
    private ApplicationContext applicationContext;

    private static final String SQL_GET_LATEST_VERSION = "SELECT VERSION FROM %sMIGRATION_MODEL";


    enum MigrationStrategy {
        UPDATE, VALIDATE, MANUAL
    }

    private Map<String, String> operationalInfo;

    private KeycloakSessionFactory factory;


    @Override
    public Connection getConnection() {
        return DataSourceUtils.getConnection(dataSource);
    }

    @Override
    public String getSchema() {
        return config.get("schema");
    }

    @Override
    public JpaConnectionProvider create(KeycloakSession session) {
        DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
        TransactionStatus transactionStatus = transactionManager.getTransaction(definition);
        EntityManager em = EntityManagerFactoryUtils.getTransactionalEntityManager(emf);
        em = PersistenceExceptionConverter.create(session, em);
        session.getTransactionManager().enlist(new SpringJpaKeycloakTransaction(transactionStatus, transactionManager));
        return new DefaultJpaConnectionProvider(em);
    }


    @Override
    @SneakyThrows
    public void postInit(KeycloakSessionFactory factory) {
        this.factory = factory;
        KeycloakSession session = factory.create();
        boolean initSchema;
        try (Connection connection = getConnection()) {
            createOperationalInfo(connection);
            initSchema = createOrUpdateSchema(getSchema(), connection, session);
        } catch (SQLException cause) {
            throw new RuntimeException("Failed to update database.", cause);
        } finally {
            session.close();
        }
        if (initSchema) {
            runJobInTransaction(factory, this::initSchemaOrExport);
        }
    }


    @Override
    public String getId() {
        return "spring";
    }

    @Override
    public Map<String, String> getOperationalInfo() {
        return operationalInfo;
    }


    private MigrationStrategy getMigrationStrategy() {
        String migrationStrategy = config.get("migrationStrategy");
        if (migrationStrategy == null) {
            // Support 'databaseSchema' for backwards compatibility
            migrationStrategy = config.get("databaseSchema");
        }
        if (migrationStrategy != null) {
            return MigrationStrategy.valueOf(migrationStrategy.toUpperCase());
        } else {
            return MigrationStrategy.UPDATE;
        }
    }

    private void initSchemaOrExport(KeycloakSession session) {
        ExportImportManager exportImportManager = new ExportImportManager(session);

        /*
         * Migrate model is executed just in case following providers are "jpa".
         * In Map Storage, there is an assumption that migrateModel is not needed.
         */
        if ((Config.getProvider("realm") == null || "jpa".equals(Config.getProvider("realm"))) &&
                (Config.getProvider("client") == null || "jpa".equals(Config.getProvider("client"))) &&
                (Config.getProvider("clientScope") == null || "jpa".equals(Config.getProvider("clientScope")))) {
            log.debug("Calling migrateModel");
            migrateModel(session);
        }
        DBLockManager dbLockManager = new DBLockManager(session);
        dbLockManager.checkForcedUnlock();
        DBLockProvider dbLock = dbLockManager.getDBLock();
        dbLock.waitForLock(DBLockProvider.Namespace.KEYCLOAK_BOOT);
        try {
            createMasterRealm(exportImportManager);
        } finally {
            dbLock.releaseLock();
        }
        if (exportImportManager.isRunExport()) {
            exportImportManager.runExport();
            ((ConfigurableApplicationContext) applicationContext).close();
        }
    }


    private ExportImportManager createMasterRealm(ExportImportManager exportImportManager) {
        log.debug("bootstrap");
        KeycloakSession session = factory.create();
        try {
            session.getTransactionManager().begin();
            ApplianceBootstrap applianceBootstrap = new ApplianceBootstrap(session);
            boolean createMasterRealm = applianceBootstrap.isNewInstall();
            if (exportImportManager.isRunImport() && exportImportManager.isImportMasterIncluded()) {
                createMasterRealm = false;
            }
            if (createMasterRealm) {
                applianceBootstrap.createMasterRealm();
            }
            session.getTransactionManager().commit();
        } catch (RuntimeException re) {
            if (session.getTransactionManager().isActive()) {
                session.getTransactionManager().rollback();
            }
            throw re;
        } finally {
            session.close();
        }
        if (exportImportManager.isRunImport()) {
            exportImportManager.runImport();
            ((ConfigurableApplicationContext) applicationContext).close();
        } else {
            importRealms();
        }
        importAddUser();
        return exportImportManager;
    }

    private void migrateModel(KeycloakSession session) {
        try {
            MigrationModelManager.migrate(session);
        } catch (Exception e) {
            throw e;
        }
    }

    private void importRealms() {
        String files = System.getProperty("keycloak.import");
        if (files != null) {
            StringTokenizer tokenizer = new StringTokenizer(files, ",");
            while (tokenizer.hasMoreTokens()) {
                String file = tokenizer.nextToken().trim();
                RealmRepresentation rep;
                try {
                    rep = JsonSerialization.readValue(new FileInputStream(file), RealmRepresentation.class);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                importRealm(rep, "file " + file);
            }
        }
    }

    private void importRealm(RealmRepresentation rep, String from) {
        KeycloakSession session = factory.create();
        boolean exists = false;
        try {
            session.getTransactionManager().begin();
            try {
                RealmManager manager = new RealmManager(session);
                if (rep.getId() != null && manager.getRealm(rep.getId()) != null) {
                    ServicesLogger.LOGGER.realmExists(rep.getRealm(), from);
                    exists = true;
                }
                if (manager.getRealmByName(rep.getRealm()) != null) {
                    ServicesLogger.LOGGER.realmExists(rep.getRealm(), from);
                    exists = true;
                }
                if (!exists) {
                    RealmModel realm = manager.importRealm(rep);
                    ServicesLogger.LOGGER.importedRealm(realm.getName(), from);
                }
                session.getTransactionManager().commit();
            } catch (Throwable t) {
                session.getTransactionManager().rollback();
                if (!exists) {
                    ServicesLogger.LOGGER.unableToImportRealm(t, rep.getRealm(), from);
                }
            }
        } finally {
            session.close();
        }
    }

    private void importAddUser() {
        String configDir = System.getProperty("jboss.server.config.dir");
        if (configDir != null) {
            File addUserFile = new File(configDir + File.separator + "keycloak-add-user.json");
            if (addUserFile.isFile()) {
                ServicesLogger.LOGGER.imprtingUsersFrom(addUserFile);
                List<RealmRepresentation> realms;
                try {
                    realms = JsonSerialization
                            .readValue(new FileInputStream(addUserFile), new TypeReference<List<RealmRepresentation>>() {
                            });
                } catch (IOException e) {
                    ServicesLogger.LOGGER.failedToLoadUsers(e);
                    return;
                }
                for (RealmRepresentation realmRep : realms) {
                    for (UserRepresentation userRep : realmRep.getUsers()) {
                        KeycloakSession session = factory.create();
                        try {
                            session.getTransactionManager().begin();
                            RealmModel realm = session.realms().getRealmByName(realmRep.getRealm());
                            if (realm == null) {
                                ServicesLogger.LOGGER.addUserFailedRealmNotFound(userRep.getUsername(), realmRep.getRealm());
                            }
                            UserProvider users = session.users();
                            if (users.getUserByUsername(realm, userRep.getUsername()) != null) {
                                ServicesLogger.LOGGER.notCreatingExistingUser(userRep.getUsername());
                            } else {
                                UserModel user = users.addUser(realm, userRep.getUsername());
                                user.setEnabled(userRep.isEnabled());
                                RepresentationToModel.createCredentials(userRep, session, realm, user, false);
                                RepresentationToModel.createRoleMappings(userRep, user, realm);
                                ServicesLogger.LOGGER.addUserSuccess(userRep.getUsername(), realmRep.getRealm());
                            }
                            session.getTransactionManager().commit();
                        } catch (ModelDuplicateException e) {
                            session.getTransactionManager().rollback();
                            ServicesLogger.LOGGER.addUserFailedUserExists(userRep.getUsername(), realmRep.getRealm());
                        } catch (Throwable t) {
                            session.getTransactionManager().rollback();
                            ServicesLogger.LOGGER.addUserFailed(t, userRep.getUsername(), realmRep.getRealm());
                        } finally {
                            session.close();
                        }
                    }
                }
                if (!addUserFile.delete()) {
                    ServicesLogger.LOGGER.failedToDeleteFile(addUserFile.getAbsolutePath());
                }
            }
        }
    }

    private String getSchema(String schema) {
        return schema == null ? "" : schema + ".";
    }

    private File getDatabaseUpdateFile() {
        String databaseUpdateFile = config.get("migrationExport", "keycloak-database-update.sql");
        return new File(databaseUpdateFile);
    }

    private void createOperationalInfo(Connection connection) {
        try {
            operationalInfo = new LinkedHashMap<>();
            DatabaseMetaData md = connection.getMetaData();
            operationalInfo.put("databaseUrl", md.getURL());
            operationalInfo.put("databaseUser", md.getUserName());
            operationalInfo.put("databaseProduct", md.getDatabaseProductName() + " " + md.getDatabaseProductVersion());
            operationalInfo.put("databaseDriver", md.getDriverName() + " " + md.getDriverVersion());
            log.debug("Database info: {}", operationalInfo.toString());
        } catch (SQLException e) {
            log.warn("Unable to prepare operational info due database exception: " + e.getMessage());
        }
    }

    private boolean createOrUpdateSchema(String schema, Connection connection, KeycloakSession session) {
        MigrationStrategy strategy = getMigrationStrategy();
        boolean initializeEmpty = config.getBoolean("initializeEmpty", true);
        File databaseUpdateFile = getDatabaseUpdateFile();
        String version = null;
        try {
            try (Statement statement = connection.createStatement()) {
                try (ResultSet rs = statement.executeQuery(String.format(SQL_GET_LATEST_VERSION, getSchema(schema)))) {
                    if (rs.next()) {
                        version = rs.getString(1);
                    }
                }
            }
        } catch (SQLException ignore) {
            // migration model probably does not exist so we assume the database is empty
        }
        JpaUpdaterProvider updater = session.getProvider(JpaUpdaterProvider.class);
        boolean requiresMigration = version == null || !version.equals(new ModelVersion(Version.VERSION_KEYCLOAK).toString());
        JpaUpdaterProvider.Status status = updater.validate(connection, schema);
        if (status == JpaUpdaterProvider.Status.VALID) {
            log.debug("Database is up-to-date");
        } else if (status == JpaUpdaterProvider.Status.EMPTY) {
            if (initializeEmpty) {
                update(connection, schema, session, updater);
            } else {
                switch (strategy) {
                    case UPDATE:
                        update(connection, schema, session, updater);
                        break;
                    case MANUAL:
                        export(connection, schema, databaseUpdateFile, session, updater);
                        throw new ServerStartupError("Database not initialized, please initialize database with " + databaseUpdateFile.getAbsolutePath(), false);
                    case VALIDATE:
                        throw new ServerStartupError("Database not initialized, please enable database initialization", false);
                }
            }
        } else {
            switch (strategy) {
                case UPDATE:
                    update(connection, schema, session, updater);
                    break;
                case MANUAL:
                    export(connection, schema, databaseUpdateFile, session, updater);
                    throw new ServerStartupError("Database not up-to-date, please migrate database with " + databaseUpdateFile.getAbsolutePath(), false);
                case VALIDATE:
                    throw new ServerStartupError("Database not up-to-date, please enable database migration", false);
            }
        }
        return requiresMigration;
    }

    private void update(Connection connection, String schema, KeycloakSession session, JpaUpdaterProvider updater) {
        DBLockManager dbLockManager = new DBLockManager(session);
        DBLockProvider dbLock2 = dbLockManager.getDBLock();
        dbLock2.waitForLock(DBLockProvider.Namespace.DATABASE);
        try {
            updater.update(connection, schema);
        } finally {
            dbLock2.releaseLock();
        }
    }

    private void export(Connection connection, String schema, File databaseUpdateFile, KeycloakSession session,
                        JpaUpdaterProvider updater) {
        DBLockManager dbLockManager = new DBLockManager(session);
        DBLockProvider dbLock2 = dbLockManager.getDBLock();
        dbLock2.waitForLock(DBLockProvider.Namespace.DATABASE);
        try {
            updater.export(connection, schema, databaseUpdateFile);
        } finally {
            dbLock2.releaseLock();
        }
    }
}
