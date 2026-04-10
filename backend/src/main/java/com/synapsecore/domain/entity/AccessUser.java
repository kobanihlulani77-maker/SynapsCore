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

@Entity
@Table(
    name = "access_users",
    uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "username"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccessUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @Column(nullable = false, length = 80)
    private String username;

    @Column(nullable = false, length = 120)
    private String fullName;

    @Column(nullable = false, length = 120)
    private String passwordHash;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @Builder.Default
    @Column(nullable = false)
    private boolean passwordChangeRequired = false;

    @Builder.Default
    @Column(nullable = false)
    private long sessionVersion = 1L;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "access_operator_id", nullable = false)
    private AccessOperator operator;

    @Column(nullable = false)
    private Instant passwordUpdatedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (passwordUpdatedAt == null) {
            passwordUpdatedAt = now;
        }
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        if (passwordUpdatedAt == null) {
            passwordUpdatedAt = Instant.now();
        }
        updatedAt = Instant.now();
    }
}
