package com.synapsecore.access.dto;

public record TenantWorkspaceSecuritySettings(
    int passwordRotationDays,
    int sessionTimeoutMinutes,
    long securityPolicyVersion
) {
}
