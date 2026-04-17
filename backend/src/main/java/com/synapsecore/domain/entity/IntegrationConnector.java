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
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "integration_connectors",
    uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "sourceSystem", "type"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntegrationConnector {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @Column(nullable = false, length = 80)
    private String sourceSystem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private IntegrationConnectorType type;

    @Column(nullable = false, length = 120)
    private String displayName;

    @Column(nullable = false)
    private boolean enabled;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private IntegrationSyncMode syncMode = IntegrationSyncMode.REALTIME_PUSH;

    private Integer syncIntervalMinutes;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private IntegrationValidationPolicy validationPolicy = IntegrationValidationPolicy.STANDARD;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private IntegrationTransformationPolicy transformationPolicy = IntegrationTransformationPolicy.NONE;

    private Integer mappingVersion;

    @Builder.Default
    @Column(nullable = false)
    private boolean allowDefaultWarehouseFallback = false;

    @Column(length = 40)
    private String defaultWarehouseCode;

    @Column(length = 240)
    private String notes;

    @Column(length = 80)
    private String supportOwnerActorName;

    @Column(length = 64)
    private String inboundAccessTokenHash;

    @Column(length = 24)
    private String inboundAccessTokenHint;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (mappingVersion == null || mappingVersion < 1) {
            mappingVersion = 1;
        }
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        if (mappingVersion == null || mappingVersion < 1) {
            mappingVersion = 1;
        }
        updatedAt = Instant.now();
    }
}
