# Dashboard Lifecycle Phase 1 Evidence

**Scope:** Dashboard authority, warehouse scope, cross-domain truth consistency,
activity privacy, realtime privacy, and frontend count semantics.

**Starting HEAD:** `60e4c533f570072c68e74dc91dfa18ea707485b9`

**Phase boundary:** This evidence does not start Dashboard Phase 2. Freshness,
partial-service failures, stale markers, reconnect polling, cache TTL, and
currentness indicators remain Phase 2 work.

## 1. Field Scope Matrix

| Field | Business meaning | Authority | Scope | Time semantics | Phase 1 decision |
| --- | --- | --- | --- | --- | --- |
| `totalOrders` | All operational Orders in the tenant, or only assigned warehouse Orders for a scoped actor | `CustomerOrderRepository` | Warehouse-sensitive | Historical total | Scoped query for assigned warehouses; tenant-wide for tenant-wide actors |
| `activeAlerts` | Active visible operational Alerts | `AlertScopeService` | Warehouse-sensitive | Current | Existing scope-aware count retained |
| `lowStockItems` | Inventory rows currently at or below reorder threshold | `InventoryRepository` | Warehouse-sensitive | Current | Scoped query for assigned warehouses |
| `recommendationsCount` | Current recommendations visible to the actor | `RecommendationScopeService` | Warehouse-sensitive, including both sides of a transfer | Current | Existing scope-aware visibility retained |
| `fulfillmentBacklogCount` | Current queued, picking, or packed fulfillment work | `FulfillmentService` | Warehouse-sensitive | Current | Existing scoped overview retained |
| `delayedShipmentCount` | Current fulfillment tasks with delivery-delay risk | `FulfillmentService` | Warehouse-sensitive | Current | Existing scoped overview retained |
| `fulfillmentRiskCount` | Current fulfillment tasks with at-risk status | `FulfillmentService` | Warehouse-sensitive | Current | Existing scoped overview retained |
| `totalProducts` | Distinct products represented by tenant inventory; this is not a complete catalog-row count | `InventoryRepository` | Tenant-wide inventory-backed metric | Historical/current snapshot | Remains tenant-wide under the established Catalog contract |
| `totalWarehouses` | Company warehouse count used by workspace configuration and the company-level dashboard metric | `WarehouseRepository` | Tenant-wide | Configuration total | Remains tenant-wide; visible location pages use their authorized warehouse list |
| `recentOrderCount` | Orders created during the last 24 hours | `CustomerOrderRepository` | Warehouse-sensitive | Recent 24-hour window | Scoped query for assigned warehouses |
| `inventoryRecordsCount` | Inventory rows in the tenant | `InventoryRepository` | Warehouse-sensitive | Current snapshot | Scoped query for assigned warehouses |
| `lastUpdatedAt` | Time the summary was calculated or read from its tenant cache | `DashboardService` | Not business data | Snapshot timestamp | Phase 1 does not assess staleness or TTL |

An empty warehouse scope means tenant-wide authority under the existing access
contract. A non-empty scope is normalized warehouse-code authority, not a UI
filter.

## 2. Authority and Tenant Results

### Single warehouse

The existing access-boundary rehearsal provisions two warehouses and a North-
scoped integration operator. The Phase 1 regression verifies that the scoped
Dashboard summary uses repository predicates for the assigned warehouse and
that the snapshot recent-order list contains only the assigned warehouse.

### Multi-warehouse

The repository queries accept the complete normalized scope collection. A future
multi-warehouse actor therefore receives the union of exactly the assigned
warehouse rows before ordering and limiting; an unassigned warehouse cannot
contribute to a warehouse-sensitive total or preview.

### Tenant-wide

The same rehearsal verifies that the tenant-wide Tenant Admin receives all
tenant-owned Orders and Inventory rows while `totalWarehouses` remains the
company configuration count.

### Cross-tenant

The accepted `PlatformTenantAccessBoundaryIntegrationTest` fixture creates a
second tenant and exercises tenant-qualified reads. Phase 1 retains those
tenant predicates and adds no cross-tenant fallback. Dashboard counts therefore
cannot use another tenant's rows.

## 3. Dashboard-to-Domain Equality Matrix

| Dashboard output | Equivalent domain truth | Result |
| --- | --- | --- |
| `activeAlerts` | Visible active Alerts for the same session | PASS; existing `AlertScopeService` count and feed visibility are reused |
| `recommendationsCount` | Current visible Recommendations for the same session | PASS; existing recommendation scope, including transfer dual-warehouse authority, is reused |
| `lowStockItems` | Inventory rows at or below threshold in assigned warehouses | PASS; repository-scoped count added |
| `inventoryRecordsCount` | Inventory rows in assigned warehouses | PASS; repository-scoped count added |
| `totalOrders` | Orders in assigned warehouses | PASS; repository-scoped count added |
| `recentOrderCount` | Orders in assigned warehouses created in the last 24 hours | PASS; repository-scoped count added |
| `recentOrders` | Recent authorized Orders | PASS; warehouse qualification now occurs before the 12-row limit |
| Fulfillment counts | Existing scoped `FulfillmentService` overview | PASS; closed Fulfillment contract retained |
| Replay pressure | Existing scoped Replay queue | PASS by existing Integration/Replay evidence; Dashboard snapshot applies the established replay scope |

The Dashboard summary is authoritative for totals. A top-12 or top-20 preview
length is never used as a total.

## 4. Activity, Audit, and Incident Scope

Business event and audit DTOs do not carry a consistently authoritative
warehouse identity. Parsing arbitrary payload text would be unsafe. For a
warehouse-scoped actor, the Dashboard snapshot therefore returns empty raw
event and audit collections rather than risk exposing another warehouse's
Order ID, SKU, quantity, failure, or actor detail.

System incidents retain their existing rule: warehouse-coded connector,
inbound, replay, and Scenario incidents are filtered by warehouse; incidents
without warehouse attribution are not visible to a scoped actor. Tenant-wide
runtime context remains a separate trust surface.

This is an intentional safe-boundary decision, not a redesign of the Activity
or Audit domains. Rich warehouse-attributed Activity DTOs are a future
extension (Classification D).

## 5. Realtime Authority

`RealtimeService` publishes tenant-topic snapshots for the tenant-wide control
plane. The websocket subscription interceptor now classifies
`events.recent`, `audit.recent`, and `system.incidents` as tenant-wide raw
topics. Warehouse-scoped sessions are rejected server-side, so frontend
filtering is not the security boundary.

The frontend subscribes to those raw activity topics only for tenant-wide
sessions. Scoped sessions continue to use their permitted domain change signals
and REST snapshot reads. Existing raw domain topic protections for Inventory,
Orders, Fulfillment, Alerts, Recommendations, Integrations, and Scenarios were
preserved.

The focused websocket test now covers all three raw activity topics for a
warehouse-scoped session.

## 6. Top-N and Frontend Semantics

The previous Order path selected the newest 12 tenant Orders and filtered them
after retrieval. It now selects newest Orders from the authorized warehouse
set and then applies the 12-row limit. This preserves an authorized older North
Order when newer Coast Orders exist.

The Dashboard page, shared workspace chrome, and page-context decision count no
longer use `Math.max(summaryValue, visibleListLength)`. Summary counts are used
for totals; preview arrays remain previews. Location navigation uses the visible
authorized warehouse list, while the company-level `totalWarehouses` metric
remains tenant-wide and is labeled `Warehouses`.

Unrelated layout/time-boundary `Math.max` calls remain and do not combine an
operational total with a preview length.

## 7. Role Matrix

All tenant roles retain Dashboard read access. Scope remains independent of
role:

| Role | Dashboard access | Warehouse-sensitive data |
| --- | --- | --- |
| `TENANT_ADMIN` | Read | Tenant-wide when scope is empty |
| `REVIEW_OWNER` | Read | Assigned warehouses only when scoped |
| `FINAL_APPROVER` | Read | Assigned warehouses only when scoped |
| `ESCALATION_OWNER` | Read | Assigned warehouses only when scoped |
| `INTEGRATION_OPERATOR` | Read | Assigned warehouses only when scoped; integration topics remain role-gated |
| `INTEGRATION_ADMIN` | Read | Assigned warehouses only when scoped; integration topics remain role-gated |

No role grants cross-tenant or cross-warehouse authority.

## 8. Scenario, Runtime, Platform, and Cache Boundaries

- Scenario PREVIEW remains a smoke boundary only. Existing Scenario evidence
  proves projected intelligence does not become live Dashboard Alert,
  Recommendation, Inventory, or Order truth.
- Runtime health remains a separate trust/readiness surface and is not converted
  into business counts.
- Platform Owner authority remains separate from tenant Dashboard data.
- Tenant-wide summary caching remains available for tenant-wide sessions.
  Warehouse-scoped sessions bypass the tenant-wide summary cache and refresh
  through scoped calculation. Cache TTL and stale-read behavior are Phase 2.
- A fresh empty tenant is expected to show zero operational totals and empty
  previews, with setup guidance allowed as a non-operational onboarding state.

## 9. Classification Table

| Finding | Classification | Result |
| --- | --- | --- |
| Tenant-wide Order totals shown to scoped actors | A - required capability | Fixed |
| Tenant-wide low-stock and inventory totals shown to scoped actors | A - required capability | Fixed |
| Recent Order top-N applied before warehouse filtering | A - required capability | Fixed |
| Raw activity realtime topics available to scoped actors | A - authority/privacy capability | Fixed server-side and in frontend subscription path |
| Raw event/audit REST payload has no reliable warehouse identity | B - intentional current boundary | Safe omission for scoped actors |
| Tenant-wide `totalProducts` | B - established Catalog/inventory-backed metric boundary | Preserved |
| Tenant-wide `totalWarehouses` | B - company configuration metric | Preserved and separated from visible location scope |
| Rich warehouse-attributed Activity/Audit DTOs | D - future extension | Not part of Phase 1 |
| Full freshness, stale markers, cache TTL, and partial-failure behavior | D - Dashboard Phase 2 | Deferred |

No Classification A finding remains open.

## 10. Tests and Files

Focused verification:

- `PlatformTenantAccessBoundaryIntegrationTest`: 33 tests, 0 failures, 0
  errors.
- `WebSocketAccessBoundaryTest`: 5 tests, 0 failures, 0 errors.
- Combined focused result: **38 tests, 0 failures, 0 errors**.
- The new Dashboard regression asserts scoped summary equality against
  repository/domain truth, tenant-wide totals, recent-order scope, preview
  semantics, and safe activity omission.

Production files changed:

- `CustomerOrderRepository`
- `InventoryRepository`
- `DashboardService`
- `OperationalViewService`
- `WebSocketConfig`

Frontend files changed:

- `Dashboard.jsx`
- `useWorkspaceChrome.js`
- `useWorkspacePageContexts.js`
- `useWorkspaceRealtime.js`

Test files changed:

- `PlatformTenantAccessBoundaryIntegrationTest`
- `WebSocketAccessBoundaryTest`
- Existing service-constructor fixtures updated for the new access-directory
  dependency.

Hosted proof was not rerun in this local Phase 1 turn. The change requires
deployment before hosted proof is considered current; no Phase 2 work was
started.

## 11. Phase 1 Verdict

Dashboard Phase 1 authority and consistency findings are implemented and
focused-tested. Tenant-wide fields remain tenant-wide only where their product
meaning supports it, warehouse-sensitive totals now match authorized domain
truth, recent-order qualification is ordered safely, frontend totals no longer
use preview-length fallbacks, and raw activity cannot be delivered to scoped
websocket sessions.

**Phase 1 status after required full verification:** full backend suite passed
with 243 tests, 0 failures, and 0 errors (`BUILD SUCCESS`). Frontend lint,
build, and verify passed; `git diff --check` passed. Dashboard Phase 2 is not
started.
