# Order Lifecycle Phase 1: Creation, Authority, Atomicity and Duplicate Safety

**Status:** `ORDER CREATION LIFECYCLE VERIFIED AND CLOSED`

**Scope:** This phase verifies the live-order creation boundary and its immediate
database, inventory, fulfillment, event and audit effects. It does not reopen
Catalog or Inventory lifecycle closure and does not begin the deeper integration,
CSV, webhook, replay or Order Phase 2 work.

**Starting repository HEAD:** `f8514b572bb53bd30f7c3936fddf137bc74fc499`

**Test environment:** Spring Boot test profile with an isolated H2 database and
the repository's existing Flyway migrations. This is repository evidence, not a
claim that a hosted database was used for this phase.

## Acceptance Summary

The Order creation boundary is transactionally safe for the tested paths:

- only integration-authorized operators can create live orders;
- warehouse scope is enforced before the write and again through domain guards;
- tenant and product resolution are tenant-scoped;
- each order creation is all-or-nothing across its order, order items and stock
  reservations;
- the database uniqueness constraint and service precheck prevent duplicate
  stable external IDs from creating a second order;
- concurrent duplicate stable IDs produce one created order and one conflict;
- generated IDs are supported for direct convenience creation but are not a
  general retry-idempotency mechanism;
- one successful order creates one fulfillment lane and produces operational
  event/audit/realtime effects through the existing service path.

## Detailed Closure Report

### 1. Starting HEAD

`f8514b572bb53bd30f7c3936fddf137bc74fc499`.

### 2. Creation role matrix

`POST /api/orders` reaches `OrderController.createOrder`, which first calls
`AccessControlService.requireOperationalWrite(request.warehouseCode(), ...)`.
The supported creation roles are:

| Role | Create live order | Boundary |
| --- | --- | --- |
| `INTEGRATION_ADMIN` | Allowed | Must be authorized for the target warehouse. |
| `INTEGRATION_OPERATOR` | Allowed | Must be authorized for the target warehouse. |
| `TENANT_ADMIN` | Denied | Workspace/setup authority is not operational ingestion authority. |
| `REVIEW_OWNER` | Denied | Governance review authority is not order-ingestion authority. |
| `FINAL_APPROVER` | Denied | Approval authority is not order-ingestion authority. |
| `ESCALATION_OWNER` | Denied | Escalation/SLA authority is not order-ingestion authority. |
| Platform/control-plane identity | Denied for tenant order creation | Platform visibility does not grant tenant operational write authority. |

The role separation is covered by
`PlatformTenantAccessBoundaryIntegrationTest.operationalWriteAuthoritySeparatesSetupIntegrationAndGovernanceResponsibilities`.

### 3. Scoped warehouse matrix

For an allowed integration role, the submitted `warehouseCode` must be present
in that operator's warehouse scope. A scoped operator may create only in its
assigned lane. A tenant-wide setup role does not become an integration writer
merely because it can manage workspace configuration.

The boundary is exercised by
`PlatformTenantAccessBoundaryIntegrationTest.warehouseScopeFiltersReadsAndDeniesWritesOutsideAssignedLane`.
The order service then resolves the warehouse within the current tenant and
applies tenant/warehouse guards while resolving products and reserving stock.

### 4. Tenant-wide authority result

Tenant-wide authority means the operator can manage supported tenant/workspace
configuration. It does **not** mean that every role may create orders in every
warehouse. Live order creation remains an integration responsibility and still
requires the target warehouse to be authorized for that integration identity.

### 5. Cross-tenant result

Tenant context is resolved before order creation, warehouse lookup, product
resolution and inventory reservation. A product, warehouse or order belonging
to another tenant is not accepted as a resource for the current tenant. Existing
platform/tenant boundary coverage includes cross-tenant reads and writes, and
the creation path uses the same tenant-scoped repositories and guards.

### 6. Product resolution

Every submitted SKU is resolved through the tenant-scoped inventory/product
service. An unknown SKU fails the request with a clear client error; a SKU that
exists only in another tenant is not a valid product for the current order.
The existing downstream Catalog boundary test and
`MvpFlowIntegrationTest.orderIngestionFailsClearlyWhenInventoryRecordIsMissing`
cover the product/inventory resolution boundary.

### 7. Missing inventory

If a valid tenant product has no inventory row for the target warehouse, order
creation fails with a clear `400` response and the order is not created. The
missing-inventory case is covered by
`MvpFlowIntegrationTest.orderIngestionFailsClearlyWhenInventoryRecordIsMissing`.

### 8. Insufficient stock

If available stock is lower than the requested quantity, the reservation fails
with a conflict and the request does not create a live order. Existing inventory
concurrency coverage verifies that an insufficient single-line request does not
leave a reservation behind.

### 9. Single-line semantics

A valid single-line order resolves one product, reserves the requested quantity,
creates one order item, transitions the order into the received operational
state, initializes fulfillment and emits the normal operational effects. The
existing MVP order-creation and inventory-concurrency tests cover this baseline.

### 10. Rollback

`OrderService.createOrder` is transactional. Reservation, order-item creation,
order persistence, fulfillment initialization and operational side effects share
the creation transaction boundary. The added
`MvpFlowIntegrationTest.multiLineOrderFailureRollsBackEarlierReservationsAndOrderPersistence`
submits two lines where the second line cannot be reserved and verifies all of
the following after the request fails:

- the first line's reservation is zero;
- the first line's available quantity is unchanged;
- the second line's reservation is zero;
- the second line's available quantity is unchanged;
- no order exists for the submitted external ID.

The test uses a fresh read transaction for its assertions so the result reflects
committed database state rather than a stale test persistence context.

### 11. Multi-line success

The added
`MvpFlowIntegrationTest.multiLineOrderReservesEachLineAndCreatesOneFulfillmentLane`
verifies that a valid two-line order:

- returns `201 Created`;
- retains both submitted items;
- reserves each line's requested quantity in the same warehouse;
- persists one order under the tenant and external ID;
- creates one fulfillment task/lane for that order.

### 12. Partial failure

Creation is not a partial-success operation. A failure in any submitted line
rolls back the whole order, as proven in item 10. Partial fulfillment is a
subsequent order lifecycle concern and is not treated as partial creation in this
phase.

### 13. Duplicate product-line behavior

The direct order API accepts repeated product lines as submitted. The added
`MvpFlowIntegrationTest.duplicateProductLinesReserveExactlyTheirSubmittedTotal`
verifies two lines for the same SKU are retained as two order items and reserve
the aggregate quantity exactly once per submitted unit. On that direct API path,
quantity-on-hand remains unchanged while quantity-reserved increases and
quantity-available decreases.

Connector normalization is a separate boundary:

- strict connectors reject duplicate product lines;
- relaxed connectors consolidate repeated SKU lines before order creation.

This difference is intentional and is not silently treated as one universal
line-normalization contract.

### 14. External-ID uniqueness

The business identity for an incoming order is the tenant plus
`externalOrderId`. The database has a unique constraint on
`(tenant_id, external_order_id)`, in addition to the service-level availability
check. The existing
`MvpFlowIntegrationTest.duplicateExternalOrderIdIsRejectedWithoutFurtherInventoryImpact`
proves that the same stable external ID cannot create a second order or consume
additional inventory.

### 15. Duplicate create

Sequential duplicate creation returns `409 Conflict` with the existing
external-ID conflict message. The first request remains the only persisted order
and only one reservation is applied.

### 16. Concurrent duplicate external ID

The added
`InventoryConcurrencyIntegrationTest.concurrentDuplicateExternalOrderIdCreatesOneOrderAndReturnsOneConflict`
runs two simultaneous creates with the same tenant and external ID. The
observed result is exactly one `201` and one `409`; one order is persisted and
only one unit is reserved.

The second concurrent request is ultimately protected by the database unique
constraint. The current generic `DataIntegrityViolationException` handler
returns a safe `409` operational conflict. H2 logs the underlying unique-index
warning during this test; that is expected evidence of the race being closed,
not a second order or reservation.

### 17. Generated-ID retry

When `externalOrderId` is absent on the direct order API, the service generates
an `ORD-...` identifier. The existing
`MvpFlowIntegrationTest.generatedOrderIdsUseOperationalPrefixAndChangeOperationalState`
proves generated-ID creation and operational state change.

An identical retry without a caller-supplied stable ID would generate a new
business ID. It is therefore not safe to claim that a generated-ID retry is
deduplicated.

### 18. Generated-ID retry classification

**Classification B: intentional API boundary / controlled pilot requirement.**

The direct API supports generated IDs as a convenience path, while connector
ingress requires an external ID. Retry-safe integration callers must preserve
and resubmit the same source-system external ID. A future general idempotency-key
contract would be a separate improvement and is not part of Order Phase 1.

### 19. Stable external-ID retry

Stable external-ID retry is proven for a sequential repeated request by
`MvpFlowIntegrationTest.duplicateExternalOrderIdIsRejectedWithoutFurtherInventoryImpact`.
The duplicate is rejected and inventory is unchanged after the first reservation.
Concurrent stable-ID protection is separately proven by the test in item 16.

### 20. Request-ID and business-ID behavior

The platform carries a request correlation ID through request handling, logs and
error responses. That request ID is diagnostic correlation, not the business
deduplication key. `externalOrderId` is the business identity used for duplicate
protection within a tenant. There is no general request-ID idempotency contract
that turns a retried request with a newly generated business ID into the same
order.

### 21. Connector default-warehouse boundary

Connector policy resolves a default warehouse only when the connector is
configured to allow default-warehouse fallback and no warehouse was supplied.
Strict connector policy rejects a supplied warehouse that conflicts with its
configured default. The resulting warehouse still passes the normal tenant and
operator authority checks before order creation. Connector behavior is only
smoke-covered here; deeper connector lifecycle verification remains deferred.

### 22. CSV/webhook smoke

Existing MVP and boundary suites confirm the basic creation boundary for CSV and
webhook ingress, including disabled-connector failure and successful supported
paths. This Phase 1 record does not claim complete CSV, webhook, scheduled-pull
or replay lifecycle closure. Those domains remain outside this phase.

### 23. Order, inventory and fulfillment consistency

For successful creation, the order stores the submitted warehouse and items,
inventory reservation reflects every accepted quantity, and one fulfillment lane
is initialized. For failed creation, the order and reservations are absent after
the transaction rolls back. For duplicate creation, order count and reservation
count do not increase a second time.

Quantity semantics are explicit: `quantityOnHand` is not reduced by order
reservation; `quantityReserved` increases and `quantityAvailable` decreases.

### 24. Event and audit truth

The order service records the existing `ORDER_INGESTED` business event, records a
successful order audit entry, records inventory reservation effects through the
inventory service, records operational metrics and publishes the existing
`ORDER_FLOW` update. Failed and conflicting requests use the existing error/audit
path. This phase verifies the creation path's integration with those existing
services; it does not claim that every downstream realtime consumer was tested
in this backend-focused suite.

### 25. Production defects

No Classification A production defect was found in the Phase 1 creation,
authority, atomicity or duplicate-safety scope.

No backend runtime code was changed.

### 26. Test or fixture defects

The first version of the new rollback test observed an earlier reservation while
the class-level test transaction still held a stale persistence context. That
was a test-observation defect, not a production atomicity defect. The test was
corrected to run outside the class transaction, use explicit `REQUIRES_NEW`
fixture/assertion transactions and dirty the context after completion.

The concurrent duplicate test intentionally exposes the database conflict log
because it verifies the actual race boundary. The HTTP outcome remains the
expected `201/409` pair.

### 27. Fixes made

Only verification changes were made:

- added multi-line success coverage;
- added multi-line rollback coverage;
- added duplicate product-line coverage;
- added concurrent duplicate external-ID coverage;
- corrected the rollback test transaction isolation so assertions read committed
  state;
- added this evidence document.

No product source, API contract, frontend code, database migration or proof
selector was changed.

### 28. Classification A/B/C/D

| Classification | Phase 1 result |
| --- | --- |
| A: required operational work / production defect | None found. No blocker. |
| B: intentional boundary or controlled-pilot limitation | Generated IDs are not retry-idempotent; stable external IDs are required for retry-safe integration behavior. Direct duplicate-line semantics differ from connector normalization by policy. |
| C: evidence or fixture limitation | Phase 1 concurrency evidence runs on H2; PostgreSQL-specific contention behavior and production load characteristics are not claimed here. Full connector/realtime consumer observation is not part of this phase. |
| D: deferred scope | Deep CSV/webhook/integration/replay verification, Order Phase 2 lifecycle work, and broader performance/scale validation. |

### 29. Focused tests

Command run:

```powershell
cd C:\Users\asus\Downloads\synapsecore_starter\synapsecore\backend
cmd /c "mvnw.cmd -Dtest=MvpFlowIntegrationTest,InventoryConcurrencyIntegrationTest,PlatformTenantAccessBoundaryIntegrationTest test"
```

Result:

- `MvpFlowIntegrationTest`: 84 tests, 0 failures, 0 errors;
- `InventoryConcurrencyIntegrationTest`: 11 tests, 0 failures, 0 errors;
- `PlatformTenantAccessBoundaryIntegrationTest`: 31 tests, 0 failures, 0 errors;
- total: **126 tests, 0 failures, 0 errors**;
- Maven result: `BUILD SUCCESS`.

The corrected rollback test was also run in isolation and passed: 1 test, 0
failures, 0 errors.

### 30. Full backend result if required

Not required and not rerun. No production code changed. The focused suite covers
the intended Order Phase 1 boundary and the relevant existing authority suites.

### 31. Frontend checks

Not run. No frontend files changed and this phase is backend lifecycle evidence
only. The frontend Orders surface remains observation-oriented for this scope;
no mutation UI or selector contract was changed.

### 32. Files changed

Intended Phase 1 files:

- `backend/src/test/java/com/synapsecore/MvpFlowIntegrationTest.java`
- `backend/src/test/java/com/synapsecore/InventoryConcurrencyIntegrationTest.java`
- `docs/evidence/order-lifecycle-phase-1-creation-atomicity.md`

Unrelated pre-existing worktree changes were preserved and are not part of this
closure:

- `frontend/Dockerfile`
- `.gitattributes`
- `docs/evidence/scenario-lifecycle-phase-0-authority-census.md`
- `docs/evidence/scenario-lifecycle-phase-10-source-observation.md`

### 33. Commits

The intended files are to be committed as one Phase 1 evidence change with the
message:

```text
Verify Order creation atomicity and duplicate safety
```

The commit SHA and push result are recorded in the closure response after the
commit is created.

### 34. Critical blockers

**0.** No critical blocker was found in the Phase 1 scope.

### 35. High blockers

**0.** No high blocker was found in the Phase 1 scope.

### 36. Classification A remaining

**None.** The tested Order creation path does not require a production fix before
Phase 1 closure.

### 37. Readiness for Phase 2

The repository is ready to begin Order Phase 2 only after this Phase 1 evidence
is committed and pushed. Phase 2 is not started by this change. Its scope must
be agreed separately and should not reinterpret the generated-ID limitation as
already solved.

### 38. Phase 1 verdict

**ORDER CREATION LIFECYCLE VERIFIED AND CLOSED**

The creation boundary is proven for role authority, warehouse scope, tenant-safe
product resolution, missing/insufficient inventory, multi-line success,
transaction rollback, duplicate lines, sequential duplicate IDs, concurrent
duplicate IDs and immediate fulfillment/event/audit consistency. The remaining
limitations are explicitly classified rather than hidden:

- stable external IDs are required for retry-safe ingress;
- no general request-ID idempotency key exists;
- Phase 1 concurrency evidence uses H2 rather than a production PostgreSQL
  contention run;
- deeper connector, CSV, webhook, replay and subsequent Order lifecycle work is
  deferred.

**STOP:** Do not start Order Phase 2 from this document.
