package com.synapsecore.domain.dto;

import com.synapsecore.domain.entity.FulfillmentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record FulfillmentUpdateRequest(
    @NotBlank @Size(max = 80) String externalOrderId,
    @NotNull FulfillmentStatus status,
    Integer fulfilledUnits,
    @Size(max = 80) String carrier,
    @Size(max = 120) String trackingReference,
    Instant promisedDispatchAt,
    Instant expectedDeliveryAt,
    Instant occurredAt,
    @Size(max = 512) String note
) {
}
