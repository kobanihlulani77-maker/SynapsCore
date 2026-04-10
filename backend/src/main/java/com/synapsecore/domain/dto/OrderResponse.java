package com.synapsecore.domain.dto;

import com.synapsecore.domain.entity.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
    Long id,
    String externalOrderId,
    OrderStatus status,
    String warehouseCode,
    String warehouseName,
    BigDecimal totalAmount,
    Integer itemCount,
    Instant createdAt,
    List<OrderItemResponse> items
) {
}
