package com.synapsecore.domain.repository;

import com.synapsecore.domain.entity.OperationalDispatchStatus;
import com.synapsecore.domain.entity.OperationalDispatchWorkItem;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface OperationalDispatchWorkItemRepository extends JpaRepository<OperationalDispatchWorkItem, Long> {

    List<OperationalDispatchWorkItem> findByStatusInOrderByCreatedAtAsc(Collection<OperationalDispatchStatus> statuses,
                                                                        Pageable pageable);

    @Query("""
        select item from OperationalDispatchWorkItem item
        where item.status = :pending
           or (item.status = :processing and item.updatedAt <= :expiredBefore)
        order by item.createdAt asc, item.id asc
        """)
    List<OperationalDispatchWorkItem> findReadyForDispatch(
        @Param("pending") OperationalDispatchStatus pending,
        @Param("processing") OperationalDispatchStatus processing,
        @Param("expiredBefore") Instant expiredBefore,
        Pageable pageable
    );

    @Modifying
    @Transactional
    @Query("""
        update OperationalDispatchWorkItem item
        set item.status = :processing, item.attemptCount = item.attemptCount + 1,
            item.updatedAt = :claimedAt, item.lastError = null
        where item.id = :id and item.attemptCount = :expectedAttempt
          and (item.status = :pending
               or (item.status = :processing and item.updatedAt <= :expiredBefore))
        """)
    int claimForDispatch(
        @Param("id") Long id,
        @Param("expectedAttempt") int expectedAttempt,
        @Param("pending") OperationalDispatchStatus pending,
        @Param("processing") OperationalDispatchStatus processing,
        @Param("expiredBefore") Instant expiredBefore,
        @Param("claimedAt") Instant claimedAt
    );

    @Modifying
    @Transactional
    @Query("""
        update OperationalDispatchWorkItem item
        set item.status = :completed, item.processedAt = :processedAt,
            item.updatedAt = :processedAt, item.lastError = null
        where item.id = :id and item.status = :processing and item.attemptCount = :attempt
        """)
    int completeDispatch(
        @Param("id") Long id,
        @Param("attempt") int attempt,
        @Param("processing") OperationalDispatchStatus processing,
        @Param("completed") OperationalDispatchStatus completed,
        @Param("processedAt") Instant processedAt
    );

    @Modifying
    @Transactional
    @Query("""
        update OperationalDispatchWorkItem item
        set item.status = :failed, item.lastError = :error, item.updatedAt = :failedAt
        where item.id = :id and item.status = :processing and item.attemptCount = :attempt
        """)
    int failDispatch(
        @Param("id") Long id,
        @Param("attempt") int attempt,
        @Param("processing") OperationalDispatchStatus processing,
        @Param("failed") OperationalDispatchStatus failed,
        @Param("error") String error,
        @Param("failedAt") Instant failedAt
    );

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
