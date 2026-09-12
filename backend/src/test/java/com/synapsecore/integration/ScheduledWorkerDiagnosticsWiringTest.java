package com.synapsecore.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.synapsecore.observability.ScheduledTaskExecutionDiagnostics;
import java.util.function.IntSupplier;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ScheduledWorkerDiagnosticsWiringTest {

    @Test
    void replayAutomationIdentifiesItsScheduledWork() {
        RecordingDiagnostics diagnostics = new RecordingDiagnostics(3);
        IntegrationReplayAutomationService service = new IntegrationReplayAutomationService(null, diagnostics);
        ReflectionTestUtils.setField(service, "automationEnabled", true);

        service.processAutomatedReplay();

        assertThat(diagnostics.taskName).isEqualTo("integration-replay-automation");
    }

    @Test
    void scheduledPullIdentifiesItsScheduledWork() {
        RecordingDiagnostics diagnostics = new RecordingDiagnostics(2);
        IntegrationScheduledPullWorkerService service = new IntegrationScheduledPullWorkerService(
            null,
            null,
            null,
            null,
            null,
            null,
            diagnostics
        );
        ReflectionTestUtils.setField(service, "workerEnabled", true);

        service.processScheduledPulls();

        assertThat(diagnostics.taskName).isEqualTo("integration-scheduled-pull");
    }

    private static final class RecordingDiagnostics extends ScheduledTaskExecutionDiagnostics {
        private final int result;
        private String taskName;

        private RecordingDiagnostics(int result) {
            super(null);
            this.result = result;
        }

        @Override
        public int observe(String taskName, IntSupplier work) {
            this.taskName = taskName;
            return result;
        }
    }
}
