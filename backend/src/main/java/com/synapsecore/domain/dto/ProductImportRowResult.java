package com.synapsecore.domain.dto;

public record ProductImportRowResult(
    int rowNumber,
    String sku,
    String status,
    String message,
    ProductResponse product
) {
}
