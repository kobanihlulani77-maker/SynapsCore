package com.synapsecore.domain.repository;

import com.synapsecore.domain.entity.IntegrationInboundRecord;
import com.synapsecore.domain.entity.IntegrationConnectorType;
import com.synapsecore.domain.entity.IntegrationInboundStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IntegrationInboundRecordRepository extends JpaRepository<IntegrationInboundRecord, Long> {

    long countByTenantCodeIgnoreCaseAndStatusIn(String tenantCode, Collection<IntegrationInboundStatus> statuses);

    long countByTenantCodeIgnoreCaseAndStatusInAndCreatedAtAfter(String tenantCode,
                                                                 Collection<IntegrationInboundStatus> statuses,
                                                                 Instant createdAt);

    long countByTenantCodeIgnoreCaseAndSourceSystemIgnoreCaseAndConnectorTypeAndStatusInAndCreatedAtAfter(
        String tenantCode,
        String sourceSystem,
        IntegrationConnectorType connectorType,
        Collection<IntegrationInboundStatus> statuses,
        Instant createdAt
    );

    List<IntegrationInboundRecord> findTop8ByTenantCodeIgnoreCaseAndStatusInOrderByUpdatedAtDesc(String tenantCode,
                                                                                                  Collection<IntegrationInboundStatus> statuses);

    Optional<IntegrationInboundRecord> findTopByTenantCodeIgnoreCaseAndSourceSystemIgnoreCaseAndConnectorTypeOrderByCreatedAtDesc(
        String tenantCode,
        String sourceSystem,
        IntegrationConnectorType connectorType
    );

    Optional<IntegrationInboundRecord> findTopByTenantCodeIgnoreCaseAndSourceSystemIgnoreCaseAndConnectorTypeAndStatusInOrderByCreatedAtDesc(
        String tenantCode,
        String sourceSystem,
        IntegrationConnectorType connectorType,
        Collection<IntegrationInboundStatus> statuses
    );

    Optional<IntegrationInboundRecord> findTopByTenantCodeIgnoreCaseAndSourceSystemIgnoreCaseAndConnectorTypeAndStatusInOrderByUpdatedAtDesc(
        String tenantCode,
        String sourceSystem,
        IntegrationConnectorType connectorType,
        Collection<IntegrationInboundStatus> statuses
    );
}
