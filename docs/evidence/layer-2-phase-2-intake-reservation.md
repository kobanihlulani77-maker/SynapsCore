# Layer 2 Phase 2 Evidence: Intake, Order Creation, and Reservation

**Scope:** Source intake through validation, tenant-scoped order creation, order-line persistence, inventory reservation, queued fulfillment initialization, and downstream operational observation.

**Phase status:** Focused verification complete. This evidence does not start or verify the later fulfillment, replay execution, scenario governance, or broad hosted proof phases.

## 1. Baseline and test boundary

The repository HEAD immediately before Layer 2 Phase 2 work was:

`61df0354897c08cd1d894cd829542e6246532797`

The preceding Layer 2 Phase 1 CI prerequisite was GitHub Actions run `33325299216`, completed successfully for commit `61df0354897c08cd1d894cd829542e6246532797`.

The focused test is:

`backend/src/test/java/com/synapsecore/Layer2Phase2IntakeReservationIntegrationTest.java`

It runs the Spring application with MockMvc, H2, isolated tenants, isolated warehouses, and a local HTTP server for scheduled-pull input. It does not use a hosted database, hosted credentials, or a hosted browser session. Consequently, it proves the application and persistence flow, not hosted deployment or browser websocket rendering.

## 2. Operational chain

```text
Source event or operator request
  -> webhook, CSV import, direct order API, or scheduled pull
  -> connector, tenant, warehouse, and request validation
  -> CustomerOrder validation and persistence
  -> OrderItem persistence for each accepted line
  -> inventory reservation
       on-hand unchanged
       reserved increases
       available decreases
  -> FulfillmentTask created in QUEUED state
  -> business event and audit record
  -> durable operational dispatch work item
  -> dashboard and later realtime observation surfaces
```

The order is accepted only after the complete requested order can be validated and reserved. A failed order does not create a partial order, partial line set, fulfillment task, or successful operational event.

## 3. Isolated fixtures

The test provisions two synthetic tenants:

- `L2-P2-TENANT-A`
- `L2-P2-TENANT-B`

Tenant A has `L2-P2-WH-A` and `L2-P2-WH-B`. Tenant B has `L2-P2-WH-B-ONLY`. Product and inventory records are created through supported application APIs, with the same operator-facing SKU also present in the separate tenant to test tenant scoping.

Tenant A includes active webhook and CSV connectors, a disabled CSV connector for the disabled-connector failure path, and a scheduled-pull connector backed by a local bounded HTTP fixture. No real customer data or credentials are used.

## 4. Successful webhook intake

The test submits synthetic webhook order `L2-P2-HAPPY-001` to Tenant A and warehouse `L2-P2-WH-A` with two product lines:

- `L2-SKU-A`, quantity 8
- `L2-SKU-B`, quantity 6

The application returns HTTP `201 Created` and the order is `RECEIVED`. Both order lines are persisted with their catalog SKUs. Each line has reserved quantity equal to ordered quantity, with zero fulfilled, cancelled, and returned quantity.

The reservation contract is verified for both products:

- `quantityOnHand` is unchanged;
- `quantityReserved` increases by the ordered amount;
- `quantityAvailable` decreases by the ordered amount.

The resulting fulfillment task is associated with the order, warehouse `L2-P2-WH-A`, status `QUEUED`, and total units 14. Fulfillment is not advanced by this phase.

## 5. Downstream evidence

The successful webhook produces the expected downstream evidence:

- the inbound record is `ACCEPTED`;
- an `ORDER_INGESTED` business event exists;
- a successful `ORDER_PROCESSED` audit entry exists;
- an `ORDER_FLOW` operational dispatch work item is persisted and is not `FAILED`;
- dashboard totals include the accepted order and fulfillment backlog.

Dispatch is asynchronous. A work item may already be `COMPLETED` when the request assertion runs because the application listener can drain it immediately. The test therefore checks durable creation and non-failure during intake, then drains pending work and verifies the final Tenant A `ORDER_FLOW` statuses are `COMPLETED`.

This test does not claim that a hosted browser websocket received the update. Hosted realtime and rendered observation remain separate proof concerns.

## 6. CSV intake

CSV input covers a valid single-line order, a valid multi-line order, and a missing-product row. The import result is verified as a partial success:

- valid rows are imported and reserved;
- the missing-product row fails with `PRODUCT_NOT_FOUND`;
- the import run records the partial result;
- valid reservations obey the same on-hand/reserved/available invariant as webhook intake.

CSV connector and import records are retained as operational evidence rather than silently converting a failed row into an order.

## 7. Direct API parity

The direct `POST /api/orders` path is exercised with the same tenant-scoped order semantics. A valid direct request creates an order, lines, reservation, queued fulfillment task, audit/event evidence, and dispatch work item through the same service boundary. The API path does not bypass tenant or warehouse validation.

## 8. Scheduled pull

The test starts a local HTTP server with a synthetic order response, creates a scheduled-pull connector pointing to that bounded local target, and invokes the worker directly with a bounded connector count.

The worker fetches the payload, normalizes it through the connector, ingests the order through the regular inbound service, and creates the same order/reservation/queued-fulfillment chain. The connector readback records `lastPullStatus=SUCCESS`.

## 9. Duplicate and concurrent duplicate safety

Repeating an already accepted external order identifier returns a conflict and does not create another order, reservation, fulfillment task, replay record, or successful event.

Two concurrent requests using the same tenant, connector, and external order identifier are also exercised. The resulting invariant is:

- exactly one order is persisted;
- exactly one reservation is applied;
- exactly one fulfillment task exists;
- both requests remain below an unhandled server-error contract in the focused test;
- the database uniqueness constraint is the final duplicate-safety boundary.

The concurrent loser can produce an expected unique-constraint/DataIntegrityViolation diagnostic in the application log while the accepted API/test contract remains green. This is observable race-path noise, not hidden success, and is a candidate for future error-log refinement rather than a reason to weaken duplicate protection.

## 10. Failure and replay boundaries

The test verifies that these failures do not create an order or queued fulfillment task:

| Failure | Result | Replay evidence |
| --- | --- | --- |
| Missing product | `PRODUCT_NOT_FOUND` | replay record `PENDING` |
| Missing inventory row | `INVENTORY_NOT_FOUND` | replay record `PENDING` |
| Insufficient available inventory | `INSUFFICIENT_INVENTORY` | replay record `PENDING` |
| Disabled CSV connector | `CONNECTOR_DISABLED` | replay record `PENDING` |

Failed inbound evidence remains available for operator recovery. This phase checks creation of the replayable failure record and absence of false operational success; it does not execute replay.

## 11. Atomicity

An order containing one valid line and one missing product is rejected as a whole. The valid line does not reserve stock, and no order, order item set, fulfillment task, successful `ORDER_INGESTED` event, or successful `ORDER_PROCESSED` audit is left behind.

This is the required boundary between validation and persistence: a multi-line order is not partially operational.

## 12. Tenant and warehouse boundaries

The test verifies both boundaries through real authenticated sessions:

- a warehouse-scoped Tenant A operator cannot create an order for the other Tenant A warehouse;
- a Tenant A session cannot use Tenant B's warehouse and same-SKU product context;
- Tenant B order counts and inventory remain unchanged after the cross-tenant attempt.

The order service resolves products, warehouses, inventory, connectors, and order identifiers within the active tenant context. A client cannot substitute a database identifier from another tenant to bypass that boundary.

## 13. Operational state interpretation

`QUEUED` fulfillment means the order has been accepted and its work has been initialized. It does not mean picking, dispatch, shipment, or completion has happened. This phase intentionally stops at queued fulfillment and does not advance physical or external execution.

The dashboard assertion verifies that accepted orders and backlog are visible in the tenant snapshot. Durable dispatch evidence verifies the route toward operational/realtime surfaces. A browser-level hosted websocket assertion is outside this focused test and is not represented as passed here.

## 14. Production correction

The scheduled-pull worker contained a real JPA optimistic-lock seam. It saved a connector as `RUNNING`, retained the pre-save entity version, and then attempted to save that stale object as `SUCCESS` or failure. The focused scheduled-pull run reproduced the resulting stale-version failure.

The smallest correction was made in:

`backend/src/main/java/com/synapsecore/integration/IntegrationScheduledPullWorkerService.java`

`markPullAttempt` now returns the entity returned by the `RUNNING` save, and `processConnector` continues with that returned instance. The subsequent status update therefore uses the current persistence version. No API, connector contract, or product scope was changed.

## 15. Verification record

Focused Phase 2 result:

```text
Tests run: 1
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

The focused run covered webhook, CSV, direct API, scheduled pull, duplicate and concurrent duplicate safety, replayable failures, warehouse and tenant boundaries, atomic rollback, dashboard evidence, and dispatch completion.

Required final checks:

- full backend regression: `272` tests, `0` failures, `0` errors, `0` skipped, `BUILD SUCCESS`;
- documentation link check: passed;
- `git diff --check`: passed;
- secret scan: passed with no critical findings.

No frontend source was changed. No hosted proof or deployment was run for this phase.

## 16. Classification

Classification is based on this bounded phase and the completed command results:

- Classification A: no known correctness blocker in the covered intake/reservation lanes;
- Classification B: no known covered-path regression; full backend confirmation passed;
- Classification C: hosted browser websocket observation, hosted tenant proof, and replay execution are intentionally outside this phase;
- Classification D: future connector breadth, queue/worker separation, and scale architecture remain future evolution.

The final phase verdict is closed for the bounded scope after the full backend regression and repository checks passed. This evidence does not claim hosted or production-scale validation.
