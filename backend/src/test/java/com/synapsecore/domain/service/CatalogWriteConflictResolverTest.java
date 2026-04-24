package com.synapsecore.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CatalogWriteConflictResolverTest {

    private final CatalogWriteConflictResolver resolver = new CatalogWriteConflictResolver();

    @Test
    void describeBusinessEventsFailureIncludesRootCauseSummary() {
        RuntimeException exception = new RuntimeException(
            "could not execute statement",
            new IllegalStateException("insert into business_events failed because check constraint rejected PRODUCT_CATALOG_UPDATED")
        );

        String message = resolver.describe(exception, "SKU-PILOT-TENANT-PROOF");

        assertThat(message)
            .contains("business_events")
            .contains("Root cause:")
            .contains("IllegalStateException")
            .contains("PRODUCT_CATALOG_UPDATED");
    }

    @Test
    void describeDuplicateSkuConflictKeepsTenantFriendlyMessage() {
        RuntimeException exception = new RuntimeException(
            "duplicate key value violates unique constraint \"uk_products_sku\" on table products.sku"
        );

        String message = resolver.describe(exception, "SKU-PILOT-TENANT-PROOF");

        assertThat(message)
            .isEqualTo("Product SKU already exists for this tenant or a hidden legacy catalog row still occupies SKU-PILOT-TENANT-PROOF.");
    }
}
