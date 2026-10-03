package com.synapsecore.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.synapsecore.audit.RequestTraceContext;
import com.synapsecore.config.SynapseRealtimeProperties;
import com.synapsecore.domain.dto.DashboardSummaryResponse;
import com.synapsecore.domain.entity.OperationalDispatchStatus;
import com.synapsecore.domain.entity.OperationalDispatchWorkItem;
import com.synapsecore.domain.repository.AlertRepository;
import com.synapsecore.domain.repository.FulfillmentTaskRepository;
import com.synapsecore.domain.repository.IntegrationReplayRecordRepository;
import com.synapsecore.domain.repository.OperationalDispatchWorkItemRepository;
import com.synapsecore.domain.service.CoreIdentityWriteIsolationService;
import com.synapsecore.domain.service.DashboardService;
import com.synapsecore.observability.OperationalMetricsService;
import com.synapsecore.observability.ScheduledTaskExecutionDiagnostics;
import com.synapsecore.realtime.RealtimeService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.IntSupplier;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

class OperationalDispatchQueueServiceTest {

    @Test
    void scheduledDrainObservesQueueSelectionEvenWhenNoWorkIsPending() {
        List<String> stages = new ArrayList<>();
        OperationalDispatchWorkItemRepository repository = repositoryProxy(
            OperationalDispatchWorkItemRepository.class,
            (method, args) -> {
                if (method.getName().equals("findReadyForDispatch")) {
                    stages.add("queue-selection");
                    return List.of();
                }
                return defaultValue(method.getReturnType());
            }
        );
        RecordingScheduledTaskExecutionDiagnostics diagnostics =
            new RecordingScheduledTaskExecutionDiagnostics(stages);
        OperationalDispatchQueueService service = new OperationalDispatchQueueService(
            repository,
            new StaticObjectProvider<>(new RecordingDashboardService()),
            new StaticObjectProvider<>(new RecordingRealtimeService()),
            new RequestTraceContext(),
            noOpMetricsService(),
            null,
            diagnostics
        );

        service.drainOnSchedule();

        assertThat(diagnostics.taskName).isEqualTo("operational-dispatch");
        assertThat(stages).containsExactly("telemetry-start", "queue-selection", "telemetry-complete");
    }

    @Test
    void processPendingWorkCollapsesOperationalAndIntegrationFanoutByTenant() {
        OperationalDispatchWorkItemRepository repository = inMemoryRepository(List.of(
            workItem(1L, "PILOT-TENANT", OperationalUpdateType.ORDER_FLOW, "order-api", "req-1"),
            workItem(2L, "PILOT-TENANT", OperationalUpdateType.FULFILLMENT_UPDATE, "fulfillment", "req-2"),
            workItem(3L, "PILOT-TENANT", OperationalUpdateType.INTEGRATION_STATE, "replay", "req-3")
        ));
        RecordingDashboardService dashboardService = new RecordingDashboardService();
        RecordingRealtimeService realtimeService = new RecordingRealtimeService();

        OperationalDispatchQueueService service = new OperationalDispatchQueueService(
            repository,
            new StaticObjectProvider<>(dashboardService),
            new StaticObjectProvider<>(realtimeService),
            new RequestTraceContext(),
            noOpMetricsService(),
            null,
            new ScheduledTaskExecutionDiagnostics(null)
        );

        int processedCount = service.processPendingWork();

        assertThat(processedCount).isEqualTo(3);
        assertThat(dashboardService.refreshCalls).isEqualTo(1);
        assertThat(realtimeService.operationalBroadcasts).isEqualTo(1);
        assertThat(realtimeService.integrationBroadcasts).isZero();
        assertThat(realtimeService.lastOperationalTenantCode).isEqualTo("PILOT-TENANT");
        assertThat(realtimeService.lastIntegrationTenantCode).isNull();
    }

    @Test
    void processPendingWorkRestoresCallerTraceContextAfterInlineDrain() {
        RequestTraceContext traceContext = new RequestTraceContext();
        traceContext.setCurrentRequestId("runtime-request");
        traceContext.setCurrentActor("runtime-admin");
        traceContext.setCurrentTenant("RUNTIME-TENANT");
        MDC.put("requestId", "runtime-request");
        MDC.put("actor", "runtime-admin");
        MDC.put("tenant", "RUNTIME-TENANT");

        RecordingDashboardService dashboardService = new RecordingDashboardService(traceContext);
        OperationalDispatchQueueService service = new OperationalDispatchQueueService(
            inMemoryRepository(List.of(
                workItem(1L, "DISPATCH-TENANT", OperationalUpdateType.ORDER_FLOW, "order-api", "dispatch-request")
            )),
            new StaticObjectProvider<>(dashboardService),
            new StaticObjectProvider<>(new RecordingRealtimeService()),
            traceContext,
            noOpMetricsService(),
            null,
            new ScheduledTaskExecutionDiagnostics(null)
        );

        try {
            assertThat(service.processPendingWork()).isEqualTo(1);
            assertThat(dashboardService.observedRequestId).isEqualTo("dispatch-request");
            assertThat(dashboardService.observedActor).isEqualTo("system-queue");
            assertThat(dashboardService.observedTenant).isEqualTo("DISPATCH-TENANT");
            assertThat(traceContext.getRequiredRequestId()).isEqualTo("runtime-request");
            assertThat(traceContext.getCurrentActorOrAnonymous()).isEqualTo("runtime-admin");
            assertThat(traceContext.getCurrentTenantOrDefault()).isEqualTo("RUNTIME-TENANT");
            assertThat(MDC.get("requestId")).isEqualTo("runtime-request");
            assertThat(MDC.get("actor")).isEqualTo("runtime-admin");
            assertThat(MDC.get("tenant")).isEqualTo("RUNTIME-TENANT");
        } finally {
            traceContext.clear();
            MDC.clear();
        }
    }

    @Test
    void staleProcessingWorkIsReclaimedButFreshProcessingWorkIsNot() {
        OperationalDispatchWorkItem stale = workItem(
            1L, "PILOT-TENANT", OperationalUpdateType.ORDER_FLOW, "order-api", "stale-request"
        );
        stale.setStatus(OperationalDispatchStatus.PROCESSING);
        stale.setAttemptCount(1);
        stale.setUpdatedAt(Instant.now().minusSeconds(600));
        OperationalDispatchWorkItem fresh = workItem(
            2L, "PILOT-TENANT", OperationalUpdateType.ORDER_FLOW, "order-api", "fresh-request"
        );
        fresh.setStatus(OperationalDispatchStatus.PROCESSING);
        fresh.setAttemptCount(1);
        fresh.setUpdatedAt(Instant.now().minusSeconds(120));

        RecordingRealtimeService realtimeService = new RecordingRealtimeService();
        OperationalDispatchQueueService service = new OperationalDispatchQueueService(
            inMemoryRepository(List.of(stale, fresh)),
            new StaticObjectProvider<>(new RecordingDashboardService()),
            new StaticObjectProvider<>(realtimeService),
            new RequestTraceContext(),
            noOpMetricsService(),
            null,
            new ScheduledTaskExecutionDiagnostics(null)
        );
        ReflectionTestUtils.setField(service, "processingLeaseMs", 300_000L);

        assertThat(service.processPendingWork()).isEqualTo(1);
        assertThat(stale.getStatus()).isEqualTo(OperationalDispatchStatus.COMPLETED);
        assertThat(stale.getAttemptCount()).isEqualTo(2);
        assertThat(fresh.getStatus()).isEqualTo(OperationalDispatchStatus.PROCESSING);
        assertThat(fresh.getAttemptCount()).isEqualTo(1);
        assertThat(realtimeService.operationalBroadcasts).isEqualTo(1);
    }

    @Test
    void failedWorkIsNotSilentlyReplayed() {
        OperationalDispatchWorkItem failed = workItem(
            1L, "PILOT-TENANT", OperationalUpdateType.ORDER_FLOW, "order-api", "failed-request"
        );
        failed.setStatus(OperationalDispatchStatus.FAILED);
        failed.setUpdatedAt(Instant.now().minusSeconds(600));
        RecordingRealtimeService realtimeService = new RecordingRealtimeService();
        OperationalDispatchQueueService service = new OperationalDispatchQueueService(
            inMemoryRepository(List.of(failed)),
            new StaticObjectProvider<>(new RecordingDashboardService()),
            new StaticObjectProvider<>(realtimeService),
            new RequestTraceContext(),
            noOpMetricsService(),
            null,
            new ScheduledTaskExecutionDiagnostics(null)
        );

        assertThat(service.processPendingWork()).isZero();
        assertThat(failed.getStatus()).isEqualTo(OperationalDispatchStatus.FAILED);
        assertThat(realtimeService.operationalBroadcasts).isZero();
    }

    private OperationalDispatchWorkItemRepository inMemoryRepository(List<OperationalDispatchWorkItem> workItems) {
        return repositoryProxy(OperationalDispatchWorkItemRepository.class, (method, args) -> {
            return switch (method.getName()) {
                case "findReadyForDispatch" -> workItems.stream()
                    .filter(workItem -> workItem.getStatus() == OperationalDispatchStatus.PENDING
                        || (workItem.getStatus() == OperationalDispatchStatus.PROCESSING
                            && !workItem.getUpdatedAt().isAfter((Instant) args[2])))
                    .toList();
                case "claimForDispatch" -> {
                    OperationalDispatchWorkItem item = workItems.stream()
                        .filter(workItem -> workItem.getId().equals(args[0]))
                        .findFirst().orElseThrow();
                    Instant expiredBefore = (Instant) args[4];
                    if (item.getAttemptCount() != (int) args[1]
                        || (item.getStatus() != OperationalDispatchStatus.PENDING
                            && (item.getStatus() != OperationalDispatchStatus.PROCESSING
                                || item.getUpdatedAt().isAfter(expiredBefore)))) {
                        yield 0;
                    }
                    yield 1;
                }
                case "completeDispatch", "failDispatch" -> {
                    OperationalDispatchWorkItem item = workItems.stream()
                        .filter(workItem -> workItem.getId().equals(args[0]))
                        .findFirst().orElseThrow();
                    if (item.getStatus() != OperationalDispatchStatus.PROCESSING
                        || item.getAttemptCount() != (int) args[1]) {
                        yield 0;
                    }
                    item.setStatus(method.getName().equals("completeDispatch")
                        ? OperationalDispatchStatus.COMPLETED : OperationalDispatchStatus.FAILED);
                    yield 1;
                }
                case "countByStatusIn", "countByTenantCodeIgnoreCaseAndStatusIn" -> 0L;
                case "findTopByStatusInOrderByCreatedAtAsc",
                     "findTopByTenantCodeIgnoreCaseAndStatusInOrderByCreatedAtAsc",
                     "findTopByTenantCodeIgnoreCaseAndStatusOrderByProcessedAtDesc" -> java.util.Optional.empty();
                default -> defaultValue(method.getReturnType());
            };
        });
    }

    private OperationalMetricsService noOpMetricsService() {
        return new OperationalMetricsService(
            new SimpleMeterRegistry(),
            new SynapseRealtimeProperties(),
            zeroRepository(AlertRepository.class),
            zeroRepository(FulfillmentTaskRepository.class),
            zeroRepository(IntegrationReplayRecordRepository.class),
            zeroRepository(OperationalDispatchWorkItemRepository.class)
        );
    }

    private OperationalDispatchWorkItem workItem(Long id,
                                                 String tenantCode,
                                                 OperationalUpdateType updateType,
                                                 String source,
                                                 String requestId) {
        Instant occurredAt = Instant.now();
        return OperationalDispatchWorkItem.builder()
            .id(id)
            .tenantCode(tenantCode)
            .updateType(updateType)
            .source(source)
            .requestId(requestId)
            .status(OperationalDispatchStatus.PENDING)
            .attemptCount(0)
            .occurredAt(occurredAt)
            .createdAt(occurredAt)
            .updatedAt(occurredAt)
            .build();
    }

    @SuppressWarnings("unchecked")
    private <T> T zeroRepository(Class<T> repositoryType) {
        return repositoryProxy(repositoryType, (method, args) -> defaultValue(method.getReturnType()));
    }

    @SuppressWarnings("unchecked")
    private <T> T repositoryProxy(Class<T> repositoryType, RepositoryInvocation invocation) {
        return (T) Proxy.newProxyInstance(
            repositoryType.getClassLoader(),
            new Class<?>[] { repositoryType },
            (proxy, method, args) -> {
                if (method.getDeclaringClass() == Object.class) {
                    return switch (method.getName()) {
                        case "toString" -> repositoryType.getSimpleName() + "TestProxy";
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == args[0];
                        default -> null;
                    };
                }
                return invocation.invoke(method, args == null ? new Object[0] : args);
            }
        );
    }

    private Object defaultValue(Class<?> returnType) {
        if (returnType == Void.TYPE) {
            return null;
        }
        if (returnType == Boolean.TYPE) {
            return false;
        }
        if (returnType == Integer.TYPE) {
            return 0;
        }
        if (returnType == Long.TYPE) {
            return 0L;
        }
        if (returnType == Double.TYPE) {
            return 0.0d;
        }
        if (returnType == Float.TYPE) {
            return 0.0f;
        }
        if (returnType == Short.TYPE) {
            return (short) 0;
        }
        if (returnType == Byte.TYPE) {
            return (byte) 0;
        }
        if (returnType == Character.TYPE) {
            return '\0';
        }
        return null;
    }

    @FunctionalInterface
    private interface RepositoryInvocation {
        Object invoke(Method method, Object[] args);
    }

    private static final class StaticObjectProvider<T> implements ObjectProvider<T> {

        private final T value;

        private StaticObjectProvider(T value) {
            this.value = value;
        }

        @Override
        public T getObject(Object... args) {
            return value;
        }

        @Override
        public T getIfAvailable() {
            return value;
        }

        @Override
        public T getIfUnique() {
            return value;
        }

        @Override
        public T getObject() {
            return value;
        }

        @Override
        public Iterator<T> iterator() {
            return List.of(value).iterator();
        }
    }

    private static final class RecordingDashboardService extends DashboardService {

        private int refreshCalls;
        private final RequestTraceContext traceContext;
        private String observedRequestId;
        private String observedActor;
        private String observedTenant;

        private RecordingDashboardService() {
            this(null);
        }

        private RecordingDashboardService(RequestTraceContext traceContext) {
            super(null, null, null, null, null, null, null, null, null, null);
            this.traceContext = traceContext;
        }

        @Override
        public DashboardSummaryResponse refreshSummary() {
            refreshCalls++;
            if (traceContext != null) {
                observedRequestId = traceContext.getRequiredRequestId();
                observedActor = traceContext.getCurrentActorOrAnonymous();
                observedTenant = traceContext.getCurrentTenantOrDefault();
            }
            return null;
        }
    }

    private static final class RecordingRealtimeService extends RealtimeService {

        private int operationalBroadcasts;
        private int integrationBroadcasts;
        private String lastOperationalTenantCode;
        private String lastIntegrationTenantCode;

        private RecordingRealtimeService() {
            super(null, null, null, null, null);
        }

        @Override
        public void broadcastOperationalUpdates(String tenantCode) {
            operationalBroadcasts++;
            lastOperationalTenantCode = tenantCode;
        }

        @Override
        public void broadcastIntegrationUpdates(String tenantCode) {
            integrationBroadcasts++;
            lastIntegrationTenantCode = tenantCode;
        }
    }

    private static final class RecordingScheduledTaskExecutionDiagnostics
            extends ScheduledTaskExecutionDiagnostics {

        private final List<String> stages;
        private String taskName;

        private RecordingScheduledTaskExecutionDiagnostics(List<String> stages) {
            super(null);
            this.stages = stages;
        }

        @Override
        public int observe(String taskName, IntSupplier work) {
            this.taskName = taskName;
            stages.add("telemetry-start");
            int processed = work.getAsInt();
            stages.add("telemetry-complete");
            return processed;
        }
    }
}
