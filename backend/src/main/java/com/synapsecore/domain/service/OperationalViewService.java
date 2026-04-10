package com.synapsecore.domain.service;

import com.synapsecore.audit.AuditLogService;
import com.synapsecore.domain.dto.AlertFeedResponse;
import com.synapsecore.domain.dto.AlertResponse;
import com.synapsecore.domain.dto.AuditLogResponse;
import com.synapsecore.domain.dto.BusinessEventResponse;
import com.synapsecore.domain.dto.DashboardSnapshotResponse;
import com.synapsecore.domain.dto.FulfillmentOverviewResponse;
import com.synapsecore.domain.dto.InventoryStatusResponse;
import com.synapsecore.domain.dto.OrderItemResponse;
import com.synapsecore.domain.dto.OrderResponse;
import com.synapsecore.domain.dto.RecommendationResponse;
import com.synapsecore.domain.dto.SystemIncidentResponse;
import com.synapsecore.integration.IntegrationConnectorService;
import com.synapsecore.integration.IntegrationImportRunService;
import com.synapsecore.integration.IntegrationReplayService;
import com.synapsecore.integration.dto.IntegrationConnectorResponse;
import com.synapsecore.integration.dto.IntegrationImportRunResponse;
import com.synapsecore.integration.dto.IntegrationReplayRecordResponse;
import com.synapsecore.scenario.ScenarioHistoryService;
import com.synapsecore.scenario.dto.ScenarioNotificationResponse;
import com.synapsecore.scenario.dto.ScenarioRunResponse;
import com.synapsecore.domain.entity.Alert;
import com.synapsecore.domain.entity.AlertStatus;
import com.synapsecore.domain.entity.CustomerOrder;
import com.synapsecore.domain.entity.Inventory;
import com.synapsecore.domain.entity.OrderItem;
import com.synapsecore.domain.entity.Recommendation;
import com.synapsecore.domain.repository.AlertRepository;
import com.synapsecore.domain.repository.CustomerOrderRepository;
import com.synapsecore.domain.repository.InventoryRepository;
import com.synapsecore.domain.repository.RecommendationRepository;
import com.synapsecore.event.BusinessEventQueryService;
import com.synapsecore.fulfillment.FulfillmentService;
import com.synapsecore.intelligence.InventoryInsight;
import com.synapsecore.intelligence.InventoryIntelligenceService;
import com.synapsecore.prediction.StockPrediction;
import com.synapsecore.prediction.StockPredictionService;
import com.synapsecore.simulation.SimulationStateService;
import com.synapsecore.tenant.TenantContextService;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OperationalViewService {

    private final AlertRepository alertRepository;
    private final RecommendationRepository recommendationRepository;
    private final InventoryRepository inventoryRepository;
    private final CustomerOrderRepository customerOrderRepository;
    private final StockPredictionService stockPredictionService;
    private final InventoryIntelligenceService inventoryIntelligenceService;
    private final DashboardService dashboardService;
    private final SimulationStateService simulationStateService;
    private final BusinessEventQueryService businessEventQueryService;
    private final AuditLogService auditLogService;
    private final SystemIncidentService systemIncidentService;
    private final ScenarioHistoryService scenarioHistoryService;
    private final IntegrationConnectorService integrationConnectorService;
    private final IntegrationImportRunService integrationImportRunService;
    private final IntegrationReplayService integrationReplayService;
    private final FulfillmentService fulfillmentService;
    private final TenantContextService tenantContextService;

    public AlertFeedResponse getAlertFeed() {
        String tenantCode = tenantContextService.getCurrentTenantCodeOrDefault();
        return new AlertFeedResponse(
            alertRepository.findTop12ByTenant_CodeIgnoreCaseAndStatusOrderByCreatedAtDesc(tenantCode, AlertStatus.ACTIVE)
                .stream().map(this::toAlertResponse).toList(),
            alertRepository.findTop12ByTenant_CodeIgnoreCaseOrderByCreatedAtDesc(tenantCode)
                .stream().map(this::toAlertResponse).toList()
        );
    }

    public List<RecommendationResponse> getRecommendations() {
        return recommendationRepository.findTop12ByTenant_CodeIgnoreCaseOrderByCreatedAtDesc(
                tenantContextService.getCurrentTenantCodeOrDefault())
            .stream()
            .map(this::toRecommendationResponse)
            .toList();
    }

    public List<InventoryStatusResponse> getInventoryOverview() {
        return inventoryRepository.findAllWithProductAndWarehouseByTenantCode(
                tenantContextService.getCurrentTenantCodeOrDefault())
            .stream()
            .map(this::toInventoryStatusResponse)
            .toList();
    }

    public List<OrderResponse> getRecentOrders() {
        List<Long> orderIds = customerOrderRepository.findRecentOrderIdsByTenantCode(
            tenantContextService.getCurrentTenantCodeOrDefault(),
            PageRequest.of(0, 12));
        if (orderIds.isEmpty()) {
            return List.of();
        }

        return customerOrderRepository.findByIdIn(orderIds).stream()
            .sorted(Comparator.comparing(CustomerOrder::getCreatedAt).reversed())
            .map(this::toOrderResponse)
            .toList();
    }

    public FulfillmentOverviewResponse getFulfillmentOverview() {
        return fulfillmentService.getOverview();
    }

    public List<BusinessEventResponse> getRecentEvents() {
        return businessEventQueryService.getRecentEvents();
    }

    public List<AuditLogResponse> getRecentAuditLogs() {
        return auditLogService.getRecentAuditLogs();
    }

    public List<SystemIncidentResponse> getSystemIncidents() {
        return systemIncidentService.getActiveIncidents();
    }

    public List<IntegrationConnectorResponse> getIntegrationConnectors() {
        return integrationConnectorService.getConnectors();
    }

    public List<IntegrationImportRunResponse> getRecentIntegrationImportRuns() {
        return integrationImportRunService.getRecentRuns();
    }

    public List<IntegrationReplayRecordResponse> getIntegrationReplayQueue() {
        return integrationReplayService.getReplayQueue();
    }

    public List<ScenarioRunResponse> getRecentScenarios() {
        return scenarioHistoryService.getRecentScenarioRuns();
    }

    public List<ScenarioNotificationResponse> getScenarioNotifications() {
        return scenarioHistoryService.getScenarioNotifications();
    }

    public List<ScenarioRunResponse> getSlaEscalations() {
        return scenarioHistoryService.getScenarioRunsForInbox(true, 6);
    }

    public DashboardSnapshotResponse getSnapshot() {
        return new DashboardSnapshotResponse(
            dashboardService.getSummary(),
            getAlertFeed(),
            getRecommendations(),
            getInventoryOverview(),
            getFulfillmentOverview(),
            getRecentOrders(),
            getRecentEvents(),
            getRecentAuditLogs(),
            getSystemIncidents(),
            getIntegrationConnectors(),
            getRecentIntegrationImportRuns(),
            getIntegrationReplayQueue(),
            getScenarioNotifications(),
            getSlaEscalations(),
            getRecentScenarios(),
            simulationStateService.getStatus(),
            Instant.now()
        );
    }

    private AlertResponse toAlertResponse(Alert alert) {
        return new AlertResponse(
            alert.getId(),
            alert.getType(),
            alert.getSeverity(),
            alert.getTitle(),
            alert.getDescription(),
            alert.getImpactSummary(),
            alert.getRecommendedAction(),
            alert.getStatus(),
            alert.getCreatedAt()
        );
    }

    private RecommendationResponse toRecommendationResponse(Recommendation recommendation) {
        return new RecommendationResponse(
            recommendation.getId(),
            recommendation.getType(),
            recommendation.getTitle(),
            recommendation.getDescription(),
            recommendation.getPriority(),
            recommendation.getCreatedAt()
        );
    }

    private InventoryStatusResponse toInventoryStatusResponse(Inventory inventory) {
        StockPrediction prediction = stockPredictionService.estimate(inventory);
        InventoryInsight insight = inventoryIntelligenceService.evaluate(inventory, prediction);
        return new InventoryStatusResponse(
            inventory.getId(),
            inventory.getProduct().getSku(),
            inventory.getProduct().getName(),
            inventory.getProduct().getCategory(),
            inventory.getWarehouse().getCode(),
            inventory.getWarehouse().getName(),
            inventory.getQuantityAvailable(),
            inventory.getReorderThreshold(),
            insight.lowStock(),
            insight.rapidConsumption(),
            insight.riskLevel(),
            prediction.unitsPerHour(),
            prediction.hoursToStockout(),
            inventory.getUpdatedAt()
        );
    }

    private OrderResponse toOrderResponse(CustomerOrder order) {
        List<OrderItemResponse> items = order.getItems().stream()
            .map(this::toOrderItemResponse)
            .toList();

        return new OrderResponse(
            order.getId(),
            order.getExternalOrderId(),
            order.getStatus(),
            order.getWarehouse().getCode(),
            order.getWarehouse().getName(),
            order.getTotalAmount(),
            items.stream().mapToInt(OrderItemResponse::quantity).sum(),
            order.getCreatedAt(),
            items
        );
    }

    private OrderItemResponse toOrderItemResponse(OrderItem orderItem) {
        return new OrderItemResponse(
            orderItem.getProduct().getSku(),
            orderItem.getProduct().getName(),
            orderItem.getQuantity(),
            orderItem.getUnitPrice()
        );
    }
}
