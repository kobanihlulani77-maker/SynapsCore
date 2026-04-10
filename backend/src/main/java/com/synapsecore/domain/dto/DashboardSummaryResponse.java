package com.synapsecore.domain.dto;

import java.time.Instant;

public record DashboardSummaryResponse(
    long totalOrders,
    long activeAlerts,
    long lowStockItems,
    long recommendationsCount,
    long fulfillmentBacklogCount,
    long delayedShipmentCount,
    long fulfillmentRiskCount,
    long totalProducts,
    long totalWarehouses,
    long recentOrderCount,
    long inventoryRecordsCount,
    boolean simulationRunning,
    Instant lastUpdatedAt
) {
}
