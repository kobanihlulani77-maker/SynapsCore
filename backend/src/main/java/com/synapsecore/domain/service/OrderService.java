package com.synapsecore.domain.service;

import com.synapsecore.audit.AuditLogService;
import com.synapsecore.audit.RequestTraceContext;
import com.synapsecore.domain.dto.OrderCreateRequest;
import com.synapsecore.domain.dto.OrderItemResponse;
import com.synapsecore.domain.dto.OrderLifecycleTransitionRequest;
import com.synapsecore.domain.dto.OrderResponse;
import com.synapsecore.domain.entity.BusinessEventType;
import com.synapsecore.domain.entity.CustomerOrder;
import com.synapsecore.domain.entity.FulfillmentTask;
import com.synapsecore.domain.entity.Inventory;
import com.synapsecore.domain.entity.OrderItem;
import com.synapsecore.domain.entity.OrderStatus;
import com.synapsecore.domain.entity.Product;
import com.synapsecore.domain.entity.Warehouse;
import com.synapsecore.domain.repository.CustomerOrderRepository;
import com.synapsecore.domain.repository.FulfillmentTaskRepository;
import com.synapsecore.event.BusinessEventService;
import com.synapsecore.event.OperationalStateChangePublisher;
import com.synapsecore.event.OperationalUpdateType;
import com.synapsecore.fulfillment.FulfillmentService;
import com.synapsecore.observability.OperationalMetricsService;
import com.synapsecore.tenant.TenantContextService;
import com.synapsecore.tenant.TenantScopeGuard;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final CustomerOrderRepository customerOrderRepository;
    private final FulfillmentTaskRepository fulfillmentTaskRepository;
    private final InventoryService inventoryService;
    private final BusinessEventService businessEventService;
    private final OperationalStateChangePublisher operationalStateChangePublisher;
    private final AuditLogService auditLogService;
    private final ObjectProvider<FulfillmentService> fulfillmentServiceProvider;
    private final TenantContextService tenantContextService;
    private final TenantScopeGuard tenantScopeGuard;
    private final OperationalMetricsService operationalMetricsService;
    private final RequestTraceContext requestTraceContext;

    @Transactional
    public OrderResponse createOrder(OrderCreateRequest request) {
        return createOrder(request, "order-api");
    }

    @Transactional
    public OrderResponse createOrder(OrderCreateRequest request, String source) {
        return createOrderForTenant(tenantContextService.getCurrentTenantCodeOrDefault(), request, source);
    }

    @Transactional
    public OrderResponse createOrderForTenant(String tenantCode, OrderCreateRequest request, String source) {
        Warehouse warehouse = inventoryService.requireWarehouse(tenantCode, request.warehouseCode(), "order creation");
        String externalOrderId = resolveExternalOrderId(request.externalOrderId());
        ensureOrderIdIsAvailable(tenantCode, externalOrderId);

        CustomerOrder order = CustomerOrder.builder()
            .tenant(warehouse.getTenant())
            .externalOrderId(externalOrderId)
            .status(OrderStatus.CREATED)
            .statusReason("Order created from " + source + ".")
            .totalAmount(BigDecimal.ZERO)
            .warehouse(warehouse)
            .items(new ArrayList<>())
            .build();

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (var itemRequest : request.items()) {
            Product product = inventoryService.requireProduct(tenantCode, itemRequest.productSku(), "order creation");
            Inventory inventory = inventoryService.reserveStock(
                tenantCode,
                warehouse,
                product,
                itemRequest.quantity(),
                source,
                "Reserved for order " + externalOrderId
            );

            OrderItem orderItem = OrderItem.builder()
                .tenant(warehouse.getTenant())
                .customerOrder(order)
                .product(product)
                .quantity(itemRequest.quantity())
                .reservedQuantity(itemRequest.quantity())
                .fulfilledQuantity(0)
                .cancelledQuantity(0)
                .returnedQuantity(0)
                .unitPrice(itemRequest.unitPrice())
                .build();

            order.getItems().add(orderItem);
            totalAmount = totalAmount.add(itemRequest.unitPrice().multiply(BigDecimal.valueOf(itemRequest.quantity())));
            tenantScopeGuard.requireInventoryForTenant(inventory, warehouse, tenantCode, "order creation");
        }

        order.setTotalAmount(totalAmount);
        applyOperationalStatus(order, OrderStatus.RECEIVED, source, "Order received from " + source + ".");
        CustomerOrder savedOrder = customerOrderRepository.save(order);
        tenantScopeGuard.requireCustomerOrder(savedOrder, "order creation");
        savedOrder.getItems().forEach(item -> inventoryService.reevaluateOperationalSignals(
            tenantCode,
            savedOrder.getWarehouse(),
            item.getProduct(),
            source
        ));
        fulfillmentServiceProvider.getObject().initializeForOrder(savedOrder, source);
        operationalMetricsService.recordOrderIngested(tenantCode, source);

        businessEventService.record(
            BusinessEventType.ORDER_INGESTED,
            source,
            "Order " + savedOrder.getExternalOrderId() + " received for warehouse " + warehouse.getCode()
                + " with " + savedOrder.getItems().size() + " line items."
        );
        auditLogService.recordSuccess(
            "ORDER_PROCESSED",
            currentActor(source),
            source,
            "CustomerOrder",
            savedOrder.getExternalOrderId(),
            "Created order " + savedOrder.getExternalOrderId() + " in state " + savedOrder.getStatus() + "."
        );
        operationalStateChangePublisher.publish(OperationalUpdateType.ORDER_FLOW, source);
        return toOrderResponse(savedOrder);
    }

    @Transactional
    public OrderResponse transitionOrder(String externalOrderId,
                                         OrderLifecycleTransitionRequest request,
                                         String source) {
        String tenantCode = tenantContextService.getCurrentTenantCodeOrDefault();
        CustomerOrder order = customerOrderRepository.findByTenant_CodeIgnoreCaseAndExternalOrderId(tenantCode, externalOrderId.trim())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Order not found: " + externalOrderId));
        tenantScopeGuard.requireCustomerOrder(order, "order transition");
        applyTransition(order, request.status(), source, request.note(), Boolean.TRUE.equals(request.restockInventory()));
        CustomerOrder savedOrder = customerOrderRepository.save(order);
        operationalStateChangePublisher.publish(OperationalUpdateType.ORDER_FLOW, source);
        return toOrderResponse(savedOrder);
    }

    @Transactional
    public CustomerOrder synchronizeOrderLifecycleFromFulfillment(FulfillmentTask task,
                                                                  int requestedFulfilledUnits,
                                                                  String source,
                                                                  String note) {
        CustomerOrder order = task.getCustomerOrder();
        tenantScopeGuard.requireCustomerOrder(order, "fulfillment lifecycle synchronization");

        switch (task.getStatus()) {
            case QUEUED, PICKING, PACKED -> applyOperationalStatus(order, OrderStatus.PROCESSING, source, note);
            case DISPATCHED -> {
                int fulfilledUnits = requestedFulfilledUnits > 0
                    ? requestedFulfilledUnits
                    : remainingReservedUnits(order);
                if (fulfilledUnits > 0) {
                    fulfillReservedUnits(order, fulfilledUnits, source, "Fulfillment dispatch");
                }
                applyOperationalStatus(
                    order,
                    remainingReservedUnits(order) > 0 ? OrderStatus.PARTIALLY_FULFILLED : OrderStatus.FULFILLED,
                    source,
                    note
                );
            }
            case DELIVERED -> {
                if (remainingReservedUnits(order) > 0) {
                    fulfillReservedUnits(order, remainingReservedUnits(order), source, "Fulfillment delivery");
                }
                applyOperationalStatus(order, OrderStatus.DELIVERED, source, note);
            }
            case DELAYED -> applyOperationalStatus(order, OrderStatus.BLOCKED, source, note == null ? "Delivery delay detected." : note);
            case EXCEPTION -> applyOperationalStatus(order, OrderStatus.FAILED, source, note == null ? "Fulfillment exception detected." : note);
            default -> {
            }
        }

        return customerOrderRepository.save(order);
    }

    @Transactional
    public CustomerOrder cancelOrder(String externalOrderId, String source, String reason) {
        return transitionAndReturnOrder(externalOrderId, OrderStatus.CANCELLED, source, reason, false);
    }

    @Transactional
    public CustomerOrder returnOrder(String externalOrderId, String source, String reason, boolean restockInventory) {
        return transitionAndReturnOrder(externalOrderId, OrderStatus.RETURNED, source, reason, restockInventory);
    }

    private CustomerOrder transitionAndReturnOrder(String externalOrderId,
                                                   OrderStatus status,
                                                   String source,
                                                   String reason,
                                                   boolean restockInventory) {
        String tenantCode = tenantContextService.getCurrentTenantCodeOrDefault();
        CustomerOrder order = customerOrderRepository.findByTenant_CodeIgnoreCaseAndExternalOrderId(tenantCode, externalOrderId.trim())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Order not found: " + externalOrderId));
        tenantScopeGuard.requireCustomerOrder(order, "order transition");
        applyTransition(order, status, source, reason, restockInventory);
        return customerOrderRepository.save(order);
    }

    private void applyTransition(CustomerOrder order,
                                 OrderStatus targetStatus,
                                 String source,
                                 String note,
                                 boolean restockInventory) {
        validateTransition(order.getStatus(), targetStatus);
        switch (targetStatus) {
            case CANCELLED -> cancelOutstandingReservations(order, source, note);
            case RETURNED -> processReturn(order, source, note, restockInventory);
            case FAILED -> {
                if (fulfilledUnits(order) == 0) {
                    cancelOutstandingReservations(order, source, note == null ? "Failed before fulfillment." : note);
                }
                applyOperationalStatus(order, OrderStatus.FAILED, source, note);
            }
            case BLOCKED -> applyOperationalStatus(order, OrderStatus.BLOCKED, source, note);
            case PROCESSING, PARTIALLY_FULFILLED, FULFILLED, DELIVERED, RECEIVED, CREATED -> applyOperationalStatus(order, targetStatus, source, note);
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Manual transition to " + targetStatus + " is not supported.");
        }
    }

    private void cancelOutstandingReservations(CustomerOrder order, String source, String note) {
        for (OrderItem item : order.getItems()) {
            if (item.getReservedQuantity() > 0) {
                Inventory inventory = inventoryService.requireInventory(
                    order.getTenant().getCode(),
                    order.getWarehouse().getCode(),
                    item.getProduct().resolveCatalogSku(),
                    "order cancellation");
                inventoryService.releaseReservedStock(
                    inventory,
                    item.getReservedQuantity(),
                    source,
                    "Released due to order cancellation " + order.getExternalOrderId()
                );
                item.setCancelledQuantity(item.getCancelledQuantity() + item.getReservedQuantity());
                item.setReservedQuantity(0);
            }
        }
        applyOperationalStatus(order, OrderStatus.CANCELLED, source, note == null ? "Order cancelled." : note);
    }

    private void processReturn(CustomerOrder order, String source, String note, boolean restockInventory) {
        int returnableUnits = returnableUnits(order);
        if (returnableUnits < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Order " + order.getExternalOrderId() + " has no fulfilled units available to return.");
        }

        int remaining = returnableUnits;
        for (OrderItem item : order.getItems()) {
            int itemReturnable = item.getFulfilledQuantity() - item.getReturnedQuantity();
            if (itemReturnable <= 0) {
                continue;
            }
            int toReturn = Math.min(itemReturnable, remaining);
            item.setReturnedQuantity(item.getReturnedQuantity() + toReturn);
            if (restockInventory) {
                Inventory inventory = inventoryService.requireInventory(
                    order.getTenant().getCode(),
                    order.getWarehouse().getCode(),
                    item.getProduct().resolveCatalogSku(),
                    "order return");
                inventoryService.restockReturnedStock(
                    inventory,
                    toReturn,
                    source,
                    "Restocked due to order return " + order.getExternalOrderId()
                );
            }
            remaining -= toReturn;
            if (remaining == 0) {
                break;
            }
        }

        applyOperationalStatus(order, OrderStatus.RETURNED, source, note == null ? "Order returned." : note);
    }

    private void fulfillReservedUnits(CustomerOrder order, int units, String source, String note) {
        int remaining = units;
        for (OrderItem item : order.getItems()) {
            if (remaining == 0) {
                break;
            }
            int fulfillable = Math.min(item.getReservedQuantity(), remaining);
            if (fulfillable <= 0) {
                continue;
            }
            Inventory inventory = inventoryService.requireInventory(
                order.getTenant().getCode(),
                order.getWarehouse().getCode(),
                item.getProduct().resolveCatalogSku(),
                "order fulfillment");
            inventoryService.fulfillReservedStock(
                inventory,
                fulfillable,
                source,
                note == null ? "Committed during fulfillment." : note
            );
            item.setReservedQuantity(item.getReservedQuantity() - fulfillable);
            item.setFulfilledQuantity(item.getFulfilledQuantity() + fulfillable);
            remaining -= fulfillable;
        }
    }

    private void applyOperationalStatus(CustomerOrder order, OrderStatus nextStatus, String source, String note) {
        OrderStatus previousStatus = order.getStatus();
        if (previousStatus == nextStatus) {
            order.setStatusReason(note);
            return;
        }

        Instant now = Instant.now();
        order.setStatus(nextStatus);
        order.setStatusReason(note);
        switch (nextStatus) {
            case PROCESSING -> order.setProcessingStartedAt(now);
            case PARTIALLY_FULFILLED, FULFILLED -> order.setFulfilledAt(now);
            case DELIVERED -> order.setDeliveredAt(now);
            case CANCELLED -> order.setCancelledAt(now);
            case RETURNED -> order.setReturnedAt(now);
            case FAILED -> order.setFailedAt(now);
            case BLOCKED -> order.setBlockedAt(now);
            default -> {
            }
        }

        businessEventService.record(
            BusinessEventType.ORDER_STATUS_TRANSITIONED,
            source,
            "Order " + order.getExternalOrderId() + " moved from " + previousStatus + " to " + nextStatus + "."
        );
        auditLogService.recordSuccess(
            "ORDER_STATUS_UPDATED",
            currentActor(source),
            source,
            "CustomerOrder",
            order.getExternalOrderId(),
            "Order moved from " + previousStatus + " to " + nextStatus
                + (note == null || note.isBlank() ? "." : " with note: " + note)
        );
    }

    private void validateTransition(OrderStatus currentStatus, OrderStatus nextStatus) {
        if (currentStatus == nextStatus) {
            return;
        }
        boolean allowed = switch (currentStatus) {
            case CREATED -> nextStatus == OrderStatus.RECEIVED || nextStatus == OrderStatus.CANCELLED;
            case RECEIVED -> nextStatus == OrderStatus.PROCESSING
                || nextStatus == OrderStatus.BLOCKED
                || nextStatus == OrderStatus.CANCELLED
                || nextStatus == OrderStatus.FAILED;
            case PROCESSING -> nextStatus == OrderStatus.PARTIALLY_FULFILLED
                || nextStatus == OrderStatus.FULFILLED
                || nextStatus == OrderStatus.BLOCKED
                || nextStatus == OrderStatus.CANCELLED
                || nextStatus == OrderStatus.FAILED;
            case PARTIALLY_FULFILLED -> nextStatus == OrderStatus.FULFILLED
                || nextStatus == OrderStatus.DELIVERED
                || nextStatus == OrderStatus.BLOCKED
                || nextStatus == OrderStatus.RETURNED
                || nextStatus == OrderStatus.CANCELLED;
            case FULFILLED -> nextStatus == OrderStatus.DELIVERED
                || nextStatus == OrderStatus.RETURNED
                || nextStatus == OrderStatus.BLOCKED;
            case DELIVERED -> nextStatus == OrderStatus.RETURNED;
            case BLOCKED -> nextStatus == OrderStatus.PROCESSING
                || nextStatus == OrderStatus.CANCELLED
                || nextStatus == OrderStatus.FAILED;
            case CANCELLED, RETURNED, FAILED -> false;
        };

        if (!allowed) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Order cannot transition from " + currentStatus + " to " + nextStatus + ".");
        }
    }

    private int remainingReservedUnits(CustomerOrder order) {
        return order.getItems().stream().mapToInt(OrderItem::getReservedQuantity).sum();
    }

    private int fulfilledUnits(CustomerOrder order) {
        return order.getItems().stream().mapToInt(OrderItem::getFulfilledQuantity).sum();
    }

    private int returnableUnits(CustomerOrder order) {
        return order.getItems().stream()
            .mapToInt(item -> item.getFulfilledQuantity() - item.getReturnedQuantity())
            .sum();
    }

    private String resolveExternalOrderId(String externalOrderId) {
        if (externalOrderId != null && !externalOrderId.isBlank()) {
            return externalOrderId.trim();
        }
        return "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private void ensureOrderIdIsAvailable(String tenantCode, String externalOrderId) {
        if (customerOrderRepository.existsByTenant_CodeIgnoreCaseAndExternalOrderId(tenantCode, externalOrderId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Order already exists for externalOrderId " + externalOrderId);
        }
    }

    private String currentActor(String fallback) {
        return requestTraceContext.getCurrentActor().orElse(fallback);
    }

    private OrderResponse toOrderResponse(CustomerOrder order) {
        List<OrderItemResponse> items = order.getItems().stream()
            .map(item -> new OrderItemResponse(
                item.getProduct().resolveCatalogSku(),
                item.getProduct().getName(),
                item.getQuantity(),
                item.getReservedQuantity(),
                item.getFulfilledQuantity(),
                item.getCancelledQuantity(),
                item.getReturnedQuantity(),
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
