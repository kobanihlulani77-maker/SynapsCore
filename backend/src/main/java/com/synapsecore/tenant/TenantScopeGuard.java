package com.synapsecore.tenant;

import com.synapsecore.domain.entity.CustomerOrder;
import com.synapsecore.domain.entity.FulfillmentTask;
import com.synapsecore.domain.entity.Inventory;
import com.synapsecore.domain.entity.Warehouse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class TenantScopeGuard {

    public void requireWarehouseForTenant(Warehouse warehouse, String tenantCode, String context) {
        assertConsistency(() -> TenantOwnershipAssertions.requireWarehouseOwnedByTenant(warehouse, tenantCode, context), context);
    }

    public void requireInventoryForTenant(Inventory inventory, Warehouse expectedWarehouse, String tenantCode, String context) {
        assertConsistency(() -> TenantOwnershipAssertions.requireInventoryOwnedByTenant(inventory, expectedWarehouse, tenantCode, context), context);
    }

    public void requireCustomerOrder(CustomerOrder order, String context) {
        assertConsistency(() -> TenantOwnershipAssertions.requireCustomerOrderConsistency(order, context), context);
    }

    public void requireFulfillmentTask(FulfillmentTask task, String context) {
        assertConsistency(() -> TenantOwnershipAssertions.requireFulfillmentTaskConsistency(task, context), context);
    }

    private void assertConsistency(Runnable assertion, String context) {
        try {
            assertion.run();
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Tenant scope consistency violation during " + context + ".",
                exception
            );
        }
    }
}
