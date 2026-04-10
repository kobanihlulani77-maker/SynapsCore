package com.synapsecore.integration.dto;

import com.synapsecore.domain.dto.OrderResponse;
import java.time.Instant;

public record ExternalOrderWebhookResponse(
    String sourceSystem,
    String ingestionSource,
    Instant acceptedAt,
    OrderResponse order
) {
}
