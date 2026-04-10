package com.synapsecore.domain.repository;

import com.synapsecore.domain.entity.IntegrationConnector;
import com.synapsecore.domain.entity.IntegrationConnectorType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IntegrationConnectorRepository extends JpaRepository<IntegrationConnector, Long> {

    Optional<IntegrationConnector> findBySourceSystemIgnoreCaseAndType(String sourceSystem, IntegrationConnectorType type);

    Optional<IntegrationConnector> findByTenant_CodeIgnoreCaseAndSourceSystemIgnoreCaseAndType(String tenantCode,
                                                                                               String sourceSystem,
                                                                                               IntegrationConnectorType type);

    Optional<IntegrationConnector> findByTenant_CodeIgnoreCaseAndId(String tenantCode, Long id);

    List<IntegrationConnector> findAllByOrderByTypeAscSourceSystemAsc();

    List<IntegrationConnector> findAllByTenant_CodeIgnoreCaseOrderByTypeAscSourceSystemAsc(String tenantCode);

    long countByEnabledFalse();

    long countByTenant_CodeIgnoreCaseAndEnabledFalse(String tenantCode);
}
