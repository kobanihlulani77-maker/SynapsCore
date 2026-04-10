package com.synapsecore.fulfillment;

import com.synapsecore.domain.entity.AlertSeverity;

public record FulfillmentAssessment(
    long backlogCount,
    long overdueDispatchCount,
    long delayedShipmentCount,
    double backlogGrowthPerHour,
    Double estimatedBacklogClearHours,
    Double hoursUntilDispatchDue,
    Double hoursUntilDeliveryDue,
    boolean backlogRisk,
    boolean deliveryDelayRisk,
    boolean anomalyDetected,
    AlertSeverity severity,
    String riskLevel,
    String impactSummary
) {
}
