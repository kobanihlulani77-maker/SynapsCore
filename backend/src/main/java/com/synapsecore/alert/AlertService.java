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
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRepository alertRepository;
    private final AlertConditionLockService alertConditionLockService;
    private final BusinessEventService businessEventService;
    private final TenantOperationalPolicyService tenantOperationalPolicyService;

    @Transactional
    public void syncInventoryAlerts(Inventory inventory,
                                    InventoryInsight insight,
                                    StockPrediction prediction,
                                    Recommendation recommendation,
                                    String source) {
        syncLowStockAlert(inventory, insight, recommendation, source);
        syncDepletionRiskAlert(inventory, insight, recommendation);
    }

    @Transactional
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
        String conditionKey = buildInventoryConditionKey(AlertType.LOW_STOCK, inventory);
        return alertConditionLockService.withLock(conditionKey,
            () -> syncLowStockAlertLocked(inventory, insight, recommendation, source, conditionKey));
    }

    private Alert syncLowStockAlertLocked(Inventory inventory,
                                    InventoryInsight insight,
                                    Recommendation recommendation,
                                    String source,
                                    String conditionKey) {
        // MVP lifecycle choice:
        // - keep at most one ACTIVE low-stock alert per SKU/warehouse pair
        // - refresh that alert while the low-stock condition persists
        // - mark it RESOLVED once stock rises above threshold
        String title = "Low stock detected for SKU " + inventory.getProduct().resolveCatalogSku()
            + " in " + inventory.getWarehouse().getCode();
        String tenantCode = inventory.getWarehouse().getTenant() == null ? null : inventory.getWarehouse().getTenant().getCode();
        Alert existing = alertRepository.findFirstByTenant_CodeIgnoreCaseAndTypeAndStatusAndConditionKey(
                tenantCode,
                AlertType.LOW_STOCK,
                AlertStatus.ACTIVE,
                conditionKey)
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
        setInventoryIdentity(alert, inventory, AlertType.LOW_STOCK, conditionKey);
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
        String conditionKey = buildInventoryConditionKey(AlertType.DEPLETION_RISK, inventory);
        return alertConditionLockService.withLock(conditionKey,
            () -> syncDepletionRiskAlertLocked(inventory, insight, recommendation, conditionKey));
    }

    private Alert syncDepletionRiskAlertLocked(Inventory inventory,
                                         InventoryInsight insight,
                                         Recommendation recommendation,
                                         String conditionKey) {
        String title = "Depletion risk rising for SKU " + inventory.getProduct().resolveCatalogSku()
            + " in " + inventory.getWarehouse().getCode();
        String tenantCode = inventory.getWarehouse().getTenant() == null ? null : inventory.getWarehouse().getTenant().getCode();
        Alert existing = alertRepository.findFirstByTenant_CodeIgnoreCaseAndTypeAndStatusAndConditionKey(
                tenantCode,
                AlertType.DEPLETION_RISK,
                AlertStatus.ACTIVE,
                conditionKey)
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
        setInventoryIdentity(alert, inventory, AlertType.DEPLETION_RISK, conditionKey);
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
        String conditionKey = buildFulfillmentConditionKey(AlertType.FULFILLMENT_BACKLOG, task);
        return alertConditionLockService.withLock(conditionKey,
            () -> syncFulfillmentBacklogAlertLocked(task, assessment, recommendation, source, conditionKey));
    }

    private Alert syncFulfillmentBacklogAlertLocked(FulfillmentTask task,
                                              FulfillmentAssessment assessment,
                                              Recommendation recommendation,
                                              String source,
                                              String conditionKey) {
        String title = "Fulfillment backlog building in " + task.getWarehouse().getCode();
        Alert existing = alertRepository.findFirstByTenant_CodeIgnoreCaseAndTypeAndStatusAndConditionKey(
                task.getTenant().getCode(),
                AlertType.FULFILLMENT_BACKLOG,
                AlertStatus.ACTIVE,
                conditionKey)
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
        setFulfillmentIdentity(alert, task, AlertType.FULFILLMENT_BACKLOG, conditionKey);
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
        String conditionKey = buildFulfillmentConditionKey(AlertType.DELIVERY_DELAY_RISK, task);
        return alertConditionLockService.withLock(conditionKey,
            () -> syncDeliveryDelayAlertLocked(task, assessment, recommendation, source, conditionKey));
    }

    private Alert syncDeliveryDelayAlertLocked(FulfillmentTask task,
                                         FulfillmentAssessment assessment,
                                         Recommendation recommendation,
                                         String source,
                                         String conditionKey) {
        String title = "Delivery delay risk rising in " + task.getWarehouse().getCode();
        Alert existing = alertRepository.findFirstByTenant_CodeIgnoreCaseAndTypeAndStatusAndConditionKey(
                task.getTenant().getCode(),
                AlertType.DELIVERY_DELAY_RISK,
                AlertStatus.ACTIVE,
                conditionKey)
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
        setFulfillmentIdentity(alert, task, AlertType.DELIVERY_DELAY_RISK, conditionKey);
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
        String conditionKey = buildFulfillmentConditionKey(AlertType.FULFILLMENT_ANOMALY, task);
        return alertConditionLockService.withLock(conditionKey,
            () -> syncFulfillmentAnomalyAlertLocked(task, assessment, recommendation, source, conditionKey));
    }

    private Alert syncFulfillmentAnomalyAlertLocked(FulfillmentTask task,
                                              FulfillmentAssessment assessment,
                                              Recommendation recommendation,
                                              String source,
                                              String conditionKey) {
        String title = "Logistics anomaly detected in " + task.getWarehouse().getCode();
        Alert existing = alertRepository.findFirstByTenant_CodeIgnoreCaseAndTypeAndStatusAndConditionKey(
                task.getTenant().getCode(),
                AlertType.FULFILLMENT_ANOMALY,
                AlertStatus.ACTIVE,
                conditionKey)
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
        setFulfillmentIdentity(alert, task, AlertType.FULFILLMENT_ANOMALY, conditionKey);
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

    private void setInventoryIdentity(Alert alert,
                                      Inventory inventory,
                                      AlertType type,
                                      String conditionKey) {
        alert.setTenant(inventory.getWarehouse().getTenant());
        alert.setWarehouse(inventory.getWarehouse());
        alert.setProduct(inventory.getProduct());
        alert.setSourceType("INVENTORY_PRODUCT_WAREHOUSE");
        alert.setSourceRef(inventory.getProduct().getId() + "@" + inventory.getWarehouse().getId());
        alert.setConditionKey(conditionKey);
        alert.setType(type);
    }

    private void setFulfillmentIdentity(Alert alert,
                                        FulfillmentTask task,
                                        AlertType type,
                                        String conditionKey) {
        alert.setTenant(task.getTenant());
        alert.setWarehouse(task.getWarehouse());
        alert.setProduct(null);
        alert.setSourceType("FULFILLMENT_WAREHOUSE");
        alert.setSourceRef(task.getWarehouse().getCode());
        alert.setConditionKey(conditionKey);
        alert.setType(type);
    }

    private String buildInventoryConditionKey(AlertType type, Inventory inventory) {
        return type.name() + "|PRODUCT:" + inventory.getProduct().getId()
            + "|WAREHOUSE:" + inventory.getWarehouse().getId();
    }

    private String buildFulfillmentConditionKey(AlertType type, FulfillmentTask task) {
        return type.name() + "|WAREHOUSE:" + task.getWarehouse().getId();
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
