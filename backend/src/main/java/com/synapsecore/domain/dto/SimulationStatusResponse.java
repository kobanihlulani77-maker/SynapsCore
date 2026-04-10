package com.synapsecore.domain.dto;

import java.time.Instant;

public record SimulationStatusResponse(
    boolean active,
    Instant updatedAt
) {
}
