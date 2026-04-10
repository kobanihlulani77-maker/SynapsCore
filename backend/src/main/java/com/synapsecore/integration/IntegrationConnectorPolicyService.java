package com.synapsecore.integration;

import com.synapsecore.domain.dto.OrderCreateRequest;
import com.synapsecore.domain.dto.OrderItemRequest;
import com.synapsecore.domain.entity.IntegrationConnector;
import com.synapsecore.domain.entity.IntegrationConnectorType;
import com.synapsecore.domain.entity.IntegrationTransformationPolicy;
import com.synapsecore.domain.entity.IntegrationValidationPolicy;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class IntegrationConnectorPolicyService {

    public PreparedConnectorOrder prepareOrder(IntegrationConnector connector,
                                               String externalOrderId,
                                               String warehouseCode,
                                               String customerReference,
                                               Instant occurredAt,
                                               List<OrderItemRequest> rawItems) {
        if (externalOrderId == null || externalOrderId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "externalOrderId is required");
        }
        if (rawItems == null || rawItems.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "items must contain at least one entry");
        }

        String resolvedWarehouseCode = resolveWarehouseCode(connector, warehouseCode);
        List<OrderItemRequest> preparedItems = normalizeItems(connector, rawItems);
        validatePreparedOrder(connector, resolvedWarehouseCode, customerReference, occurredAt, preparedItems);

        return new PreparedConnectorOrder(
            new OrderCreateRequest(externalOrderId.trim(), resolvedWarehouseCode, preparedItems),
            preparedItems.size()
        );
    }

    private String resolveWarehouseCode(IntegrationConnector connector, String warehouseCode) {
        String normalizedWarehouseCode = normalizeCode(warehouseCode, connector.getTransformationPolicy());
        String defaultWarehouseCode = normalizeCode(connector.getDefaultWarehouseCode(), connector.getTransformationPolicy());

        if (normalizedWarehouseCode == null) {
            if (connector.isAllowDefaultWarehouseFallback() && defaultWarehouseCode != null) {
                return defaultWarehouseCode;
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "warehouseCode is required for connector " + connector.getSourceSystem());
        }

        if (connector.getValidationPolicy() == IntegrationValidationPolicy.STRICT
            && defaultWarehouseCode != null
            && !defaultWarehouseCode.equalsIgnoreCase(normalizedWarehouseCode)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Connector " + connector.getSourceSystem() + " only accepts warehouse " + defaultWarehouseCode);
        }

        return normalizedWarehouseCode;
    }

    private List<OrderItemRequest> normalizeItems(IntegrationConnector connector, List<OrderItemRequest> rawItems) {
        List<OrderItemRequest> normalizedItems = rawItems.stream()
            .map(item -> new OrderItemRequest(
                normalizeProductSku(item.productSku(), connector.getTransformationPolicy()),
                item.quantity(),
                item.unitPrice()
            ))
            .toList();

        if (connector.getValidationPolicy() == IntegrationValidationPolicy.RELAXED) {
            return deduplicateItems(normalizedItems);
        }
        return normalizedItems;
    }

    private void validatePreparedOrder(IntegrationConnector connector,
                                       String warehouseCode,
                                       String customerReference,
                                       Instant occurredAt,
                                       List<OrderItemRequest> items) {
        if (connector.getType() == IntegrationConnectorType.WEBHOOK_ORDER
            && connector.getValidationPolicy() == IntegrationValidationPolicy.STRICT) {
            if (customerReference == null || customerReference.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "customerReference is required for strict webhook connectors");
            }
            if (occurredAt == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "occurredAt is required for strict webhook connectors");
            }
        }

        if (connector.getValidationPolicy() == IntegrationValidationPolicy.STRICT) {
            Map<String, Long> counts = items.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    item -> item.productSku().trim().toUpperCase(Locale.ROOT),
                    LinkedHashMap::new,
                    java.util.stream.Collectors.counting()
                ));
            List<String> duplicates = counts.entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .map(Map.Entry::getKey)
                .toList();
            if (!duplicates.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Strict connector validation does not allow duplicate product lines: " + duplicates);
            }
        }

        if ((warehouseCode == null || warehouseCode.isBlank())
            && (!connector.isAllowDefaultWarehouseFallback() || connector.getDefaultWarehouseCode() == null || connector.getDefaultWarehouseCode().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "warehouseCode is required for connector " + connector.getSourceSystem());
        }
    }

    private List<OrderItemRequest> deduplicateItems(List<OrderItemRequest> items) {
        Map<String, OrderItemRequest> consolidated = new LinkedHashMap<>();
        for (OrderItemRequest item : items) {
            String key = item.productSku().trim().toUpperCase(Locale.ROOT);
            OrderItemRequest existing = consolidated.get(key);
            if (existing == null) {
                consolidated.put(key, item);
            } else {
                consolidated.put(key, new OrderItemRequest(
                    existing.productSku(),
                    existing.quantity() + item.quantity(),
                    item.unitPrice()
                ));
            }
        }
        return List.copyOf(consolidated.values());
    }

    private String normalizeProductSku(String productSku, IntegrationTransformationPolicy transformationPolicy) {
        if (productSku == null || productSku.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "productSku is required");
        }
        return normalizeCode(productSku, transformationPolicy);
    }

    private String normalizeCode(String value, IntegrationTransformationPolicy transformationPolicy) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (transformationPolicy == IntegrationTransformationPolicy.NORMALIZE_CODES) {
            return normalized.toUpperCase(Locale.ROOT);
        }
        return normalized;
    }

    public record PreparedConnectorOrder(
        OrderCreateRequest orderRequest,
        int lineItemCount
    ) {
    }
}
