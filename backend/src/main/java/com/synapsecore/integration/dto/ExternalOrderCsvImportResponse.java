package com.synapsecore.integration.dto;

import java.time.Instant;
import java.util.List;

public record ExternalOrderCsvImportResponse(
    String sourceSystemDefault,
    int rowsReceived,
    int ordersImported,
    int ordersFailed,
    Instant processedAt,
    List<ExternalOrderCsvImportOrderResult> importedOrders,
    List<ExternalOrderCsvImportFailure> failedOrders
) {
}
