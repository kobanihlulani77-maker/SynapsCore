package com.synapsecore.api.controller;

import com.synapsecore.access.AccessControlService;
import com.synapsecore.domain.dto.ProductImportResponse;
import com.synapsecore.domain.dto.ProductResponse;
import com.synapsecore.domain.dto.ProductUpsertRequest;
import com.synapsecore.domain.service.ProductService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RequestPart;

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

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse createProduct(@Valid @RequestBody ProductUpsertRequest request) {
        var actor = accessControlService.requireTenantAdmin("create products");
        return productService.createProduct(request, actor.actorName());
    }

    @PutMapping("/{productId}")
    public ProductResponse updateProduct(@PathVariable Long productId,
                                          @Valid @RequestBody ProductUpsertRequest request) {
        var actor = accessControlService.requireTenantAdmin("update products");
        return productService.updateProduct(productId, request, actor.actorName());
    }

    @PostMapping(path = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProductImportResponse importProducts(@RequestPart("file") MultipartFile file) {
        var actor = accessControlService.requireTenantAdmin("import products");
        return productService.importProducts(file, actor.actorName());
    }
}
