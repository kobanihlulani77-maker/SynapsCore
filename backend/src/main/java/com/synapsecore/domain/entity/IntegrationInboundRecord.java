package com.synapsecore.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@Table(name = "integration_inbound_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntegrationInboundRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 64)
    private String tenantCode;

    @Column(nullable = false, length = 80)
    private String sourceSystem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private IntegrationConnectorType connectorType;

    @Column(length = 160)
    private String fileName;

    @Column(length = 80)
    private String externalOrderId;

    @Column(length = 40)
    private String warehouseCode;

    @Column(length = 64)
    private String requestId;

    @Column(length = 120)
    private String ingestionSource;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private IntegrationInboundStatus status;

    @Column(nullable = false, columnDefinition = "text")
    private String requestPayload;

    @Column(length = 320)
    private String failureMessage;

    @Enumerated(EnumType.STRING)
    @Column(length = 48)
    private com.synapsecore.integration.IntegrationFailureCode failureCode;

    private Long replayRecordId;

    @Column(length = 80)
    private String replayedOrderExternalId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
