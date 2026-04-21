package com.synapsecore.domain.service;

import com.synapsecore.domain.dto.ProductResponse;
import com.synapsecore.domain.repository.ProductRepository;
import com.synapsecore.tenant.TenantContextService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final TenantContextService tenantContextService;

    public List<ProductResponse> getProducts() {
        return productRepository.findAllByTenant_CodeIgnoreCaseOrderByNameAsc(
                tenantContextService.getCurrentTenantCodeOrDefault())
            .stream()
            .map(product -> new ProductResponse(
                product.getId(),
                product.resolveCatalogSku(),
                product.getName(),
                product.getCategory(),
                product.getTenant() == null ? null : product.getTenant().getCode()
            ))
            .toList();
    }
}
