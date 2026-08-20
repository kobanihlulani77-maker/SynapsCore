package com.synapsecore.platform.dto;

import java.time.Instant;
import java.util.List;

public record PlatformOverviewResponse(
    PlatformRuntimeResponse runtime,
    List<PlatformTenantSummary> tenants,
    List<PlatformActivityResponse> activity,
    Instant observedAt
) {
}
