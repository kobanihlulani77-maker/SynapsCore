package com.synapsecore.access.dto;

import com.synapsecore.domain.entity.AuditStatus;
import java.time.Instant;

public record TenantWorkspaceSupportActivity(
    Long id,
    String category,
    String action,
    String title,
    String actor,
    AuditStatus status,
    String targetRef,
    String details,
    String requestId,
    Instant createdAt
) {
}
