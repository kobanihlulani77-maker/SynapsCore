package com.synapsecore.domain.dto;

import java.time.Instant;
import java.util.List;

public record FulfillmentOverviewResponse(
    long backlogCount,
    long overdueDispatchCount,
    long delayedShipmentCount,
    long atRiskCount,
    List<FulfillmentStatusResponse> activeFulfillments,
    Instant generatedAt
) {
}
