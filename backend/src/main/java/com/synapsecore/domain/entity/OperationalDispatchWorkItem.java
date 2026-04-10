package com.synapsecore.domain.entity;

import com.synapsecore.event.OperationalUpdateType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
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
@Table(
    name = "operational_dispatch_work_items",
    indexes = {
        @Index(name = "idx_dispatch_work_status_created", columnList = "status, createdAt"),
        @Index(name = "idx_dispatch_work_tenant_status_created", columnList = "tenantCode, status, createdAt")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OperationalDispatchWorkItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String tenantCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 48)
    private OperationalUpdateType updateType;

    @Column(nullable = false, length = 120)
    private String source;

    @Column(nullable = false, length = 80)
    private String requestId;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private OperationalDispatchStatus status = OperationalDispatchStatus.PENDING;

    @Builder.Default
    @Column(nullable = false)
    private int attemptCount = 0;

    @Column(nullable = false)
    private Instant occurredAt;

    private Instant processedAt;

    @Column(length = 320)
    private String lastError;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (occurredAt == null) {
            occurredAt = now;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
