# Orders Phase 2: State Transitions, Terminal Truth, and Concurrency

## Verdict

The Orders Phase 2 lifecycle is verified against the current repository:

- focused proof: 6 tests, 0 failures, 0 errors;
- full backend proof: 205 tests, 0 failures, 0 errors;
- no frontend source changed;
- no hosted proof was run because this phase changed backend runtime code and deployment confirmation is owner-managed.

The changes are limited to order lifecycle locking, terminal-state protection, partial failure/cancellation accounting, and fulfillment quantity preservation.

## Scope and Evidence

Primary implementation surfaces:

- `backend/src/main/java/com/synapsecore/domain/service/OrderService.java`
- `backend/src/main/java/com/synapsecore/fulfillment/FulfillmentService.java`
- `backend/src/main/java/com/synapsecore/domain/repository/CustomerOrderRepository.java`
- `backend/src/main/java/com/synapsecore/domain/repository/FulfillmentTaskRepository.java`
- `backend/src/main/java/com/synapsecore/api/controller/OrderController.java`
- `backend/src/main/java/com/synapsecore/api/controller/FulfillmentController.java`

Focused evidence:

- `backend/src/test/java/com/synapsecore/OrderLifecyclePhase2IntegrationTest.java`
- existing `backend/src/test/java/com/synapsecore/InventoryConcurrencyIntegrationTest.java`
- existing `backend/src/test/java/com/synapsecore/MvpFlowIntegrationTest.java`
- existing `backend/src/test/java/com/synapsecore/PlatformTenantAccessBoundaryIntegrationTest.java`

The focused command was:

```powershell
cd backend
cmd /c "mvnw.cmd -Dtest=OrderLifecyclePhase2IntegrationTest test"
```

The complete command was:

```powershell
cd backend
cmd /c "mvnw.cmd test"
```

The full run reported `Tests run: 205, Failures: 0, Errors: 0`.

## Entry Points and Authority

### Order creation

`POST /api/orders` is exposed by `OrderController`. It requires an operational write identity and a warehouse scope before `OrderService.createOrder` runs. The service:

1. resolves the tenant warehouse;
2. requires the product in that tenant;
3. requires a caller-supplied stable `externalOrderId`;
4. reserves inventory for every line;
5. persists the order and line items in `RECEIVED` state;
6. creates one fulfillment task;
7. reevaluates operational signals;
8. records business/audit evidence and publishes an operational state change.

The tenant plus external order id is unique. Duplicate creation is rejected rather than creating a second order or reservation.

### Manual lifecycle transition

`POST /api/orders/{externalOrderId}/transition` first resolves the order warehouse for access control, then `OrderService.transitionOrder` reloads the order with a pessimistic write lock and refreshes the entity before applying the transition. Cancellation and return use the same locked transition path.

### Fulfillment update

`POST /api/fulfillment/updates` resolves the tenant-scoped order and fulfillment task, locks both records, refreshes the order, applies the fulfillment update, synchronizes order status and line quantities, then records business/audit evidence, evaluates fulfillment alerts/recommendations, and publishes a realtime update.

Operational order and fulfillment writes are restricted to the existing integration write roles and warehouse scope. Tenant, warehouse, and entity ownership checks remain in the existing access and scope guards.

## State Machine

The manual order transition rules currently implemented in `OrderService` are:

| Current | Allowed next states |
| --- | --- |
| `CREATED` | `RECEIVED`, `CANCELLED` |
| `RECEIVED` | `PROCESSING`, `PARTIALLY_FULFILLED`, `FULFILLED`, `BLOCKED`, `CANCELLED`, `FAILED` |
| `PROCESSING` | `PARTIALLY_FULFILLED`, `FULFILLED`, `BLOCKED`, `CANCELLED`, `FAILED` |
| `PARTIALLY_FULFILLED` | `FULFILLED`, `DELIVERED`, `BLOCKED`, `RETURNED`, `CANCELLED`, `FAILED` |
| `FULFILLED` | `DELIVERED`, `RETURNED`, `BLOCKED` |
| `DELIVERED` | `RETURNED` |
| `BLOCKED` | `PROCESSING`, `CANCELLED`, `FAILED` |
| `CANCELLED` | no other state |
| `RETURNED` | no other state |
| `FAILED` | no other state |

Same-state transitions are accepted as an idempotent status readback/update. Terminal states cannot be reopened.

There is one deliberately narrow fulfillment compatibility rule: a fulfillment event may move a newly created `RECEIVED` order directly to `DELIVERED`, because that direct fulfillment path is an established existing contract. The manual order transition endpoint does not receive that exception. This preserves existing fulfillment behavior without broadening manual authority.

## Successful Lifecycle Paths

### Normal order path

```text
POST /api/orders
  -> CREATED during assembly
  -> inventory reservation
  -> RECEIVED persisted
  -> fulfillment task QUEUED
  -> dashboard/realtime/audit signals
```

Fulfillment can then move the task through `QUEUED`, `PICKING`, `PACKED`, `DISPATCHED`, and `DELIVERED`. The order status is derived from the task and line quantities:

```text
DISPATCHED with some units
  -> line fulfilled increases by requested units
  -> line reserved decreases by the same units
  -> inventory on-hand decreases by fulfilled units
  -> order PARTIALLY_FULFILLED while reservation remains

DISPATCHED with all remaining units
  -> order FULFILLED

DELIVERED
  -> any remaining reserved units are fulfilled
  -> order DELIVERED
```

### Line and inventory conservation

For each order line, the verified accounting invariant is:

```text
ordered quantity = fulfilled quantity + cancelled quantity + reserved quantity
```

Inventory reflects the same transition:

```text
available = on-hand - reserved
```

Partial dispatch leaves the unfulfilled quantity reserved. It does not cancel or silently consume the outstanding quantity.

## Cancellation, Failure, and Return Paths

### Cancellation

Cancellation releases only outstanding reservations and increments the line's cancelled quantity by the released amount. Units already fulfilled remain fulfilled. A repeated cancellation is a same-state no-op and does not release or consume stock twice.

### Failure

Failure releases outstanding reservations regardless of whether some units were already fulfilled. It then moves the order to `FAILED`. A partial failure therefore preserves fulfilled quantity, marks only the outstanding quantity cancelled, and returns only the outstanding reservation to availability.

### Return

Return is permitted only when fulfilled units are available. It increments returned quantity and may restock fulfilled units when the request enables restocking. A repeated return with no returnable units is rejected and does not restock again. The current return API is whole-order/returnable-unit based; it is not a separate per-line partial-return workflow.

### Invalid or terminal paths

The transition validator rejects unsupported transitions with `400`. In particular, `CANCELLED`, `FAILED`, and `RETURNED` cannot be reopened. `FULFILLED` and `DELIVERED` do not accept cancellation. Fulfillment synchronization also uses the same protection, so a late fulfillment event cannot reopen a terminal order.

## Concurrency and Locking

The mutation paths use database pessimistic write locks on the tenant-scoped order. The service refreshes the locked order before applying changes, preventing a stale persistence-context snapshot from overwriting a concurrent outcome. Fulfillment updates additionally lock their fulfillment task and refresh the order before synchronization.

The focused concurrent tests cover:

- cancellation versus partial fulfillment;
- failure versus partial fulfillment;
- terminal order reopening attempts;
- inventory and line-item conservation after serialized outcomes;
- partial cancellation and partial failure reservation release.

The accepted race result is whichever transaction acquires the lock first. The other request either applies to the current state or receives a transition rejection; it cannot create a mixed order/inventory state.

## Realtime, Audit, and Operational Effects

Successful order creation, lifecycle transitions, and fulfillment updates record business/audit evidence and publish an internal operational state change. The existing dispatch/realtime path then refreshes focused operational views. Fulfillment evaluation may update operational alerts and recommendations based on actual fulfillment pressure.

The Phase 2 changes do not make hypothetical data live, do not bypass authorization, and do not create a new event bus or worker architecture.

## Retry and Duplicate Safety Boundary

The following are verified:

- stable external order identity prevents duplicate order creation;
- repeated cancellation does not release twice;
- repeated return does not restock twice;
- concurrent fulfillment/cancellation/failure is serialized by locks;
- terminal fulfillment synchronization cannot reopen a terminal order.

Positive fulfillment deltas now use the existing `X-Request-Id` as the caller-stable retry identity. The request ID is persisted in the successful `FULFILLMENT_UPDATED` audit record, scoped by tenant, action, and Order external ID. After the Order and FulfillmentTask locks are acquired, a repeat with the same identity returns the committed task state before applying the delta or publishing downstream operational evidence.

The controlled-pilot contract is explicit: callers performing an additive fulfillment update must generate `X-Request-Id` before the first request and reuse it after a timeout or lost response. A new request ID means a distinct fulfillment operation. If the first request omitted the header, the server-generated response ID cannot be recovered after a lost response, so arbitrary blind retry safety is not claimed for that request.

This is the same persisted-audit identity pattern already used by Inventory receive, adjustment, and reconciliation. No new event table, event bus, or broad idempotency subsystem was introduced.

## Orders Phase 2 Final Retry Completeness

### Production caller census

The only production mutation entry point is `POST /api/fulfillment/updates` in `FulfillmentController`, which delegates to `FulfillmentService.recordUpdate`. No frontend source, webhook path, CSV importer, scheduled connector worker, or integration replay service directly calls this fulfillment update path. `scripts/verify-company-readiness.ps1` is support/manual verification tooling and currently submits a non-additive `DELAYED` update without a positive fulfillment delta. Tests are the remaining direct callers.

There is no automatic fulfillment retry in the frontend or backend. A controlled external or operator caller can safely retry an uncertain additive update by reusing the original caller-supplied `X-Request-Id`.

### Identity model

- `externalOrderId` is the tenant-scoped business Order identity and the fulfillment target reference.
- `X-Request-Id` is the caller-stable transport/operation identity for a retryable request. It remains distinct from the business Order identity and internal FulfillmentTask ID.
- FulfillmentTask ID is the internal persisted fulfillment-lane identity.
- There is no separate source-system fulfillment event ID in the current API contract. The supported retry contract uses the existing request trace identity.

### Focused retry proof

The focused `OrderLifecyclePhase2IntegrationTest` now includes a seven-test regression suite. Its isolated fixture creates an Order for 10 units, applies event A as `+3`, repeats event A with the same `X-Request-Id`, then applies event B as `+7` with a different request ID.

| Check | Result |
| --- | --- |
| Same logical `+3` retry | Repeat returns `200` without a second quantity application. Fulfilled remains `3`; reserved remains `7`; on-hand becomes `7`; available remains `0`. |
| Distinct `+7` event | New request identity progresses the same Order to fulfilled `10`; reserved, on-hand, and available each become `0`. |
| Audit evidence | One successful `FULFILLMENT_UPDATED` audit record for event A and one for event B; no duplicate business fulfillment evidence for the retry. |
| Business event evidence | One `FULFILLMENT_UPDATED` event for event A and one for event B. |
| Multi-line behavior | The current fulfillment request is aggregate at Order/FulfillmentTask level and has no line identifier. Line-specific fulfillment updates are not part of the supported API, so no separate-line dedupe claim is made. |

Before this correction, the same additive `+3` request entered the normal delta path again and would have advanced the task and Order line to `6` while consuming another three units from the reservation. That was a Classification A operational defect for a realistic lost-response retry and is now fixed at the existing locked fulfillment boundary.

## Findings and Disposition

| Finding | Classification | Disposition |
| --- | --- | --- |
| Stale concurrent order snapshot could overwrite a fulfillment outcome | Classification A, fixed | Locked and refreshed order in lifecycle mutation paths; race proof passes. |
| Partial failure did not release outstanding reservation | Classification A, fixed | Failure now releases outstanding reservation while preserving fulfilled units; focused proof passes. |
| Fulfillment could reopen a cancelled/failed order | Classification A, fixed | Shared operational status validation blocks terminal reopening; focused proof passes. |
| Direct fulfillment delivery from `RECEIVED` is an existing supported path | Compatibility boundary | Preserved only for fulfillment-derived delivery; manual transition remains guarded. |
| Positive partial fulfillment retry could double-apply an uncertain delta | Classification A, fixed | Reused caller-supplied `X-Request-Id` is matched against the persisted successful fulfillment audit after the locked Order/Task read; the repeat returns committed state without a second stock, Order, event, audit, or realtime mutation. |

## Verification Interpretation

The Orders Phase 2 retry-completeness gate is green. The focused suite now reports 7 tests, 0 failures, and 0 errors. The full backend suite after the narrow retry correction reports 206 tests, 0 failures, and 0 errors. The test environment uses the repository's existing test profile and H2-backed integration contexts; this report does not substitute for deployment-owner confirmation against the live PostgreSQL/Redis deployment.

Orders Phase 3 is intentionally not started.
