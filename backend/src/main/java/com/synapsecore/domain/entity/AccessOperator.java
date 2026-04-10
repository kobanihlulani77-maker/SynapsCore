package com.synapsecore.domain.entity;

import com.synapsecore.access.SynapseAccessRole;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
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
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.collection.spi.PersistentCollection;

@Entity
@Table(
    name = "access_operators",
    uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "actor_name"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccessOperator {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @Column(name = "actor_name", nullable = false, length = 80)
    private String actorName;

    @Column(nullable = false, length = 80)
    private String displayName;

    @Column(length = 160)
    private String description;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "access_operator_roles", joinColumns = @JoinColumn(name = "access_operator_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role_code", nullable = false, length = 32)
    private Set<SynapseAccessRole> roles = EnumSet.noneOf(SynapseAccessRole.class);

    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "access_operator_warehouse_scopes", joinColumns = @JoinColumn(name = "access_operator_id"))
    @Column(name = "warehouse_code", nullable = false, length = 40)
    private Set<String> warehouseScopes = new LinkedHashSet<>();

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (roles == null) {
            roles = EnumSet.noneOf(SynapseAccessRole.class);
        }
        setWarehouseScopes(warehouseScopes);
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
        if (roles == null) {
            roles = EnumSet.noneOf(SynapseAccessRole.class);
        }
        setWarehouseScopes(warehouseScopes);
    }

    public void setRoles(Set<SynapseAccessRole> roles) {
        if (this.roles instanceof PersistentCollection<?>) {
            this.roles.clear();
            if (roles != null) {
                this.roles.addAll(roles);
            }
            return;
        }
        this.roles = roles == null || roles.isEmpty()
            ? EnumSet.noneOf(SynapseAccessRole.class)
            : EnumSet.copyOf(roles);
    }

    public void setWarehouseScopes(Set<String> warehouseScopes) {
        Set<String> normalized = normalizeWarehouseScopes(warehouseScopes);
        if (this.warehouseScopes instanceof PersistentCollection<?>) {
            this.warehouseScopes.clear();
            this.warehouseScopes.addAll(normalized);
            return;
        }
        this.warehouseScopes = new LinkedHashSet<>(normalized);
    }

    private Set<String> normalizeWarehouseScopes(Set<String> values) {
        if (values == null || values.isEmpty()) {
            return new LinkedHashSet<>();
        }
        return values.stream()
            .filter(value -> value != null && !value.isBlank())
            .map(value -> value.trim().toUpperCase(Locale.ROOT))
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
