package com.synapsecore.access.dto;

import com.synapsecore.access.SynapseAccessRole;
import java.time.Instant;
import java.util.List;

public record AccessUserResponse(
    Long id,
    String tenantCode,
    String tenantName,
    String username,
    String fullName,
    String operatorActorName,
    String operatorDisplayName,
    List<SynapseAccessRole> roles,
    List<String> warehouseScopes,
    boolean active,
    boolean passwordChangeRequired,
    Instant passwordUpdatedAt,
    Instant createdAt,
    Instant updatedAt
) {
}
