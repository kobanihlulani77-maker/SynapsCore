package com.synapsecore.domain.dto;

import com.synapsecore.scenario.dto.ScenarioRunResponse;
import com.synapsecore.scenario.dto.ScenarioNotificationResponse;
import com.synapsecore.integration.dto.IntegrationConnectorResponse;
import com.synapsecore.integration.dto.IntegrationImportRunResponse;
import com.synapsecore.integration.dto.IntegrationReplayRecordResponse;
import java.time.Instant;
import java.util.List;

public record DashboardSnapshotResponse(
    DashboardSummaryResponse summary,
    AlertFeedResponse alerts,
    List<RecommendationResponse> recommendations,
    List<InventoryStatusResponse> inventory,
    FulfillmentOverviewResponse fulfillment,
    List<OrderResponse> recentOrders,
    List<BusinessEventResponse> recentEvents,
    List<AuditLogResponse> auditLogs,
    List<SystemIncidentResponse> systemIncidents,
    List<IntegrationConnectorResponse> integrationConnectors,
    List<IntegrationImportRunResponse> integrationImportRuns,
    List<IntegrationReplayRecordResponse> integrationReplayQueue,
    List<ScenarioNotificationResponse> scenarioNotifications,
    List<ScenarioRunResponse> slaEscalations,
    List<ScenarioRunResponse> recentScenarios,
    SimulationStatusResponse simulation,
    Instant generatedAt
) {
}
