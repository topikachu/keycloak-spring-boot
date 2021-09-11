package com.github.topikachu.keycloak.spring.support.provider;

import lombok.NonNull;
import org.jboss.logging.Logger;
import org.keycloak.connections.jpa.JpaKeycloakTransaction;
import org.keycloak.connections.jpa.PersistenceExceptionConverter;
import org.keycloak.models.KeycloakTransaction;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.persistence.PersistenceException;

public class SpringJpaKeycloakTransaction implements KeycloakTransaction {
    private static final Logger logger = Logger.getLogger(JpaKeycloakTransaction.class);


    @NonNull
    protected PlatformTransactionManager transactionManager;
    @NonNull
    private TransactionStatus transactionStatus;

    public SpringJpaKeycloakTransaction(@NonNull TransactionStatus transactionStatus, @NonNull PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
        this.transactionStatus = transactionStatus;
    }


    @Override
    public void begin() {
        //since we already the platform transaction before create this object. We can't start a new one again.
        throw new IllegalStateException("Can't begin a new transaction again");
    }

    @Override
    public void commit() {
        try {
            logger.trace("Committing transaction");
            transactionManager.commit(transactionStatus);
        } catch (PersistenceException var2) {
            throw PersistenceExceptionConverter.convert((Throwable) (var2.getCause() != null ? var2.getCause() : var2));
        }
    }

    @Override
    public void rollback() {
        logger.trace("Rollback transaction");
        transactionManager.rollback(transactionStatus);
    }

    @Override
    public void setRollbackOnly() {
        transactionStatus.setRollbackOnly();
    }

    @Override
    public boolean getRollbackOnly() {
        return transactionStatus.isRollbackOnly();
    }

    @Override
    public boolean isActive() {
        return TransactionSynchronizationManager.isActualTransactionActive();
    }


}
