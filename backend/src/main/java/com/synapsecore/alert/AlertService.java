package com.synapsecore.alert;

import com.synapsecore.domain.entity.Alert;
import com.synapsecore.domain.entity.AlertStatus;
import com.synapsecore.domain.entity.AlertType;
import com.synapsecore.domain.entity.BusinessEventType;
import com.synapsecore.domain.entity.FulfillmentTask;
import com.synapsecore.domain.entity.Inventory;
import com.synapsecore.domain.entity.Recommendation;
import com.synapsecore.domain.entity.TenantOperationalPolicy;
import com.synapsecore.domain.repository.AlertRepository;
import com.synapsecore.domain.service.TenantOperationalPolicyService;
import com.synapsecore.event.BusinessEventService;
import com.synapsecore.fulfillment.FulfillmentAssessment;
import com.synapsecore.intelligence.InventoryInsight;
import com.synapsecore.prediction.StockPrediction;
import com.synapsecore.scenario.dto.ScenarioAlertProjection;
import com.synapsecore.scenario.dto.ScenarioRecommendationProjection;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRepository alertRepository;
    private final BusinessEventService businessEventService;
    private final TenantOperationalPolicyService tenantOperationalPolicyService;

    public void syncInventoryAlerts(Inventory inventory,
                                    InventoryInsight insight,
                                    StockPrediction prediction,
                                    Recommendation recommendation,
                                    String source) {
        syncLowStockAlert(inventory, insight, recommendation, source);
        syncDepletionRiskAlert(inventory, insight, recommendation);
    }

    public void syncFulfillmentAlerts(FulfillmentTask task,
                                      FulfillmentAssessment assessment,
                                      Recommendation recommendation,
                                      String source) {
        syncFulfillmentBacklogAlert(task, assessment, recommendation, source);
        syncDeliveryDelayAlert(task, assessment, recommendation, source);
        syncFulfillmentAnomalyAlert(task, assessment, recommendation, source);
    }

    public List<ScenarioAlertProjection> previewInventoryAlerts(Inventory inventory,
                                                                InventoryInsight insight,
                                                                ScenarioRecommendationProjection recommendation) {
        List<ScenarioAlertProjection> alerts = new ArrayList<>();
        ScenarioAlertProjection lowStockAlert = previewLowStockAlert(inventory, insight, recommendation);
        if (lowStockAlert != null) {
            alerts.add(lowStockAlert);
        }

        ScenarioAlertProjection depletionRiskAlert = previewDepletionRiskAlert(inventory, insight, recommendation);
        if (depletionRiskAlert != null) {
            alerts.add(depletionRiskAlert);
        }
        return alerts;
    }

    private Alert syncLowStockAlert(Inventory inventory,
                                    InventoryInsight insight,
                                    Recommendation recommendation,
                                    String source) {
        // MVP lifecycle choice:
        // - keep at most one ACTIVE low-stock alert per SKU/warehouse pair
        // - refresh that alert while the low-stock condition persists
        // - mark it RESOLVED once stock rises above threshold
        String title = "Low stock detected for SKU " + inventory.getProduct().resolveCatalogSku()
            + " in " + inventory.getWarehouse().getCode();
        String tenantCode = inventory.getWarehouse().getTenant() == null ? null : inventory.getWarehouse().getTenant().getCode();
        Alert existing = alertRepository.findFirstByTenant_CodeIgnoreCaseAndTypeAndStatusAndTitle(
                tenantCode,
                AlertType.LOW_STOCK,
                AlertStatus.ACTIVE,
                title)
            .orElse(null);

        if (!insight.lowStock()) {
            if (existing != null) {
                existing.setStatus(AlertStatus.RESOLVED);
                alertRepository.save(existing);
            }
            return null;
        }

        ScenarioAlertProjection preview = previewLowStockAlert(
            inventory,
            insight,
            recommendation == null
                ? null
                : new ScenarioRecommendationProjection(
                    recommendation.getType(),
                    recommendation.getPriority(),
                    recommendation.getTitle(),
                    recommendation.getDescription()
                )
        );

        Alert alert = existing == null
            ? Alert.builder().tenant(inventory.getWarehouse().getTenant()).type(AlertType.LOW_STOCK).title(title).build()
            : existing;
        alert.setSeverity(preview.severity());
        alert.setDescription(preview.description());
        alert.setImpactSummary(preview.impactSummary());
        alert.setRecommendedAction(preview.recommendedAction());
        alert.setPolicyExplanation(buildInventoryPolicyExplanation(inventory, insight, "low stock"));
        alert.setStatus(AlertStatus.ACTIVE);
        Alert saved = alertRepository.save(alert);

        if (existing == null) {
            businessEventService.record(
                BusinessEventType.LOW_STOCK_DETECTED,
                source,
                inventory.getProduct().resolveCatalogSku() + " fell below threshold in " + inventory.getWarehouse().getCode()
            );
        }

        return saved;
    }

    private Alert syncDepletionRiskAlert(Inventory inventory,
                                         InventoryInsight insight,
                                         Recommendation recommendation) {
        String title = "Depletion risk rising for SKU " + inventory.getProduct().resolveCatalogSku()
            + " in " + inventory.getWarehouse().getCode();
        String tenantCode = inventory.getWarehouse().getTenant() == null ? null : inventory.getWarehouse().getTenant().getCode();
        Alert existing = alertRepository.findFirstByTenant_CodeIgnoreCaseAndTypeAndStatusAndTitle(
                tenantCode,
                AlertType.DEPLETION_RISK,
                AlertStatus.ACTIVE,
                title)
            .orElse(null);

        if (!insight.depletionRisk()) {
            if (existing != null) {
                existing.setStatus(AlertStatus.RESOLVED);
                alertRepository.save(existing);
            }
            return null;
        }

        ScenarioAlertProjection preview = previewDepletionRiskAlert(
            inventory,
            insight,
            recommendation == null
                ? null
                : new ScenarioRecommendationProjection(
                    recommendation.getType(),
                    recommendation.getPriority(),
                    recommendation.getTitle(),
                    recommendation.getDescription()
                )
        );

        Alert alert = existing == null
            ? Alert.builder().tenant(inventory.getWarehouse().getTenant()).type(AlertType.DEPLETION_RISK).title(title).build()
            : existing;
        alert.setSeverity(preview.severity());
        alert.setDescription(preview.description());
        alert.setImpactSummary(preview.impactSummary());
        alert.setRecommendedAction(preview.recommendedAction());
        alert.setPolicyExplanation(buildInventoryPolicyExplanation(inventory, insight, "depletion risk"));
        alert.setStatus(AlertStatus.ACTIVE);
        return alertRepository.save(alert);
    }

    private ScenarioAlertProjection previewLowStockAlert(Inventory inventory,
                                                         InventoryInsight insight,
                                                         ScenarioRecommendationProjection recommendation) {
        if (!insight.lowStock()) {
            return null;
        }

        return new ScenarioAlertProjection(
            AlertType.LOW_STOCK,
            insight.severity(),
            "Low stock detected for SKU " + inventory.getProduct().resolveCatalogSku()
                + " in " + inventory.getWarehouse().getCode(),
            "Available quantity has fallen below threshold in " + inventory.getWarehouse().getName() + ".",
            insight.impactSummary(),
            recommendation != null
                ? recommendation.description()
                : "Review replenishment options for this item."
        );
    }

    private ScenarioAlertProjection previewDepletionRiskAlert(Inventory inventory,
                                                              InventoryInsight insight,
                                                              ScenarioRecommendationProjection recommendation) {
        if (!insight.depletionRisk()) {
            return null;
        }

        return new ScenarioAlertProjection(
            AlertType.DEPLETION_RISK,
            insight.severity(),
            "Depletion risk rising for SKU " + inventory.getProduct().resolveCatalogSku()
                + " in " + inventory.getWarehouse().getCode(),
            "Recent order velocity is rising quickly in " + inventory.getWarehouse().getName()
                + " and may outpace the current stock buffer.",
            insight.impactSummary(),
            recommendation != null
                ? recommendation.description()
                : "Review reorder settings and prepare replenishment before stock pressure deepens."
        );
    }

    private Alert syncFulfillmentBacklogAlert(FulfillmentTask task,
                                              FulfillmentAssessment assessment,
                                              Recommendation recommendation,
                                              String source) {
        String title = "Fulfillment backlog building in " + task.getWarehouse().getCode();
        Alert existing = alertRepository.findFirstByTenant_CodeIgnoreCaseAndTypeAndStatusAndTitle(
                task.getTenant().getCode(),
                AlertType.FULFILLMENT_BACKLOG,
                AlertStatus.ACTIVE,
                title)
            .orElse(null);

        if (!assessment.backlogRisk()) {
            if (existing != null) {
                existing.setStatus(AlertStatus.RESOLVED);
                alertRepository.save(existing);
            }
            return null;
        }

        var previousSeverity = existing == null ? null : existing.getSeverity();
        var previousImpactSummary = existing == null ? null : existing.getImpactSummary();
        Alert alert = existing == null
            ? Alert.builder().tenant(task.getTenant()).type(AlertType.FULFILLMENT_BACKLOG).title(title).build()
            : existing;
        alert.setSeverity(assessment.severity());
        alert.setDescription("Dispatch backlog is building in " + task.getWarehouse().getName()
            + " with " + assessment.backlogCount() + " open fulfillment tasks and "
            + assessment.overdueDispatchCount() + " past the dispatch SLA.");
        alert.setImpactSummary(assessment.impactSummary());
        alert.setRecommendedAction(recommendation != null
            ? recommendation.getDescription()
            : "Prioritize picking and packing for the most time-sensitive orders in this warehouse.");
        alert.setPolicyExplanation(buildFulfillmentPolicyExplanation(task, assessment, "fulfillment backlog"));
        alert.setStatus(AlertStatus.ACTIVE);
        Alert saved = alertRepository.save(alert);
        if (existing == null
            || !java.util.Objects.equals(previousImpactSummary, saved.getImpactSummary())
            || previousSeverity != saved.getSeverity()) {
            businessEventService.record(
                BusinessEventType.FULFILLMENT_BACKLOG_DETECTED,
                source,
                task.getWarehouse().getCode() + " backlog rose to " + assessment.backlogCount() + " active warehouse tasks."
            );
        }
        return saved;
    }

    private Alert syncDeliveryDelayAlert(FulfillmentTask task,
                                         FulfillmentAssessment assessment,
                                         Recommendation recommendation,
                                         String source) {
        String title = "Delivery delay risk rising in " + task.getWarehouse().getCode();
        Alert existing = alertRepository.findFirstByTenant_CodeIgnoreCaseAndTypeAndStatusAndTitle(
                task.getTenant().getCode(),
                AlertType.DELIVERY_DELAY_RISK,
                AlertStatus.ACTIVE,
                title)
            .orElse(null);

        if (!assessment.deliveryDelayRisk()) {
            if (existing != null) {
                existing.setStatus(AlertStatus.RESOLVED);
                alertRepository.save(existing);
            }
            return null;
        }

        Alert alert = existing == null
            ? Alert.builder().tenant(task.getTenant()).type(AlertType.DELIVERY_DELAY_RISK).title(title).build()
            : existing;
        alert.setSeverity(assessment.severity());
        alert.setDescription("Shipment " + task.getCustomerOrder().getExternalOrderId()
            + " is outside the expected delivery lane for " + task.getWarehouse().getName() + ".");
        alert.setImpactSummary(assessment.impactSummary());
        alert.setRecommendedAction(recommendation != null
            ? recommendation.getDescription()
            : "Review the carrier lane, notify stakeholders, and escalate the delivery route.");
        alert.setPolicyExplanation(buildFulfillmentPolicyExplanation(task, assessment, "delivery delay"));
        alert.setStatus(AlertStatus.ACTIVE);
        Alert saved = alertRepository.save(alert);
        if (existing == null) {
            businessEventService.record(
                BusinessEventType.DELIVERY_DELAY_REPORTED,
                source,
                "Delivery delay pressure was raised for " + task.getWarehouse().getCode() + "."
            );
        }
        return saved;
    }

    private Alert syncFulfillmentAnomalyAlert(FulfillmentTask task,
                                              FulfillmentAssessment assessment,
                                              Recommendation recommendation,
                                              String source) {
        String title = "Logistics anomaly detected in " + task.getWarehouse().getCode();
        Alert existing = alertRepository.findFirstByTenant_CodeIgnoreCaseAndTypeAndStatusAndTitle(
                task.getTenant().getCode(),
                AlertType.FULFILLMENT_ANOMALY,
                AlertStatus.ACTIVE,
                title)
            .orElse(null);

        if (!assessment.anomalyDetected()) {
            if (existing != null) {
                existing.setStatus(AlertStatus.RESOLVED);
                alertRepository.save(existing);
            }
            return null;
        }

        Alert alert = existing == null
            ? Alert.builder().tenant(task.getTenant()).type(AlertType.FULFILLMENT_ANOMALY).title(title).build()
            : existing;
        alert.setSeverity(assessment.severity());
        alert.setDescription("Exceptions, repeated delivery delays, or stacked overdue dispatches are building in "
            + task.getWarehouse().getName() + ".");
        alert.setImpactSummary(assessment.impactSummary());
        alert.setRecommendedAction(recommendation != null
            ? recommendation.getDescription()
            : "Investigate the blocked lane, reassign the work, and review carrier or warehouse execution health.");
        alert.setPolicyExplanation(buildFulfillmentPolicyExplanation(task, assessment, "fulfillment anomaly"));
        alert.setStatus(AlertStatus.ACTIVE);
        Alert saved = alertRepository.save(alert);
        if (existing == null) {
            businessEventService.record(
                BusinessEventType.FULFILLMENT_ANOMALY_DETECTED,
                source,
                "Logistics anomaly pressure was raised for " + task.getWarehouse().getCode() + "."
            );
        }
        return saved;
    }

    private String buildInventoryPolicyExplanation(Inventory inventory,
                                                   InventoryInsight insight,
                                                   String signalName) {
        TenantOperationalPolicy policy = tenantOperationalPolicyService.getPolicy(
            inventory.getTenant() != null
                ? inventory.getTenant().getCode()
                : inventory.getWarehouse().getTenant().getCode()
        );
        return "Tenant policy raised " + signalName + " at " + insight.severity()
            + " because available stock is " + inventory.getQuantityAvailable()
            + ", reorder threshold is " + inventory.getReorderThreshold()
            + ", low-stock critical ratio is " + policy.getLowStockCriticalRatio()
            + ", depletion threshold is " + policy.getDepletionRiskHoursThreshold()
            + "h, and urgent depletion threshold is " + policy.getUrgentDepletionRiskHoursThreshold() + "h.";
    }

    private String buildFulfillmentPolicyExplanation(FulfillmentTask task,
                                                     FulfillmentAssessment assessment,
                                                     String signalName) {
        TenantOperationalPolicy policy = tenantOperationalPolicyService.getPolicy(task.getTenant().getCode());
        return "Tenant policy raised " + signalName + " at " + assessment.severity()
            + " because backlog is " + assessment.backlogCount()
            + " (risk " + policy.getBacklogRiskCount() + ", critical " + policy.getBacklogCriticalCount()
            + "), delayed shipments are " + assessment.delayedShipmentCount()
            + " (threshold " + policy.getDelayedShipmentCountThreshold()
            + "), and overdue dispatch count is " + assessment.overdueDispatchCount()
            + " (threshold " + policy.getOverdueDispatchCountThreshold() + ").";
    }
}
