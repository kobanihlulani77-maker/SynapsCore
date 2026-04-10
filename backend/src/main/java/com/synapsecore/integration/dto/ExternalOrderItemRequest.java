package com.synapsecore.integration.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record ExternalOrderItemRequest(
    @NotBlank(message = "productSku is required")
    String productSku,

    @NotNull(message = "quantity is required")
    @Positive(message = "quantity must be greater than zero")
    Integer quantity,

    @NotNull(message = "unitPrice is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "unitPrice must be greater than zero")
    BigDecimal unitPrice
) {
}
