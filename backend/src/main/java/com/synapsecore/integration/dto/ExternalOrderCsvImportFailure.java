package com.synapsecore.integration.dto;

import java.util.List;

public record ExternalOrderCsvImportFailure(
    String sourceSystem,
    String externalOrderId,
    String warehouseCode,
    List<Integer> rowNumbers,
    com.synapsecore.integration.IntegrationFailureCode failureCode,
    String message
) {
}
