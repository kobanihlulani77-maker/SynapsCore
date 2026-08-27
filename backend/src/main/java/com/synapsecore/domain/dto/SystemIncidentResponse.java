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
    Instant createdAt,
    String warehouseCode
) {

    public SystemIncidentResponse(String incidentKey,
                                  SystemIncidentType type,
                                  SystemIncidentSeverity severity,
                                  String title,
                                  String detail,
                                  String context,
                                  boolean actionRequired,
                                  Instant createdAt) {
        this(incidentKey, type, severity, title, detail, context, actionRequired, createdAt, null);
    }
}
