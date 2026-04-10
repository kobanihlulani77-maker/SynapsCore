package com.synapsecore.domain.repository;

import com.synapsecore.domain.entity.Inventory;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    @EntityGraph(attributePaths = {"product", "warehouse"})
    @Query("select i from Inventory i order by i.warehouse.name asc, i.product.name asc")
    List<Inventory> findAllWithProductAndWarehouse();

    @EntityGraph(attributePaths = {"product", "warehouse", "warehouse.tenant"})
    @Query("""
        select i
        from Inventory i
        where upper(i.warehouse.tenant.code) = upper(:tenantCode)
        order by i.warehouse.name asc, i.product.name asc
        """)
    List<Inventory> findAllWithProductAndWarehouseByTenantCode(@Param("tenantCode") String tenantCode);

    @EntityGraph(attributePaths = {"product", "warehouse"})
    Optional<Inventory> findByProductIdAndWarehouseId(Long productId, Long warehouseId);

    @EntityGraph(attributePaths = {"product", "warehouse"})
    @Query("""
        select i
        from Inventory i
        where i.product.id = :productId
          and i.warehouse.id <> :warehouseId
          and upper(i.warehouse.tenant.code) = upper(:tenantCode)
        order by i.quantityAvailable desc
        """)
    List<Inventory> findTransferCandidatesByTenantCode(@Param("tenantCode") String tenantCode,
                                                       @Param("productId") Long productId,
                                                       @Param("warehouseId") Long warehouseId);

    @EntityGraph(attributePaths = {"product", "warehouse"})
    @Query("select i from Inventory i where i.quantityAvailable > 0 order by i.quantityAvailable desc")
    List<Inventory> findAvailableInventory();

    @EntityGraph(attributePaths = {"product", "warehouse"})
    @Query("""
        select i
        from Inventory i
        where i.quantityAvailable > 0
          and upper(i.warehouse.tenant.code) = upper(:tenantCode)
        order by i.quantityAvailable desc
        """)
    List<Inventory> findAvailableInventoryByTenantCode(@Param("tenantCode") String tenantCode);

    @Query("select count(i) from Inventory i where i.quantityAvailable <= i.reorderThreshold")
    long countLowStockItems();

    @Query("""
        select count(i)
        from Inventory i
        where i.quantityAvailable <= i.reorderThreshold
          and upper(i.warehouse.tenant.code) = upper(:tenantCode)
        """)
    long countLowStockItemsByTenantCode(@Param("tenantCode") String tenantCode);

    @Query("select i from Inventory i where i.quantityAvailable <= i.reorderThreshold")
    List<Inventory> findLowStockItems();

    @Query("""
        select i
        from Inventory i
        where i.quantityAvailable <= i.reorderThreshold
          and upper(i.warehouse.tenant.code) = upper(:tenantCode)
        """)
    List<Inventory> findLowStockItemsByTenantCode(@Param("tenantCode") String tenantCode);

    @Query("""
        select count(i)
        from Inventory i
        where upper(i.warehouse.tenant.code) = upper(:tenantCode)
        """)
    long countByTenantCode(@Param("tenantCode") String tenantCode);
}
