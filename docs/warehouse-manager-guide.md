# Warehouse Manager Guide

This guide explains how warehouse teams should understand SynapseCore.

## What SynapseCore Means For A Warehouse

For a warehouse team, SynapseCore is a live coordination surface around products, inventory, orders, recommendations, approvals, integrations, replay, and runtime trust.

It is not a replacement for every warehouse execution system. It is a command-center layer that helps warehouse and operations leaders understand pressure, failures, and decisions.

## Warehouse Questions SynapseCore Helps Answer

- Which products and SKUs are visible in the workspace?
- What inventory state is visible?
- Which orders create pressure?
- Are there recommendations or alerts tied to stock movement?
- Did inbound order data fail?
- Are integrations healthy?
- Are approvals blocking execution?
- Is the runtime healthy enough to trust the current state?

## Warehouse Operating Loop

```text
Open workspace
-> check runtime trust
-> review inventory
-> inspect orders
-> review alerts/recommendations
-> check replay for failed inbound data
-> confirm approvals
-> coordinate action
-> review audit/history
```

## Product And Catalog

The catalog is the product identity layer.

Warehouse teams should treat SKU quality as important because orders, inventory, and recommendations depend on product identity being consistent.

If a product or SKU is wrong, downstream operational decisions become weaker.

## Inventory

Inventory is the warehouse-aware operational stock context. It is where warehouse teams inspect stock pressure and availability.

SynapseCore helps inventory become part of the command-center view instead of being isolated in a warehouse-only tool or spreadsheet.

## Orders

Orders represent operational demand.

Orders can arrive through supported integration paths or be visible through tenant-scoped product surfaces. When orders create pressure against inventory, operators can see the downstream operational impact.

## Replay For Warehouses

Replay matters to warehouse teams because failed inbound orders or data imports can cause real-world mismatch:

- items may not be picked
- allocation can be delayed
- manual re-entry can create duplicate work
- inventory pressure may be misunderstood

SynapseCore keeps failed inbound work visible so it can be reviewed and recovered.

## Approvals For Warehouses

Some warehouse actions may require approval before execution. This is useful when a decision affects customer commitments, stock movement, priority allocation, or operational risk.

Approval outcomes:

- approve and continue
- reject and stop
- revise the scenario
- escalate ownership

## Realtime And Runtime

Warehouse decisions depend on whether the system is current.

If realtime is reconnecting or runtime is degraded, warehouse managers should treat the view as operationally limited until health is restored.

## Warehouse Success Signals

SynapseCore is helping when:

- inventory/order pressure is easier to see
- failed inbound work is no longer hidden
- warehouse teams can coordinate with planners and managers in one workspace
- approvals are visible instead of stuck in side channels
- runtime trust is considered before operational decisions
