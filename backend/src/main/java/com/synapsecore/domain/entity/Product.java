package com.synapsecore.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @Column(nullable = false, unique = true, length = 128)
    private String sku;

    @Column(name = "catalog_sku", length = 64)
    private String catalogSku;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 120)
    private String category;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Builder.Default
    @OneToMany(mappedBy = "product")
    private List<Inventory> inventoryRecords = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "product")
    private List<OrderItem> orderItems = new ArrayList<>();

    @PrePersist
    void onCreate() {
        normalizeCatalogIdentity();
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        normalizeCatalogIdentity();
        updatedAt = Instant.now();
    }

    public String resolveCatalogSku() {
        return catalogSku == null || catalogSku.isBlank() ? sku : catalogSku;
    }

    private void normalizeCatalogIdentity() {
        if (tenant == null || tenant.getCode() == null || tenant.getCode().isBlank()) {
            throw new IllegalStateException("Products must belong to an explicit tenant.");
        }

        String resolvedCatalogSku = resolveCatalogSku();
        if (resolvedCatalogSku == null || resolvedCatalogSku.isBlank()) {
            throw new IllegalStateException("Products must carry a tenant-visible catalog SKU.");
        }

        catalogSku = resolvedCatalogSku.trim().toUpperCase(Locale.ROOT);
        sku = buildInternalSku(tenant.getCode(), catalogSku);
    }

    public static String buildInternalSku(String tenantCode, String catalogSku) {
        return tenantCode.trim().toUpperCase(Locale.ROOT) + "::" + catalogSku.trim().toUpperCase(Locale.ROOT);
    }
}
