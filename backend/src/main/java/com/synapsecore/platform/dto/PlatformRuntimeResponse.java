package com.synapsecore.platform.dto;

import com.synapsecore.domain.dto.SystemBuildInfo;
import java.time.Instant;
import java.util.List;

public record PlatformRuntimeResponse(
    String applicationName,
    SystemBuildInfo build,
    List<String> activeProfiles,
    String overallStatus,
    String livenessState,
    String readinessState,
    boolean secureSessionCookies,
    String realtimeBrokerMode,
    boolean realtimeDistributedMode,
    boolean realtimeRedisPubSubConfigured,
    boolean realtimeStompRelayConfigured,
    boolean alertHookConfigured,
    long pendingDispatchCount,
    long failedDispatchCount,
    Instant observedAt
) {
}
