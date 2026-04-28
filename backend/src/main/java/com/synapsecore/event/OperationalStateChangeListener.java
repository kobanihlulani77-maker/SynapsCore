package com.synapsecore.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class OperationalStateChangeListener {

    private final OperationalDispatchQueueService operationalDispatchQueueService;

    @Async("operationalDispatchExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onOperationalStateChanged(OperationalStateChangedEvent event) {
        int processedCount = operationalDispatchQueueService.processPendingWork();
        log.debug("Operational dispatch queue accepted {} via {} request {} and drained {} item(s)",
            event.updateType(), event.source(), event.requestId(), processedCount);
    }
}
