package com.synapsecore.domain.repository;

import com.synapsecore.domain.entity.AccessUser;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccessUserRepository extends JpaRepository<AccessUser, Long> {

    Optional<AccessUser> findByUsernameIgnoreCaseAndActiveTrue(String username);

    Optional<AccessUser> findByTenant_CodeIgnoreCaseAndUsernameIgnoreCaseAndActiveTrue(String tenantCode, String username);

    Optional<AccessUser> findByTenant_CodeIgnoreCaseAndUsernameIgnoreCase(String tenantCode, String username);

    Optional<AccessUser> findByTenant_CodeIgnoreCaseAndId(String tenantCode, Long id);

    List<AccessUser> findAllByTenant_CodeIgnoreCaseOrderByFullNameAscUsernameAsc(String tenantCode);

    long countByTenant_CodeIgnoreCaseAndActiveTrue(String tenantCode);

    long countByTenant_CodeIgnoreCaseAndActiveFalse(String tenantCode);
}
