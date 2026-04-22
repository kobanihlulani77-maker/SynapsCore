package com.synapsecore.domain.dto;

import java.util.List;

public record ProductImportResponse(
    int totalRows,
    int created,
    int updated,
    int failed,
    List<ProductImportRowResult> rows
) {
}
