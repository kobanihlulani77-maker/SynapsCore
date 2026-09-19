package com.synapsecore.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.synapsecore.access.AccessDirectoryService;
import com.synapsecore.domain.dto.AuditLogResponse;
import com.synapsecore.domain.dto.BusinessEventResponse;
import com.synapsecore.domain.dto.InventoryStatusResponse;
import com.synapsecore.domain.dto.OrderResponse;
import com.synapsecore.domain.dto.RecommendationResponse;
import com.synapsecore.domain.dto.SystemIncidentResponse;
import com.synapsecore.integration.dto.IntegrationConnectorResponse;
import com.synapsecore.integration.dto.IntegrationImportRunResponse;
import com.synapsecore.integration.dto.IntegrationReplayRecordResponse;
import com.synapsecore.scenario.dto.ScenarioNotificationResponse;
import com.synapsecore.scenario.dto.ScenarioRunResponse;
import com.synapsecore.tenant.TenantContextService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class OperationalViewServiceSnapshotReuseTest {

    @Test
    void snapshotReusesAlreadyComposedSourcesForIncidentAssembly() {
        List<AuditLogResponse> auditLogs = new ArrayList<>();
        List<IntegrationConnectorResponse> connectors = new ArrayList<>();
        List<IntegrationReplayRecordResponse> replayQueue = new ArrayList<>();
        List<ScenarioNotificationResponse> notifications = new ArrayList<>();
        List<SystemIncidentResponse> incidents = new ArrayList<>();
        AtomicInteger incidentCompositions = new AtomicInteger();

        SystemIncidentService incidentService = new SystemIncidentService(
            null, null, null, null, null, null, null, null
        ) {
            @Override
            public List<SystemIncidentResponse> getActiveIncidents(
                    List<AuditLogResponse> suppliedAuditLogs,
                    List<IntegrationReplayRecordResponse> suppliedReplayQueue,
                    List<IntegrationConnectorResponse> suppliedConnectors,
                    List<ScenarioNotificationResponse> suppliedNotifications) {
                incidentCompositions.incrementAndGet();
                assertThat(suppliedAuditLogs).isSameAs(auditLogs);
                assertThat(suppliedReplayQueue).isSameAs(replayQueue);
                assertThat(suppliedConnectors).isSameAs(connectors);
                assertThat(suppliedNotifications).isSameAs(notifications);
                return incidents;
            }
        };
        DashboardService dashboardService = new DashboardService(
            null, null, null, null, null, null, null, null, null, null
        ) {
            @Override
            public com.synapsecore.domain.dto.DashboardSummaryResponse getSummary() {
                return null;
            }
        };
        TenantContextService tenantContextService = new TenantContextService(null, null, null, null, null) {
            @Override
            public String getCurrentTenantCodeOrDefault() {
                return "SNAPSHOT-REUSE";
            }
        };
        AccessDirectoryService accessDirectoryService = new AccessDirectoryService(null, null, null, null, null) {
            @Override
            public Optional<com.synapsecore.domain.entity.AccessOperator> getCurrentOperator() {
                return Optional.empty();
            }
        };

        OperationalViewService service = new OperationalViewService(
            null, null, null, null, null, null, null, dashboardService, null, null,
            incidentService, null, null, null, null, null, tenantContextService, accessDirectoryService
        ) {
            @Override public com.synapsecore.domain.dto.AlertFeedResponse getAlertFeed() { return null; }
            @Override public List<RecommendationResponse> getRecommendations() { return List.of(); }
            @Override public List<InventoryStatusResponse> getInventoryOverview() { return List.of(); }
            @Override public com.synapsecore.domain.dto.FulfillmentOverviewResponse getFulfillmentOverview() { return null; }
            @Override public List<OrderResponse> getRecentOrders() { return List.of(); }
            @Override public List<BusinessEventResponse> getRecentEvents() { return List.of(); }
            @Override public List<AuditLogResponse> getRecentAuditLogs() { return auditLogs; }
            @Override public List<IntegrationConnectorResponse> getIntegrationConnectors() { return connectors; }
            @Override public List<IntegrationImportRunResponse> getRecentIntegrationImportRuns() { return List.of(); }
            @Override public List<IntegrationReplayRecordResponse> getIntegrationReplayQueue() { return replayQueue; }
            @Override public List<ScenarioNotificationResponse> getScenarioNotifications() { return notifications; }
            @Override public List<ScenarioRunResponse> getSlaEscalations() { return List.of(); }
            @Override public List<ScenarioRunResponse> getRecentScenarios() { return List.of(); }
        };

        var snapshot = service.getSnapshot();

        assertThat(snapshot.auditLogs()).isSameAs(auditLogs);
        assertThat(snapshot.integrationConnectors()).isSameAs(connectors);
        assertThat(snapshot.integrationReplayQueue()).isSameAs(replayQueue);
        assertThat(snapshot.scenarioNotifications()).isSameAs(notifications);
        assertThat(snapshot.systemIncidents()).isSameAs(incidents);
        assertThat(incidentCompositions).hasValue(1);
    }
}
