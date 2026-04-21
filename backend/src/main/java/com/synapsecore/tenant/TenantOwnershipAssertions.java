package com.synapsecore.tenant;

import com.synapsecore.domain.entity.CustomerOrder;
import com.synapsecore.domain.entity.FulfillmentTask;
import com.synapsecore.domain.entity.Inventory;
import com.synapsecore.domain.entity.OrderItem;
import com.synapsecore.domain.entity.Product;
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
        requireProductConsistency(inventory.getProduct(), context);
        requireTenantAssigned(inventory.getWarehouse().getTenant(), "Warehouse " + inventory.getWarehouse().getCode());
        if (!sameTenant(inventory.getProduct().getTenant(), inventory.getWarehouse().getTenant())) {
            throw new IllegalStateException(
                "Inventory product " + inventory.getProduct().resolveCatalogSku()
                    + " belongs to tenant " + inventory.getProduct().getTenant().getCode()
                    + " while warehouse " + inventory.getWarehouse().getCode()
                    + " belongs to tenant " + inventory.getWarehouse().getTenant().getCode() + "."
            );
        }
        if (inventory.getTenant() != null && !sameTenant(inventory.getTenant(), inventory.getWarehouse().getTenant())) {
            throw new IllegalStateException(
                "Inventory tenant " + inventory.getTenant().getCode()
                    + " does not match warehouse tenant " + inventory.getWarehouse().getTenant().getCode() + "."
            );
        }
    }

    public static void requireProductConsistency(Product product, String context) {
        if (product == null) {
            throw new IllegalStateException(context + " requires a product.");
        }
        requireTenantAssigned(product.getTenant(), "Product " + product.resolveCatalogSku());
    }

    public static void requireOrderItemConsistency(OrderItem orderItem, String context) {
        if (orderItem == null) {
            throw new IllegalStateException(context + " requires an order item.");
        }
        if (orderItem.getCustomerOrder() == null) {
            throw new IllegalStateException(context + " requires an order item customer order binding.");
        }
        if (orderItem.getProduct() == null) {
            throw new IllegalStateException(context + " requires an order item product binding.");
        }
        requireCustomerOrderConsistency(orderItem.getCustomerOrder(), context);
        requireProductConsistency(orderItem.getProduct(), context);
        if (!sameTenant(orderItem.getCustomerOrder().getTenant(), orderItem.getProduct().getTenant())) {
            throw new IllegalStateException(
                "Order item product " + orderItem.getProduct().resolveCatalogSku()
                    + " belongs to tenant " + orderItem.getProduct().getTenant().getCode()
                    + " while order " + orderItem.getCustomerOrder().getExternalOrderId()
                    + " belongs to tenant " + orderItem.getCustomerOrder().getTenant().getCode() + "."
            );
        }
        if (orderItem.getTenant() != null && !sameTenant(orderItem.getTenant(), orderItem.getCustomerOrder().getTenant())) {
            throw new IllegalStateException(
                "Order item tenant " + orderItem.getTenant().getCode()
                    + " does not match order tenant " + orderItem.getCustomerOrder().getTenant().getCode() + "."
            );
        }
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
