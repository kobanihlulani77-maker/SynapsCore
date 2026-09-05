package com.synapsecore.domain.repository;

import com.synapsecore.domain.entity.FulfillmentStatus;
import com.synapsecore.domain.entity.FulfillmentTask;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FulfillmentTaskRepository extends JpaRepository<FulfillmentTask, Long> {

    @EntityGraph(attributePaths = {"tenant", "customerOrder", "warehouse"})
    Optional<FulfillmentTask> findByTenant_CodeIgnoreCaseAndCustomerOrder_ExternalOrderId(String tenantCode, String externalOrderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"tenant", "customerOrder", "warehouse"})
    @Query("""
        select f
        from FulfillmentTask f
        where upper(f.tenant.code) = upper(:tenantCode)
          and upper(f.customerOrder.externalOrderId) = upper(:externalOrderId)
        """)
    Optional<FulfillmentTask> findByTenant_CodeIgnoreCaseAndCustomerOrder_ExternalOrderIdForUpdate(
        @Param("tenantCode") String tenantCode,
        @Param("externalOrderId") String externalOrderId
    );

    @EntityGraph(attributePaths = {"tenant", "customerOrder", "customerOrder.items", "warehouse"})
    List<FulfillmentTask> findAllByTenant_CodeIgnoreCaseAndStatusInOrderByUpdatedAtDesc(String tenantCode, Collection<FulfillmentStatus> statuses);

    @EntityGraph(attributePaths = {"tenant", "customerOrder", "customerOrder.items", "warehouse"})
    List<FulfillmentTask> findAllByStatusInOrderByUpdatedAtDesc(Collection<FulfillmentStatus> statuses);

    @EntityGraph(attributePaths = {"tenant", "customerOrder", "warehouse"})
    Optional<FulfillmentTask> findTop1ByTenant_CodeIgnoreCaseAndStatusInOrderByUpdatedAtAsc(String tenantCode, Collection<FulfillmentStatus> statuses);

    long countByStatusIn(Collection<FulfillmentStatus> statuses);

    long countByTenant_CodeIgnoreCaseAndStatusIn(String tenantCode, Collection<FulfillmentStatus> statuses);

    long countByTenant_CodeIgnoreCaseAndWarehouse_IdAndStatusInAndUpdatedAtAfter(String tenantCode,
                                                                                 Long warehouseId,
                                                                                 Collection<FulfillmentStatus> statuses,
                                                                                 Instant updatedAt);
}
