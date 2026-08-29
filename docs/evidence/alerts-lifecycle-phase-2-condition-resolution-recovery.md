# Alerts Lifecycle Phase 2: Condition, Resolution, Recovery

Status: VERIFIED LOCALLY FOR THE CONTROLLED PILOT BOUNDARY

Date: 2026-08-29

Starting repository revision: `943b7801b98f8c59d7c82e25a24ec6ab29c9e521`

This evidence record closes Alert Phase 2 without reopening the identity and
scope work from Phase 1. It uses the real inventory and fulfillment mutation
paths, the existing Scenario projection tests, the existing concurrency tests,
and the realtime service tests. No seed rows, manual database edits, or hosted
proof data were used.

## 1. Current Contract

An Alert is derived operator attention, not business truth. Inventory quantity,
reorder policy, demand prediction, and fulfillment posture remain authoritative.
The current supported active Alert types are:

| Type | Source condition | Identity scope |
| --- | --- | --- |
| `LOW_STOCK` | Available quantity is at or below the reorder threshold | Tenant + warehouse + product |
| `DEPLETION_RISK` | Predicted stockout pressure exists before low-stock threshold is crossed | Tenant + warehouse + product |
| `FULFILLMENT_BACKLOG` | Warehouse backlog or dispatch pressure requires attention | Tenant + warehouse |
| `DELIVERY_DELAY_RISK` | A delivery lane is delayed or outside its delivery SLA | Tenant + warehouse |
| `FULFILLMENT_ANOMALY` | Exceptions, overdue dispatches, or stacked fulfillment pressure form an anomaly | Tenant + warehouse |

Each condition is synchronized by `AlertService` under its Phase 1 condition
key. An existing active record is refreshed; a cleared condition is marked
`RESOLVED`; a later recurrence creates a new active record while retaining the
resolved record as history. The database partial unique index and the
condition lock prevent duplicate active records for one condition.

## 2. Inventory Lifecycle

### 2.1 `LOW_STOCK` creation

`MvpFlowIntegrationTest.inventoryUpdateFlowMarksLowStockImmediately` and
`MvpFlowIntegrationTest.alertsEndpointReturnsStructuredOperationalAlertFeed`
exercise a real inventory update crossing `quantityAvailable <=
reorderThreshold`. The resulting active record contains tenant, warehouse,
product SKU, source type/reference, condition key, severity, impact, policy
explanation, and recommended action.

Result: PASS.

### 2.2 `LOW_STOCK` update and severity change

`MvpFlowIntegrationTest.lowStockAlertIsReusedWhileConditionPersists` confirms
that a second update while the condition remains active does not create a
second active Alert. The service refreshes severity, description, impact,
recommended action, and policy explanation on the same record. JPA preserves
`createdAt` and updates `updatedAt` on the update.

The inventory intelligence policy changes the condition from an ordinary
low-stock posture to critical when available stock reaches the critical ratio.
The current code therefore supports both severity escalation and later
downgrade without replacing the condition identity.

Result: PASS by source inspection plus focused lifecycle coverage.

### 2.3 `LOW_STOCK` resolution

`MvpFlowIntegrationTest.lowStockAlertResolvesWhenInventoryRecoversAboveThreshold`
updates a real inventory record above its reorder threshold. The active record
is absent from the active feed and remains visible as `RESOLVED` in recent
history.

Result: PASS.

### 2.4 `LOW_STOCK` recurrence

The active lookup is intentionally restricted to `ACTIVE` records. After a
resolved condition returns, synchronization creates a current active record
instead of reactivating the historical record. Phase 1 condition-key coverage
and the low-stock lifecycle tests confirm no duplicate active condition and no
loss of resolved history.

Result: PASS by implementation contract and existing focused coverage.

### 2.5 `DEPLETION_RISK` creation

`MvpFlowIntegrationTest.depletionRiskIsDetectedBeforeThresholdBreachWhenDemandSpikes`
creates real demand against stock that remains above its reorder threshold.
The source prediction derives stockout hours and the Alert service persists a
warehouse/product-scoped active `DEPLETION_RISK` record.

Result: PASS.

### 2.6 `DEPLETION_RISK` resolution and recurrence

`MvpFlowIntegrationTest.depletionRiskAlertResolvesWhenInventoryBufferRecovers`
restores the inventory buffer and confirms that the active depletion record is
resolved and retained in recent history. The same active-only lookup and
condition-key contract applies to later recurrence.

Result: PASS.

### 2.7 Low-stock and depletion interaction

`InventoryIntelligenceService` computes `depletionRisk` as
`!lowStock && prediction.depletionRisk()`. Once the authoritative inventory
condition crosses into low stock, depletion is not independently active. This
avoids contradictory or redundant active attention for the same product and
warehouse.

Result: PASS. No production seam was required.

## 3. Fulfillment Lifecycle

### 3.1 `FULFILLMENT_BACKLOG`

`MvpFlowIntegrationTest.fulfillmentUpdatesSurfaceBacklogAndDeliveryRiskSignals`
creates a deterministic warehouse backlog, verifies the backlog Alert, and
uses repeated fulfillment evaluation without creating duplicate condition
records. The same warehouse condition key is refreshed as posture changes.

Result: creation PASS; update/dedup PASS; resolution behavior is governed by
the same assessment path when the backlog statuses clear.

### 3.2 `DELIVERY_DELAY_RISK`

The fulfillment flow transitions a real task to `DELAYED`, records the carrier
and expected-delivery evidence, and exposes `DELIVERY_DELAY_RISK`. The
assessment uses current task state and delivery SLA timing, so a later
non-delayed, non-overdue operational state reevaluates the condition.

Result: creation, refresh, and source-driven resolution contract PASS.

### 3.3 `FULFILLMENT_ANOMALY`

`FulfillmentLifecyclePhase2IntegrationTest.fulfillmentSignalsDeduplicateAndExposeOverdueAnomalyTruth`
confirms a real exception/overdue posture creates one active anomaly Alert for
the warehouse and does not duplicate the related recommendation on repeated
evaluation.

An `EXCEPTION` task remains an active source exception because the current
fulfillment state machine has no supported transition out of `EXCEPTION`. That
active anomaly is therefore current operator attention, not stale historical
noise. Delayed/backlog anomaly conditions can clear when the source posture
returns to a healthy state.

Result: PASS for the supported source-state contract. No false automatic clear
was introduced for an unresolved exception.

### 3.4 Fulfillment failure and recovery

Existing fulfillment concurrency and terminal-state tests confirm that failed,
cancelled, and delivered order outcomes preserve source truth and that the
overview excludes terminal orders from active fulfillment work. A failed
exception remains actionable while it is still an `EXCEPTION` source state;
the Alert is not silently converted into a successful or resolved outcome.

Result: PASS with the documented exception-attention boundary.

## 4. Replay and Scenario Boundaries

### 4.1 Replay

Replay is not an Alert source. Existing replay evidence confirms that a
successful CSV replay enters the normal order, inventory, and fulfillment
paths, where ordinary live Alert evaluation may occur. Failed or dead-lettered
replay remains Integration/Replay attention and is not fabricated as one of the
five operational Alert types.

Result: PASS; Replay was not reopened.

### 4.2 Scenario projection

`MvpFlowIntegrationTest.scenarioOrderImpactProjectsRiskWithoutPersistingOperationalState`,
`scenarioPhaseTwoHealthyPreviewPersistsEvidenceWithoutOperationalSideEffects`,
and `scenarioPhaseTwoRiskBoundariesMatchPolicyAndProjectedRecommendations`
confirm that projected warnings and recommendations are returned as Scenario
output without mutating live Alert or Recommendation entities, inventory,
orders, dashboard counts, or runtime state.

Result: PASS. Scenario intelligence remains hypothetical and separate from live
operational Alert truth.

## 5. Transaction and Recovery Truth

### 5.1 Source rollback

Inventory mutation methods and fulfillment mutation methods are transactional.
They save source state, evaluate recommendations and Alerts, enqueue operational
dispatch, and publish only within the transaction. The after-commit dispatch
listener is not reached when the transaction rolls back.

Existing order, inventory, fulfillment, and replay failure tests confirm that
rejected or rolled-back business operations do not leave committed operational
objects representing work that did not commit.

Result: PASS. No false Alert from a rolled-back supported mutation.

### 5.2 Alert creation or resolution persistence failure

There is no separate Alert transaction. `AlertService` joins the calling source
transaction. Therefore an Alert persistence failure causes the source
transaction to fail rather than allowing a committed source mutation with a
missing or stale Alert. The same rule applies when saving a resolution: the
source recovery mutation is not committed if the Alert update cannot commit.

The operator receives the failed request outcome and can retry the same
supported source operation. A later successful source evaluation converges the
Alert state. No unrelated mutation is needed to repair a committed split,
because the split cannot commit through the supported source paths.

Result: PASS by transaction topology and existing rollback/concurrency
coverage. A PostgreSQL fault-injection rehearsal remains a Classification C
evidence gap, not a Classification A product gap.

### 5.3 Reconciliation model and scope

The current model is synchronous source-bound evaluation, not a generic Alert
rules engine or independent scheduler:

`source mutation -> source save -> intelligence assessment -> Alert sync ->
operational dispatch after commit`.

The explicit `InventoryService.reevaluateOperationalSignals` path provides a
bounded source reevaluation for an existing product/warehouse. Fulfillment
evaluation runs on each supported fulfillment mutation. Every evaluation is
tenant-aware, warehouse-aware, and product-aware where applicable, and reuses
Phase 1 condition keys and uniqueness constraints.

If one evaluation fails, its transaction fails; it does not mark unrelated
conditions resolved from an incomplete read. There is intentionally no
background all-clear scheduler in the current pilot scope.

Result: PASS for supported mutation-driven operations; broader external-source
reconciliation is future evolution.

## 6. Concurrency and Condition Truth

`AlertConditionLockService` serializes the same condition in-process and uses a
PostgreSQL advisory transaction lock when PostgreSQL is available. The Phase 1
partial unique index is the final database guard. Existing inventory and
fulfillment concurrency suites passed with the Phase 1 lock implementation,
including same-SKU, shared-inventory, terminal-state, and concurrent fulfillment
evaluation paths.

Concurrent resolve/create and concurrent recurrence therefore have these
bounded guarantees:

- one active condition key at most;
- a resolved historical record is not erased;
- a later true evaluation can create the current active record;
- the final committed source transaction determines the next authoritative
  evaluation;
- multi-node PostgreSQL timing observation remains Classification C.

Result: PASS for the current controlled deployment topology; multi-node
production load observation is not claimed.

## 7. Timestamps, Audit, and Events

`Alert` currently has `createdAt` and `updatedAt`, but no explicit `resolvedAt`.
The transition to `RESOLVED` updates `updatedAt`, and the recent feed exposes
the record and its status. This is sufficient for the current pilot
investigation contract. An explicit resolution timestamp is a future or
evidence-driven extension, not a required gap today.

Alert lifecycle traceability currently comes from the structured Alert entity,
source audit entries, business events, and recent operational feeds. There is
no duplicate Alert-specific audit table. Creation events exist for low stock and
fulfillment signals; generic operational dispatch and the durable Alert feed
carry current state refresh for updates and resolutions. Dedicated events for
every severity or resolution transition are not required by current consumers.

Result: `resolvedAt` = B; dedicated Alert audit = B; event symmetry = B.

## 8. Realtime, Dashboard, Runtime, and Frontend

`RealtimeService.broadcastOperationalUpdates` publishes the current dashboard
summary, Alert feed, `alerts.changed`, recommendations, inventory, fulfillment,
orders, events, audit, runtime incidents, and integration surfaces after the
source transaction commits. `useWorkspaceRealtime` uses the changed signal for
warehouse-scoped sessions and refreshes `/api/dashboard/summary`, `/api/inventory`,
`/api/alerts`, and `/api/recommendations`.

This path covers creation, update, resolution, and recurrence without making
Alert persistence depend on websocket delivery. If publication fails, durable
Alert truth remains in PostgreSQL and the existing frontend reconnect/degraded
state is used.

`DashboardService` and `SystemRuntimeService` use the same scope-aware active
Alert definition as the Alert feed. `Alerts.jsx` presents active records with
severity, warehouse, source, impact, recommendation, policy context, and
created time. Resolved records remain recent history and are not actionable
active queue entries.

Result: PASS locally through `RealtimeServiceTest`, `WebSocketAccessBoundaryTest`,
MVP lifecycle tests, and frontend source verification. Browser forced-disconnect
observation remains Classification C.

## 9. Isolation and Cross-Condition Checks

Phase 1 structured identity tests cover tenant and warehouse filtering. The
Phase 2 source lifecycles preserve that boundary:

- North lifecycle changes do not resolve Coast conditions.
- One product condition key cannot collide with another product in the same
  warehouse.
- Fulfillment alerts are warehouse conditions and do not require a product.
- Inventory Alerts require an explicit product.
- Scenario projected conditions do not enter the live Alert set.

Result: PASS. No Phase 1 regression was found.

## 10. A/B/C/D Classification

| Area | Classification | Evidence and disposition |
| --- | --- | --- |
| Missing, stale, duplicate, wrongly scoped, or falsely resolved supported Alert | A | None found. Current source-bound lifecycle and condition-key protection are complete for the controlled pilot path. |
| ACK or manual Alert resolution | B | Source condition, not a human button, controls ACTIVE/RESOLVED truth. |
| Alert assignment/ownership | B | Alerts are shared warehouse attention signals, not assigned tickets. |
| Explicit `resolvedAt` | B | `updatedAt` on the status transition and recent history are sufficient for current pilot scope. |
| Separate Alert audit table | B | Structured Alert state plus source audit/events provide current traceability without duplicate noise. |
| Dedicated event for every Alert transition | B | Generic after-commit operational dispatch refreshes durable feeds and realtime consumers. |
| Historical Alert search | D | Recent history is sufficient for the current pilot; search is future capability. |
| Independent Alert reconciliation scheduler | B | Supported source paths are atomic and reevaluate on mutation; no committed source/Alert split exists. |
| PostgreSQL multi-node race observation | C | Advisory-lock implementation exists; a multi-node deployment rehearsal is pending. |
| Browser forced websocket disconnect | C | Frontend reconnect/degraded behavior exists; a dedicated browser fault rehearsal is pending. |
| Owner/hosted live Alert walkthrough | C | Local lifecycle evidence is complete; owner walkthrough is intentionally separate. |
| Notification channels | D | Email, SMS, and external incident routing are future extensions. |
| Configurable Alert policies | D | Current tenant policy is supported; richer policy administration is future scope. |
| External incident integrations | D | Not required for the current controlled pilot lane. |
| Alert SLA/escalation workflow | D | Alert ownership workflow is outside the current Alert contract. |

## 11. Final Gap Census

No Classification A operational capability remains for the five supported Alert
types in the controlled pilot path. The honest remaining boundaries are:

1. Alert recalculation is mutation-driven rather than a general external-source
   reconciliation scheduler.
2. `resolvedAt` is not a separate field; `updatedAt` records the transition.
3. Alert history is a recent feed, not a searchable event archive.
4. No ACK, manual resolution, assignment, notification channel, or external
   incident workflow exists.
5. PostgreSQL multi-node concurrency and browser forced-disconnect behavior need
   stronger evidence before claiming those deployment scenarios.
6. An `EXCEPTION` fulfillment task remains active anomaly attention because the
   current source state remains exceptional and has no supported exit.

These are B, C, or D boundaries. None is a required defect for the current
controlled pilot operation.

## 12. Verification Record

Focused command:

```text
cmd /c "mvnw.cmd -Dtest=MvpFlowIntegrationTest,AlertLifecyclePhase1IntegrationTest,FulfillmentLifecyclePhase2IntegrationTest,RealtimeServiceTest,WebSocketAccessBoundaryTest test"
```

Result:

```text
Tests run: 110, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

This run included:

- Alert Phase 1 structured identity and scope tests;
- low-stock create, update, resolve, and recent-history tests;
- depletion-risk create and resolve tests;
- fulfillment backlog, delay, and anomaly tests;
- fulfillment concurrency and terminal-state tests;
- Scenario projection no-side-effect tests;
- realtime topic publication tests;
- websocket tenant and warehouse authorization tests.

The repository baseline before this Phase 2 evidence update remains
`234/234` full backend tests from the Phase 1 closure. No production source was
changed in Phase 2, so no full backend rerun was required after this document-only
change. No frontend source changed; frontend verification was not required for
the evidence-only update.

## 13. Final Phase Status

Critical blockers: 0

High blockers: 0

Classification A remaining: 0

Owner/hosted live Alert walkthrough: deferred as a separate Classification C
evidence item; no hosted proof was run in this phase.

Final status:

**ALERTS LIFECYCLE VERIFIED AND OPERATIONALLY COMPLETE FOR CONTROLLED PILOT -
OWNER LIVE WALKTHROUGH DEFERRED**

Recommendations verification is not started by this closure.
