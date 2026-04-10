package com.synapsecore.domain.dto;

import java.time.Instant;

public record SystemBackboneSummary(
    long pendingDispatchCount,
    long failedDispatchCount,
    Long oldestPendingAgeSeconds,
    Instant latestProcessedAt,
    long dispatchIntervalMs,
    int batchSize
) {
}
