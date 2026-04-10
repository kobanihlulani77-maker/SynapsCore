package com.synapsecore.domain.dto;

public record ProductResponse(
    Long id,
    String sku,
    String name,
    String category
) {
}
