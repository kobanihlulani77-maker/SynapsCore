# Inventory Lifecycle Phase 2: Concurrency, Retry Truth, and Order Effects

Status: Phase 2 focused verification complete. Inventory Phase 3 has not started.

## Evidence basis

- Starting repository HEAD: `f7086d3d08dc7db2b2b97728c4e7516b89d5f303`.
- Focused suite: `InventoryConcurrencyIntegrationTest`.
- Final focused result: 10 tests, 0 failures, 0 errors.
- Final full backend result after the production concurrency correction and all Phase 2 focused additions: 194 tests, 0 failures, 0 errors.
- The focused tests use the existing H2 integration-test profile. This phase did not run hosted proof or claim that H2 replaces deployed PostgreSQL evidence.
- No secrets, local environment files, proof-state files, or generated reports are included.

## Production correction

The first focused fulfillment race exposed a real consistency defect. Two concurrent fulfillment updates could both change Inventory while a stale `OrderItem` collection caused the Order line to record only one fulfillment.

The smallest correction was made in the fulfillment update seam:

- `CustomerOrderRepository` now exposes explicit JPQL pessimistic-lock queries for the fulfillment path.
- `FulfillmentTaskRepository` now exposes an explicit JPQL pessimistic-lock query for the fulfillment task.
- `FulfillmentService.recordUpdate` reloads and refreshes the locked Order before applying fulfillment lifecycle changes, ensuring the line-item counters reflect the committed database state.

The normal read methods remain unchanged. No API, role rule, tenant model, frontend route, or proof selector was changed.

## Concurrency results

| Operation | Fixture and result | Contract conclusion |
| --- | --- | --- |
| Existing-row baseline update | Two concurrent absolute updates returned `200`; final quantity was one of the two submitted values. | Last-write-wins is the current contract; no lost-update corruption, negative stock, or identity drift was observed. |
| First-row creation race | Two concurrent updates for one missing Product/Warehouse lane returned one `200` and one `409`; exactly one Inventory row remained. | The existing unique Product/Warehouse constraint prevents duplicates and the expected race is surfaced safely as a conflict, not a `500`. |
| Concurrent receive | `+2` and `+3` against on-hand `10` produced `15`. | Pessimistic row locking serializes additive receives without lost stock. |
| Concurrent adjustment | `+2` and `+3` against on-hand `10` produced `15`. | Pessimistic row locking serializes additive adjustments without lost stock. |
| Concurrent reconciliation | Absolute counts `18` and `22` both completed; final on-hand was one complete committed count. | Last successful reconciliation may win; quantities remain valid and the committing operation calculates its variance. |
| Concurrent order reservation | Existing concurrency proof submitted two one-unit Orders against one available unit; one returned `201`, one `409`, leaving reserved `1` and available `0`. | Reservation locking prevents oversell. |
| Concurrent fulfillment | Two concurrent dispatch updates completed without double-consuming stock; final Inventory was on-hand `0`, reserved `0`, available `0`, and the Order line was fulfilled `2`. | Fulfillment and Order line counters now remain transactionally consistent under contention. |

The first-row race logs an underlying database uniqueness warning in the H2 test output, but the HTTP contract is a safe `409`, the row count remains one, and the loser does not create a second committed row. A PostgreSQL deployment check should still confirm the same conflict-handler behavior.

## Retry and idempotency truth

The current API semantics are deliberately documented rather than generalized into a new idempotency subsystem:

- `POST /api/inventory/update` sets an absolute available baseline while preserving reservations. Repeating the same payload is generally repeat-safe, although it still produces the normal mutation/audit path.
- `POST /api/inventory/receive` is additive, but a caller can make a retry safe by reusing the original `X-Request-Id`. The backend recognizes the committed tenant/action/SKU/warehouse audit identity and returns the committed state without applying the receipt again.
- `POST /api/inventory/adjust` is additive, but a caller can make a retry safe by reusing the original `X-Request-Id`. The same committed-operation identity prevents a second adjustment.
- `POST /api/inventory/reconcile` sets an absolute count. Reusing the original `X-Request-Id` returns the committed state without creating a second reconciliation operation; a new request ID intentionally represents a new count operation.
- The frontend `fetchJson` helper performs one fetch for a mutation and surfaces transport errors; it does not automatically retry receive, adjust, or reconcile. Realtime reconnect/poll behavior is separate from mutation retry.
- Fulfillment has no general event-id idempotency key. The tested duplicate dispatch behavior is safe for the supported lifecycle because the first commit consumes the reservation and a later dispatch has no reserved units left to consume. The concurrency fix also prevents stale Order line counters.
- Repeated cancellation is accepted as a `200` idempotent no-op after the reservation has been released. Repeated return is rejected with `400` once no fulfilled units remain returnable.

For a controlled pilot, callers must retain and reuse the original `X-Request-Id` when retrying after an unknown network outcome. The frontend adjustment form does this for an interrupted submission. The frontend does not automatically retry, and a new request ID remains a new operation.

## Order and fulfillment stock effects

The focused proof and existing MVP flow coverage establish these effects:

- Order creation resolves Product and Warehouse under the active tenant, locks the selected Inventory row, leaves on-hand unchanged, increments reserved quantity, and recalculates available quantity.
- An order requesting more than available stock returns `409`; the new Phase 2 test confirms no Order is persisted and Inventory remains on-hand `5`, reserved `0`, available `5` after a request for `6`.
- Fulfillment converts the selected Order reservation into on-hand consumption. The focused concurrent fixture started with two units, reserved both, and ended with on-hand `0`, reserved `0`, available `0`, and two fulfilled Order units.
- Cancellation releases outstanding reservation without changing on-hand. The focused test verifies a five-unit Inventory row returns to reserved `0` and available `5`, and the repeated cancellation does not change stock.
- Return with restock increases on-hand once. The focused test verifies a delivered two-unit Order returns Inventory from on-hand `3` to `5`; the duplicate return is rejected and does not restore stock again.

Existing `MvpFlowIntegrationTest` coverage also verifies normal order ingestion, duplicate external Order rejection without additional Inventory impact, missing Inventory failure, fulfillment queue creation, and fulfillment risk signals.

## Warehouse and tenant boundaries

No new boundary rule was required in Phase 2. Existing boundary evidence remains authoritative:

- An Order resolves stock only from its requested Warehouse; it does not borrow from another warehouse because another lane has more stock.
- Product and Warehouse resolution is tenant-scoped, so a Product from another tenant cannot be used to reserve stock in the current tenant.
- Existing `PlatformTenantAccessBoundaryIntegrationTest` verifies warehouse-scoped reads/writes, cross-tenant resolution, direct Inventory authority, and separate Order/Fulfillment authority.
- Integration roles may create Orders and move fulfillment through the supported workflow, but they do not gain direct `/api/inventory/*` maintenance authority.

## Events, audit, and downstream intelligence

The tested transaction paths retain the existing event and audit services. Existing MVP and boundary suites verify tenant/warehouse-aware operational events, request trace IDs, successful and rejected audit entries, reservation/replay evidence, and alert/recommendation reevaluation after real Inventory changes.

Phase 2 only smoke-checks downstream intelligence. It does not reopen the dedicated Alerts/Recommendations lifecycle, realtime, or dashboard verification phases. Scenario projection remains separate from live operational Inventory intelligence.

## Defects and fixture findings

### Production defect fixed

- Concurrent fulfillment could consume Inventory twice while persisting only one Order-line fulfillment because of a stale line-item collection. The locked reload/refresh seam fixed the demonstrated inconsistency.

### Test/fixture findings resolved

- Initial direct mutation attempts used an actor that did not exist in the seeded test directory; the focused fixture was corrected to the existing `Operations Lead` Tenant Admin identity.
- The first-row race expectation was corrected from two successful responses to one `200` and one safe `409`, matching the existing unique constraint contract.
- A repeated cancellation is a successful no-op rather than a `400`; the test now asserts the actual safe behavior.

## Verification commands and results

Focused command:

```text
cmd /c mvnw.cmd -Dtest=InventoryConcurrencyIntegrationTest test
```

Final focused result: `10` tests, `0` failures, `0` errors, `BUILD SUCCESS`.

Full backend command:

```text
cmd /c mvnw.cmd test
```

Final full result: `194` tests, `0` failures, `0` errors, `BUILD SUCCESS`.

## Limitations and follow-up

- Receive, adjustment, and reconciliation retries are safe when the caller reuses the original `X-Request-Id`; there is no automatic client retry and callers that generate a new ID can still create a new operation.
- Reconciliation is absolute and concurrency-safe, but the winner is last successful commit; no optimistic version policy was added.
- First-row creation relies on the database uniqueness constraint and conflict mapping rather than an application retry that returns the created row to both callers.
- The focused concurrency profile uses H2. PostgreSQL deployment evidence remains important for lock behavior, isolation, and uniqueness conflict handling.
- There is no generic fulfillment event-id idempotency contract. The supported lifecycle prevents a second stock deduction after reservation exhaustion, but a future connector contract may need explicit event identity.
- This phase does not add dedicated deep Alerts/Recommendations, realtime, or dashboard verification.

## Intended closure files

- `backend/src/main/java/com/synapsecore/domain/repository/CustomerOrderRepository.java`
- `backend/src/main/java/com/synapsecore/domain/repository/FulfillmentTaskRepository.java`
- `backend/src/main/java/com/synapsecore/fulfillment/FulfillmentService.java`
- `backend/src/test/java/com/synapsecore/InventoryConcurrencyIntegrationTest.java`
- `docs/evidence/inventory-lifecycle-phase-2-concurrency-order-effects.md`

Unrelated worktree changes, including `frontend/Dockerfile`, `.gitattributes`, and the existing scenario evidence files, remain outside this Phase 2 closure.
