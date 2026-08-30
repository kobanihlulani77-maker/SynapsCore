# Layer 2 Phase 3: Order, Fulfillment and Inventory Convergence

**Scope:** Cross-domain verification of the supported order, fulfillment, and
inventory lifecycle. This phase proves that order-line quantities, fulfillment
state, reservations, on-hand stock, available stock, audit evidence, and
durable operational dispatch remain convergent across successful and rejected
paths.

**Starting HEAD:** `08b3170dc53993d4366a3370463900b6a503d42f`

**Phase status:** Focused cross-domain verification complete. This evidence is
local Spring Boot integration proof using the real application controllers,
services, repositories, authorization checks, H2 migrations, and durable
dispatch queue. It is not a new hosted proof run.

## 1. Boundary and implementation under test

The focused test is:

```text
backend/src/test/java/com/synapsecore/Layer2Phase3FulfillmentConvergenceIntegrationTest.java
```

The test uses the production paths rather than direct row edits:

```text
tenant provisioning API
  -> catalog product API
  -> inventory update API
  -> order creation API
  -> fulfillment update API
  -> order transition API
  -> fulfillment/dashboard read APIs
  -> persisted business events, audit logs, and dispatch work items
```

The fixture creates two disposable tenants. Tenant A has two warehouses and
scoped integration actors. Tenant B has a separate warehouse and a separate
SKU with the same operator-facing identity as one Tenant A SKU. This makes
tenant and warehouse leakage observable without touching `OWNER-ACCEPT-02`.

No production Java, frontend, migration, configuration, or deployment file was
changed for this phase.

## 2. Focused verification result

Command:

```powershell
cd backend
cmd /c mvnw.cmd -Dtest=Layer2Phase3FulfillmentConvergenceIntegrationTest test
```

Result:

```text
Tests run: 4
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

The test profile started Hikari, applied all 13 Flyway migrations to an empty
H2 schema, initialized JPA, and started the Spring messaging broker before the
four test methods ran.

## 3. Inventory and order equations

For every tested order line:

```text
ordered = fulfilled + reserved + cancelled
available = onHand - reservedStock
onHand = openingOnHand - fulfilled + restockedReturns
```

The implementation treats reserved stock as a commitment, not consumed stock.
Dispatch consumes the fulfilled quantity and releases the corresponding
reservation. Cancellation and exception release only the outstanding
reservation. A return decreases fulfilled/returned flow and restocks only when
the explicit return request enables restocking.

## 4. Primary multi-line convergence ledger

Primary fixture:

```text
Tenant: disposable Tenant A
Warehouse: disposable Warehouse A
SKU-A: quantity 8
SKU-B: quantity 6
Opening on-hand for each SKU: 100
```

The following table records the expected persisted line and stock state. The
order-level state is shared by both lines; line-level quantities remain
independent.

| Stage | Order state | Fulfillment state | SKU-A ordered/reserved/fulfilled | SKU-A on-hand/reserved/available | SKU-B ordered/reserved/fulfilled | SKU-B on-hand/reserved/available |
| --- | --- | --- | --- | --- | --- | --- |
| Create | RECEIVED | QUEUED | 8 / 8 / 0 | 100 / 8 / 92 | 6 / 6 / 0 | 100 / 6 / 94 |
| Picking | PROCESSING | PICKING | 8 / 8 / 0 | 100 / 8 / 92 | 6 / 6 / 0 | 100 / 6 / 94 |
| Packed | PROCESSING | PACKED | 8 / 8 / 0 | 100 / 8 / 92 | 6 / 6 / 0 | 100 / 6 / 94 |
| Partial dispatch A | PARTIALLY_FULFILLED | DISPATCHED | 8 / 5 / 3 | 97 / 5 / 92 | 6 / 6 / 0 | 100 / 6 / 94 |
| Complete dispatch A | PARTIALLY_FULFILLED | DISPATCHED | 8 / 0 / 8 | 92 / 0 / 92 | 6 / 6 / 0 | 100 / 6 / 94 |
| Complete dispatch B | FULFILLED | DISPATCHED | 8 / 0 / 8 | 92 / 0 / 92 | 6 / 0 / 6 | 94 / 0 / 94 |
| Delivery | DELIVERED | DELIVERED | 8 / 0 / 8 | 92 / 0 / 92 | 6 / 0 / 6 | 94 / 0 / 94 |

The delivery transition changes terminal state and timestamps only. It does not
consume stock a second time or alter fulfilled, reserved, or available values.

## 5. Successful lifecycle path

The primary test proves this full loop:

```text
order creation
  -> both lines reserve independently
  -> PICKING
  -> PACKED
  -> SKU-A partial dispatch of 3
  -> SKU-A completion dispatch of 5
  -> SKU-B completion dispatch of 6
  -> FULFILLED order
  -> DELIVERED terminal state
  -> backlog returns to zero
  -> dashboard fulfillment backlog returns to zero
  -> durable dispatch work completes
  -> business events and success audits exist
```

The test checks the fulfillment overview and dashboard snapshot while the order
is packed and after delivery. It also verifies the order contains both lines,
that all reservations are released by completion, and that the final inventory
math remains exact.

## 6. Fulfillment state handling

The focused coverage verifies:

| Path | Expected result |
| --- | --- |
| `QUEUED -> PICKING` | Accepted; order becomes `PROCESSING`; stock unchanged |
| `PICKING -> PICKING` | Accepted as an idempotent same-state update |
| `PICKING -> PACKED` | Accepted; stock unchanged |
| `PACKED -> PICKING` | Rejected; state and stock unchanged |
| `PACKED -> DISPATCHED` | Accepted only with valid fulfillment quantities |
| partial `DISPATCHED` | Consumes only the dispatched line quantity |
| complete `DISPATCHED` | Consumes the remaining reservation and may complete the order |
| `DISPATCHED -> DELIVERED` | Accepted; no second inventory consumption |
| terminal repeat return | Rejected; no second restock |

Existing Fulfillment and Order lifecycle suites provide additional invalid
transition and terminal-state coverage; this phase adds the cross-domain
ledger that ties the state transitions to persisted inventory facts.

## 7. Rejected fulfillment and atomicity

The boundary test attempts:

```text
unknown SKU on a valid order
quantity greater than the line reservation
quantity supplied for a non-dispatch status
backward fulfillment transition
over-fulfillment on one line of a multi-line order
same request ID with a different payload
```

The application returns the existing contract statuses (`400` for malformed or
invalid business requests and `409` for an idempotency conflict). After each
rejection the test verifies that the order line, order status, fulfillment
task, and inventory ledger are unchanged.

The multi-line check uses a separate SKU for the rejected line so that a second
order's legitimate reservation cannot be mistaken for a mutation of the first
order's stock ledger.

## 8. Cancellation, exception, delay, and return branches

The focused test proves the following stock rules:

| Branch | Result |
| --- | --- |
| cancel before fulfillment | Outstanding reservation released; on-hand unchanged |
| cancel after partial fulfillment | Fulfilled units remain consumed; only outstanding reservation is released |
| exception before dispatch | Order fails; all reservation is released; on-hand unchanged |
| exception after partial fulfillment | Fulfilled units remain consumed; remaining reservation is released |
| delayed fulfillment | Order becomes `BLOCKED`; reservation remains visible; stock is unchanged |
| delayed recovery to picking | Task returns to active processing without stock drift |
| return without restock | Returned quantity is recorded; inventory is not restored |
| return with restock | Returned quantity is recorded and on-hand increases by the returned amount |
| repeated return | Rejected as a terminal duplicate; no second inventory change |

The cancellation and exception checks account for prior same-SKU activity in the
fixture. This is important because inventory is warehouse/SKU scoped and is
shared by multiple orders, while the order-line ledger is order scoped.

## 9. Authority and isolation boundaries

The test verifies:

```text
Tenant A Integration Admin / Integration Operator + Warehouse A
  -> may create and process Tenant A Warehouse A orders

Tenant A Warehouse B operator
  -> cannot process a Warehouse A order; HTTP 403

Tenant A Tenant Admin
  -> cannot use the fulfillment write path; HTTP 403

Tenant B operator
  -> cannot resolve Tenant A's external order ID; HTTP 404

Tenant A Warehouse B inventory
  -> unchanged by Warehouse A order fulfillment

Tenant B inventory
  -> unchanged by Tenant A order fulfillment
```

The test also creates the same SKU identity in Tenant B and confirms that
tenant-scoped product resolution does not cross the tenant boundary.

## 10. Concurrent dispatch

Two concurrent dispatch requests attempt to fulfill six units against a line
with ten reserved units. The test asserts:

```text
both requests complete with an allowed application result
no request produces an unhandled test error
fulfilled <= ordered
fulfilled + reserved + cancelled = ordered
onHand = openingOnHand - fulfilled
available = onHand - reservedStock
Warehouse B remains unchanged
```

Pessimistic order/task locking and the inventory reservation checks therefore
prevent over-fulfillment in the tested race.

## 11. Durable operational evidence

For the successful primary lifecycle, the test verifies:

```text
FULFILLMENT_UPDATED business event
ORDER_STATUS_TRANSITIONED business event
FULFILLMENT_UPDATED success audit
ORDER_STATUS_UPDATED success audit
```

The test records the request IDs for each fulfillment operation and waits with
a bounded five-second polling loop. It requires every request ID to appear in
completed durable work items, allowing multiple work items for one request when
the application publishes both fulfillment and inventory operational updates.

The observed queue contract is:

```text
fulfillment request
  -> FULFILLMENT_UPDATE dispatch item
  -> INVENTORY_UPDATE dispatch item when stock changes
  -> bounded queue processing
  -> COMPLETED status
```

This is intentionally different from asserting one work item per HTTP request.
The durable unit is the published operational update, while request identity
links the updates back to the originating action.

## 12. Downstream operational surfaces

The primary path verifies that:

```text
PICKING/PACKED open work
  -> fulfillment backlog is visible
  -> dashboard snapshot reports the same backlog

DELIVERED terminal work
  -> fulfillment backlog is zero
  -> dashboard snapshot reports zero fulfillment backlog
```

The proof is limited to the fulfillment and dashboard read contracts needed for
this phase. It does not reopen the separate Replay, Recommendations, Alerts,
Realtime, Scenario, or Phase 4 validation programs.

## 13. Evidence classifications

| Classification | Meaning in this phase | Remaining |
| --- | --- | ---: |
| A | Required Phase 3 order/fulfillment/inventory convergence assertions | 0 |
| B | Existing adjacent lifecycle coverage reused as supporting evidence | 0 new gap |
| C | Hosted or owner-managed evidence outside this local cross-domain phase | Deferred |
| D | Out of scope by explicit phase boundary | Deferred |

Classification C includes PostgreSQL/Render execution of this exact new
cross-domain suite and any browser-level visual walkthrough. Those are not
claimed by this local test run and should be scheduled only under the relevant
hosted or pilot evidence program.

## 14. Verification boundary and conclusion

No hosted proof was run. No production behavior was changed. The focused test
passes with four tests and demonstrates that the supported order, fulfillment,
and inventory paths converge across normal progression, partial completion,
terminal delivery, cancellation, exception, delay, return, rejection,
authority, tenant isolation, warehouse isolation, and concurrent dispatch.

**Phase conclusion:** Classification A remaining is `0` for the local Phase 3
contract. Hosted/owner evidence remains explicitly deferred rather than implied.
