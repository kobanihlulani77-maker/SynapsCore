package com.synapsecore.domain.repository;

import com.synapsecore.domain.entity.OperationalDispatchStatus;
import com.synapsecore.domain.entity.OperationalDispatchWorkItem;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OperationalDispatchWorkItemRepository extends JpaRepository<OperationalDispatchWorkItem, Long> {

    List<OperationalDispatchWorkItem> findByStatusInOrderByCreatedAtAsc(Collection<OperationalDispatchStatus> statuses,
                                                                        Pageable pageable);

    long countByStatusIn(Collection<OperationalDispatchStatus> statuses);

    long countByTenantCodeIgnoreCaseAndStatusIn(String tenantCode, Collection<OperationalDispatchStatus> statuses);

    Optional<OperationalDispatchWorkItem> findTopByStatusInOrderByCreatedAtAsc(Collection<OperationalDispatchStatus> statuses);

    Optional<OperationalDispatchWorkItem> findTopByTenantCodeIgnoreCaseAndStatusInOrderByCreatedAtAsc(
        String tenantCode,
        Collection<OperationalDispatchStatus> statuses
    );

    Optional<OperationalDispatchWorkItem> findTopByTenantCodeIgnoreCaseAndStatusOrderByProcessedAtDesc(
        String tenantCode,
        OperationalDispatchStatus status
    );

    List<OperationalDispatchWorkItem> findTop8ByTenantCodeIgnoreCaseAndStatusOrderByUpdatedAtDesc(
        String tenantCode,
        OperationalDispatchStatus status
    );
}
