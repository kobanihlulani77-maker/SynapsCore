package com.synapsecore.domain.repository;

import com.synapsecore.domain.entity.OrderItem;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @EntityGraph(attributePaths = {"tenant", "customerOrder", "product", "product.tenant", "customerOrder.tenant"})
    List<OrderItem> findAll();

    @Query("""
        select coalesce(sum(oi.quantity), 0)
        from OrderItem oi
        where oi.product.id = :productId
          and oi.customerOrder.warehouse.id = :warehouseId
          and oi.customerOrder.createdAt >= :since
        """)
    Long sumRecentQuantityByProductAndWarehouse(@Param("productId") Long productId,
                                                @Param("warehouseId") Long warehouseId,
                                                @Param("since") Instant since);
}
