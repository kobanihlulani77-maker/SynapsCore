package com.synapsecore.domain.repository;

import com.synapsecore.domain.entity.TenantOperationalPolicy;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantOperationalPolicyRepository extends JpaRepository<TenantOperationalPolicy, Long> {

    Optional<TenantOperationalPolicy> findByTenant_CodeIgnoreCase(String tenantCode);
}
