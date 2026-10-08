package com.synapsecore.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.synapsecore.audit.RequestTraceContext;
import com.synapsecore.domain.entity.OperationalDispatchStatus;
import com.synapsecore.domain.entity.OperationalDispatchWorkItem;
import com.synapsecore.domain.repository.OperationalDispatchWorkItemRepository;
import com.synapsecore.domain.service.DashboardService;
import com.synapsecore.observability.OperationalMetricsService;
import com.synapsecore.observability.ScheduledTaskExecutionDiagnostics;
import com.synapsecore.realtime.RealtimeService;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class OperationalDispatchTerminalWritePostgresProof {

    @Autowired
    private OperationalDispatchWorkItemRepository repository;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private OperationalMetricsService metrics;

    @Test
    void failedPostgresCompletionLeavesClaimForLeaseRecoveryAfterBroadcast() {
        String tenantCode = "DISPATCH-TERMINAL-" + UUID.randomUUID();
        OperationalDispatchWorkItem item = repository.saveAndFlush(OperationalDispatchWorkItem.builder()
            .tenantCode(tenantCode)
            .updateType(OperationalUpdateType.INTEGRATION_STATE)
            .source("postgres-failure-proof")
            .requestId(UUID.randomUUID().toString())
            .status(OperationalDispatchStatus.PENDING)
            .occurredAt(Instant.now())
            .build());
        RecordingRealtimeService realtime = new RecordingRealtimeService();
        DefaultListableBeanFactory realtimeBeans = new DefaultListableBeanFactory();
        realtimeBeans.registerSingleton("realtime", realtime);
        OperationalDispatchQueueService service = new OperationalDispatchQueueService(
            repository,
            new DefaultListableBeanFactory().getBeanProvider(DashboardService.class),
            realtimeBeans.getBeanProvider(RealtimeService.class),
            new RequestTraceContext(),
            metrics,
            null,
            new ScheduledTaskExecutionDiagnostics(null)
        );

        try {
            jdbc.execute("""
                create function test_dispatch_completion_failure() returns trigger as $$
                begin
                    if new.status = 'COMPLETED' and new.tenant_code like 'DISPATCH-TERMINAL-%' then
                        raise exception 'test terminal completion blocked';
                    end if;
                    return new;
                end;
                $$ language plpgsql
                """);
            jdbc.execute("""
                create trigger test_dispatch_completion_failure
                before update on operational_dispatch_work_items
                for each row execute function test_dispatch_completion_failure()
                """);

            assertThatThrownBy(service::processPendingWork)
                .isInstanceOf(RuntimeException.class)
                .hasRootCauseInstanceOf(org.postgresql.util.PSQLException.class);
            assertThat(realtime.integrationBroadcasts).isEqualTo(1);
            OperationalDispatchWorkItem unfinished = repository.findById(item.getId()).orElseThrow();
            assertThat(unfinished.getStatus()).isEqualTo(OperationalDispatchStatus.PROCESSING);
            assertThat(unfinished.getAttemptCount()).isEqualTo(1);
            assertThat(unfinished.getProcessedAt()).isNull();
            assertThat(unfinished.getLastError()).isNull();
            assertThat(service.isDraining()).isFalse();

            jdbc.execute("drop trigger test_dispatch_completion_failure on operational_dispatch_work_items");
            jdbc.update("update operational_dispatch_work_items set updated_at = ? where id = ?",
                Timestamp.from(Instant.now().minusSeconds(600)), item.getId());
            assertThat(service.processPendingWork()).isEqualTo(1);
            assertThat(realtime.integrationBroadcasts).isEqualTo(2);
            OperationalDispatchWorkItem completed = repository.findById(item.getId()).orElseThrow();
            assertThat(completed.getStatus()).isEqualTo(OperationalDispatchStatus.COMPLETED);
            assertThat(completed.getAttemptCount()).isEqualTo(2);
            assertThat(completed.getProcessedAt()).isNotNull();
            assertThat(completed.getLastError()).isNull();
        } finally {
            jdbc.execute("drop trigger if exists test_dispatch_completion_failure on operational_dispatch_work_items");
            jdbc.execute("drop function if exists test_dispatch_completion_failure()");
            repository.deleteById(item.getId());
        }
    }

    private static final class RecordingRealtimeService extends RealtimeService {
        private int integrationBroadcasts;

        private RecordingRealtimeService() {
            super(null, null, null, null, null);
        }

        @Override
        public void broadcastIntegrationUpdates(String tenantCode) {
            integrationBroadcasts++;
        }
    }
}
