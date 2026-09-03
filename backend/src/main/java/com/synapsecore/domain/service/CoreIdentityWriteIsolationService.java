package com.synapsecore.domain.service;

import lombok.extern.slf4j.Slf4j;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@Slf4j
public class CoreIdentityWriteIsolationService {

    private final IdentitySequenceMigrationService identitySequenceMigrationService;
    private final EntityManager entityManager;
    private final TransactionTemplate requiresNewTransactionTemplate;

    @Autowired
    public CoreIdentityWriteIsolationService(PlatformTransactionManager transactionManager,
                                             IdentitySequenceMigrationService identitySequenceMigrationService,
                                             EntityManager entityManager) {
        this.identitySequenceMigrationService = identitySequenceMigrationService;
        this.entityManager = entityManager;
        this.requiresNewTransactionTemplate = new TransactionTemplate(transactionManager);
        this.requiresNewTransactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public CoreIdentityWriteIsolationService(PlatformTransactionManager transactionManager,
                                             IdentitySequenceMigrationService identitySequenceMigrationService) {
        this(transactionManager, identitySequenceMigrationService, null);
    }

    public void persistWithSequenceRepair(String writeDescription, Runnable writeAction) {
        if (entityManager != null && TransactionSynchronizationManager.isActualTransactionActive()) {
            // Catalog writes must keep events, dispatch work, and audit rows in the same commit.
            identitySequenceMigrationService.acquireCoreIdentityWriteLock();
            writeAction.run();
            entityManager.flush();
            return;
        }
        try {
            executeRequiresNew(writeAction);
        } catch (DataIntegrityViolationException exception) {
            log.warn("{} conflicted; synchronizing core identity sequences and retrying once.", writeDescription);
            synchronizeCoreIdentitySequencesSafely();
            executeRequiresNew(writeAction);
        }
    }

    private void executeRequiresNew(Runnable writeAction) {
        requiresNewTransactionTemplate.executeWithoutResult(status -> {
            identitySequenceMigrationService.acquireCoreIdentityWriteLock();
            writeAction.run();
        });
    }

    private void synchronizeCoreIdentitySequencesSafely() {
        requiresNewTransactionTemplate.executeWithoutResult(status ->
            identitySequenceMigrationService.synchronizeCoreIdentitySequences()
        );
    }
}
