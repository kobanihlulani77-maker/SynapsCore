package com.synapsecore.access.dto;

import com.synapsecore.access.SynapseAccessRole;
import java.time.Instant;
import java.util.List;

public record AccessOperatorResponse(
    Long id,
    String tenantCode,
    String tenantName,
    String actorName,
    String displayName,
    List<SynapseAccessRole> roles,
    List<String> warehouseScopes,
    boolean active,
    String description,
    Instant createdAt,
    Instant updatedAt
) {
}
