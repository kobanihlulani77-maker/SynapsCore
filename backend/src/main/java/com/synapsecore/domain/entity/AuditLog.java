package com.synapsecore.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 64)
    private String tenantCode;

    @Column(nullable = false, length = 96)
    private String action;

    @Column(nullable = false, length = 96)
    private String actor;

    @Column(nullable = false, length = 128)
    private String source;

    @Column(nullable = false, length = 96)
    private String targetType;

    @Column(nullable = false, length = 256)
    private String targetRef;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AuditStatus status;

    @Column(nullable = false, length = 2048)
    private String details;

    @Column(nullable = false, length = 64)
    private String requestId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
