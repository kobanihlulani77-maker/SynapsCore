package com.synapsecore.domain.service;

import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class CatalogWriteConflictResolver {

    public ResponseStatusException toResponseStatus(DataIntegrityViolationException exception, String catalogSku) {
        return new ResponseStatusException(HttpStatus.CONFLICT, describe(exception, catalogSku));
    }

    public String describe(DataIntegrityViolationException exception, String catalogSku) {
        String skuLabel = normalizeSkuLabel(catalogSku);
        String rootMessage = exception.getMostSpecificCause() == null
            ? exception.getMessage()
            : exception.getMostSpecificCause().getMessage();
        String normalizedMessage = rootMessage == null ? "" : rootMessage.toLowerCase(Locale.ROOT);

        if (normalizedMessage.contains("products") && (normalizedMessage.contains("sku") || normalizedMessage.contains("catalog_sku"))) {
            return skuLabel == null
                ? "Product catalog write conflicted with an existing tenant-visible or legacy hidden SKU."
                : "Product SKU already exists for this tenant or a hidden legacy catalog row still occupies " + skuLabel + ".";
        }
        if (normalizedMessage.contains("business_events")) {
            return skuLabel == null
                ? "Product catalog write rolled back while recording the business event stream. Repair business_events identity state before hosted proof can continue."
                : "Catalog write for " + skuLabel + " rolled back while recording the business event stream. Repair business_events identity state before hosted proof can continue.";
        }
        if (normalizedMessage.contains("audit_logs")) {
            return skuLabel == null
                ? "Product catalog write rolled back while recording the audit trail. Repair audit_logs identity state before hosted proof can continue."
                : "Catalog write for " + skuLabel + " rolled back while recording the audit trail. Repair audit_logs identity state before hosted proof can continue.";
        }
        if (normalizedMessage.contains("operational_dispatch_work_items")) {
            return skuLabel == null
                ? "Product catalog write rolled back while enqueuing operational dispatch work. Repair operational_dispatch_work_items identity state before hosted proof can continue."
                : "Catalog write for " + skuLabel + " rolled back while enqueuing operational dispatch work. Repair operational_dispatch_work_items identity state before hosted proof can continue.";
        }
        return skuLabel == null
            ? "Product catalog write conflicted with current production database state and did not commit."
            : "Catalog write for " + skuLabel + " conflicted with current production database state and did not commit.";
    }

    private String normalizeSkuLabel(String catalogSku) {
        if (catalogSku == null || catalogSku.isBlank()) {
            return null;
        }
        return catalogSku.trim().toUpperCase(Locale.ROOT);
    }
}
