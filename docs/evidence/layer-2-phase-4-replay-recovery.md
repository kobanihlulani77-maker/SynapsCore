# Layer 2 Phase 4: Replay Recovery Evidence

## Scope

This evidence closes the Layer 2 cross-domain failure path: source intake can
fail without creating business state, a supported prerequisite can be repaired,
and the retained inbound record can be replayed into the normal order,
reservation, and queued fulfillment flow exactly once. The test uses real
tenant provisioning, authentication, catalog, inventory, connector, intake,
replay, dashboard, event, audit, and dispatch services. It does not edit
database rows directly.

The starting repository revision was:

```text
47c2f5bacfe9cbae1c7f1b4652b39d3185ddcd8a
```

The Phase 4 implementation adds a focused test only. No production source,
frontend source, configuration, or deployment behavior changed.

## Focused Fixture

`Layer2Phase4ReplayRecoveryIntegrationTest` provisions a unique disposable
Tenant A with two warehouses and a separately provisioned Tenant B. Tenant A
has tenant-wide administration, an integration administrator and operator
scoped to Warehouse A, and another integration operator scoped to Warehouse B.
Tenant B has its own operator and warehouse. Each test uses a unique suffix, so
the tenant baseline is clean even when the Spring test context contains earlier
test data.

The fixture creates supported products and inventory through the product and
inventory APIs, and creates enabled and disabled CSV connectors through the
connector API. Recovery setup is therefore representative of the supported
operational path rather than a repository shortcut.

## New Focused Proof

| Test | Evidence |
| --- | --- |
| `missingProductRepairReplaysIntoExactlyOneOrderReservationAndQueuedFulfillment` | Missing product returns `PRODUCT_NOT_FOUND`, inbound becomes `REPLAY_QUEUED`, replay becomes `PENDING`, no order is created, tenant admin repairs product and inventory through APIs, manual replay produces exactly one order, one reservation, one queued fulfillment task, linked inbound/replay state, dashboard visibility, business event, audit evidence, and terminal dispatch. A second replay is rejected. |
| `inventoryAndDisabledConnectorFailuresRecoverWithoutAutomatedOrDuplicateSideEffects` | `INVENTORY_NOT_FOUND` and `INSUFFICIENT_INVENTORY` remain non-operational until inventory repair. `CONNECTOR_DISABLED` remains pending and is skipped by automation; after enablement it is still recovered manually and produces one order. |
| `automatedRecoveryAndNegativeBoundariesRemainScopedAndNonOperational` | An eligible repaired replay succeeds through automation, the `RequestTraceContext` is clear after completion, invalid quantity is non-replayable with no order, wrong-warehouse replay is denied without state mutation, and another tenant cannot replay the record. |

## Existing Deep Evidence Reused

The focused class is intentionally not a replacement for the established
domain tests. The following existing tests provide the deeper cases required by
the Phase 4 closure rule:

- `MvpFlowIntegrationTest.duplicateFailedIntegrationDeliverySharesOneActiveReplayRecord`
- `MvpFlowIntegrationTest.replayReconcilesExistingBusinessOrderWithoutCreatingDuplicate`
- `MvpFlowIntegrationTest.replayRecordDeadLettersAfterConfiguredFailures`
- `MvpFlowIntegrationTest.deadLetteredRecoverableOrderCanBeRequeuedAfterPrerequisiteRepair`
- `MvpFlowIntegrationTest.automatedReplaySkipsDisabledConnectorRecordsAndKeepsManualRecoveryVisible`
- `MvpFlowIntegrationTest.disabledConnectorReplayRemainsManualAfterEnableAndDoesNotDoubleProcess`
- `MvpFlowIntegrationTest.replayDoesNotRewriteHistoricalImportRunOutcome`
- `Layer2Phase2IntakeReservationIntegrationTest.sourceIntakeCreatesOneReservedQueuedOrderAndFailuresRemainNonOperational`
- `InventoryConcurrencyIntegrationTest.concurrentDuplicateExternalOrderIdCreatesOneOrderAndReturnsOneConflict`
- `InventoryConcurrencyIntegrationTest.concurrentReservationsDoNotOversellSingleAvailableUnit`
- `PlatformTenantAccessBoundaryIntegrationTest` replay, tenant, warehouse, and integration authority cases
- `SecurityVerificationIntegrationTest` malformed input, connector token, and authority denial cases

Together, the focused and reused evidence covers source identity deduplication,
existing-order reconciliation, backoff and exhaustion, dead-letter retention and
requeue, invalid source/quantity/price/warehouse/token boundaries, cross-tenant
and wrong-warehouse denial, retained payload metadata privacy, concurrency,
reservation safety, fulfillment initialization, dashboard agreement, activity
and audit behavior, and bounded operational dispatch.

## Recovery Ledger

### 1. Missing Product

- Source: supported CSV intake through the enabled CSV connector.
- Initial result: one failed inbound with `PRODUCT_NOT_FOUND`; no Order,
  reservation, or Fulfillment task.
- Replay: one `PENDING` replay linked to the inbound record; retained payload
  is server-owned and the queue response is metadata-only.
- Repair: Tenant Admin creates the product and its inventory through APIs.
- Recovery: scoped Integration Operator manually replays the retained request.
- Final result: one `REPLAYED` record, one `REPLAYED` inbound, one Order in
  `RECEIVED`, one reserved inventory quantity, and one `QUEUED` Fulfillment
  task. A second replay is rejected and does not change counts.

### 2. Missing Inventory

- Source: enabled CSV connector with a catalog product but no inventory row.
- Initial result: `INVENTORY_NOT_FOUND`; no business order or fulfillment.
- Repair: Tenant Admin creates inventory through the inventory API.
- Recovery: manual replay succeeds exactly once and reserves the submitted
  quantity.

### 3. Insufficient Inventory

- Source: enabled CSV connector with quantity greater than available stock.
- Initial result: `INSUFFICIENT_INVENTORY`; no partial reservation or order.
- Repair: Tenant Admin increases available quantity through the inventory API.
- Recovery: manual replay succeeds exactly once through normal OrderService
  reservation and fulfillment initialization.

### 4. Disabled Connector

- Source: disabled CSV connector.
- Initial result: `CONNECTOR_DISABLED`; inbound and replay remain visible as
  pending recovery work, with no business order.
- Automation: automated replay processes zero records and does not turn the
  disabled source into live traffic.
- Repair: Integration Admin enables the connector.
- Recovery: the disabled-connector replay remains manual-only; an Integration
  Operator performs the replay and produces one order without duplication.

### 5. Automation Context

Eligible repaired work is processed with the `system-replay` actor by the
service. The focused test verifies that request id, actor, and tenant context
are cleared after the batch returns. Existing event/audit assertions verify
that automation uses system replay semantics and does not borrow a prior human
request context.

### 6. Negative and Boundary Paths

Invalid quantity, invalid price, missing or unknown warehouse, invalid connector
token, duplicate source identity, dead-letter equivalent re-ingestion, wrong
warehouse replay, Tenant Admin replay attempts, and cross-tenant replay are
covered by the reused integration, security, concurrency, and access-boundary
tests. These paths remain rejected or terminal and do not create live business
state.

## Persistence and Observability Invariants

For a successful recovery, the following counts must remain one for the
external source identity: active replay identity, Order, Order line, inventory
reservation, and queued Fulfillment task. Inbound status changes from
`REPLAY_QUEUED` to `REPLAYED`; Replay changes from `PENDING` to `REPLAYED` and
records the resulting external order id. Import history remains the historical
source outcome and is not rewritten by replay.

Failure and recovery are visible in the integration queue, Dashboard snapshot,
Business Events, Audit Logs, and operational dispatch records. The platform
does not claim operational success while the source prerequisite is missing.
The retained payload is used by the server-side replay path and is never
accepted from a client replay request.

## Classification

- Classification A: 0 remaining. No production contradiction was found.
- Classification B: 0 unresolved implementation blockers in the Phase 4
  failure/recovery contract.
- Classification C: 0 unresolved Phase 4 cross-domain evidence blockers after
  focused and reused local integration evidence. A hosted proof rerun is not a
  substitute for these backend tests and is outside this bounded local gate.
- Classification D: 0 release blockers. Distributed workers, HA, and larger
  scale behavior remain future evolution, not claims of this phase.

## 73-Item Closure Report

1. **Starting HEAD:** `47c2f5bacfe9cbae1c7f1b4652b39d3185ddcd8a`, the Phase 3 closure revision.
2. **Fixture design:** disposable Tenant A with two warehouses plus separate Tenant B, unique per test, provisioned through APIs.
3. **Clean baseline:** each fixture tenant starts without Phase 4 orders, products, inventory, connectors, inbound records, or replay records.
4. **Missing Product failure:** CSV intake returns `PRODUCT_NOT_FOUND`, no order or reservation.
5. **Missing Product repair:** Tenant Admin creates product and inventory through APIs.
6. **Missing Product replay result:** manual replay reaches `REPLAYED` and creates the normal order flow.
7. **Missing Product reservation ledger:** submitted quantity is reserved once; available stock decreases once.
8. **Repeated replay result:** resolved replay is rejected; no second order or reservation.
9. **Missing Inventory failure:** CSV intake returns `INVENTORY_NOT_FOUND`, no business state.
10. **Missing Inventory repair/recovery:** API inventory repair enables one successful manual replay.
11. **Insufficient Inventory failure:** CSV intake returns `INSUFFICIENT_INVENTORY`, no partial reservation.
12. **Insufficient Inventory repair/recovery:** API stock repair enables one successful replay.
13. **Disabled connector failure:** CSV intake returns `CONNECTOR_DISABLED` and queues recoverable evidence.
14. **Automation-skip result:** automated batch processes zero disabled-connector records.
15. **Connector repair/manual recovery:** enablement does not auto-execute the old disabled replay; manual replay succeeds.
16. **Automated replay success:** repaired eligible replay creates one order through `system-replay`.
17. **Automation trace-context result:** request, actor, and tenant ThreadLocal context is clear after the batch.
18. **Unrepaired replay failure:** existing deep tests return the typed source failure with no business mutation.
19. **Backoff result:** existing deep replay tests verify retry eligibility and failure state before the next attempt.
20. **Dead-letter exhaustion:** existing deep test reaches `DEAD_LETTERED` at configured attempt exhaustion.
21. **Dead-letter evidence:** dead-letter timestamp, failure code, message, and queue visibility are retained.
22. **Dead-letter requeue result:** repaired dead-lettered replay is explicitly requeued and then succeeds.
23. **History retention:** prior attempts and dead-letter history remain retained after recovery.
24. **Invalid quantity result:** `INVALID_QUANTITY` is non-replayable and creates no order.
25. **Invalid price result:** existing integration validation rejects non-positive price without replay.
26. **Missing warehouse result:** missing warehouse input is rejected before order creation.
27. **Warehouse-not-found result:** unknown warehouse is rejected without business effects.
28. **Invalid token result:** connector token mismatch is denied before intake creates live business state.
29. **Duplicate source result:** duplicate external source identity is terminal/conflict behavior.
30. **Existing-Order reconciliation:** existing Order is recognized and replay does not create a duplicate.
31. **Duplicate active replay identity:** equivalent failed deliveries share one active replay record.
32. **Dead-letter equivalent re-ingestion:** equivalent source delivery does not bypass terminal dead-letter policy.
33. **Wrong-warehouse replay:** an operator scoped to another warehouse is denied and replay remains pending.
34. **Tenant Admin replay boundary:** tenant administration does not substitute for integration-operator replay authority.
35. **Cross-tenant replay:** Tenant B cannot address or replay Tenant A’s record.
36. **Replay queue scope:** queue reads are tenant and warehouse scoped.
37. **Response privacy:** queue DTOs expose metadata, not retained request payloads or secrets.
38. **Retained-payload integrity:** replay deserializes the server-retained request; client replay bodies cannot override it.
39. **Inbound/replay/order linkage:** inbound `replayRecordId`, replayed order id, and final Order identity agree.
40. **Reservation exactly-once ledger:** one successful replay applies one reservation transaction.
41. **Queued fulfillment exactly-once result:** one recovered Order initializes one `QUEUED` Fulfillment task.
42. **Dashboard failure state:** pending recovery remains represented in Dashboard integration state.
43. **Dashboard recovered state:** resolved replay is removed from pending queue and Order state is visible.
44. **Activity evidence:** existing lifecycle tests verify operational event visibility for accepted/replayed work.
45. **Audit evidence:** failure, queue, replay, and completion audit entries retain target/request linkage.
46. **False-success matrix:** prerequisites missing means failure/pending; only successful replay produces Order state.
47. **Transaction consistency:** OrderService reservation and fulfillment initialization are transactional; failures do not leak partial business state.
48. **Concurrent replay result:** existing concurrency coverage prevents duplicate external-order effects.
49. **Automated/manual race result:** row locking and existing manual/automation tests bound one active replay identity and one terminal outcome.
50. **Dispatch result:** relevant operational updates reach bounded terminal `COMPLETED` state through the dispatch queue.
51. **Control Product:** fixture products are created and read through supported product APIs; control data is not mutated.
52. **Control warehouse:** fixture warehouse definitions remain unchanged during recovery.
53. **Control tenant:** fixture tenant identity and isolation remain unchanged during recovery.
54. **Tests added:** `Layer2Phase4ReplayRecoveryIntegrationTest` with 3 focused tests.
55. **Tests reused:** MvpFlow, Layer2 Phase 2, InventoryConcurrency, SecurityVerification, and PlatformTenantAccessBoundary suites.
56. **Production defects:** none found; expected production changes were zero.
57. **Authority/security defects:** none found in the Phase 4 boundary.
58. **Fixes:** one test-only lazy-loading assertion correction; no production fix.
59. **Focused result:** 3 tests, 0 failures, 0 errors, 0 skipped.
60. **Adjacent regression result:** 143 tests, 0 failures, 0 errors, 0 skipped across MvpFlow, Layer 2 Phase 2, InventoryConcurrency, SecurityVerification, and PlatformTenantAccessBoundary.
61. **Full backend result:** 279 tests, 0 failures, 0 errors, 0 skipped. This is the Phase 3 baseline of 276 plus the three Phase 4 focused tests.
62. **Frontend result:** no frontend changes; frontend verification is not applicable to this backend-only phase.
63. **Docs/diff/secret scan:** docs-link check clean with 778 local links checked; `git diff --check` clean; secret scan PASS with 0 critical findings and 5 pre-existing fixture findings.
64. **Files changed:** the focused Phase 4 test and this evidence document only, excluding preserved unrelated worktree files.
65. **Commits:** `1dffaaff49f5e7fcd22574ee75e9247537809ac2` (`Verify Layer 2 Phase 4 replay recovery`), followed by this evidence-only closure update.
66. **GitHub Actions run ID/status:** run `33332237585` completed with `success` for exact-main SHA `1dffaaff49f5e7fcd22574ee75e9247537809ac2`.
67. **Remaining B:** 0 unresolved Phase 4 implementation blockers.
68. **Remaining C:** 0 unresolved Phase 4 evidence blockers.
69. **Remaining D:** 0 release blockers; future scale evolution remains outside this phase.
70. **Critical blockers:** 0.
71. **High blockers:** 0.
72. **Classification A remaining:** 0.
73. **PHASE 4 FINAL VERDICT:** `LAYER 2 PHASE 4 — FAILURE, REPLAY AND EXACTLY-ONCE RECOVERY VERIFIED CROSS-DOMAIN`.

## Verification Gate

Phase 4 closes only after the focused test, adjacent replay/integration/order/
inventory regressions, full backend suite, docs link check, diff check, secret
scan, push, and exact-main GitHub Actions success are recorded here. No hosted
proof or Phase 5 work is part of this bounded backend closure.
