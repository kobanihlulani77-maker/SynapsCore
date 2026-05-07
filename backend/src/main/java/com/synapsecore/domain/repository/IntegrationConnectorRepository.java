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

    @Query("""
        select connector
        from IntegrationConnector connector
        join fetch connector.tenant tenant
        where lower(tenant.code) = lower(:tenantCode)
          and lower(connector.sourceSystem) = lower(:sourceSystem)
          and connector.type = :type
        """)
    Optional<IntegrationConnector> findByTenantCodeAndSourceSystemAndTypeWithTenant(@Param("tenantCode") String tenantCode,
                                                                                     @Param("sourceSystem") String sourceSystem,
                                                                                     @Param("type") IntegrationConnectorType type);

    Optional<IntegrationConnector> findBySourceSystemIgnoreCaseAndTypeAndInboundAccessTokenHash(String sourceSystem,
                                                                                                 IntegrationConnectorType type,
                                                                                                 String inboundAccessTokenHash);

    @Query("""
        select connector
        from IntegrationConnector connector
        join fetch connector.tenant tenant
        where lower(connector.sourceSystem) = lower(:sourceSystem)
          and connector.type = :type
          and connector.inboundAccessTokenHash = :inboundAccessTokenHash
        """)
    Optional<IntegrationConnector> findBySourceSystemIgnoreCaseAndTypeAndInboundAccessTokenHashWithTenant(@Param("sourceSystem") String sourceSystem,
                                                                                                           @Param("type") IntegrationConnectorType type,
                                                                                                           @Param("inboundAccessTokenHash") String inboundAccessTokenHash);

    Optional<IntegrationConnector> findByTenant_CodeIgnoreCaseAndId(String tenantCode, Long id);

    @Query("select connector from IntegrationConnector connector join fetch connector.tenant where connector.id = :id")
    Optional<IntegrationConnector> findByIdWithTenant(@Param("id") Long id);

    List<IntegrationConnector> findAllByOrderByTypeAscSourceSystemAsc();

    List<IntegrationConnector> findAllByTenant_CodeIgnoreCaseOrderByTypeAscSourceSystemAsc(String tenantCode);

    List<IntegrationConnector> findAllByTenant_CodeIgnoreCaseAndSourceSystemIgnoreCaseOrderByTypeAscSourceSystemAsc(String tenantCode,
                                                                                                                    String sourceSystem);

    @Query("select connector from IntegrationConnector connector join fetch connector.tenant where connector.enabled = true and connector.syncMode = :syncMode")
    List<IntegrationConnector> findAllEnabledBySyncModeWithTenant(@Param("syncMode") IntegrationSyncMode syncMode);

    long countByEnabledFalse();

    long countByTenant_CodeIgnoreCaseAndEnabledFalse(String tenantCode);
}
