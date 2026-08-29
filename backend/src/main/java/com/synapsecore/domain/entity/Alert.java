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
@Table(name = "alerts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Alert {

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

    @Column(name = "source_type", nullable = false, length = 64)
    private String sourceType;

    @Column(name = "source_ref", nullable = false, length = 256)
    private String sourceRef;

    @Column(name = "condition_key", nullable = false, length = 320)
    private String conditionKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AlertType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AlertSeverity severity;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(nullable = false, length = 1024)
    private String description;

    @Column(nullable = false, length = 1024)
    private String impactSummary;

    @Column(nullable = false, length = 1024)
    private String recommendedAction;

    @Column(length = 1024)
    private String policyExplanation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AlertStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        validateIdentity();
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        validateIdentity();
        updatedAt = Instant.now();
    }

    private void validateIdentity() {
        if (tenant == null) {
            throw new IllegalStateException("Operational Alerts must belong to an explicit tenant.");
        }
        if (warehouse == null) {
            throw new IllegalStateException("Operational Alerts must identify an explicit warehouse.");
        }
        if (sourceType == null || sourceType.isBlank()
            || sourceRef == null || sourceRef.isBlank()
            || conditionKey == null || conditionKey.isBlank()) {
            throw new IllegalStateException("Operational Alerts must carry structured source and condition identity.");
        }
        if ((type == AlertType.LOW_STOCK || type == AlertType.DEPLETION_RISK) && product == null) {
            throw new IllegalStateException("Inventory Alerts must identify an explicit product.");
        }
    }
}
