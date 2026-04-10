package com.synapsecore.domain.repository;

import com.synapsecore.domain.entity.Tenant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantRepository extends JpaRepository<Tenant, Long> {

    Optional<Tenant> findByCodeIgnoreCase(String code);

    List<Tenant> findAllByActiveTrueOrderByNameAsc();
}
