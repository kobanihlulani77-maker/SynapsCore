package com.synapsecore.domain.dto;

import com.synapsecore.domain.entity.AuditStatus;
import java.time.Instant;

public record AuditLogResponse(
    Long id,
    String action,
    String actor,
    String source,
    String targetType,
    String targetRef,
    AuditStatus status,
    String details,
    String requestId,
    Instant createdAt
) {
}
