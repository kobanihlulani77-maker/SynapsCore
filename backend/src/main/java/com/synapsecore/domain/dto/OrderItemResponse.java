package com.synapsecore.domain.dto;

import java.math.BigDecimal;

public record OrderItemResponse(
    String productSku,
    String productName,
    Integer quantity,
    Integer reservedQuantity,
    Integer fulfilledQuantity,
    Integer cancelledQuantity,
    Integer returnedQuantity,
    BigDecimal unitPrice
) {
}
