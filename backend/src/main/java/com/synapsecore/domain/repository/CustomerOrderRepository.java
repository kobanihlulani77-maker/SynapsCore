package com.synapsecore.domain.repository;

import com.synapsecore.domain.entity.CustomerOrder;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {

    boolean existsByExternalOrderId(String externalOrderId);

    boolean existsByTenant_CodeIgnoreCaseAndExternalOrderId(String tenantCode, String externalOrderId);

    @EntityGraph(attributePaths = {"tenant", "warehouse", "items", "items.product"})
    java.util.Optional<CustomerOrder> findByTenant_CodeIgnoreCaseAndExternalOrderId(String tenantCode, String externalOrderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"tenant", "warehouse", "items", "items.product"})
    @Query("""
        select o
        from CustomerOrder o
        where upper(o.tenant.code) = upper(:tenantCode)
          and upper(o.externalOrderId) = upper(:externalOrderId)
        """)
    java.util.Optional<CustomerOrder> findByTenant_CodeIgnoreCaseAndExternalOrderIdForUpdate(
        @Param("tenantCode") String tenantCode,
        @Param("externalOrderId") String externalOrderId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"tenant", "warehouse", "items", "items.product"})
    @Query("select o from CustomerOrder o where o.id = :id")
    java.util.Optional<CustomerOrder> findByIdForUpdate(@Param("id") Long id);

    @Query("select o.id from CustomerOrder o order by o.createdAt desc")
    List<Long> findRecentOrderIds(Pageable pageable);

    @Query("select o.id from CustomerOrder o where upper(o.tenant.code) = upper(:tenantCode) order by o.createdAt desc")
    List<Long> findRecentOrderIdsByTenantCode(String tenantCode, Pageable pageable);

    @Query("""
        select o.id
        from CustomerOrder o
        where upper(o.tenant.code) = upper(:tenantCode)
          and upper(o.warehouse.code) in :warehouseCodes
        order by o.createdAt desc
        """)
    List<Long> findRecentOrderIdsByTenantCodeAndWarehouseCodes(
        @Param("tenantCode") String tenantCode,
        @Param("warehouseCodes") Collection<String> warehouseCodes,
        Pageable pageable
    );

    @EntityGraph(attributePaths = {"warehouse", "items", "items.product"})
    List<CustomerOrder> findByIdIn(List<Long> ids);

    @EntityGraph(attributePaths = {"warehouse", "items", "items.product"})
    @Query("""
        select o
        from CustomerOrder o
        where upper(o.tenant.code) = upper(:tenantCode)
          and upper(o.externalOrderId) = upper(:externalOrderId)
        order by o.createdAt desc
        """)
    java.util.Optional<CustomerOrder> findLatestByTenantCodeAndExternalOrderId(String tenantCode, String externalOrderId);

    long countByCreatedAtAfter(Instant createdAt);

    long countByTenant_CodeIgnoreCase(String tenantCode);

    long countByTenant_CodeIgnoreCaseAndCreatedAtAfter(String tenantCode, Instant createdAt);

    @Query("""
        select count(o)
        from CustomerOrder o
        where upper(o.tenant.code) = upper(:tenantCode)
          and upper(o.warehouse.code) in :warehouseCodes
        """)
    long countByTenantCodeAndWarehouseCodes(
        @Param("tenantCode") String tenantCode,
        @Param("warehouseCodes") Collection<String> warehouseCodes
    );

    @Query("""
        select count(o)
        from CustomerOrder o
        where upper(o.tenant.code) = upper(:tenantCode)
          and upper(o.warehouse.code) in :warehouseCodes
          and o.createdAt > :createdAt
        """)
    long countByTenantCodeAndWarehouseCodesAndCreatedAtAfter(
        @Param("tenantCode") String tenantCode,
        @Param("warehouseCodes") Collection<String> warehouseCodes,
        @Param("createdAt") Instant createdAt
    );

    long countByTenant_CodeIgnoreCaseAndWarehouse_IdAndCreatedAtAfter(String tenantCode, Long warehouseId, Instant createdAt);
}
