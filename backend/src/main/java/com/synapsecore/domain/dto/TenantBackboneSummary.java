package com.synapsecore.domain.dto;

import java.time.Instant;

public record TenantBackboneSummary(
    String realtimeBrokerMode,
    String realtimeBrokerDetail,
    long pendingDispatchCount,
    long failedDispatchCount,
    Long oldestPendingAgeSeconds,
    Instant latestProcessedAt
) {
}
