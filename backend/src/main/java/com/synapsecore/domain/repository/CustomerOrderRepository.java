package com.synapsecore.domain.repository;

import com.synapsecore.domain.entity.CustomerOrder;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {

    boolean existsByExternalOrderId(String externalOrderId);

    boolean existsByTenant_CodeIgnoreCaseAndExternalOrderId(String tenantCode, String externalOrderId);

    @EntityGraph(attributePaths = {"tenant", "warehouse", "items", "items.product"})
    java.util.Optional<CustomerOrder> findByTenant_CodeIgnoreCaseAndExternalOrderId(String tenantCode, String externalOrderId);

    @Query("select o.id from CustomerOrder o order by o.createdAt desc")
    List<Long> findRecentOrderIds(Pageable pageable);

    @Query("select o.id from CustomerOrder o where upper(o.tenant.code) = upper(:tenantCode) order by o.createdAt desc")
    List<Long> findRecentOrderIdsByTenantCode(String tenantCode, Pageable pageable);

    @EntityGraph(attributePaths = {"warehouse", "items", "items.product"})
    List<CustomerOrder> findByIdIn(List<Long> ids);

    long countByCreatedAtAfter(Instant createdAt);

    long countByTenant_CodeIgnoreCase(String tenantCode);

    long countByTenant_CodeIgnoreCaseAndCreatedAtAfter(String tenantCode, Instant createdAt);

    long countByTenant_CodeIgnoreCaseAndWarehouse_IdAndCreatedAtAfter(String tenantCode, Long warehouseId, Instant createdAt);
}
