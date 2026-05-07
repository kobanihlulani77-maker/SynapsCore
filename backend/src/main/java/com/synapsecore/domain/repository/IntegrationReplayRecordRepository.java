package com.synapsecore.domain.repository;

import com.synapsecore.domain.entity.IntegrationReplayRecord;
import com.synapsecore.domain.entity.IntegrationReplayStatus;
import com.synapsecore.integration.IntegrationFailureCode;
import com.synapsecore.integration.dto.IntegrationReplayRecordResponse;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface IntegrationReplayRecordRepository extends JpaRepository<IntegrationReplayRecord, Long> {

    List<IntegrationReplayRecord> findByStatusInOrderByCreatedAtDesc(Collection<IntegrationReplayStatus> statuses,
                                                                     Pageable pageable);

    List<IntegrationReplayRecord> findByTenantCodeIgnoreCaseAndStatusInOrderByCreatedAtDesc(String tenantCode,
                                                                                             Collection<IntegrationReplayStatus> statuses,
                                                                                             Pageable pageable);

    @Query("""
        select new com.synapsecore.integration.dto.IntegrationReplayRecordResponse(
            record.id,
            record.sourceSystem,
            record.connectorType,
            record.externalOrderId,
            record.warehouseCode,
            record.failureCode,
            record.failureMessage,
            record.status,
            record.replayAttemptCount,
            record.lastReplayMessage,
            record.lastAttemptedAt,
            record.nextEligibleAt,
            record.resolvedAt,
            record.deadLetteredAt,
            record.replayedOrderExternalId,
            record.createdAt,
            record.updatedAt
        )
        from IntegrationReplayRecord record
        where lower(record.tenantCode) = lower(?1)
          and record.status in ?2
        order by record.createdAt desc
        """)
    List<IntegrationReplayRecordResponse> findQueueSummariesByTenantCodeIgnoreCaseAndStatusIn(String tenantCode,
                                                                                               Collection<IntegrationReplayStatus> statuses,
                                                                                               Pageable pageable);

    long countByStatusIn(Collection<IntegrationReplayStatus> statuses);

    long countByTenantCodeIgnoreCaseAndStatusIn(String tenantCode, Collection<IntegrationReplayStatus> statuses);

    long countByTenantCodeIgnoreCaseAndSourceSystemIgnoreCaseAndConnectorTypeAndStatusIn(
        String tenantCode,
        String sourceSystem,
        com.synapsecore.domain.entity.IntegrationConnectorType connectorType,
        Collection<IntegrationReplayStatus> statuses
    );

    java.util.Optional<IntegrationReplayRecord> findTopByTenantCodeIgnoreCaseAndSourceSystemIgnoreCaseAndConnectorTypeAndStatusInOrderByUpdatedAtDesc(
        String tenantCode,
        String sourceSystem,
        com.synapsecore.domain.entity.IntegrationConnectorType connectorType,
        Collection<IntegrationReplayStatus> statuses
    );

    java.util.Optional<IntegrationReplayRecord> findTopByTenantCodeIgnoreCaseAndSourceSystemIgnoreCaseAndConnectorTypeAndStatusInOrderByCreatedAtAsc(
        String tenantCode,
        String sourceSystem,
        com.synapsecore.domain.entity.IntegrationConnectorType connectorType,
        Collection<IntegrationReplayStatus> statuses
    );

    java.util.Optional<IntegrationReplayRecord> findByTenantCodeIgnoreCaseAndId(String tenantCode, Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select record
        from IntegrationReplayRecord record
        where record.id = ?1
        """)
    java.util.Optional<IntegrationReplayRecord> findByIdForUpdate(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select record
        from IntegrationReplayRecord record
        where lower(record.tenantCode) = lower(?1)
          and record.id = ?2
        """)
    java.util.Optional<IntegrationReplayRecord> findByTenantCodeIgnoreCaseAndIdForUpdate(String tenantCode, Long id);

    @Query("""
        select record.id
        from IntegrationReplayRecord record
        where record.status in ?1
          and record.nextEligibleAt is not null
          and record.nextEligibleAt <= ?2
          and (record.failureCode is null or record.failureCode not in ?3)
          and exists (
              select 1
              from com.synapsecore.domain.entity.IntegrationConnector connector
              where lower(connector.sourceSystem) = lower(record.sourceSystem)
                and connector.type = record.connectorType
                and lower(connector.tenant.code) = lower(record.tenantCode)
                and connector.enabled = true
          )
        order by record.nextEligibleAt asc, record.createdAt asc
        """)
    List<Long> findEligibleIdsForAutomatedReplay(Collection<IntegrationReplayStatus> statuses,
                                                 Instant eligibleAt,
                                                 Collection<IntegrationFailureCode> excludedFailureCodes,
                                                 Pageable pageable);
}
