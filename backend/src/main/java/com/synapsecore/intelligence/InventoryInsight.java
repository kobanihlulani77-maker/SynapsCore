package com.synapsecore.intelligence;

import com.synapsecore.domain.entity.AlertSeverity;

public record InventoryInsight(
    boolean lowStock,
    boolean depletionRisk,
    boolean elevatedUrgency,
    boolean rapidConsumption,
    AlertSeverity severity,
    String riskLevel,
    String impactSummary
) {
}
