package com.synapsecore.platform.dto;

import java.time.Instant;

public record PlatformTenantSummary(
    Long id,
    String code,
    String name,
    boolean active,
    long activeUserCount,
    long activeOperatorCount,
    long connectorCount,
    long disabledConnectorCount,
    long failedInboundCount,
    long replayAttentionCount,
    long activeAlertCount,
    String supportState,
    Instant updatedAt
) {
}
