package com.synapsecore.domain.dto;

public record SystemTelemetrySummary(
    long disabledConnectorCount,
    long replayQueueDepth,
    long recentImportIssues,
    long recentAuditFailures,
    long activeAlertCount,
    long fulfillmentBacklogCount,
    long delayedFulfillmentCount,
    long dispatchQueueDepth,
    long failedDispatchCount
) {
}
