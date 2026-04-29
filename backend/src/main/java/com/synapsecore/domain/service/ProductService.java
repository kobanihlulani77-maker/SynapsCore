package com.synapsecore.domain.service;

import com.synapsecore.audit.AuditLogService;
import com.synapsecore.domain.dto.ProductImportResponse;
import com.synapsecore.domain.dto.ProductImportRowResult;
import com.synapsecore.domain.dto.ProductResponse;
import com.synapsecore.domain.dto.ProductUpsertRequest;
import com.synapsecore.domain.entity.BusinessEventType;
import com.synapsecore.domain.entity.Product;
import com.synapsecore.domain.entity.Tenant;
import com.synapsecore.domain.repository.AuditLogRepository;
import com.synapsecore.domain.repository.BusinessEventRepository;
import com.synapsecore.domain.repository.OperationalDispatchWorkItemRepository;
import com.synapsecore.domain.repository.ProductRepository;
import com.synapsecore.event.BusinessEventService;
import com.synapsecore.event.OperationalStateChangePublisher;
import com.synapsecore.event.OperationalUpdateType;
import com.synapsecore.observability.OperationalMetricsService;
import com.synapsecore.tenant.TenantContextService;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private static final Pattern CATALOG_SKU_PATTERN = Pattern.compile("^[A-Z0-9][A-Z0-9._-]{0,63}$");

    private final ProductRepository productRepository;
    private final TenantContextService tenantContextService;
    private final BusinessEventService businessEventService;
    private final OperationalStateChangePublisher operationalStateChangePublisher;
    private final AuditLogService auditLogService;
    private final IdentitySequenceMigrationService identitySequenceMigrationService;
    private final BusinessEventRepository businessEventRepository;
    private final AuditLogRepository auditLogRepository;
    private final OperationalDispatchWorkItemRepository operationalDispatchWorkItemRepository;
    private final CatalogWriteConflictResolver catalogWriteConflictResolver;
    private final OperationalMetricsService operationalMetricsService;

    @Transactional(readOnly = true)
    public List<ProductResponse> getProducts() {
        String tenantCode = tenantContextService.getCurrentTenantCodeOrDefault();
        return productRepository.findAllByTenant_CodeIgnoreCaseOrderByNameAsc(
                tenantCode)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public ProductResponse createProduct(ProductUpsertRequest request, String actorName) {
        identitySequenceMigrationService.synchronizeCoreIdentitySequences();
        Tenant tenant = tenantContextService.getCurrentTenantOrDefault();
        String catalogSku = normalizeCatalogSku(request.sku());
        String normalizedName = normalizeRequiredText(request.name(), "Product name", 120);
        String normalizedCategory = normalizeRequiredText(request.category(), "Product category", 120);
        ensureSkuIsAvailable(tenant.getCode(), catalogSku);
        try {
            Product adoptedProduct = adoptOrphanedProductIfPresent(tenant, catalogSku, normalizedName, normalizedCategory);
            if (adoptedProduct != null) {
                recordCatalogChange(
                    tenant.getCode(),
                    actorName,
                    "PRODUCT_CREATED",
                    adoptedProduct.resolveCatalogSku(),
                    "Adopted orphan catalog product " + adoptedProduct.resolveCatalogSku() + " (" + adoptedProduct.getName() + ") into tenant " + tenant.getCode() + "."
                );
                flushCatalogWritePath();
                operationalMetricsService.recordCatalogWrite(tenant.getCode(), "PRODUCT_CREATED", true);
                log.info("Catalog product {} adopted into tenant {}.", adoptedProduct.resolveCatalogSku(), tenant.getCode());
                return toResponse(adoptedProduct);
            }
            ensureInternalSkuIsAvailable(tenant.getCode(), catalogSku);

            Product product = productRepository.save(Product.builder()
                .tenant(tenant)
                .catalogSku(catalogSku)
                .name(normalizedName)
                .category(normalizedCategory)
                .build());

            recordCatalogChange(
                tenant.getCode(),
                actorName,
                "PRODUCT_CREATED",
                product.resolveCatalogSku(),
                "Created catalog product " + product.resolveCatalogSku() + " (" + product.getName() + ")."
            );
            flushCatalogWritePath();
            operationalMetricsService.recordCatalogWrite(tenant.getCode(), "PRODUCT_CREATED", true);
            log.info("Catalog product {} created for tenant {} by {}.", product.resolveCatalogSku(), tenant.getCode(), actorName);
            return toResponse(product);
        } catch (DataIntegrityViolationException exception) {
            operationalMetricsService.recordCatalogWrite(tenant.getCode(), "PRODUCT_CREATED", false);
            log.warn("Catalog product create failed for tenant {} sku {}: {}", tenant.getCode(), catalogSku, exception.getMostSpecificCause() == null ? exception.getMessage() : exception.getMostSpecificCause().getMessage());
            throw catalogWriteConflictResolver.toResponseStatus(exception, catalogSku);
        }
    }

    @Transactional
    public ProductResponse updateProduct(Long productId, ProductUpsertRequest request, String actorName) {
        Tenant tenant = tenantContextService.getCurrentTenantOrDefault();
        Product product = productRepository.findByTenant_CodeIgnoreCaseAndId(tenant.getCode(), productId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Product not found for tenant " + tenant.getCode() + ": " + productId));
        String catalogSku = normalizeCatalogSku(request.sku());
        productRepository.findByTenant_CodeIgnoreCaseAndCatalogSkuIgnoreCase(tenant.getCode(), catalogSku)
            .filter(existing -> !existing.getId().equals(product.getId()))
            .ifPresent(existing -> {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Product SKU already exists for this tenant: " + catalogSku);
            });

        product.setCatalogSku(catalogSku);
        product.setName(normalizeRequiredText(request.name(), "Product name", 120));
        product.setCategory(normalizeRequiredText(request.category(), "Product category", 120));
        try {
            Product savedProduct = productRepository.save(product);

            recordCatalogChange(
                tenant.getCode(),
                actorName,
                "PRODUCT_UPDATED",
                savedProduct.resolveCatalogSku(),
                "Updated catalog product " + savedProduct.resolveCatalogSku() + " (" + savedProduct.getName() + ")."
            );
            flushCatalogWritePath();
            operationalMetricsService.recordCatalogWrite(tenant.getCode(), "PRODUCT_UPDATED", true);
            log.info("Catalog product {} updated for tenant {} by {}.", savedProduct.resolveCatalogSku(), tenant.getCode(), actorName);
            return toResponse(savedProduct);
        } catch (DataIntegrityViolationException exception) {
            operationalMetricsService.recordCatalogWrite(tenant.getCode(), "PRODUCT_UPDATED", false);
            log.warn("Catalog product update failed for tenant {} sku {}: {}", tenant.getCode(), catalogSku, exception.getMostSpecificCause() == null ? exception.getMessage() : exception.getMostSpecificCause().getMessage());
            throw catalogWriteConflictResolver.toResponseStatus(exception, catalogSku);
        }
    }

    @Transactional
    public ProductImportResponse importProducts(MultipartFile file, String actorName) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A non-empty product CSV file is required.");
        }

        identitySequenceMigrationService.synchronizeCoreIdentitySequences();
        Tenant tenant = tenantContextService.getCurrentTenantOrDefault();
        List<ProductImportRowResult> rowResults = new ArrayList<>();
        Set<String> seenSkus = new HashSet<>();
        int totalRows = 0;
        int created = 0;
        int updated = 0;
        int failed = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product CSV must include a header row.");
            }

            List<String> headers = parseCsvLine(headerLine);
            Map<String, Integer> headerIndex = buildHeaderIndex(headers);
            int skuIndex = requireHeader(headerIndex, "sku", "catalogsku", "productsku");
            int nameIndex = requireHeader(headerIndex, "name", "productname");
            int categoryIndex = requireHeader(headerIndex, "category");

            String line;
            int rowNumber = 1;
            while ((line = reader.readLine()) != null) {
                rowNumber++;
                if (line.isBlank()) {
                    continue;
                }

                totalRows++;
                List<String> values = parseCsvLine(line);
                String rawSku = valueAt(values, skuIndex);
                try {
                    String catalogSku = normalizeCatalogSku(rawSku);
                    String name = normalizeRequiredText(valueAt(values, nameIndex), "Product name", 120);
                    String category = normalizeRequiredText(valueAt(values, categoryIndex), "Product category", 120);
                    if (!seenSkus.add(catalogSku)) {
                        failed++;
                        rowResults.add(new ProductImportRowResult(
                            rowNumber,
                            catalogSku,
                            "FAILED",
                            "Duplicate SKU inside this import file.",
                            null
                        ));
                        continue;
                    }

                    var existingProduct = productRepository
                        .findByTenant_CodeIgnoreCaseAndCatalogSkuIgnoreCase(tenant.getCode(), catalogSku);
                    Product adoptedProduct = adoptOrphanedProductIfPresent(tenant, catalogSku, name, category);
                    boolean wasCreated = existingProduct.isEmpty() && adoptedProduct == null;
                    Product product = existingProduct
                        .map(existing -> {
                            existing.setName(name);
                            existing.setCategory(category);
                            return productRepository.save(existing);
                        })
                        .orElseGet(() -> {
                            if (adoptedProduct != null) {
                                return adoptedProduct;
                            }
                            ensureInternalSkuIsAvailable(tenant.getCode(), catalogSku);
                            return productRepository.save(Product.builder()
                                .tenant(tenant)
                                .catalogSku(catalogSku)
                                .name(name)
                                .category(category)
                                .build());
                        });

                    if (wasCreated) {
                        created++;
                    } else {
                        updated++;
                    }
                    rowResults.add(new ProductImportRowResult(
                        rowNumber,
                        catalogSku,
                        wasCreated ? "CREATED" : "UPDATED",
                        wasCreated ? "Product created." : "Existing product updated.",
                        toResponse(product)
                    ));
                } catch (DataIntegrityViolationException exception) {
                    failed++;
                    rowResults.add(new ProductImportRowResult(
                        rowNumber,
                        rawSku == null ? "" : rawSku.trim(),
                        "FAILED",
                        catalogWriteConflictResolver.describe(exception, rawSku == null ? "" : rawSku.trim()),
                        null
                    ));
                } catch (ResponseStatusException exception) {
                    failed++;
                    rowResults.add(new ProductImportRowResult(
                        rowNumber,
                        rawSku == null ? "" : rawSku.trim(),
                        "FAILED",
                        exception.getReason(),
                        null
                    ));
                }
            }
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to read product CSV file.");
        }

        if (totalRows == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product CSV did not contain any product rows.");
        }

        recordCatalogChange(
            tenant.getCode(),
            actorName,
            "PRODUCT_IMPORT",
            file.getOriginalFilename() == null ? "catalog-import" : file.getOriginalFilename(),
            "Imported product catalog rows: created " + created + ", updated " + updated + ", failed " + failed + "."
        );
        operationalMetricsService.recordCatalogWrite(tenant.getCode(), "PRODUCT_IMPORT", failed == 0);
        log.info("Product catalog import completed for tenant {} by {}: created {}, updated {}, failed {}.", tenant.getCode(), actorName, created, updated, failed);
        return new ProductImportResponse(totalRows, created, updated, failed, rowResults);
    }

    private void ensureSkuIsAvailable(String tenantCode, String catalogSku) {
        productRepository.findByTenant_CodeIgnoreCaseAndCatalogSkuIgnoreCase(tenantCode, catalogSku)
            .ifPresent(existing -> {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Product SKU already exists for this tenant: " + catalogSku);
            });
    }

    private void ensureInternalSkuIsAvailable(String tenantCode, String catalogSku) {
        String internalSku = Product.buildInternalSku(tenantCode, catalogSku);
        productRepository.findBySku(internalSku)
            .ifPresent(existing -> {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Product internal SKU already exists for this tenant context: " + internalSku);
            });
    }

    private Product adoptOrphanedProductIfPresent(Tenant tenant, String catalogSku, String name, String category) {
        String internalSku = Product.buildInternalSku(tenant.getCode(), catalogSku);
        Map<Long, Product> orphanMatches = new LinkedHashMap<>();
        productRepository.findBySku(internalSku)
            .filter(existing -> existing.getTenant() == null)
            .ifPresent(existing -> orphanMatches.put(existing.getId(), existing));
        for (Product orphanByCatalogSku : productRepository.findAllByTenantIsNullAndCatalogSkuIgnoreCase(catalogSku)) {
            orphanMatches.put(orphanByCatalogSku.getId(), orphanByCatalogSku);
        }

        if (orphanMatches.isEmpty()) {
            return null;
        }
        if (orphanMatches.size() > 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Multiple orphan catalog rows exist for tenant " + tenant.getCode() + " and SKU " + catalogSku + ".");
        }

        Product existing = orphanMatches.values().iterator().next();
        existing.setTenant(tenant);
        existing.setCatalogSku(catalogSku);
        existing.setName(name);
        existing.setCategory(category);
        return productRepository.save(existing);
    }

    private String normalizeCatalogSku(String value) {
        String normalized = normalizeRequiredText(value, "Product SKU", 64).toUpperCase(Locale.ROOT);
        if (!CATALOG_SKU_PATTERN.matcher(normalized).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Product SKU must start with a letter or number and may only contain letters, numbers, dots, underscores, and hyphens.");
        }
        return normalized;
    }

    private String normalizeRequiredText(String value, String fieldName, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " is required.");
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                fieldName + " must be " + maxLength + " characters or fewer.");
        }
        return normalized;
    }

    private Map<String, Integer> buildHeaderIndex(List<String> headers) {
        Map<String, Integer> headerIndex = new HashMap<>();
        for (int index = 0; index < headers.size(); index++) {
            headerIndex.put(normalizeHeader(headers.get(index)), index);
        }
        return headerIndex;
    }

    private int requireHeader(Map<String, Integer> headerIndex, String... aliases) {
        for (String alias : aliases) {
            Integer index = headerIndex.get(alias);
            if (index != null) {
                return index;
            }
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
            "Product CSV must include headers for sku, name, and category.");
    }

    private String normalizeHeader(String header) {
        return header == null ? "" : header.replace("\uFEFF", "").trim().toLowerCase(Locale.ROOT).replace("_", "").replace("-", "").replace(" ", "");
    }

    private String valueAt(List<String> values, int index) {
        return index < values.size() ? values.get(index) : "";
    }

    private List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < line.length(); index++) {
            char currentChar = line.charAt(index);
            if (currentChar == '"') {
                boolean escapedQuote = quoted && index + 1 < line.length() && line.charAt(index + 1) == '"';
                if (escapedQuote) {
                    current.append('"');
                    index++;
                } else {
                    quoted = !quoted;
                }
            } else if (currentChar == ',' && !quoted) {
                values.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(currentChar);
            }
        }
        values.add(current.toString().trim());
        return values;
    }

    private void recordCatalogChange(String tenantCode,
                                     String actorName,
                                     String action,
                                     String targetRef,
                                     String details) {
        businessEventService.record(
            BusinessEventType.PRODUCT_CATALOG_UPDATED,
            "product-catalog",
            details
        );
        operationalStateChangePublisher.publish(OperationalUpdateType.INVENTORY_UPDATE, "product-catalog");
        auditLogService.recordSuccessForTenant(
            tenantCode,
            action,
            actorName,
            "product-catalog",
            "Product",
            targetRef,
            details
        );
    }

    private void flushCatalogWritePath() {
        productRepository.flush();
        businessEventRepository.flush();
        auditLogRepository.flush();
        operationalDispatchWorkItemRepository.flush();
    }

    private ProductResponse toResponse(Product product) {
        return new ProductResponse(
            product.getId(),
            product.resolveCatalogSku(),
            product.resolveCatalogSku(),
            product.getSku(),
            product.getName(),
            product.getCategory(),
            product.getTenant() == null ? null : product.getTenant().getCode()
        );
    }
}
