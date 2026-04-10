package com.synapsecore.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record OrderCreateRequest(
    @Size(max = 80) String externalOrderId,
    @NotBlank @Size(max = 40) String warehouseCode,
    @NotEmpty List<@Valid OrderItemRequest> items
) {
}
