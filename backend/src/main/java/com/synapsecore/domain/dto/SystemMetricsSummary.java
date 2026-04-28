package com.synapsecore.domain.dto;

public record SystemMetricsSummary(
    double ordersIngested,
    double fulfillmentUpdates,
    double integrationImportRuns,
    double replayAttempts,
    double authFailures,
    double tenantOperations,
    double catalogWrites,
    double inventoryLockConflicts,
    double realtimePublishes,
    double rateLimitRejections,
    double dispatchQueued,
    double dispatchProcessed,
    double dispatchFailures,
    double httpRequests,
    double failedHttpRequests,
    double averageHttpRequestLatencyMs
) {
}
