package com.synapsecore.intelligence;

import com.synapsecore.domain.entity.AlertSeverity;
import com.synapsecore.domain.entity.Inventory;
import com.synapsecore.domain.service.TenantOperationalPolicyService;
import com.synapsecore.prediction.StockPrediction;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InventoryIntelligenceService {

    private final TenantOperationalPolicyService tenantOperationalPolicyService;

    public InventoryInsight evaluate(Inventory inventory, StockPrediction prediction) {
        var policy = tenantOperationalPolicyService.getPolicy(
            inventory.getTenant() != null
                ? inventory.getTenant().getCode()
                : inventory.getWarehouse().getTenant().getCode()
        );
        boolean lowStock = inventory.getQuantityAvailable() <= inventory.getReorderThreshold();
        boolean depletionRisk = !lowStock && prediction.depletionRisk();
        boolean criticalQuantity = inventory.getQuantityAvailable() == 0
            || inventory.getQuantityAvailable() <= Math.max(1, Math.round(inventory.getReorderThreshold() * policy.getLowStockCriticalRatio()));
        boolean elevatedUrgency = (lowStock && criticalQuantity) || prediction.urgentRisk();

        AlertSeverity severity;
        if (lowStock && elevatedUrgency) {
            severity = policy.getLowStockCriticalSeverity();
        } else if (lowStock || depletionRisk || prediction.urgentRisk()) {
            severity = prediction.urgentRisk()
                ? policy.getUrgentDepletionRiskSeverity()
                : lowStock ? policy.getLowStockSeverity() : policy.getDepletionRiskSeverity();
        } else {
            severity = AlertSeverity.MEDIUM;
        }

        String riskLevel;
        if (lowStock && elevatedUrgency) {
            riskLevel = "critical";
        } else if (lowStock || depletionRisk) {
            riskLevel = "high";
        } else {
            riskLevel = "stable";
        }

        String impactSummary;
        if (lowStock && prediction.hoursToStockout() != null) {
            impactSummary = "Continued order flow may cause stockout within "
                + String.format(Locale.US, "%.1f", prediction.hoursToStockout())
                + " hours.";
        } else if (depletionRisk && prediction.hoursToStockout() != null) {
            impactSummary = "Demand is accelerating and may exhaust current stock within "
                + String.format(Locale.US, "%.1f", prediction.hoursToStockout())
                + " hours even before the reorder threshold is crossed.";
        } else if (lowStock) {
            impactSummary = "Available stock is below the configured threshold and needs replenishment attention.";
        } else {
            impactSummary = "Inventory is operating within the current threshold.";
        }

        return new InventoryInsight(
            lowStock,
            depletionRisk,
            elevatedUrgency,
            prediction.rapidConsumption(),
            severity,
            riskLevel,
            impactSummary
        );
    }
}
