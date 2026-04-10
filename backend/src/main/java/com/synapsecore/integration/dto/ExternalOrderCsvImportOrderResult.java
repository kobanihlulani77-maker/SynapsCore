package com.synapsecore.integration.dto;

import com.synapsecore.domain.dto.OrderResponse;
import java.time.Instant;

public record ExternalOrderCsvImportOrderResult(
    String sourceSystem,
    String ingestionSource,
    String externalOrderId,
    String warehouseCode,
    int lineItemCount,
    Instant acceptedAt,
    OrderResponse order
) {
}
