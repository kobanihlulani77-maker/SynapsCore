package com.synapsecore.api.controller;

import com.synapsecore.access.AccessControlService;
import com.synapsecore.domain.dto.OrderCreateRequest;
import com.synapsecore.domain.dto.OrderLifecycleTransitionRequest;
import com.synapsecore.domain.dto.OrderResponse;
import com.synapsecore.domain.service.OperationalViewService;
import com.synapsecore.domain.service.OrderService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final AccessControlService accessControlService;
    private final OrderService orderService;
    private final OperationalViewService operationalViewService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse createOrder(@Valid @RequestBody OrderCreateRequest request) {
        accessControlService.requireWorkspaceWarehouseAccess(request.warehouseCode(), "create live orders");
        return orderService.createOrder(request);
    }

    @PostMapping("/{externalOrderId}/transition")
    @ResponseStatus(HttpStatus.OK)
    public OrderResponse transitionOrder(@PathVariable String externalOrderId,
                                         @Valid @RequestBody OrderLifecycleTransitionRequest request) {
        accessControlService.requireWorkspaceAccess("transition order lifecycle");
        return orderService.transitionOrder(externalOrderId, request, "order-api");
    }

    @GetMapping("/recent")
    public List<OrderResponse> getRecentOrders() {
        accessControlService.requireWorkspaceAccess("view recent orders");
        return operationalViewService.getRecentOrders();
    }
}
