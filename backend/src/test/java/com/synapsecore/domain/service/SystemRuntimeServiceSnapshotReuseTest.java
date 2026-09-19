package com.synapsecore.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.synapsecore.alert.AlertScopeService;
import com.synapsecore.config.SynapseRealtimeProperties;
import com.synapsecore.domain.repository.AlertRepository;
import com.synapsecore.domain.repository.AuditLogRepository;
import com.synapsecore.domain.repository.BusinessEventRepository;
import com.synapsecore.domain.repository.FulfillmentTaskRepository;
import com.synapsecore.domain.repository.IntegrationConnectorRepository;
import com.synapsecore.domain.repository.IntegrationInboundRecordRepository;
import com.synapsecore.domain.repository.IntegrationImportRunRepository;
import com.synapsecore.domain.repository.IntegrationReplayRecordRepository;
import com.synapsecore.domain.repository.OperationalDispatchWorkItemRepository;
import com.synapsecore.event.OperationalDispatchQueueService;
import com.synapsecore.integration.IntegrationConnectorService;
import com.synapsecore.integration.dto.IntegrationConnectorResponse;
import com.synapsecore.observability.OperationalAlertHookService;
import com.synapsecore.observability.OperationalMetricsService;
import com.synapsecore.realtime.RealtimeBrokerMode;
import com.synapsecore.realtime.RealtimeService;
import com.synapsecore.tenant.TenantContextService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.LivenessState;
import org.springframework.boot.availability.ReadinessState;

class SystemRuntimeServiceSnapshotReuseTest {

    @Test
    void tenantRuntimeReusesConnectorAndDispatchSnapshots() {
        AtomicInteger connectorReads = new AtomicInteger();
        AtomicInteger incidentCompositions = new AtomicInteger();
        AtomicInteger dispatchCounts = new AtomicInteger();
        List<IntegrationConnectorResponse> connectors = new ArrayList<>();

        OperationalDispatchWorkItemRepository dispatchRepository = repository(
            OperationalDispatchWorkItemRepository.class,
            methodName -> {
                if (methodName.equals("countByTenantCodeIgnoreCaseAndStatusIn")) {
                    dispatchCounts.incrementAndGet();
                    return 0L;
                }
                return defaultValue(methodName);
            }
        );
        IntegrationReplayRecordRepository replayRepository = repository(
            IntegrationReplayRecordRepository.class,
            this::defaultValue
        );
        FulfillmentTaskRepository fulfillmentRepository = repository(
            FulfillmentTaskRepository.class,
            this::defaultValue
        );
        AlertRepository alertRepository = repository(AlertRepository.class, this::defaultValue);

        OperationalMetricsService metricsService = new OperationalMetricsService(
            new SimpleMeterRegistry(),
            new SynapseRealtimeProperties(),
            alertRepository,
            fulfillmentRepository,
            replayRepository,
            dispatchRepository
        ) {
            @Override
            public com.synapsecore.domain.dto.SystemMetricsSummary snapshotForTenant(String tenantCode) {
                return null;
            }
        };
        IntegrationConnectorService connectorService = new IntegrationConnectorService(
            null, null, null, null, null, null, null, null, null, null, null, null
        ) {
            @Override
            public List<IntegrationConnectorResponse> getConnectors() {
                connectorReads.incrementAndGet();
                return connectors;
            }
        };
        SystemIncidentService incidentService = new SystemIncidentService(
            null, null, null, null, null, null, null, null
        ) {
            @Override
            public List<com.synapsecore.domain.dto.SystemIncidentResponse> getActiveIncidentsWithConnectors(
                    List<IntegrationConnectorResponse> suppliedConnectors) {
                incidentCompositions.incrementAndGet();
                assertThat(suppliedConnectors).isSameAs(connectors);
                return List.of();
            }
        };
        TenantContextService tenantContextService = new TenantContextService(null, null, null, null, null) {
            @Override
            public String getCurrentTenantCodeOrDefault() {
                return "RUNTIME-REUSE";
            }
        };
        RealtimeService realtimeService = new RealtimeService(null, null, null, null, null) {
            @Override
            public RealtimeBrokerMode brokerMode() {
                return RealtimeBrokerMode.SIMPLE_IN_MEMORY;
            }
        };
        AlertScopeService alertScopeService = new AlertScopeService(null, null) {
            @Override
            public long countVisibleActiveAlerts(String tenantCode) {
                return 0L;
            }
        };
        OperationalDispatchQueueService dispatchQueueService = new OperationalDispatchQueueService(
            null, null, null, null, null, null, null
        ) {
            @Override
            public Long oldestPendingAgeSeconds(String tenantCode) {
                return null;
            }
        };
        OperationalAlertHookService alertHookService = new OperationalAlertHookService(null, null, null, null) {
            @Override
            public boolean isConfigured() {
                return false;
            }
        };
        ApplicationAvailability availability = repository(
            ApplicationAvailability.class,
            methodName -> methodName.equals("getLivenessState")
                ? LivenessState.BROKEN
                : methodName.equals("getReadinessState") ? ReadinessState.REFUSING_TRAFFIC : null
        );

        SystemRuntimeService service = new SystemRuntimeService(
            null,
            null,
            null,
            availability,
            null,
            alertScopeService,
            repository(AuditLogRepository.class, this::defaultValue),
            repository(BusinessEventRepository.class, this::defaultValue),
            fulfillmentRepository,
            repository(IntegrationConnectorRepository.class, this::defaultValue),
            repository(IntegrationInboundRecordRepository.class, this::defaultValue),
            repository(IntegrationImportRunRepository.class, this::defaultValue),
            replayRepository,
            dispatchRepository,
            incidentService,
            dispatchQueueService,
            metricsService,
            tenantContextService,
            connectorService,
            realtimeService,
            alertHookService
        );

        service.getTenantRuntimeStatus();

        assertThat(connectorReads).hasValue(1);
        assertThat(incidentCompositions).hasValue(1);
        assertThat(dispatchCounts).hasValue(2);
    }

    @SuppressWarnings("unchecked")
    private <T> T repository(Class<T> type, java.util.function.Function<String, Object> result) {
        return (T) Proxy.newProxyInstance(
            type.getClassLoader(),
            new Class<?>[]{type},
            (proxy, method, arguments) -> result.apply(method.getName())
        );
    }

    private Object defaultValue(String methodName) {
        if (methodName.startsWith("count")) {
            return 0L;
        }
        if (methodName.startsWith("findTop20") || methodName.startsWith("findTop8")) {
            return List.of();
        }
        if (methodName.startsWith("findTopBy") || methodName.startsWith("findBy")) {
            return Optional.empty();
        }
        return List.of();
    }
}
