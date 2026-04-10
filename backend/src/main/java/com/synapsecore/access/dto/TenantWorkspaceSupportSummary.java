package com.synapsecore.access.dto;

public record TenantWorkspaceSupportSummary(
    long warehouseCount,
    long activeOperatorCount,
    long inactiveOperatorCount,
    long activeUserCount,
    long inactiveUserCount,
    long enabledConnectorCount,
    long disabledConnectorCount,
    long replayQueueDepth,
    long pendingApprovalCount,
    long activeIncidentCount
) {
}
