package com.synapsecore.domain.dto;

public record SystemMetricsSummary(
    double ordersIngested,
    double fulfillmentUpdates,
    double integrationImportRuns,
    double integrationFailures,
    double replayAttempts,
    double replayFailures,
    double authFailures,
    double tenantOperations,
    double catalogWrites,
    double inventoryLockConflicts,
    double realtimePublishes,
    double realtimePublishFailures,
    double rateLimitRejections,
    double alertHookDeliveries,
    double alertHookFailures,
    double dispatchQueued,
    double dispatchProcessed,
    double dispatchFailures,
    double httpRequests,
    double failedHttpRequests,
    double averageHttpRequestLatencyMs
) {
}
