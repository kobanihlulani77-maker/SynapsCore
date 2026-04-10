package com.synapsecore.domain.repository;

import com.synapsecore.domain.entity.FulfillmentStatus;
import com.synapsecore.domain.entity.FulfillmentTask;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FulfillmentTaskRepository extends JpaRepository<FulfillmentTask, Long> {

    @EntityGraph(attributePaths = {"tenant", "customerOrder", "warehouse"})
    Optional<FulfillmentTask> findByTenant_CodeIgnoreCaseAndCustomerOrder_ExternalOrderId(String tenantCode, String externalOrderId);

    @EntityGraph(attributePaths = {"tenant", "customerOrder", "warehouse"})
    List<FulfillmentTask> findAllByTenant_CodeIgnoreCaseAndStatusInOrderByUpdatedAtDesc(String tenantCode, Collection<FulfillmentStatus> statuses);

    @EntityGraph(attributePaths = {"tenant", "customerOrder", "warehouse"})
    Optional<FulfillmentTask> findTop1ByTenant_CodeIgnoreCaseAndStatusInOrderByUpdatedAtAsc(String tenantCode, Collection<FulfillmentStatus> statuses);

    long countByStatusIn(Collection<FulfillmentStatus> statuses);

    long countByTenant_CodeIgnoreCaseAndStatusIn(String tenantCode, Collection<FulfillmentStatus> statuses);

    long countByTenant_CodeIgnoreCaseAndWarehouse_IdAndStatusInAndUpdatedAtAfter(String tenantCode,
                                                                                 Long warehouseId,
                                                                                 Collection<FulfillmentStatus> statuses,
                                                                                 Instant updatedAt);
}
