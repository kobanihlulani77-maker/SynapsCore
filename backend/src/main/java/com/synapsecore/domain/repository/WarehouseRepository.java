package com.synapsecore.domain.repository;

import com.synapsecore.domain.entity.Warehouse;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {

    Optional<Warehouse> findByCode(String code);

    Optional<Warehouse> findByTenant_CodeIgnoreCaseAndCode(String tenantCode, String code);

    Optional<Warehouse> findByTenant_CodeIgnoreCaseAndId(String tenantCode, Long id);

    List<Warehouse> findAllByOrderByNameAsc();

    List<Warehouse> findAllByTenant_CodeIgnoreCaseOrderByNameAsc(String tenantCode);

    long countByTenant_CodeIgnoreCase(String tenantCode);
}
