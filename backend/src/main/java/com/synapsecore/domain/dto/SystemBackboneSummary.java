package com.synapsecore.domain.dto;

import java.time.Instant;

public record SystemBackboneSummary(
    String realtimeBrokerMode,
    String realtimeBrokerDetail,
    long pendingDispatchCount,
    long failedDispatchCount,
    Long oldestPendingAgeSeconds,
    Instant latestProcessedAt,
    long dispatchIntervalMs,
    int batchSize
) {
}
