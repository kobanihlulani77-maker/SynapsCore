package com.synapsecore.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
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
@Table(name = "integration_replay_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntegrationReplayRecord {

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

    @Column(nullable = false, length = 80)
    private String externalOrderId;

    @Column(nullable = false, length = 40)
    private String warehouseCode;

    @Lob
    @Column(nullable = false)
    private String requestPayload;

    @Column(nullable = false, length = 320)
    private String failureMessage;

    @Enumerated(EnumType.STRING)
    @Column(length = 48)
    private com.synapsecore.integration.IntegrationFailureCode failureCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private IntegrationReplayStatus status;

    @Column(nullable = false)
    private int replayAttemptCount;

    private Long inboundRecordId;

    @Column(length = 320)
    private String lastReplayMessage;

    private Instant lastAttemptedAt;

    private Instant nextEligibleAt;

    private Instant resolvedAt;

    private Instant deadLetteredAt;

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
