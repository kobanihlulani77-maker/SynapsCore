package com.synapsecore.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class OperationalStateChangeListenerTest {

    @Test
    void operationalStateChangeTriggersQueuedDrainAttempt() {
        RecordingQueueService queueService = new RecordingQueueService();
        OperationalStateChangeListener listener = new OperationalStateChangeListener(queueService);

        listener.onOperationalStateChanged(new OperationalStateChangedEvent(
            OperationalUpdateType.ORDER_FLOW,
            "STARTER-OPS",
            "order-api",
            "req-123",
            Instant.now()
        ));

        assertThat(queueService.processCalls).isEqualTo(1);
    }

    private static final class RecordingQueueService extends OperationalDispatchQueueService {

        private int processCalls;

        private RecordingQueueService() {
            super(null, null, null, null, null);
        }

        @Override
        public int processPendingWork() {
            processCalls++;
            return 1;
        }
    }
}
