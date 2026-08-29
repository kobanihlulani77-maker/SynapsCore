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
import com.synapsecore.realtime.RealtimeService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Pageable;

class OperationalDispatchQueueServiceTest {

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
            null
        );

        int processedCount = service.processPendingWork();

        assertThat(processedCount).isEqualTo(3);
        assertThat(dashboardService.refreshCalls).isEqualTo(1);
        assertThat(realtimeService.operationalBroadcasts).isEqualTo(1);
        assertThat(realtimeService.integrationBroadcasts).isZero();
        assertThat(realtimeService.lastOperationalTenantCode).isEqualTo("PILOT-TENANT");
        assertThat(realtimeService.lastIntegrationTenantCode).isNull();
    }

    private OperationalDispatchWorkItemRepository inMemoryRepository(List<OperationalDispatchWorkItem> workItems) {
        return repositoryProxy(OperationalDispatchWorkItemRepository.class, (method, args) -> {
            return switch (method.getName()) {
                case "findByStatusInOrderByCreatedAtAsc" -> workItems.stream()
                    .filter(workItem -> workItem.getStatus() == OperationalDispatchStatus.PENDING)
                    .toList();
                case "save" -> args[0];
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

        private RecordingDashboardService() {
            super(null, null, null, null, null, null, null, null, null, null);
        }

        @Override
        public DashboardSummaryResponse refreshSummary() {
            refreshCalls++;
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
}
