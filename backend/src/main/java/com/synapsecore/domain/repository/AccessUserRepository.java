package com.synapsecore.domain.repository;

import com.synapsecore.domain.entity.AccessUser;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AccessUserRepository extends JpaRepository<AccessUser, Long> {

    Optional<AccessUser> findByUsernameIgnoreCaseAndActiveTrue(String username);

    Optional<AccessUser> findByTenant_CodeIgnoreCaseAndUsernameIgnoreCaseAndActiveTrue(String tenantCode, String username);

    Optional<AccessUser> findByTenant_CodeIgnoreCaseAndUsernameIgnoreCase(String tenantCode, String username);

    Optional<AccessUser> findByTenant_CodeIgnoreCaseAndId(String tenantCode, Long id);

    List<AccessUser> findAllByTenant_CodeIgnoreCaseOrderByFullNameAscUsernameAsc(String tenantCode);

    long countByTenant_CodeIgnoreCaseAndActiveTrue(String tenantCode);

    @Query("select new com.synapsecore.domain.repository.TenantCount(lower(u.tenant.code), count(u)) "
        + "from AccessUser u where u.active = true group by lower(u.tenant.code)")
    List<TenantCount> countActiveByTenant();

    long countByTenant_CodeIgnoreCaseAndActiveFalse(String tenantCode);
}
