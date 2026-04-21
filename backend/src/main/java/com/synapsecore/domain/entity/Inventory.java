package com.synapsecore.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.synapsecore.tenant.TenantOwnershipAssertions;

@Entity
@Table(
    name = "inventory",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_inventory_product_warehouse", columnNames = {"product_id", "warehouse_id"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Column(nullable = false)
    private Long quantityAvailable;

    @Column(nullable = false)
    @Builder.Default
    private Long quantityOnHand = 0L;

    @Column(nullable = false)
    @Builder.Default
    private Long quantityReserved = 0L;

    @Column(nullable = false)
    @Builder.Default
    private Long quantityInbound = 0L;

    @Column(nullable = false)
    private Long reorderThreshold;

    private Instant lastReceivedAt;

    private Instant lastAdjustedAt;

    private Instant lastReconciledAt;

    private Long reconciliationVariance;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        synchronizeTenantAndStockModel();
        TenantOwnershipAssertions.requireInventoryConsistency(this, "Inventory persistence");
        updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        synchronizeTenantAndStockModel();
        TenantOwnershipAssertions.requireInventoryConsistency(this, "Inventory persistence");
        updatedAt = Instant.now();
    }

    public void synchronizeStockModel() {
        quantityOnHand = normalizeNonNegative(quantityOnHand == null ? quantityAvailable : quantityOnHand);
        quantityReserved = normalizeNonNegative(quantityReserved);
        quantityInbound = normalizeNonNegative(quantityInbound);
        quantityAvailable = Math.max(quantityOnHand - quantityReserved, 0L);
        reorderThreshold = normalizeNonNegative(reorderThreshold);
    }

    private void synchronizeTenantAndStockModel() {
        if (warehouse != null) {
            tenant = warehouse.getTenant();
        }
        synchronizeStockModel();
    }

    private long normalizeNonNegative(Long value) {
        return value == null || value < 0 ? 0L : value;
    }
}
