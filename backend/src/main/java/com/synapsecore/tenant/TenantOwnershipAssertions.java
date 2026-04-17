package com.synapsecore.tenant;

import com.synapsecore.domain.entity.CustomerOrder;
import com.synapsecore.domain.entity.FulfillmentTask;
import com.synapsecore.domain.entity.Inventory;
import com.synapsecore.domain.entity.Tenant;
import com.synapsecore.domain.entity.Warehouse;

public final class TenantOwnershipAssertions {

    private TenantOwnershipAssertions() {
    }

    public static void requireWarehouseOwnedByTenant(Warehouse warehouse, String tenantCode, String context) {
        if (warehouse == null) {
            throw new IllegalStateException(context + " requires a warehouse.");
        }
        requireTenantAssigned(warehouse.getTenant(), "Warehouse " + warehouse.getCode());
        if (!warehouse.getTenant().getCode().equalsIgnoreCase(tenantCode)) {
            throw new IllegalStateException(
                context + " resolved warehouse " + warehouse.getCode()
                    + " for tenant " + warehouse.getTenant().getCode()
                    + " while the current tenant is " + tenantCode + "."
            );
        }
    }

    public static void requireInventoryOwnedByTenant(Inventory inventory, Warehouse expectedWarehouse, String tenantCode, String context) {
        if (inventory == null) {
            throw new IllegalStateException(context + " requires an inventory record.");
        }
        if (inventory.getWarehouse() == null) {
            throw new IllegalStateException(context + " found inventory without a warehouse binding.");
        }
        requireWarehouseOwnedByTenant(inventory.getWarehouse(), tenantCode, context);
        if (expectedWarehouse != null && !inventory.getWarehouse().getId().equals(expectedWarehouse.getId())) {
            throw new IllegalStateException(
                context + " found inventory in warehouse " + inventory.getWarehouse().getCode()
                    + " while the requested warehouse is " + expectedWarehouse.getCode() + "."
            );
        }
    }

    public static void requireCustomerOrderConsistency(CustomerOrder order, String context) {
        if (order == null) {
            throw new IllegalStateException(context + " requires a customer order.");
        }
        requireTenantAssigned(order.getTenant(), "Customer order " + order.getExternalOrderId());
        if (order.getWarehouse() == null) {
            throw new IllegalStateException("Customer order " + order.getExternalOrderId() + " must reference a warehouse.");
        }
        requireTenantAssigned(order.getWarehouse().getTenant(), "Warehouse " + order.getWarehouse().getCode());
        if (!sameTenant(order.getTenant(), order.getWarehouse().getTenant())) {
            throw new IllegalStateException(
                "Customer order " + order.getExternalOrderId()
                    + " is assigned to tenant " + order.getTenant().getCode()
                    + " but warehouse " + order.getWarehouse().getCode()
                    + " belongs to tenant " + order.getWarehouse().getTenant().getCode() + "."
            );
        }
    }

    public static void requireFulfillmentTaskConsistency(FulfillmentTask task, String context) {
        if (task == null) {
            throw new IllegalStateException(context + " requires a fulfillment task.");
        }
        requireTenantAssigned(task.getTenant(), "Fulfillment task");
        if (task.getWarehouse() == null) {
            throw new IllegalStateException("Fulfillment task must reference a warehouse.");
        }
        if (task.getCustomerOrder() == null) {
            throw new IllegalStateException("Fulfillment task must reference a customer order.");
        }
        requireTenantAssigned(task.getWarehouse().getTenant(), "Warehouse " + task.getWarehouse().getCode());
        requireCustomerOrderConsistency(task.getCustomerOrder(), "Fulfillment task");

        if (!sameTenant(task.getTenant(), task.getWarehouse().getTenant())) {
            throw new IllegalStateException(
                "Fulfillment task tenant " + task.getTenant().getCode()
                    + " does not match warehouse tenant " + task.getWarehouse().getTenant().getCode() + "."
            );
        }
        if (!sameTenant(task.getTenant(), task.getCustomerOrder().getTenant())) {
            throw new IllegalStateException(
                "Fulfillment task tenant " + task.getTenant().getCode()
                    + " does not match order tenant " + task.getCustomerOrder().getTenant().getCode() + "."
            );
        }
    }

    public static void requireWarehouseConsistency(Warehouse warehouse, String context) {
        if (warehouse == null) {
            throw new IllegalStateException(context + " requires a warehouse.");
        }
        requireTenantAssigned(warehouse.getTenant(), "Warehouse " + warehouse.getCode());
    }

    public static void requireInventoryConsistency(Inventory inventory, String context) {
        if (inventory == null) {
            throw new IllegalStateException(context + " requires an inventory record.");
        }
        if (inventory.getProduct() == null) {
            throw new IllegalStateException(context + " requires inventory product binding.");
        }
        if (inventory.getWarehouse() == null) {
            throw new IllegalStateException(context + " requires inventory warehouse binding.");
        }
        requireWarehouseConsistency(inventory.getWarehouse(), context);
    }

    private static void requireTenantAssigned(Tenant tenant, String ownerLabel) {
        if (tenant == null || tenant.getCode() == null || tenant.getCode().isBlank()) {
            throw new IllegalStateException(ownerLabel + " must belong to an explicit tenant.");
        }
    }

    private static boolean sameTenant(Tenant left, Tenant right) {
        return left != null
            && right != null
            && left.getCode() != null
            && right.getCode() != null
            && left.getCode().equalsIgnoreCase(right.getCode());
    }
}
