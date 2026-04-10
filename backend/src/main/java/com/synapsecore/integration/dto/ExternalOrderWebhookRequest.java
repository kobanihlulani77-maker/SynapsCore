package com.synapsecore.integration.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import java.time.Instant;
import java.util.List;

public record ExternalOrderWebhookRequest(
    @NotBlank(message = "sourceSystem is required")
    @Pattern(
        regexp = "^[A-Za-z0-9_-]+$",
        message = "sourceSystem may only contain letters, numbers, hyphens, and underscores"
    )
    String sourceSystem,

    @NotBlank(message = "externalOrderId is required")
    String externalOrderId,

    String warehouseCode,

    String customerReference,

    Instant occurredAt,

    @NotEmpty(message = "items must contain at least one entry")
    List<@Valid ExternalOrderItemRequest> items
) {
}
