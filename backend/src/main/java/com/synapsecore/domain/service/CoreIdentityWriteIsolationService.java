package com.synapsecore.domain.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@Slf4j
public class CoreIdentityWriteIsolationService {

    private final IdentitySequenceMigrationService identitySequenceMigrationService;
    private final TransactionTemplate requiresNewTransactionTemplate;

    public CoreIdentityWriteIsolationService(PlatformTransactionManager transactionManager,
                                             IdentitySequenceMigrationService identitySequenceMigrationService) {
        this.identitySequenceMigrationService = identitySequenceMigrationService;
        this.requiresNewTransactionTemplate = new TransactionTemplate(transactionManager);
        this.requiresNewTransactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public void persistWithSequenceRepair(String writeDescription, Runnable writeAction) {
        try {
            executeRequiresNew(writeAction);
        } catch (DataIntegrityViolationException exception) {
            log.warn("{} conflicted; synchronizing core identity sequences and retrying once.", writeDescription);
            synchronizeCoreIdentitySequencesSafely();
            executeRequiresNew(writeAction);
        }
    }

    private void executeRequiresNew(Runnable writeAction) {
        requiresNewTransactionTemplate.executeWithoutResult(status -> writeAction.run());
    }

    private void synchronizeCoreIdentitySequencesSafely() {
        requiresNewTransactionTemplate.executeWithoutResult(status ->
            identitySequenceMigrationService.synchronizeCoreIdentitySequences()
        );
    }
}
