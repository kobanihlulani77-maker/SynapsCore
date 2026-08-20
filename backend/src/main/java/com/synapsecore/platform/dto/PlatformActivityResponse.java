package com.synapsecore.platform.dto;

import java.time.Instant;

public record PlatformActivityResponse(
    String tenantCode,
    String category,
    String condition,
    String status,
    Instant observedAt
) {
}
