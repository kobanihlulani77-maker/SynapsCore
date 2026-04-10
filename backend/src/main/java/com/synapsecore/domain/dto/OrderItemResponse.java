package com.synapsecore.domain.dto;

import java.math.BigDecimal;

public record OrderItemResponse(
    String productSku,
    String productName,
    Integer quantity,
    BigDecimal unitPrice
) {
}
