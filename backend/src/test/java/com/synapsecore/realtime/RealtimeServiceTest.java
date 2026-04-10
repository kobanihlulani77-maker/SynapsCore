package com.synapsecore.realtime;

import static org.assertj.core.api.Assertions.assertThat;

import com.synapsecore.domain.dto.AlertFeedResponse;
import com.synapsecore.domain.dto.AuditLogResponse;
import com.synapsecore.domain.dto.BusinessEventResponse;
import com.synapsecore.domain.dto.DashboardSummaryResponse;
import com.synapsecore.domain.dto.FulfillmentOverviewResponse;
import com.synapsecore.domain.dto.FulfillmentStatusResponse;
import com.synapsecore.domain.dto.InventoryStatusResponse;
import com.synapsecore.domain.dto.OrderResponse;
import com.synapsecore.domain.dto.RecommendationResponse;
import com.synapsecore.domain.dto.SimulationStatusResponse;
import com.synapsecore.domain.dto.SystemIncidentResponse;
import com.synapsecore.domain.dto.SystemIncidentSeverity;
import com.synapsecore.domain.dto.SystemIncidentType;
import com.synapsecore.scenario.dto.ScenarioNotificationResponse;
import com.synapsecore.scenario.dto.ScenarioNotificationType;
import com.synapsecore.scenario.dto.ScenarioRunResponse;
import com.synapsecore.domain.entity.AuditStatus;
import com.synapsecore.domain.entity.BusinessEventType;
import com.synapsecore.domain.entity.FulfillmentStatus;
import com.synapsecore.domain.entity.IntegrationConnectorType;
import com.synapsecore.domain.entity.IntegrationSyncMode;
import com.synapsecore.domain.entity.IntegrationTransformationPolicy;
import com.synapsecore.domain.entity.IntegrationValidationPolicy;
import com.synapsecore.domain.entity.IntegrationImportStatus;
import com.synapsecore.domain.entity.IntegrationReplayStatus;
import com.synapsecore.domain.service.DashboardService;
import com.synapsecore.domain.service.OperationalViewService;
import com.synapsecore.domain.entity.OrderStatus;
import com.synapsecore.domain.entity.RecommendationPriority;
import com.synapsecore.domain.entity.RecommendationType;
import com.synapsecore.integration.dto.IntegrationConnectorResponse;
import com.synapsecore.integration.dto.IntegrationImportRunResponse;
import com.synapsecore.integration.dto.IntegrationReplayRecordResponse;
import com.synapsecore.simulation.SimulationStateService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

class RealtimeServiceTest {

    private static final String TENANT_CODE = "SYNAPSE-DEMO";

    @Test
    void broadcastOperationalUpdatesPublishesFocusedRealtimeTopics() {
        RecordingMessageChannel channel = new RecordingMessageChannel();
        DashboardSummaryResponse summary = new DashboardSummaryResponse(1, 1, 1, 1, 2, 1, 2, 4, 2, 1, 8, false, Instant.now());
        AlertFeedResponse alerts = new AlertFeedResponse(List.of(), List.of());
        List<RecommendationResponse> recommendations = List.of(
            new RecommendationResponse(1L, RecommendationType.REORDER_STOCK, "Reorder Flux Sensor", "Demand is elevated.", RecommendationPriority.HIGH, Instant.now())
        );
        List<InventoryStatusResponse> inventory = List.of(
            new InventoryStatusResponse(1L, "SKU-FLX-100", "Flux Sensor", "Sensors", "WH-NORTH", "Warehouse North", 18L, 20L, true, false, "HIGH", 2.0, 9.0, Instant.now())
        );
        FulfillmentOverviewResponse fulfillment = new FulfillmentOverviewResponse(
            2,
            1,
            1,
            2,
            List.of(new FulfillmentStatusResponse(
                1L,
                "ORD-1001",
                OrderStatus.PROCESSING,
                FulfillmentStatus.DELAYED,
                "WH-NORTH",
                "Warehouse North",
                1,
                "Synapse Courier",
                "TRK-1001",
                Instant.now(),
                Instant.now().minusSeconds(3600),
                Instant.now().minusSeconds(900),
                Instant.now().minusSeconds(7200),
                null,
                1.5,
                2.0,
                -1.0,
                -0.5,
                true,
                true,
                false,
                "high",
                "Delivery pressure is rising in WH-NORTH.",
                Instant.now()
            )),
            Instant.now()
        );
        List<OrderResponse> recentOrders = List.of(
            new OrderResponse(1L, "ORD-1001", OrderStatus.RECEIVED, "WH-NORTH", "Warehouse North", BigDecimal.valueOf(95), 1, Instant.now(), List.of())
        );
        List<BusinessEventResponse> recentEvents = List.of(
            new BusinessEventResponse(1L, BusinessEventType.ORDER_INGESTED, "order-api", "Order ORD-1001 accepted.", Instant.now())
        );
        List<AuditLogResponse> auditLogs = List.of(
            new AuditLogResponse(1L, "ORDER_PROCESSED", "system", "order-api", "CustomerOrder", "ORD-1001", AuditStatus.SUCCESS, "Order accepted.", "req-1001", Instant.now())
        );
        List<SystemIncidentResponse> systemIncidents = List.of(
            new SystemIncidentResponse("connector-1", SystemIncidentType.CONNECTOR_DISABLED, SystemIncidentSeverity.MEDIUM, "ERP North Webhook disabled", "Connector is paused.", "erp_north | Webhook Order", true, Instant.now())
        );
        List<IntegrationConnectorResponse> integrationConnectors = List.of(
            new IntegrationConnectorResponse(1L, "SYNAPSE-DEMO", "erp_north", IntegrationConnectorType.WEBHOOK_ORDER, "ERP North Webhook", true, IntegrationSyncMode.REALTIME_PUSH, null, IntegrationValidationPolicy.STANDARD, IntegrationTransformationPolicy.NORMALIZE_CODES, true, "WH-NORTH", "Starter webhook connector.", "Operations Lead", "Operations Lead", Instant.now(), Instant.now())
        );
        List<IntegrationImportRunResponse> integrationImportRuns = List.of(
            new IntegrationImportRunResponse(4L, "erp_batch", IntegrationConnectorType.CSV_ORDER_IMPORT, "orders.csv", 3, 1, 1, IntegrationImportStatus.PARTIAL_SUCCESS, "Processed CSV import with 1 imported order and 1 failure.", Instant.now())
        );
        List<IntegrationReplayRecordResponse> integrationReplayQueue = List.of(
            new IntegrationReplayRecordResponse(9L, "erp_batch", IntegrationConnectorType.CSV_ORDER_IMPORT, "CSV-RPL-1001", "WH-NORTH", "Product not found: SKU-RPL-778", IntegrationReplayStatus.PENDING, 0, null, null, null, null, Instant.now(), Instant.now())
        );
        List<ScenarioNotificationResponse> scenarioNotifications = List.of(
            new ScenarioNotificationResponse(7L, ScenarioNotificationType.SLA_ESCALATED, "Critical plan rerouted: North escalation candidate", "Final approval is overdue and was rerouted.", "WH-NORTH", null, null, "Executive Operations Director", null, true, Instant.now(), Instant.now())
        );
        List<ScenarioRunResponse> slaEscalations = List.of();
        SimulationStatusResponse simulationStatus = new SimulationStatusResponse(false, Instant.now());

        RealtimeService realtimeService = new RealtimeService(
            new SimpMessagingTemplate(channel),
            new StubOperationalViewService(alerts, recommendations, inventory, fulfillment, recentOrders, recentEvents, auditLogs, systemIncidents, integrationConnectors, integrationImportRuns, integrationReplayQueue, scenarioNotifications, slaEscalations, List.of()),
            new StubDashboardService(summary),
            new StubSimulationStateService(simulationStatus),
            null
        );

        realtimeService.broadcastOperationalUpdates(TENANT_CODE);

        assertThat(channel.messagesByDestination())
            .containsOnlyKeys(
                "/topic/tenant/SYNAPSE-DEMO/dashboard.summary",
                "/topic/tenant/SYNAPSE-DEMO/alerts",
                "/topic/tenant/SYNAPSE-DEMO/recommendations",
                "/topic/tenant/SYNAPSE-DEMO/inventory",
                "/topic/tenant/SYNAPSE-DEMO/fulfillment.overview",
                "/topic/tenant/SYNAPSE-DEMO/orders.recent",
                "/topic/tenant/SYNAPSE-DEMO/events.recent",
                "/topic/tenant/SYNAPSE-DEMO/audit.recent",
                "/topic/tenant/SYNAPSE-DEMO/system.incidents",
                "/topic/tenant/SYNAPSE-DEMO/integrations.connectors",
                "/topic/tenant/SYNAPSE-DEMO/integrations.imports",
                "/topic/tenant/SYNAPSE-DEMO/integrations.replay",
                "/topic/tenant/SYNAPSE-DEMO/scenarios.notifications",
                "/topic/tenant/SYNAPSE-DEMO/scenarios.escalated",
                "/topic/tenant/SYNAPSE-DEMO/simulation.status"
            );
        assertThat(channel.payloadFor("/topic/tenant/SYNAPSE-DEMO/dashboard.summary")).isEqualTo(summary);
        assertThat(channel.payloadFor("/topic/tenant/SYNAPSE-DEMO/alerts")).isEqualTo(alerts);
        assertThat(channel.payloadFor("/topic/tenant/SYNAPSE-DEMO/recommendations")).isEqualTo(recommendations);
        assertThat(channel.payloadFor("/topic/tenant/SYNAPSE-DEMO/inventory")).isEqualTo(inventory);
        assertThat(channel.payloadFor("/topic/tenant/SYNAPSE-DEMO/fulfillment.overview")).isEqualTo(fulfillment);
        assertThat(channel.payloadFor("/topic/tenant/SYNAPSE-DEMO/orders.recent")).isEqualTo(recentOrders);
        assertThat(channel.payloadFor("/topic/tenant/SYNAPSE-DEMO/events.recent")).isEqualTo(recentEvents);
        assertThat(channel.payloadFor("/topic/tenant/SYNAPSE-DEMO/audit.recent")).isEqualTo(auditLogs);
        assertThat(channel.payloadFor("/topic/tenant/SYNAPSE-DEMO/system.incidents")).isEqualTo(systemIncidents);
        assertThat(channel.payloadFor("/topic/tenant/SYNAPSE-DEMO/integrations.connectors")).isEqualTo(integrationConnectors);
        assertThat(channel.payloadFor("/topic/tenant/SYNAPSE-DEMO/integrations.imports")).isEqualTo(integrationImportRuns);
        assertThat(channel.payloadFor("/topic/tenant/SYNAPSE-DEMO/integrations.replay")).isEqualTo(integrationReplayQueue);
        assertThat(channel.payloadFor("/topic/tenant/SYNAPSE-DEMO/scenarios.notifications")).isEqualTo(scenarioNotifications);
        assertThat(channel.payloadFor("/topic/tenant/SYNAPSE-DEMO/scenarios.escalated")).isEqualTo(slaEscalations);
        assertThat(channel.payloadFor("/topic/tenant/SYNAPSE-DEMO/simulation.status")).isEqualTo(simulationStatus);
    }

    @Test
    void broadcastIntegrationUpdatesPublishesFocusedIntegrationTopics() {
        RecordingMessageChannel channel = new RecordingMessageChannel();
        List<BusinessEventResponse> recentEvents = List.of(
            new BusinessEventResponse(1L, BusinessEventType.INTEGRATION_CONNECTOR_UPDATED, "integration-admin", "Connector erp_north updated.", Instant.now())
        );
        List<AuditLogResponse> auditLogs = List.of(
            new AuditLogResponse(1L, "INTEGRATION_CONNECTOR_UPDATED", "integration-admin", "integration-admin", "IntegrationConnector", "erp_north:WEBHOOK_ORDER", AuditStatus.SUCCESS, "Connector saved.", "req-2002", Instant.now())
        );
        List<SystemIncidentResponse> systemIncidents = List.of(
            new SystemIncidentResponse("replay-9", SystemIncidentType.REPLAY_BACKLOG, SystemIncidentSeverity.HIGH, "Replay CSV-RPL-1001", "Product not found: SKU-RPL-778", "erp_batch | Csv Order Import", true, Instant.now())
        );
        List<IntegrationConnectorResponse> integrationConnectors = List.of(
            new IntegrationConnectorResponse(1L, "SYNAPSE-DEMO", "erp_north", IntegrationConnectorType.WEBHOOK_ORDER, "ERP North Webhook", false, IntegrationSyncMode.REALTIME_PUSH, null, IntegrationValidationPolicy.STANDARD, IntegrationTransformationPolicy.NORMALIZE_CODES, true, "WH-NORTH", "Paused for maintenance.", "Operations Lead", "Operations Lead", Instant.now(), Instant.now())
        );
        List<IntegrationImportRunResponse> integrationImportRuns = List.of(
            new IntegrationImportRunResponse(4L, "erp_batch", IntegrationConnectorType.CSV_ORDER_IMPORT, "orders.csv", 3, 1, 1, IntegrationImportStatus.PARTIAL_SUCCESS, "Processed CSV import with 1 imported order and 1 failure.", Instant.now())
        );
        List<IntegrationReplayRecordResponse> integrationReplayQueue = List.of(
            new IntegrationReplayRecordResponse(9L, "erp_batch", IntegrationConnectorType.CSV_ORDER_IMPORT, "CSV-RPL-1001", "WH-NORTH", "Product not found: SKU-RPL-778", IntegrationReplayStatus.PENDING, 0, null, null, null, null, Instant.now(), Instant.now())
        );

        RealtimeService realtimeService = new RealtimeService(
            new SimpMessagingTemplate(channel),
            new StubOperationalViewService(
                new AlertFeedResponse(List.of(), List.of()),
                List.of(),
                List.of(),
                new FulfillmentOverviewResponse(0, 0, 0, 0, List.of(), Instant.now()),
                List.of(),
                recentEvents,
                auditLogs,
                systemIncidents,
                integrationConnectors,
                integrationImportRuns,
                integrationReplayQueue,
                List.of(),
                List.of(),
                List.of()
            ),
            new StubDashboardService(new DashboardSummaryResponse(0, 0, 0, 0, 0, 0, 0, 4, 2, 0, 8, false, Instant.now())),
            new StubSimulationStateService(new SimulationStatusResponse(false, Instant.now())),
            null
        );

        realtimeService.broadcastIntegrationUpdates(TENANT_CODE);

        assertThat(channel.messagesByDestination())
            .containsOnlyKeys(
                "/topic/tenant/SYNAPSE-DEMO/events.recent",
                "/topic/tenant/SYNAPSE-DEMO/audit.recent",
                "/topic/tenant/SYNAPSE-DEMO/system.incidents",
                "/topic/tenant/SYNAPSE-DEMO/integrations.connectors",
                "/topic/tenant/SYNAPSE-DEMO/integrations.imports",
                "/topic/tenant/SYNAPSE-DEMO/integrations.replay"
            );
        assertThat(channel.payloadFor("/topic/tenant/SYNAPSE-DEMO/events.recent")).isEqualTo(recentEvents);
        assertThat(channel.payloadFor("/topic/tenant/SYNAPSE-DEMO/audit.recent")).isEqualTo(auditLogs);
        assertThat(channel.payloadFor("/topic/tenant/SYNAPSE-DEMO/system.incidents")).isEqualTo(systemIncidents);
        assertThat(channel.payloadFor("/topic/tenant/SYNAPSE-DEMO/integrations.connectors")).isEqualTo(integrationConnectors);
        assertThat(channel.payloadFor("/topic/tenant/SYNAPSE-DEMO/integrations.imports")).isEqualTo(integrationImportRuns);
        assertThat(channel.payloadFor("/topic/tenant/SYNAPSE-DEMO/integrations.replay")).isEqualTo(integrationReplayQueue);
    }

    private static final class RecordingMessageChannel implements MessageChannel {

        private final Map<String, Object> messagesByDestination = new LinkedHashMap<>();

        @Override
        public boolean send(Message<?> message) {
            return record(message);
        }

        @Override
        public boolean send(Message<?> message, long timeout) {
            return record(message);
        }

        private boolean record(Message<?> message) {
            messagesByDestination.put(SimpMessageHeaderAccessor.getDestination(message.getHeaders()), message.getPayload());
            return true;
        }

        private Map<String, Object> messagesByDestination() {
            return messagesByDestination;
        }

        private Object payloadFor(String destination) {
            return messagesByDestination.get(destination);
        }
    }

    private static final class StubDashboardService extends DashboardService {

        private final DashboardSummaryResponse summary;

        private StubDashboardService(DashboardSummaryResponse summary) {
            super(null, null, null, null, null, null, null, null, null, null, null);
            this.summary = summary;
        }

        @Override
        public DashboardSummaryResponse getSummary() {
            return summary;
        }
    }

    private static final class StubOperationalViewService extends OperationalViewService {

        private final AlertFeedResponse alerts;
        private final List<RecommendationResponse> recommendations;
        private final List<InventoryStatusResponse> inventory;
        private final FulfillmentOverviewResponse fulfillment;
        private final List<OrderResponse> recentOrders;
        private final List<BusinessEventResponse> recentEvents;
        private final List<AuditLogResponse> auditLogs;
        private final List<SystemIncidentResponse> systemIncidents;
        private final List<IntegrationConnectorResponse> integrationConnectors;
        private final List<IntegrationImportRunResponse> integrationImportRuns;
        private final List<IntegrationReplayRecordResponse> integrationReplayQueue;
        private final List<ScenarioNotificationResponse> scenarioNotifications;
        private final List<ScenarioRunResponse> slaEscalations;
        private final List<ScenarioRunResponse> recentScenarios;

        private StubOperationalViewService(
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
            List<ScenarioRunResponse> recentScenarios
        ) {
            super(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
            this.alerts = alerts;
            this.recommendations = recommendations;
            this.inventory = inventory;
            this.fulfillment = fulfillment;
            this.recentOrders = recentOrders;
            this.recentEvents = recentEvents;
            this.auditLogs = auditLogs;
            this.systemIncidents = systemIncidents;
            this.integrationConnectors = integrationConnectors;
            this.integrationImportRuns = integrationImportRuns;
            this.integrationReplayQueue = integrationReplayQueue;
            this.scenarioNotifications = scenarioNotifications;
            this.slaEscalations = slaEscalations;
            this.recentScenarios = recentScenarios;
        }

        @Override
        public AlertFeedResponse getAlertFeed() {
            return alerts;
        }

        @Override
        public List<RecommendationResponse> getRecommendations() {
            return recommendations;
        }

        @Override
        public List<InventoryStatusResponse> getInventoryOverview() {
            return inventory;
        }

        @Override
        public FulfillmentOverviewResponse getFulfillmentOverview() {
            return fulfillment;
        }

        @Override
        public List<OrderResponse> getRecentOrders() {
            return recentOrders;
        }

        @Override
        public List<BusinessEventResponse> getRecentEvents() {
            return recentEvents;
        }

        @Override
        public List<AuditLogResponse> getRecentAuditLogs() {
            return auditLogs;
        }

        @Override
        public List<SystemIncidentResponse> getSystemIncidents() {
            return systemIncidents;
        }

        @Override
        public List<IntegrationConnectorResponse> getIntegrationConnectors() {
            return integrationConnectors;
        }

        @Override
        public List<IntegrationImportRunResponse> getRecentIntegrationImportRuns() {
            return integrationImportRuns;
        }

        @Override
        public List<IntegrationReplayRecordResponse> getIntegrationReplayQueue() {
            return integrationReplayQueue;
        }

        @Override
        public List<ScenarioNotificationResponse> getScenarioNotifications() {
            return scenarioNotifications;
        }

        @Override
        public List<ScenarioRunResponse> getSlaEscalations() {
            return slaEscalations;
        }

        @Override
        public List<ScenarioRunResponse> getRecentScenarios() {
            return recentScenarios;
        }
    }

    private static final class StubSimulationStateService extends SimulationStateService {

        private final SimulationStatusResponse status;

        private StubSimulationStateService(SimulationStatusResponse status) {
            super(null);
            this.status = status;
        }

        @Override
        public SimulationStatusResponse getStatus() {
            return status;
        }
    }
}
