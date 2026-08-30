# Warehouse Context Phase 1 Evidence

## Scope

This evidence covers explicit warehouse targeting, active operation options, and
authority currentness. It does not reopen the closed Catalog, Inventory, Orders,
Fulfillment, Integrations, Replay, Scenario, Dashboard, Auth, or Tenant Admin
lifecycle domains.

Starting revision: `9d5ac9621446e8a97e2d841e3e55f226843f9c02`

## Final Context Contract

SynapseCore does not require a global current-warehouse selector. Aggregate pages
operate over the tenant-wide or assigned scope returned by the backend. A
warehouse-specific creation or mutation carries an explicit `warehouseCode`,
while entity-bound workflows use the persisted warehouse on the entity or source
record. Warehouse selection never creates authority.

The authoritative active operation list is `GET /api/warehouses`. It is tenant
bound, active-only, and filtered by the current operator's persisted scope. The
frontend now uses that response to constrain operation options. Administrative
Settings continues to receive active and retired records so a Tenant Admin can
inspect or reactivate a retired warehouse.

## Surface Boundaries

| Surface | Warehouse behavior |
| --- | --- |
| Dashboard, Orders, Inventory, Alerts, Recommendations, Fulfillment | Authorized aggregate or scoped reads; no forced global selector |
| Scenario | Page-local active warehouse selector; submitted `warehouseCode` remains backend-validated |
| Settings | Administrative warehouse selection may include retired records and is not operating context |
| Replay | Retained failed-record warehouse remains source of truth |
| Integrations | Explicit validated warehouse or connector/source warehouse remains authoritative |
| Recommendations | Source and destination warehouse authority is preserved |

## Frontend Fixes

`buildWarehouseOptions` now accepts the authenticated active warehouse response
and excludes retired or unauthorized codes from operation options. It continues
to use stable warehouse codes as option identity and does not persist warehouse
authority in local storage.

The frontend refreshes the active warehouse list and `/api/auth/session` on
initial authenticated bootstrap and at a bounded interval. Changed roles or
warehouse scopes replace the in-memory session only when the server response
differs. A failed authority read fails closed for operation targets rather than
retaining potentially stale selectable warehouses.

The Scenario selector is disabled when no active warehouse is available and
shows a distinct state for loading, authority-read failure, and no active
warehouse. It never silently submits a replacement warehouse.

## Lifecycle Truth

- A new active warehouse becomes eligible for tenant-wide operators after an authoritative refresh.
- A scoped operator does not receive a new warehouse until scope assignment is persisted.
- Retiring a warehouse removes it from active operation options; administrative history remains available.
- Reactivating a warehouse does not create a new operator scope.
- Removing scope causes backend requests to use the new persisted authority and frontend options to converge on refresh.
- An empty frontend option list is never interpreted as tenant-wide authority.
- No active authorized warehouse is presented as a truthful blocked planning state, not as successful business readiness.

## A/B/C/D Classification

| Class | Result |
| --- | --- |
| A: required operational or authority capability | Closed for the implemented active-option and explicit-target seams |
| B: intentional current boundary | No global selector, aggregate reads, page-local Scenario selection, no URL or local-storage warehouse authority |
| C: evidence gap | Live browser retirement/scope-removal walkthrough, rapid page-local changes, and realtime rendering after authority change |
| D: future extension | Global selector only if evidence requires it, favorites, comparison, URL context, and per-tab warehouse workspace |

## Verification Plan

Focused checks for this phase are:

- active-only and scoped warehouse option construction;
- Scenario no-active and explicit-target behavior;
- representative backend smoke checks for active, retired, unauthorized, missing, and cross-tenant targets using the existing domain tests;
- authority refresh after persisted scope changes;
- `git diff --check`, frontend verification, and documentation link validation.

The broad authorization matrices remain the evidence for backend enforcement and
are not duplicated here. Realtime subscription lifecycle changes remain outside
Phase 1 and belong to the later realtime-focused phase.

## Remaining Risk

The repository now has an authoritative active operation option boundary, but a
live rendered walkthrough is still required to record browser evidence for scope
removal, retirement while a page is open, and realtime behavior after authority
changes. Those are evidence items, not permission grants: backend authority
remains the final boundary.
