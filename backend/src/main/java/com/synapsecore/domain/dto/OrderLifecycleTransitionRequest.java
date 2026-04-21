package com.synapsecore.domain.dto;

import com.synapsecore.domain.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record OrderLifecycleTransitionRequest(
    @NotNull OrderStatus status,
    @Size(max = 320) String note,
    Boolean restockInventory
) {
}
