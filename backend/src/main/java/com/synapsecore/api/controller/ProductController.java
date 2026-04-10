package com.synapsecore.api.controller;

import com.synapsecore.access.AccessControlService;
import com.synapsecore.domain.dto.ProductResponse;
import com.synapsecore.domain.service.ProductService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final AccessControlService accessControlService;
    private final ProductService productService;

    @GetMapping
    public List<ProductResponse> getProducts() {
        accessControlService.requireWorkspaceAccess("view products");
        return productService.getProducts();
    }
}
