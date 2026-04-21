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
                .stream()
                .sorted(this::compareOperationalAlertPriority)
                .map(this::toAlertResponse)
                .toList(),
            alertRepository.findTop12ByTenant_CodeIgnoreCaseOrderByUpdatedAtDesc(tenantCode)
                .stream().map(this::toAlertResponse).toList()
        );
    }

    public List<RecommendationResponse> getRecommendations() {
        return recommendationRepository.findTop12ByTenant_CodeIgnoreCaseOrderByCreatedAtDesc(
                tenantContextService.getCurrentTenantCodeOrDefault())
            .stream()
            .sorted(this::compareOperationalRecommendationPriority)
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
            alert.getPolicyExplanation(),
            alert.getStatus(),
            alert.getCreatedAt()
        );
    }

    private int compareOperationalAlertPriority(Alert left, Alert right) {
        int severityComparison = Integer.compare(alertSeverityPriority(left), alertSeverityPriority(right));
        if (severityComparison != 0) {
            return severityComparison;
        }
        int typeComparison = Integer.compare(alertTypePriority(left), alertTypePriority(right));
        if (typeComparison != 0) {
            return typeComparison;
        }
        return right.getCreatedAt().compareTo(left.getCreatedAt());
    }

    private int alertSeverityPriority(Alert alert) {
        return switch (alert.getSeverity()) {
            case CRITICAL -> 0;
            case HIGH -> 1;
            case MEDIUM -> 2;
        };
    }

    private int alertTypePriority(Alert alert) {
        return switch (alert.getType()) {
            case LOW_STOCK -> 0;
            case DEPLETION_RISK -> 1;
            case DELIVERY_DELAY_RISK -> 2;
            case FULFILLMENT_BACKLOG -> 3;
            case FULFILLMENT_ANOMALY -> 4;
        };
    }

    private RecommendationResponse toRecommendationResponse(Recommendation recommendation) {
        return new RecommendationResponse(
            recommendation.getId(),
            recommendation.getType(),
            recommendation.getTitle(),
            recommendation.getDescription(),
            recommendation.getPolicyExplanation(),
            recommendation.getPriority(),
            recommendation.getCreatedAt()
        );
    }

    private int compareOperationalRecommendationPriority(Recommendation left, Recommendation right) {
        int priorityComparison = Integer.compare(recommendationPriority(left), recommendationPriority(right));
        if (priorityComparison != 0) {
            return priorityComparison;
        }
        int typeComparison = Integer.compare(recommendationTypePriority(left), recommendationTypePriority(right));
        if (typeComparison != 0) {
            return typeComparison;
        }
        return right.getCreatedAt().compareTo(left.getCreatedAt());
    }

    private int recommendationPriority(Recommendation recommendation) {
        return switch (recommendation.getPriority()) {
            case CRITICAL -> 0;
            case HIGH -> 1;
            case MEDIUM -> 2;
        };
    }

    private int recommendationTypePriority(Recommendation recommendation) {
        return switch (recommendation.getType()) {
            case REORDER_URGENTLY -> 0;
            case REORDER_STOCK -> 1;
            case TRANSFER_STOCK -> 2;
            case ESCALATE_LOGISTICS -> 3;
            case PRIORITIZE_FULFILLMENT -> 4;
            case INVESTIGATE_LOGISTICS_ANOMALY -> 5;
        };
    }

    private InventoryStatusResponse toInventoryStatusResponse(Inventory inventory) {
        StockPrediction prediction = stockPredictionService.estimate(inventory);
        InventoryInsight insight = inventoryIntelligenceService.evaluate(inventory, prediction);
        return new InventoryStatusResponse(
            inventory.getId(),
            inventory.getProduct().resolveCatalogSku(),
            inventory.getProduct().getName(),
            inventory.getProduct().getCategory(),
            inventory.getWarehouse().getCode(),
            inventory.getWarehouse().getName(),
            inventory.getQuantityAvailable(),
            inventory.getQuantityOnHand(),
            inventory.getQuantityReserved(),
            inventory.getQuantityInbound(),
            inventory.getReorderThreshold(),
            insight.lowStock(),
            insight.rapidConsumption(),
            insight.riskLevel(),
            prediction.unitsPerHour(),
            prediction.hoursToStockout(),
            inventory.getLastReceivedAt(),
            inventory.getLastAdjustedAt(),
            inventory.getLastReconciledAt(),
            inventory.getReconciliationVariance(),
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
            orderItem.getProduct().resolveCatalogSku(),
            orderItem.getProduct().getName(),
            orderItem.getQuantity(),
            orderItem.getReservedQuantity(),
            orderItem.getFulfilledQuantity(),
            orderItem.getCancelledQuantity(),
            orderItem.getReturnedQuantity(),
            orderItem.getUnitPrice()
        );
    }
}
