package com.synapsecore.domain.service;

import com.synapsecore.domain.entity.Inventory;
import com.synapsecore.domain.entity.OrderItem;
import com.synapsecore.domain.entity.Product;
import com.synapsecore.domain.entity.Tenant;
import com.synapsecore.domain.repository.InventoryRepository;
import com.synapsecore.domain.repository.OrderItemRepository;
import com.synapsecore.domain.repository.ProductRepository;
import com.synapsecore.domain.repository.TenantRepository;
import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class CatalogTenantOwnershipMigrationService {

    private final ProductRepository productRepository;
    private final TenantRepository tenantRepository;
    private final InventoryRepository inventoryRepository;
    private final OrderItemRepository orderItemRepository;
    private final TransactionTemplate transactionTemplate;
    private final InventorySchemaMigrationService inventorySchemaMigrationService;

    @PostConstruct
    void migrateCatalogOwnership() {
        inventorySchemaMigrationService.migrateInventoryStockColumns();
        transactionTemplate.executeWithoutResult(status -> backfillTenantOwnedCatalog());
    }

    private void backfillTenantOwnedCatalog() {
        List<Product> products = productRepository.findAll();
        if (products.isEmpty()) {
            return;
        }

        Map<String, Product> tenantScopedProducts = new HashMap<>();
        Set<Product> productUpdates = new HashSet<>();
        for (Product product : products) {
            Tenant tenant = product.getTenant();
            if (tenant == null) {
                tenant = resolveTenantFromInternalSku(product);
                if (tenant != null) {
                    product.setTenant(tenant);
                }
            }
            if (tenant != null) {
                normalizeProduct(product);
                tenantScopedProducts.put(productKey(tenant.getCode(), product.resolveCatalogSku()), product);
                productUpdates.add(product);
            }
        }
        if (!productUpdates.isEmpty()) {
            productRepository.saveAll(productUpdates);
        }

        List<Inventory> inventoryUpdates = new java.util.ArrayList<>();
        for (Inventory inventory : inventoryRepository.findAllWithProductAndWarehouse()) {
            if (inventory.getWarehouse() == null || inventory.getWarehouse().getTenant() == null || inventory.getProduct() == null) {
                continue;
            }
            Tenant tenant = inventory.getWarehouse().getTenant();
            Product targetProduct = resolveTenantOwnedProduct(inventory.getProduct(), tenant, tenantScopedProducts);
            boolean changed = false;
            if (!Objects.equals(inventory.getProduct().getId(), targetProduct.getId())) {
                inventory.setProduct(targetProduct);
                changed = true;
            }
            if (inventory.getTenant() == null || !inventory.getTenant().getCode().equalsIgnoreCase(tenant.getCode())) {
                inventory.setTenant(tenant);
                changed = true;
            }
            if (changed) {
                inventory.synchronizeStockModel();
                inventoryUpdates.add(inventory);
            }
        }
        if (!inventoryUpdates.isEmpty()) {
            inventoryRepository.saveAll(inventoryUpdates);
        }

        List<OrderItem> orderItemUpdates = new java.util.ArrayList<>();
        for (OrderItem orderItem : orderItemRepository.findAll()) {
            if (orderItem.getCustomerOrder() == null || orderItem.getCustomerOrder().getTenant() == null || orderItem.getProduct() == null) {
                continue;
            }
            Tenant tenant = orderItem.getCustomerOrder().getTenant();
            Product targetProduct = resolveTenantOwnedProduct(orderItem.getProduct(), tenant, tenantScopedProducts);
            boolean changed = false;
            if (!Objects.equals(orderItem.getProduct().getId(), targetProduct.getId())) {
                orderItem.setProduct(targetProduct);
                changed = true;
            }
            if (orderItem.getTenant() == null || !orderItem.getTenant().getCode().equalsIgnoreCase(tenant.getCode())) {
                orderItem.setTenant(tenant);
                changed = true;
            }
            if (changed) {
                orderItemUpdates.add(orderItem);
            }
        }
        if (!orderItemUpdates.isEmpty()) {
            orderItemRepository.saveAll(orderItemUpdates);
        }
    }

    private Product resolveTenantOwnedProduct(Product source,
                                              Tenant tenant,
                                              Map<String, Product> tenantScopedProducts) {
        String catalogSku = deriveCatalogSku(source);
        String key = productKey(tenant.getCode(), catalogSku);
        Product existing = tenantScopedProducts.get(key);
        if (existing != null) {
            return existing;
        }

        if (source.getTenant() != null && source.getTenant().getCode().equalsIgnoreCase(tenant.getCode())) {
            normalizeProduct(source);
            tenantScopedProducts.put(key, source);
            productRepository.save(source);
            return source;
        }

        Product clonedProduct = productRepository.save(Product.builder()
            .tenant(tenant)
            .catalogSku(catalogSku)
            .name(source.getName())
            .category(source.getCategory())
            .build());
        tenantScopedProducts.put(key, clonedProduct);
        return clonedProduct;
    }

    private void normalizeProduct(Product product) {
        if (product.getCatalogSku() == null || product.getCatalogSku().isBlank()) {
            product.setCatalogSku(deriveCatalogSku(product));
        }
        product.setSku(Product.buildInternalSku(product.getTenant().getCode(), product.getCatalogSku()));
    }

    private String deriveCatalogSku(Product product) {
        if (product.getCatalogSku() != null && !product.getCatalogSku().isBlank()) {
            return product.getCatalogSku().trim().toUpperCase(Locale.ROOT);
        }
        if (product.getSku() == null || product.getSku().isBlank()) {
            throw new IllegalStateException("Product " + product.getId() + " does not have a usable SKU.");
        }
        String rawSku = product.getSku().trim();
        int separatorIndex = rawSku.indexOf("::");
        String derived = separatorIndex >= 0 ? rawSku.substring(separatorIndex + 2) : rawSku;
        return derived.toUpperCase(Locale.ROOT);
    }

    private Tenant resolveTenantFromInternalSku(Product product) {
        if (product.getSku() == null || product.getSku().isBlank()) {
            return null;
        }
        String rawSku = product.getSku().trim();
        int separatorIndex = rawSku.indexOf("::");
        if (separatorIndex <= 0) {
            return null;
        }
        String tenantCode = rawSku.substring(0, separatorIndex).trim();
        if (tenantCode.isEmpty()) {
            return null;
        }
        return tenantRepository.findByCodeIgnoreCase(tenantCode).orElse(null);
    }

    private String productKey(String tenantCode, String catalogSku) {
        return tenantCode.trim().toUpperCase(Locale.ROOT) + "::" + catalogSku.trim().toUpperCase(Locale.ROOT);
    }
}
