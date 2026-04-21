package com.synapsecore.domain.dto;

import java.time.Instant;

public record SystemDiagnosticsSummary(
    long windowHours,
    long businessEventsInWindow,
    long orderEventsInWindow,
    long inventorySignalsInWindow,
    long integrationEventsInWindow,
    long scenarioEventsInWindow,
    long failureAuditsInWindow,
    long activeIncidentCount,
    Instant latestBusinessEventAt,
    Instant latestFailureAt
) {
}
