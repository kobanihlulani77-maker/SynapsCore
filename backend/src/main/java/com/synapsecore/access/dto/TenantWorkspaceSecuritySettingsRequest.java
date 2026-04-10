package com.synapsecore.access.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record TenantWorkspaceSecuritySettingsRequest(
    @Min(7)
    @Max(365)
    int passwordRotationDays,

    @Min(15)
    @Max(1440)
    int sessionTimeoutMinutes,

    boolean invalidateOtherSessions
) {
}
