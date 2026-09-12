package com.synapsecore.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class ScheduledTaskExecutionDiagnosticsTest {

    @Test
    void recordsTaskIdentityDurationResultAndPoolState(CapturedOutput output) throws Exception {
        try (HikariDataSource dataSource = new HikariDataSource()) {
            dataSource.setJdbcUrl("jdbc:h2:mem:scheduled-task-diagnostics;DB_CLOSE_DELAY=-1");
            dataSource.setMaximumPoolSize(4);
            dataSource.setMinimumIdle(0);
            try (Connection heldConnection = dataSource.getConnection()) {
                int processed = new ScheduledTaskExecutionDiagnostics(dataSource)
                    .observe("integration-replay-automation", () -> 4);

                assertThat(processed).isEqualTo(4);
                assertThat(output).contains(
                    "task=integration-replay-automation stage=START",
                    "hikariTotal=1 hikariActive=1 hikariIdle=0 hikariWaiting=0",
                    "task=integration-replay-automation stage=COMPLETE",
                    "processed=4"
                );
                assertThat(dataSource.getHikariPoolMXBean().getTotalConnections()).isEqualTo(1);
                assertThat(dataSource.getHikariPoolMXBean().getActiveConnections()).isEqualTo(1);
            }
        }
    }

    @Test
    void recordsFailureWithoutSwallowingIt(CapturedOutput output) {
        ScheduledTaskExecutionDiagnostics diagnostics = new ScheduledTaskExecutionDiagnostics(null);

        assertThatThrownBy(() -> diagnostics.observe("integration-scheduled-pull", () -> {
            throw new IllegalStateException("bounded failure");
        })).isInstanceOf(IllegalStateException.class).hasMessage("bounded failure");

        assertThat(output).contains(
            "task=integration-scheduled-pull stage=START",
            "task=integration-scheduled-pull stage=FAILED",
            "failureType=IllegalStateException",
            "hikariTotal=-1 hikariActive=-1 hikariIdle=-1 hikariWaiting=-1"
        );
    }
}
