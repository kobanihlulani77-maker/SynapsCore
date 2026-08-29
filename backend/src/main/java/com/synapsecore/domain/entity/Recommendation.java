package com.synapsecore.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "recommendations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Recommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_warehouse_id")
    private Warehouse sourceWarehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_warehouse_id")
    private Warehouse destinationWarehouse;

    @Column(name = "source_type", nullable = false, length = 64)
    private String sourceType;

    @Column(name = "source_ref", nullable = false, length = 256)
    private String sourceRef;

    @Column(name = "condition_key", nullable = false, length = 320)
    private String conditionKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RecommendationType type;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(nullable = false, length = 1024)
    private String description;

    @Column(length = 1024)
    private String policyExplanation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RecommendationPriority priority;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RecommendationStatus status;

    @Column(nullable = false)
    private Instant updatedAt;

    private Long suggestedQuantity;

    @PrePersist
    void onCreate() {
        validateIdentity();
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) {
            status = RecommendationStatus.CURRENT;
        }
    }

    @PreUpdate
    void onUpdate() {
        validateIdentity();
        updatedAt = Instant.now();
    }

    private void validateIdentity() {
        if (tenant == null || warehouse == null) {
            throw new IllegalStateException("Operational Recommendations must belong to an explicit tenant and warehouse.");
        }
        if (sourceType == null || sourceType.isBlank() || sourceRef == null || sourceRef.isBlank()
            || conditionKey == null || conditionKey.isBlank()) {
            throw new IllegalStateException("Operational Recommendations must carry structured source and condition identity.");
        }
        if ("INVENTORY".equalsIgnoreCase(sourceType) && product == null) {
            throw new IllegalStateException("Inventory Recommendations must identify an explicit product.");
        }
        if (type == RecommendationType.TRANSFER_STOCK && (sourceWarehouse == null || destinationWarehouse == null)) {
            throw new IllegalStateException("Transfer Recommendations must identify source and destination warehouses.");
        }
    }
}
