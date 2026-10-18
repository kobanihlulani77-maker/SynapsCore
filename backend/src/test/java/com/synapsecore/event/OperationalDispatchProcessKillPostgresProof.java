package com.synapsecore.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import com.synapsecore.audit.RequestTraceContext;
import com.synapsecore.domain.entity.OperationalDispatchStatus;
import com.synapsecore.domain.entity.OperationalDispatchWorkItem;
import com.synapsecore.domain.repository.OperationalDispatchWorkItemRepository;
import com.synapsecore.domain.service.DashboardService;
import com.synapsecore.observability.OperationalMetricsService;
import com.synapsecore.observability.ScheduledTaskExecutionDiagnostics;
import com.synapsecore.realtime.RealtimeService;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@EnabledIfSystemProperty(named = "synapsecore.process-kill-proof", matches = "true")
class OperationalDispatchProcessKillPostgresProof {

    @Autowired
    private OperationalDispatchWorkItemRepository repository;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private OperationalMetricsService metrics;

    @Test
    void claimAndBlock() {
        String tenantCode = tenantCode();
        repository.saveAndFlush(OperationalDispatchWorkItem.builder()
            .tenantCode(tenantCode)
            .updateType(OperationalUpdateType.INTEGRATION_STATE)
            .source("postgres-process-kill-proof")
            .requestId(UUID.randomUUID().toString())
            .status(OperationalDispatchStatus.PENDING)
            .occurredAt(Instant.now())
            .build());

        DefaultListableBeanFactory realtimeBeans = new DefaultListableBeanFactory();
        realtimeBeans.registerSingleton("realtime", new BlockingRealtimeService(tenantCode, markerPath()));
        newQueueService(realtimeBeans).processPendingWork();
        fail("Worker was not killed after committing its dispatch claim");
    }

    @Test
    void reclaimAfterKill() throws IOException {
        long killedWorkerPid = Long.parseLong(Files.readString(markerPath()).trim());
        assertThat(ProcessHandle.current().pid()).isNotEqualTo(killedWorkerPid);
        String tenantCode = tenantCode();
        Long id = jdbc.queryForObject(
            "select id from operational_dispatch_work_items where tenant_code = ?",
            Long.class, tenantCode);
        assertThat(id).isNotNull();
        try {
            OperationalDispatchWorkItem claimed = repository.findById(id).orElseThrow();
            assertThat(claimed.getStatus()).isEqualTo(OperationalDispatchStatus.PROCESSING);
            assertThat(claimed.getAttemptCount()).isEqualTo(1);
            assertThat(claimed.getProcessedAt()).isNull();

            RecordingRealtimeService realtime = new RecordingRealtimeService();
            DefaultListableBeanFactory realtimeBeans = new DefaultListableBeanFactory();
            realtimeBeans.registerSingleton("realtime", realtime);
            OperationalDispatchQueueService freshWorker = newQueueService(realtimeBeans);
            assertThat(freshWorker.processPendingWork()).isZero();
            assertThat(realtime.integrationBroadcasts).isZero();

            jdbc.update("update operational_dispatch_work_items set updated_at = ? where id = ?",
                Timestamp.from(Instant.now().minusSeconds(600)), id);
            assertThat(freshWorker.processPendingWork()).isEqualTo(1);
            OperationalDispatchWorkItem completed = repository.findById(id).orElseThrow();
            assertThat(completed.getStatus()).isEqualTo(OperationalDispatchStatus.COMPLETED);
            assertThat(completed.getAttemptCount()).isEqualTo(2);
            assertThat(completed.getProcessedAt()).isNotNull();
            assertThat(completed.getLastError()).isNull();
            assertThat(realtime.integrationBroadcasts).isEqualTo(1);
        } finally {
            repository.deleteById(id);
        }
    }

    private OperationalDispatchQueueService newQueueService(DefaultListableBeanFactory realtimeBeans) {
        return new OperationalDispatchQueueService(
            repository,
            new DefaultListableBeanFactory().getBeanProvider(DashboardService.class),
            realtimeBeans.getBeanProvider(RealtimeService.class),
            new RequestTraceContext(),
            metrics,
            null,
            new ScheduledTaskExecutionDiagnostics(null)
        );
    }

    private String tenantCode() {
        String key = System.getProperty("synapsecore.process-kill-key", "");
        assertThat(key).matches("[A-Za-z0-9-]{1,32}");
        return "DISPATCH-KILL-" + key;
    }

    private Path markerPath() {
        String file = System.getProperty("synapsecore.process-kill-pid-file", "");
        assertThat(file).isNotBlank();
        return Path.of(file);
    }

    private static final class BlockingRealtimeService extends RealtimeService {
        private final String expectedTenant;
        private final Path marker;

        private BlockingRealtimeService(String expectedTenant, Path marker) {
            super(null, null, null, null, null);
            this.expectedTenant = expectedTenant;
            this.marker = marker;
        }

        @Override
        public void broadcastIntegrationUpdates(String tenantCode) {
            assertThat(tenantCode).isEqualTo(expectedTenant);
            try {
                Files.writeString(marker, Long.toString(ProcessHandle.current().pid()),
                    StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
                TimeUnit.MINUTES.sleep(3);
            } catch (IOException exception) {
                throw new UncheckedIOException(exception);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Worker was interrupted instead of killed", exception);
            }
            fail("Worker was not killed while blocked before broadcast");
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
