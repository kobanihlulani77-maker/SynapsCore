package com.synapsecore.domain.dto;

public record SystemMetricsSummary(
    double ordersIngested,
    double fulfillmentUpdates,
    double integrationImportRuns,
    double replayAttempts,
    double dispatchQueued,
    double dispatchProcessed,
    double dispatchFailures
) {
}
