package com.synapsecore.api.controller;

import com.synapsecore.access.AccessControlService;
import com.synapsecore.domain.dto.WarehouseResponse;
import com.synapsecore.domain.service.WarehouseService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/warehouses")
@RequiredArgsConstructor
public class WarehouseController {

    private final AccessControlService accessControlService;
    private final WarehouseService warehouseService;

    @GetMapping
    public List<WarehouseResponse> getWarehouses() {
        accessControlService.requireWorkspaceAccess("view warehouses");
        return warehouseService.getWarehouses();
    }
}
