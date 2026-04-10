package com.synapsecore.domain.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record OrderItemRequest(
    @NotBlank @Size(max = 64) String productSku,
    @NotNull @Min(1) Integer quantity,
    @NotNull @DecimalMin("0.01") @Digits(integer = 12, fraction = 2) BigDecimal unitPrice
) {
}
