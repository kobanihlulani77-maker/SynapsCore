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
import java.math.BigDecimal;
import com.synapsecore.tenant.TenantOwnershipAssertions;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_order_id", nullable = false)
    private CustomerOrder customerOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    @Builder.Default
    private Integer reservedQuantity = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer fulfilledQuantity = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer cancelledQuantity = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer returnedQuantity = 0;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal unitPrice;

    @PrePersist
    void onCreate() {
        synchronizeTenantLifecycleFields();
        TenantOwnershipAssertions.requireOrderItemConsistency(this, "Order item persistence");
    }

    @PreUpdate
    void onUpdate() {
        synchronizeTenantLifecycleFields();
        TenantOwnershipAssertions.requireOrderItemConsistency(this, "Order item persistence");
    }

    private void synchronizeTenantLifecycleFields() {
        if (customerOrder != null) {
            tenant = customerOrder.getTenant();
        }
        if (reservedQuantity == null) {
            reservedQuantity = 0;
        }
        if (fulfilledQuantity == null) {
            fulfilledQuantity = 0;
        }
        if (cancelledQuantity == null) {
            cancelledQuantity = 0;
        }
        if (returnedQuantity == null) {
            returnedQuantity = 0;
        }
    }
}
