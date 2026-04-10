package com.synapsecore.domain.dto;

public record WarehouseResponse(
    Long id,
    String code,
    String name,
    String location
) {
}
