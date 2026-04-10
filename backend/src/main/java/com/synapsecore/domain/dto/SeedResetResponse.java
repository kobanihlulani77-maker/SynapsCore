package com.synapsecore.domain.dto;

import java.time.Instant;

public record SeedResetResponse(
    String status,
    long productsSeeded,
    long warehousesSeeded,
    long inventoryRecordsSeeded,
    SimulationStatusResponse simulation,
    Instant reseededAt
) {
}
