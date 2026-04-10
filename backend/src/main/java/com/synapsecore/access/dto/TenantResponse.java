package com.synapsecore.access.dto;

import java.time.Instant;

public record TenantResponse(
    Long id,
    String code,
    String name,
    String description,
    boolean active,
    Instant createdAt,
    Instant updatedAt
) {
}
