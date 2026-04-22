package com.synapsecore.domain.repository;

import com.synapsecore.domain.entity.Product;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySku(String sku);

    Optional<Product> findByTenant_CodeIgnoreCaseAndCatalogSkuIgnoreCase(String tenantCode, String catalogSku);

    Optional<Product> findByTenant_CodeIgnoreCaseAndId(String tenantCode, Long id);

    List<Product> findAllByTenant_CodeIgnoreCaseOrderByNameAsc(String tenantCode);

    List<Product> findAllByTenantIsNullOrderByNameAsc();
}
