# Fulfillment / Dispatch Phase 1: Core State Truth

Status: verified locally and ready for deployment review

Scope: fulfillment task state, dispatch quantities, delivery catch-up, exception
recovery, retry identity, active-work visibility, and tenant/warehouse authority.
Phase 2 load, latency, and broader dispatch workflow work is intentionally not
started here.

## Evidence baseline

- Starting revision: `9775590cab57857a493a6c35a3921164812e5460`
- Focused test: `FulfillmentLifecyclePhase1IntegrationTest`
- Focused result: 6 tests, 0 failures, 0 errors
- Full backend result after production changes: 215 tests, 0 failures, 0 errors
- Test profile uses isolated H2 databases and Flyway migrations; this is not a
  claim of hosted deployment proof.
- No frontend changes were made and hosted proof was not rerun in this phase.

## Authority and consistency

Fulfillment mutation is exposed through the fulfillment controller and is
guarded by operational-write access for the order's tenant and warehouse.
`INTEGRATION_ADMIN` and `INTEGRATION_OPERATOR` can update a task only within
their assigned warehouse scope. Read access remains workspace-controlled.
Tenant, order, fulfillment task, and warehouse relationships are checked by
the service before mutation; the focused and existing boundary suites cover
cross-tenant and cross-warehouse rejection.

## State contract

The persisted fulfillment task remains one task per customer order. The
supported states are:

`QUEUED -> PICKING -> PACKED -> DISPATCHED -> DELIVERED`

`DELAYED` is a recoverable operational branch. It may remain delayed, return
to active preparation, dispatch, or enter exception handling. `EXCEPTION` is
terminal for the task. Forward catch-up is allowed because the current API
accepts external status snapshots, including a direct `QUEUED -> DISPATCHED`
or whole-order `QUEUED -> DELIVERED` update. Backward transitions and reopening
`DELIVERED` or `EXCEPTION` are rejected.

Direct delivery is therefore an intentional whole-order external snapshot
contract, not a separate shipment entity. It marks every order line delivered
and commits all remaining reservations. Shipment identifiers and independent
per-package dispatch state remain outside this phase.

## Retry identity

The request trace `X-Request-Id` is the fulfillment mutation identity. A
successful mutation with the same request ID and the same canonical payload is
returned repeat-safely without consuming inventory or creating another
fulfillment audit mutation. Reusing that request ID with a different
consequential payload returns HTTP 409.

The canonical fingerprint includes the order, status, quantity, SKU, carrier,
tracking reference, promised/expected timestamps, occurrence time, and note.
Older successful audit records without a fingerprint are treated conservatively
as non-replayable for a new payload rather than being silently re-applied.

## Reservation and terminal truth

- Positive dispatch quantities are valid only for `DISPATCHED` updates.
- Zero and negative quantities are rejected.
- Dispatch overage beyond remaining aggregate reservation returns HTTP 409 and
  leaves the order and inventory unchanged.
- SKU-directed dispatch uses the requested line reservation; aggregate dispatch
  cannot consume more than the remaining order reservation.
- An `EXCEPTION` update releases all outstanding reservations after preserving
  any units already fulfilled, then moves the order to `FAILED`.
- Cancelled, failed, returned, and delivered orders are excluded from the
  active fulfillment overview, while their persisted task history remains
  available to historical/audit paths.
- Repeated terminal updates are safe and cannot reopen or consume inventory.

## Focused proof coverage

The focused integration test verifies:

1. backward transitions from delivered, packed, and exception states are
   rejected;
2. same-request same-payload retry is safe;
3. same-request different-payload reuse conflicts;
4. zero-unit exception releases a full reservation;
5. partial dispatch followed by exception releases only the outstanding
   reservation;
6. cancelled and failed orders are not active fulfillment work;
7. zero, negative, non-dispatch quantities, and aggregate overage are rejected;
8. direct multi-line delivery completes every line;
9. repeated direct delivery, with the same or a new request ID, does not
   consume inventory twice.

Existing backend integration coverage additionally exercises tenant/order/
warehouse consistency, cancellation and return behavior, multi-line SKU
fulfillment, concurrent inventory operations, connector/replay inputs, and
security boundaries.

## Failure classification

### Resolved production defects

- Missing explicit fulfillment transition validation.
- Same request ID could previously be reused with a different payload without
  a conflict.
- Fulfillment exceptions could leave outstanding inventory reservations held.
- Terminal orders could remain visible as active fulfillment work.
- Aggregate dispatch quantities could be silently clamped instead of rejected.

### Test or fixture defects found during this phase

- The initial focused direct-delivery fixture expected five units on hand after
  consuming five-unit lines that had only five units seeded. The fixture was
  corrected to seed ten units; no production behavior was changed for that
  failure.
- Maven first failed before compilation because sandbox network access to
  Maven Central was denied. The same test completed successfully with approved
  network access.

### Remaining scope

- Full PostgreSQL concurrency/load behavior, dispatch latency, and contention
  characterization belong to Fulfillment Phase 2.
- The current one-task-per-order model does not represent separate shipments,
  packages, carriers, or partial delivery promises.
- Timestamp ordering and external event sequencing are not expanded here.
- Realtime, alerts, recommendations, and audit emission are verified as
  downstream effects by existing coverage, but a dedicated Phase 2 event-load
  exercise is not claimed.

## Acceptance

Critical blockers: 0

High blockers: 0

Phase 1 status: **FULFILLMENT / DISPATCH PHASE 1 VERIFIED AND CLOSED LOCALLY**

Deployment and hosted confirmation remain separate operational steps. Do not
start Fulfillment Phase 2 from this document.
