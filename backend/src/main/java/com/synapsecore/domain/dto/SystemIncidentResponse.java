package com.synapsecore.domain.dto;

import java.time.Instant;

public record SystemIncidentResponse(
    String incidentKey,
    SystemIncidentType type,
    SystemIncidentSeverity severity,
    String title,
    String detail,
    String context,
    boolean actionRequired,
    Instant createdAt
) {
}
