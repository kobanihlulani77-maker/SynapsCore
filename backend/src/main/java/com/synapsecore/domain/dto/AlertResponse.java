package com.synapsecore.domain.dto;

import com.synapsecore.domain.entity.AlertSeverity;
import com.synapsecore.domain.entity.AlertStatus;
import com.synapsecore.domain.entity.AlertType;
import java.time.Instant;

public record AlertResponse(
    Long id,
    AlertType type,
    AlertSeverity severity,
    String title,
    String description,
    String impactSummary,
    String recommendedAction,
    AlertStatus status,
    Instant createdAt
) {
}
