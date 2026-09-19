package com.synapsecore.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.synapsecore.access.AccessDirectoryService;
import com.synapsecore.audit.AuditLogService;
import com.synapsecore.domain.repository.IntegrationInboundRecordRepository;
import com.synapsecore.domain.repository.OperationalDispatchWorkItemRepository;
import com.synapsecore.integration.IntegrationConnectorService;
import com.synapsecore.integration.IntegrationReplayService;
import com.synapsecore.scenario.ScenarioHistoryService;
import com.synapsecore.tenant.TenantContextService;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class SystemIncidentServiceSnapshotReuseTest {

    @Test
    void suppliedSnapshotsAvoidReloadingComposedIncidentSources() {
        Fixture fixture = fixture();

        assertThat(fixture.service().getActiveIncidents(
            List.of(), List.of(), List.of(), List.of()
        )).isEmpty();

        assertThat(fixture.auditReads()).hasValue(0);
        assertThat(fixture.replayReads()).hasValue(0);
        assertThat(fixture.connectorReads()).hasValue(0);
        assertThat(fixture.notificationReads()).hasValue(0);
        assertThat(fixture.operatorReads()).hasValue(1);
    }

    @Test
    void connectorSnapshotIsReusedWhileOtherIncidentSourcesRemainAuthoritative() {
        Fixture fixture = fixture();

        assertThat(fixture.service().getActiveIncidentsWithConnectors(List.of())).isEmpty();

        assertThat(fixture.auditReads()).hasValue(1);
        assertThat(fixture.replayReads()).hasValue(1);
        assertThat(fixture.connectorReads()).hasValue(0);
        assertThat(fixture.notificationReads()).hasValue(1);
        assertThat(fixture.operatorReads()).hasValue(1);
    }

    private Fixture fixture() {
        AtomicInteger auditReads = new AtomicInteger();
        AtomicInteger replayReads = new AtomicInteger();
        AtomicInteger connectorReads = new AtomicInteger();
        AtomicInteger notificationReads = new AtomicInteger();
        AtomicInteger operatorReads = new AtomicInteger();

        AuditLogService auditLogService = new AuditLogService(null, null, null, null, null, null) {
            @Override
            public List<com.synapsecore.domain.dto.AuditLogResponse> getRecentAuditLogs() {
                auditReads.incrementAndGet();
                return List.of();
            }
        };
        IntegrationConnectorService connectorService = new IntegrationConnectorService(
            null, null, null, null, null, null, null, null, null, null, null, null
        ) {
            @Override
            public List<com.synapsecore.integration.dto.IntegrationConnectorResponse> getConnectors() {
                connectorReads.incrementAndGet();
                return List.of();
            }
        };
        IntegrationReplayService replayService = new IntegrationReplayService(
            null, null, null, null, null, null, null, null, null, null, null, null, null, null
        ) {
            @Override
            public List<com.synapsecore.integration.dto.IntegrationReplayRecordResponse> getReplayQueue() {
                replayReads.incrementAndGet();
                return List.of();
            }
        };
        ScenarioHistoryService scenarioHistoryService = new ScenarioHistoryService(
            null, null, null, null, null, null, null, null, null, null, null
        ) {
            @Override
            public List<com.synapsecore.scenario.dto.ScenarioNotificationResponse> getScenarioNotifications() {
                notificationReads.incrementAndGet();
                return List.of();
            }
        };
        AccessDirectoryService accessDirectoryService = new AccessDirectoryService(null, null, null, null, null) {
            @Override
            public Optional<com.synapsecore.domain.entity.AccessOperator> getCurrentOperator() {
                operatorReads.incrementAndGet();
                return Optional.empty();
            }
        };
        TenantContextService tenantContextService = new TenantContextService(null, null, null, null, null) {
            @Override
            public String getCurrentTenantCodeOrDefault() {
                return "SNAPSHOT-REUSE";
            }
        };
        IntegrationInboundRecordRepository inboundRepository = emptyListRepository(
            IntegrationInboundRecordRepository.class,
            "findTop8ByTenantCodeIgnoreCaseAndStatusInOrderByUpdatedAtDesc"
        );
        OperationalDispatchWorkItemRepository dispatchRepository = emptyListRepository(
            OperationalDispatchWorkItemRepository.class,
            "findTop8ByTenantCodeIgnoreCaseAndStatusOrderByUpdatedAtDesc"
        );

        return new Fixture(
            new SystemIncidentService(
                auditLogService, connectorService, replayService, inboundRepository,
                scenarioHistoryService, dispatchRepository, tenantContextService, accessDirectoryService
            ),
            auditReads, replayReads, connectorReads, notificationReads, operatorReads
        );
    }

    @SuppressWarnings("unchecked")
    private <T> T emptyListRepository(Class<T> type, String expectedMethod) {
        return (T) Proxy.newProxyInstance(
            type.getClassLoader(),
            new Class<?>[]{type},
            (proxy, method, arguments) -> {
                if (method.getName().equals(expectedMethod)) {
                    return List.of();
                }
                throw new AssertionError("Unexpected repository call: " + method.getName());
            }
        );
    }

    private record Fixture(
        SystemIncidentService service,
        AtomicInteger auditReads,
        AtomicInteger replayReads,
        AtomicInteger connectorReads,
        AtomicInteger notificationReads,
        AtomicInteger operatorReads
    ) {
    }
}
