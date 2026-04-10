package com.synapsecore.domain.dto;

import com.synapsecore.domain.entity.FulfillmentStatus;
import com.synapsecore.domain.entity.OrderStatus;
import java.time.Instant;

public record FulfillmentStatusResponse(
    Long id,
    String externalOrderId,
    OrderStatus orderStatus,
    FulfillmentStatus fulfillmentStatus,
    String warehouseCode,
    String warehouseName,
    int itemCount,
    String carrier,
    String trackingReference,
    Instant queuedAt,
    Instant promisedDispatchAt,
    Instant expectedDeliveryAt,
    Instant dispatchedAt,
    Instant deliveredAt,
    double backlogGrowthPerHour,
    Double estimatedBacklogClearHours,
    Double hoursUntilDispatchDue,
    Double hoursUntilDeliveryDue,
    boolean backlogRisk,
    boolean deliveryDelayRisk,
    boolean anomalyDetected,
    String riskLevel,
    String impactSummary,
    Instant updatedAt
) {
}
