package com.synapsecore.api.controller;

import com.synapsecore.access.AccessControlService;
import com.synapsecore.domain.dto.InventoryStatusResponse;
import com.synapsecore.domain.dto.InventoryUpdateRequest;
import com.synapsecore.domain.service.InventoryService;
import com.synapsecore.domain.service.OperationalViewService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final AccessControlService accessControlService;
    private final InventoryService inventoryService;
    private final OperationalViewService operationalViewService;

    @PostMapping("/update")
    @ResponseStatus(HttpStatus.OK)
    public InventoryStatusResponse updateInventory(@Valid @RequestBody InventoryUpdateRequest request) {
        accessControlService.requireWorkspaceWarehouseAccess(
            request.warehouseCode(),
            "update inventory levels"
        );
        return inventoryService.updateInventory(request);
    }

    @GetMapping
    public List<InventoryStatusResponse> getInventory() {
        accessControlService.requireWorkspaceAccess("view inventory posture");
        return operationalViewService.getInventoryOverview();
    }
}
