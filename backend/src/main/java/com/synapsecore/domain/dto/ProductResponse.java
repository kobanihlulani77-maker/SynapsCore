package com.synapsecore.domain.dto;

public record ProductResponse(
    Long id,
    String sku,
    String catalogSku,
    String internalSku,
    String name,
    String category,
    String tenantCode
) {
}
