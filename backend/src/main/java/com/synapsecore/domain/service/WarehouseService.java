package com.synapsecore.domain.service;

import com.synapsecore.access.AccessDirectoryService;
import com.synapsecore.domain.dto.WarehouseResponse;
import com.synapsecore.domain.repository.WarehouseRepository;
import com.synapsecore.tenant.TenantContextService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final TenantContextService tenantContextService;
    private final AccessDirectoryService accessDirectoryService;

    public List<WarehouseResponse> getWarehouses() {
        var currentOperator = accessDirectoryService.getCurrentOperator();
        return warehouseRepository.findAllByTenant_CodeIgnoreCaseOrderByNameAsc(
                tenantContextService.getCurrentTenantCodeOrDefault())
            .stream()
            .filter(warehouse -> currentOperator.isEmpty()
                || accessDirectoryService.hasWarehouseAccess(currentOperator.get(), warehouse.getCode()))
            .map(warehouse -> new WarehouseResponse(
                warehouse.getId(),
                warehouse.getCode(),
                warehouse.getName(),
                warehouse.getLocation()
            ))
            .toList();
    }
}
