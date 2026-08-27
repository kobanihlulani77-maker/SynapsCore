package com.synapsecore.platform.dto;

import java.time.Instant;

public record PlatformActivityResponse(
    String tenantCode,
    String category,
    String condition,
    String status,
    Instant observedAt,
    String scope,
    String classification,
    String impact,
    String severity,
    String interpretation,
    String nextAction
) {
}
