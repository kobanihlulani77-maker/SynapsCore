package com.synapsecore.domain.dto;

import java.time.Instant;

public record SystemBackboneSummary(
    String realtimeBrokerMode,
    String realtimeBrokerDetail,
    boolean realtimeSingleNodeOnly,
    boolean realtimeExternalBrokerConfigured,
    boolean realtimeDistributedMode,
    boolean realtimeRedisPubSubConfigured,
    boolean realtimeStompRelayConfigured,
    long pendingDispatchCount,
    long failedDispatchCount,
    Long oldestPendingAgeSeconds,
    Instant latestProcessedAt,
    long dispatchIntervalMs,
    int batchSize
) {
}
