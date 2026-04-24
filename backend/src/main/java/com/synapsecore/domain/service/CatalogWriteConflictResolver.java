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
        return describeThrowable(exception, catalogSku);
    }

    public String describe(Throwable exception, String catalogSku) {
        return describeThrowable(exception, catalogSku);
    }

    private String describeThrowable(Throwable exception, String catalogSku) {
        String skuLabel = normalizeSkuLabel(catalogSku);
        Throwable rootCause = exception;
        while (rootCause != null && rootCause.getCause() != null && rootCause.getCause() != rootCause) {
            rootCause = rootCause.getCause();
        }
        String rootMessage = rootCause == null ? exception.getMessage() : rootCause.getMessage();
        String normalizedMessage = rootMessage == null ? "" : rootMessage.toLowerCase(Locale.ROOT);

        if (normalizedMessage.contains("products") && (normalizedMessage.contains("sku") || normalizedMessage.contains("catalog_sku"))) {
            return skuLabel == null
                ? "Product catalog write conflicted with an existing tenant-visible or legacy hidden SKU."
                : "Product SKU already exists for this tenant or a hidden legacy catalog row still occupies " + skuLabel + ".";
        }
        if (normalizedMessage.contains("products") && (normalizedMessage.contains("tenant_id") || normalizedMessage.contains("catalog_sku"))) {
            return skuLabel == null
                ? "Product catalog write failed because the live products table is not aligned with the current tenant-owned catalog schema."
                : "Catalog write for " + skuLabel + " failed because the live products table is not aligned with the current tenant-owned catalog schema.";
        }
        if (normalizedMessage.contains("business_events")) {
            return skuLabel == null
                ? "Product catalog write rolled back while recording the business event stream. Repair or align the business_events table before hosted proof can continue."
                : "Catalog write for " + skuLabel + " rolled back while recording the business event stream. Repair or align the business_events table before hosted proof can continue.";
        }
        if (normalizedMessage.contains("audit_logs")) {
            return skuLabel == null
                ? "Product catalog write rolled back while recording the audit trail. Repair or align the audit_logs table before hosted proof can continue."
                : "Catalog write for " + skuLabel + " rolled back while recording the audit trail. Repair or align the audit_logs table before hosted proof can continue.";
        }
        if (normalizedMessage.contains("operational_dispatch_work_items")) {
            return skuLabel == null
                ? "Product catalog write rolled back while enqueuing operational dispatch work. Repair or align the operational_dispatch_work_items table before hosted proof can continue."
                : "Catalog write for " + skuLabel + " rolled back while enqueuing operational dispatch work. Repair or align the operational_dispatch_work_items table before hosted proof can continue.";
        }
        if (normalizedMessage.contains("column") && normalizedMessage.contains("does not exist")) {
            return skuLabel == null
                ? "Product catalog write failed because the live database schema is missing one or more required product-side-effect columns."
                : "Catalog write for " + skuLabel + " failed because the live database schema is missing one or more required product-side-effect columns.";
        }
        return skuLabel == null
            ? "Product catalog write failed because the live production database state is not aligned with the current server contract."
            : "Catalog write for " + skuLabel + " failed because the live production database state is not aligned with the current server contract.";
    }

    private String normalizeSkuLabel(String catalogSku) {
        if (catalogSku == null || catalogSku.isBlank()) {
            return null;
        }
        return catalogSku.trim().toUpperCase(Locale.ROOT);
    }
}
