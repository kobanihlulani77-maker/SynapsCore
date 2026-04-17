package com.synapsecore.access.dto;

import java.time.Instant;
import java.util.List;

public record TenantOnboardingResponse(
    Long tenantId,
    String tenantCode,
    String tenantName,
    String adminUsername,
    String adminActorName,
    String executiveUsername,
    String executiveActorName,
    List<String> starterWarehouseCodes,
    Instant createdAt
) {
}
