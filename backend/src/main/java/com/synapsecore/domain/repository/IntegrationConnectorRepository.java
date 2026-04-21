package com.synapsecore.domain.repository;

import com.synapsecore.domain.entity.IntegrationConnector;
import com.synapsecore.domain.entity.IntegrationConnectorType;
import com.synapsecore.domain.entity.IntegrationSyncMode;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IntegrationConnectorRepository extends JpaRepository<IntegrationConnector, Long> {

    Optional<IntegrationConnector> findBySourceSystemIgnoreCaseAndType(String sourceSystem, IntegrationConnectorType type);

    Optional<IntegrationConnector> findByTenant_CodeIgnoreCaseAndSourceSystemIgnoreCaseAndType(String tenantCode,
                                                                                               String sourceSystem,
                                                                                               IntegrationConnectorType type);

    Optional<IntegrationConnector> findBySourceSystemIgnoreCaseAndTypeAndInboundAccessTokenHash(String sourceSystem,
                                                                                                 IntegrationConnectorType type,
                                                                                                 String inboundAccessTokenHash);

    Optional<IntegrationConnector> findByTenant_CodeIgnoreCaseAndId(String tenantCode, Long id);

    List<IntegrationConnector> findAllByOrderByTypeAscSourceSystemAsc();

    List<IntegrationConnector> findAllByTenant_CodeIgnoreCaseOrderByTypeAscSourceSystemAsc(String tenantCode);

    @Query("select connector from IntegrationConnector connector join fetch connector.tenant where connector.enabled = true and connector.syncMode = :syncMode")
    List<IntegrationConnector> findAllEnabledBySyncModeWithTenant(@Param("syncMode") IntegrationSyncMode syncMode);

    long countByEnabledFalse();

    long countByTenant_CodeIgnoreCaseAndEnabledFalse(String tenantCode);
}
