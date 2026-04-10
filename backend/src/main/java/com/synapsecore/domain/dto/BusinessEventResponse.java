package com.synapsecore.domain.dto;

import com.synapsecore.domain.entity.BusinessEventType;
import java.time.Instant;

public record BusinessEventResponse(
    Long id,
    BusinessEventType eventType,
    String source,
    String payloadSummary,
    Instant createdAt
) {
}
