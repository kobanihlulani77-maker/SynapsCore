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
@Table(name = "integration_import_runs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntegrationImportRun {

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

    @Column(nullable = false)
    private int recordsReceived;

    @Column(nullable = false)
    private int ordersImported;

    @Column(nullable = false)
    private int ordersFailed;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private IntegrationImportStatus status;

    @Column(nullable = false, length = 320)
    private String summary;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
