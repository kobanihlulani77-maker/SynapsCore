# Inventory Lifecycle Phase 1: Warehouse Authority and Direct Mutations

Status: focused verification complete; Catalog is closed and Inventory Phase 2 has not started.

## Evidence basis

- Starting repository HEAD: `1cfacef34b980d845f5b4ec5dcab073114410d5f`.
- Primary focused suite: `PlatformTenantAccessBoundaryIntegrationTest`.
- Final focused result: 31 tests, 0 failures, 0 errors.
- Final full backend result after the production guard change: 185 tests, 0 failures, 0 errors.
- Test persistence uses the existing H2 integration-test profile. Hosted PostgreSQL proof was not rerun for this phase.
- No secrets, local environment files, or proof-state files are included here.

## Contract verified

Inventory is tenant-scoped and warehouse-addressed. The direct maintenance endpoints are:

| Operation | Endpoint | Required authority | Warehouse rule |
| --- | --- | --- | --- |
| Update absolute stock | `POST /api/inventory/update` | `TENANT_ADMIN` | The session must be tenant-wide or assigned to the request warehouse. |
| Receive stock | `POST /api/inventory/receive` | `TENANT_ADMIN` | The session must be tenant-wide or assigned to the request warehouse. |
| Adjust stock | `POST /api/inventory/adjust` | `TENANT_ADMIN` | The session must be tenant-wide or assigned to the request warehouse. |
| Reconcile counted stock | `POST /api/inventory/reconcile` | `TENANT_ADMIN` | The session must be tenant-wide or assigned to the request warehouse. |

The read surface remains filtered by the existing tenant and warehouse-scope rules. Inventory identity is the tenant/product/warehouse combination, with the existing unique constraint preventing duplicate rows for the same lane.

## Direct mutation authority matrix

| Identity | Update | Receive | Adjust | Reconcile | Result |
| --- | --- | --- | --- | --- | --- |
| Warehouse-scoped `TENANT_ADMIN` | Assigned warehouse only | Assigned warehouse only | Assigned warehouse only | Assigned warehouse only | Allowed in scope; wrong warehouse denied with HTTP 403. |
| Tenant-wide `TENANT_ADMIN` | Both warehouses | Both warehouses | Both warehouses | Both warehouses | Allowed across the tenant. |
| `REVIEW_OWNER` | Denied | Denied | Denied | Denied | No direct inventory mutation authority. |
| `FINAL_APPROVER` | Denied | Denied | Denied | Denied | No direct inventory mutation authority. |
| `ESCALATION_OWNER` | Denied | Denied | Denied | Denied | No direct inventory mutation authority. |
| `INTEGRATION_ADMIN` | Denied | Denied | Denied | Denied | Connector/setup authority is separate from direct inventory maintenance. |
| `INTEGRATION_OPERATOR` | Denied | Denied | Denied | Denied | Integration operations do not grant direct inventory maintenance. |
| Anonymous session | Denied | Denied | Denied | Denied | Authentication is required. |
| Platform-owner session | Denied | Denied | Denied | Denied | Platform control-plane authority is not tenant inventory authority. |

## Scoped and tenant-wide results

The focused fixture used `ACCESS-BOUNDARY-REHEARSAL` with `WH-NORTH` and `WH-COAST`.

- The North-scoped Tenant Admin successfully updated, received, adjusted, and reconciled the North row.
- The Coast-scoped Tenant Admin successfully updated, received, adjusted, and reconciled the Coast row.
- North attempts against Coast were denied for all four mutation endpoints with HTTP 403 and did not change Coast state.
- Coast attempts against North were denied for all four mutation endpoints with HTTP 403 and did not change North state.
- The tenant-wide Tenant Admin successfully operated on both lanes, confirming the intended tenant-wide exception.
- Existing scope-filtering coverage confirms warehouse-scoped reads do not expose unrelated warehouse rows.

## Cross-tenant boundary

The request carries a product SKU and warehouse code, not a foreign warehouse identifier. The service resolves both under the current tenant. The test created a product only in `ACCESS-BOUNDARY-ISOLATION`, then attempted to use that SKU from `ACCESS-BOUNDARY-REHEARSAL`; the request returned HTTP 404 and did not create an inventory row in the requesting tenant.

This verifies the effective cross-tenant boundary for the supported API shape without claiming that an arbitrary foreign warehouse object can be injected into a tenant request.

## Update behavior

### Create

`POST /api/inventory/update` creates the missing tenant/product/warehouse row for an authorized Tenant Admin. The fixture confirmed a single North row after the first update and a second Coast row after the Coast update.

### Existing row

Updating the existing North row changes the absolute available quantity and threshold while preserving its reserved quantity. The fixture created stock at 20, created a one-unit order reservation, then updated the requested available quantity to 25. The resulting row had 26 on hand, 1 reserved, and 25 available.

## Receive, adjustment, and reconciliation

- Receive adds the received amount to on-hand stock, reduces inbound quantity without going below zero, updates receipt timing, and returns the updated warehouse row.
- Adjustment applies a signed delta, updates adjustment timing, and returns the updated warehouse row.
- Reconciliation sets the counted on-hand quantity, calculates the variance from the previous count, updates reconciliation timing, and returns the updated warehouse row.
- Each valid operation was exercised positively in both warehouse lanes and was checked through response state and repository readback.

## Reserved quantity protection

The service keeps available stock derived from on-hand minus reserved quantity. The fixture proved that an update preserves an existing reservation. It also proved that an adjustment or reconciliation that would place on-hand below reserved quantity is rejected with HTTP 409 and does not commit the invalid state.

## Validation and missing-row behavior

The focused proof confirms:

- Negative update quantity is rejected with HTTP 400.
- Zero receive quantity is rejected with HTTP 400.
- Zero adjustment delta is rejected with HTTP 400.
- Blank adjustment reason is rejected with HTTP 400.
- Negative reconciliation count is rejected with HTTP 400.
- Receive, adjustment, and reconciliation against a product with no inventory row return HTTP 404.
- Request bodies include the existing validation/error contract and successful mutation responses carry `X-Request-Id`.

## Audit, events, and downstream effects

Valid update, receive, adjustment, and reconciliation operations retain the existing audit and business-event behavior. The focused proof checked inventory audit records and `INVENTORY_RECEIVED`, `INVENTORY_ADJUSTED`, and `INVENTORY_RECONCILED` business events for the affected warehouse. The existing service path also invokes the established monitoring/realtime dispatch path after a valid mutation.

The test suite's existing operational smoke coverage confirms alerts/recommendations and downstream operational surfaces remain connected. This phase did not introduce a new alert or recommendation model and did not change the monitoring engine.

## Frontend warehouse clarity

No frontend files were changed. The existing Inventory page already:

- displays warehouse code and warehouse name on inventory rows and the selected lane;
- shows warehouse coverage and lane-specific stock context;
- checks the selected warehouse against the signed-in warehouse scope before presenting the maintenance action;
- explains that only a warehouse-scoped `TENANT_ADMIN` may use the supported maintenance action;
- requires a non-zero whole-unit adjustment and a reason before submitting;
- states that source-system reconciliation remains a separate responsibility.

The current UI provides a supported controlled adjustment action. Receive and reconcile remain API/service capabilities in this phase rather than new frontend controls.

## Production correction

One real contract defect was found: the adjustment endpoint previously accepted `quantityDelta = 0`, even though a zero adjustment is not a meaningful inventory mutation. The smallest correction was added in `InventoryService.adjustInventory`: zero delta now returns HTTP 400 with the existing API error handling path.

No authority rule, backend contract, tenant model, frontend route, or proof selector was weakened or changed.

## Files intended for closure

- `backend/src/main/java/com/synapsecore/domain/service/InventoryService.java`
- `backend/src/test/java/com/synapsecore/PlatformTenantAccessBoundaryIntegrationTest.java`
- `docs/evidence/inventory-lifecycle-phase-1-warehouse-mutations.md`

Unrelated worktree changes were preserved and are not part of this closure:

- `frontend/Dockerfile`
- `.gitattributes`
- `docs/evidence/scenario-lifecycle-phase-0-authority-census.md`
- `docs/evidence/scenario-lifecycle-phase-10-source-observation.md`

## Limitations and next boundary

- The focused and full backend suites use H2; they do not replace a later PostgreSQL-backed deployment check.
- This phase does not claim idempotency, concurrent mutation, queue processing, or large-scale performance proof; those belong to Inventory Phase 2 or later evidence.
- The frontend supports controlled adjustment but does not expose new receive/reconcile forms here.
- Hosted proof and deployment verification are separate from this repository phase and should be run after the intended production commit is deployed, not as a substitute for the focused test evidence.

## Verdict

Inventory Phase 1 warehouse authority and direct mutation correctness are verified in the repository. The zero-delta adjustment defect is corrected and covered. Critical blockers: 0. High blockers: 0. Inventory Phase 2 remains the next phase and has not started.
