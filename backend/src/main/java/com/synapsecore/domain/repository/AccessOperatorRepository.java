package com.synapsecore.domain.repository;

import com.synapsecore.domain.entity.AccessOperator;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AccessOperatorRepository extends JpaRepository<AccessOperator, Long> {

    Optional<AccessOperator> findByActorNameIgnoreCaseAndActiveTrue(String actorName);

    Optional<AccessOperator> findByTenant_CodeIgnoreCaseAndActorNameIgnoreCaseAndActiveTrue(String tenantCode, String actorName);

    Optional<AccessOperator> findByTenant_CodeIgnoreCaseAndActorNameIgnoreCase(String tenantCode, String actorName);

    Optional<AccessOperator> findByTenant_CodeIgnoreCaseAndId(String tenantCode, Long id);

    List<AccessOperator> findAllByActiveTrueOrderByDisplayNameAsc();

    List<AccessOperator> findAllByTenant_CodeIgnoreCaseAndActiveTrueOrderByDisplayNameAsc(String tenantCode);

    List<AccessOperator> findAllByTenant_CodeIgnoreCaseOrderByDisplayNameAsc(String tenantCode);

    long countByTenant_CodeIgnoreCaseAndActiveTrue(String tenantCode);

    @Query("select new com.synapsecore.domain.repository.TenantCount(lower(o.tenant.code), count(o)) "
        + "from AccessOperator o where o.active = true group by lower(o.tenant.code)")
    List<TenantCount> countActiveByTenant();

    long countByTenant_CodeIgnoreCaseAndActiveFalse(String tenantCode);
}
