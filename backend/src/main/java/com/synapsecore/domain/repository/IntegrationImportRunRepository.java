package com.synapsecore.domain.repository;

import com.synapsecore.domain.entity.IntegrationImportRun;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IntegrationImportRunRepository extends JpaRepository<IntegrationImportRun, Long> {

    List<IntegrationImportRun> findTop20ByOrderByCreatedAtDesc();

    List<IntegrationImportRun> findTop20ByTenantCodeIgnoreCaseOrderByCreatedAtDesc(String tenantCode);
}
