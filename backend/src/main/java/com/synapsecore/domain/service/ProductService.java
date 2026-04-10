package com.synapsecore.domain.service;

import com.synapsecore.domain.dto.ProductResponse;
import com.synapsecore.domain.repository.ProductRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public List<ProductResponse> getProducts() {
        return productRepository.findAllByOrderByNameAsc().stream()
            .map(product -> new ProductResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getCategory()
            ))
            .toList();
    }
}
