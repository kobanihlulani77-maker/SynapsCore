package com.synapsecore.auth.dto;

import com.synapsecore.access.SynapseAccessRole;
import java.time.Instant;
import java.util.List;

public record AuthSessionResponse(
    boolean signedIn,
    String tenantCode,
    String tenantName,
    String username,
    String actorName,
    String displayName,
    List<SynapseAccessRole> roles,
    List<String> warehouseScopes,
    Instant authenticatedAt,
    Instant sessionExpiresAt,
    Instant passwordExpiresAt,
    boolean passwordChangeRequired,
    boolean passwordRotationRequired,
    Integer sessionTimeoutMinutes,
    Integer passwordRotationDays
) {
}
