package com.synapsecore.domain.service;

import com.synapsecore.audit.AuditLogService;
import com.synapsecore.domain.dto.OrderCreateRequest;
import com.synapsecore.domain.dto.OrderItemResponse;
import com.synapsecore.domain.dto.OrderResponse;
import com.synapsecore.domain.entity.BusinessEventType;
import com.synapsecore.domain.entity.CustomerOrder;
import com.synapsecore.domain.entity.Inventory;
import com.synapsecore.domain.entity.OrderItem;
import com.synapsecore.domain.entity.OrderStatus;
import com.synapsecore.domain.entity.Product;
import com.synapsecore.domain.entity.Warehouse;
import com.synapsecore.domain.repository.CustomerOrderRepository;
import com.synapsecore.domain.repository.InventoryRepository;
import com.synapsecore.domain.repository.ProductRepository;
import com.synapsecore.domain.repository.WarehouseRepository;
import com.synapsecore.event.BusinessEventService;
import com.synapsecore.event.OperationalStateChangePublisher;
import com.synapsecore.event.OperationalUpdateType;
import com.synapsecore.fulfillment.FulfillmentService;
import com.synapsecore.intelligence.InventoryMonitoringService;
import com.synapsecore.observability.OperationalMetricsService;
import com.synapsecore.tenant.TenantContextService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final CustomerOrderRepository customerOrderRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryMonitoringService inventoryMonitoringService;
    private final BusinessEventService businessEventService;
    private final OperationalStateChangePublisher operationalStateChangePublisher;
    private final AuditLogService auditLogService;
    private final FulfillmentService fulfillmentService;
    private final TenantContextService tenantContextService;
    private final OperationalMetricsService operationalMetricsService;

    @Transactional
    public OrderResponse createOrder(OrderCreateRequest request) {
        return createOrder(request, "order-api");
    }

    @Transactional
    public OrderResponse createOrder(OrderCreateRequest request, String source) {
        String tenantCode = tenantContextService.getCurrentTenantCodeOrDefault();
        Warehouse warehouse = warehouseRepository.findByTenant_CodeIgnoreCaseAndCode(tenantCode, request.warehouseCode().trim())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Warehouse not found: " + request.warehouseCode()));
        String externalOrderId = resolveExternalOrderId(request.externalOrderId(), source);
        ensureOrderIdIsAvailable(tenantCode, externalOrderId);

        CustomerOrder order = CustomerOrder.builder()
            .tenant(warehouse.getTenant())
            .externalOrderId(externalOrderId)
            .status(OrderStatus.RECEIVED)
            .totalAmount(BigDecimal.ZERO)
            .warehouse(warehouse)
            .items(new ArrayList<>())
            .build();

        BigDecimal totalAmount = BigDecimal.ZERO;
        Map<Long, Inventory> impactedInventory = new LinkedHashMap<>();

        for (var itemRequest : request.items()) {
            Product product = productRepository.findBySku(itemRequest.productSku().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Product not found: " + itemRequest.productSku()));

            Inventory inventory = inventoryRepository.findByProductIdAndWarehouseId(product.getId(), warehouse.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No inventory found for SKU " + product.getSku() + " in warehouse " + warehouse.getCode()));

            if (inventory.getQuantityAvailable() < itemRequest.quantity()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Insufficient inventory for SKU " + product.getSku() + " in warehouse " + warehouse.getCode());
            }

            inventory.setQuantityAvailable(inventory.getQuantityAvailable() - itemRequest.quantity());
            inventoryRepository.save(inventory);
            impactedInventory.put(inventory.getId(), inventory);

            OrderItem orderItem = OrderItem.builder()
                .customerOrder(order)
                .product(product)
                .quantity(itemRequest.quantity())
                .unitPrice(itemRequest.unitPrice())
                .build();

            order.getItems().add(orderItem);
            totalAmount = totalAmount.add(itemRequest.unitPrice().multiply(BigDecimal.valueOf(itemRequest.quantity())));
        }

        order.setTotalAmount(totalAmount);
        CustomerOrder savedOrder = customerOrderRepository.save(order);
        fulfillmentService.initializeForOrder(savedOrder, source);
        operationalMetricsService.recordOrderIngested(tenantCode, source);

        businessEventService.record(
            BusinessEventType.ORDER_INGESTED,
            source,
            "Order " + savedOrder.getExternalOrderId() + " ingested for warehouse " + warehouse.getCode()
                + " with " + savedOrder.getItems().size() + " line items."
        );

        impactedInventory.values().forEach(inventory -> {
            businessEventService.record(
                BusinessEventType.INVENTORY_UPDATED,
                source,
                inventory.getProduct().getSku() + " reduced to " + inventory.getQuantityAvailable()
                    + " units in " + inventory.getWarehouse().getCode()
            );
            inventoryMonitoringService.evaluateAfterChange(inventory, source);
        });

        operationalStateChangePublisher.publish(OperationalUpdateType.ORDER_FLOW, source);
        auditLogService.recordSuccess(
            "ORDER_PROCESSED",
            source,
            source,
            "CustomerOrder",
            savedOrder.getExternalOrderId(),
            "Processed order for warehouse " + warehouse.getCode()
                + " with " + savedOrder.getItems().size() + " line items and total amount " + savedOrder.getTotalAmount()
        );

        return toOrderResponse(savedOrder);
    }

    private String resolveExternalOrderId(String externalOrderId, String source) {
        if (externalOrderId != null && !externalOrderId.isBlank()) {
            return externalOrderId.trim();
        }
        String prefix = "simulation".equalsIgnoreCase(source) ? "SIM" : "ORD";
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private void ensureOrderIdIsAvailable(String tenantCode, String externalOrderId) {
        if (customerOrderRepository.existsByTenant_CodeIgnoreCaseAndExternalOrderId(tenantCode, externalOrderId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Order already exists for externalOrderId " + externalOrderId);
        }
    }

    private OrderResponse toOrderResponse(CustomerOrder order) {
        List<OrderItemResponse> items = order.getItems().stream()
            .map(item -> new OrderItemResponse(
                item.getProduct().getSku(),
                item.getProduct().getName(),
                item.getQuantity(),
                item.getUnitPrice()
            ))
            .toList();

        return new OrderResponse(
            order.getId(),
            order.getExternalOrderId(),
            order.getStatus(),
            order.getWarehouse().getCode(),
            order.getWarehouse().getName(),
            order.getTotalAmount(),
            items.stream().mapToInt(OrderItemResponse::quantity).sum(),
            order.getCreatedAt(),
            items
        );
    }
}
