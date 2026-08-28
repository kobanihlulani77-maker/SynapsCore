# Scenario Lifecycle Phase 2: PREVIEW Projection and Intelligence

**Status:** Implementation and automated evidence complete; owner verification remains pending
**Evidence date:** 2026-08-28
**Repository HEAD at evidence capture:** `7c99152373bf70322cdebef30d9d8f82ad705dd6`
**Scope:** PREVIEW projection, comparison intelligence, risk classification, recommendations, alerts, side-effect boundaries, and negative paths

## 1. Phase Boundary

This phase answers whether a Scenario `PREVIEW` accurately describes the
operational consequences of a proposed order without becoming a live order.
It covers validation, tenant and warehouse scope, projected inventory, value,
stock prediction, intelligence, recommendations, alerts, persisted planning
evidence, comparison scoring, insufficient inventory, and direct PREVIEW
execute/approve attacks.

It does not approve, reject, escalate, or execute a saved plan, perform CSV
acceptance, or begin Phase 3.

## 2. Current Processing Path

```text
POST /api/scenarios/order-impact
  -> ScenarioController
  -> AccessControlService warehouse check
  -> ScenarioAnalysisService
  -> ScenarioProjectionService
  -> tenant-scoped warehouse/product/inventory reads
  -> transient projected Inventory values
  -> StockPredictionService
  -> InventoryIntelligenceService
  -> RecommendationService preview
  -> AlertService preview
  -> ScenarioHistoryService.recordPreview
  -> SCENARIO_ANALYZED business event
  -> frontend response
```

The projection resolves the warehouse and product inside the current tenant,
then checks the returned relationships through `TenantScopeGuard`. It creates
transient inventory objects for calculation. A successful PREVIEW intentionally
persists only planning history and a business event; it does not persist the
projected inventory, alert, or recommendation.

## 3. Projection Formulas

Duplicate request lines for the same inventory record are accumulated first.

```text
projectedRequestedUnits = sum(request quantities for the inventory record)
projectedQuantityAvailable = current available - projected requested units
projectedQuantityReserved = current reserved + projected requested units
projectedQuantityOnHand = current quantity on hand
projectedQuantityInbound = current inbound quantity
projectedOrderValue = sum(unit price * requested quantity)
totalUnits = sum(request quantities)
```

The request is rejected before history persistence when requested units exceed
available units. Request validation requires a positive integer quantity and a
positive decimal unit price with no more than two fractional digits.

## 4. Intelligence Rules

`StockPredictionService` reads recent consumption for the same product and
warehouse over the last hour:

```text
unitsPerHour = recent units in the one-hour window
hoursToStockout = available / unitsPerHour when unitsPerHour > 0
depletionRisk = hoursToStockout <= depletionRiskHoursThreshold
urgentRisk = hoursToStockout <= urgentDepletionHoursThreshold
rapidConsumption = recentUnits >= max(rapidMinimum, round(threshold * rapidRatio))
```

With no recent consumption, units per hour is zero and hours to stockout is
null. That is a stable result, not an error.

The current default policy is:

| Policy | Default |
|---|---:|
| Low-stock critical ratio | `0.5` |
| Depletion-risk horizon | `8` hours |
| Urgent-depletion horizon | `4` hours |
| Rapid-consumption minimum | `5` units/hour |
| Rapid-consumption ratio | `0.5` |
| High score threshold | `40` |
| Critical score threshold | `100` |
| Low-stock severity | `HIGH` |
| Critical low-stock severity | `CRITICAL` |

```text
lowStock = projected available <= reorder threshold
criticalQuantity = available == 0 OR available <= max(1, round(threshold * 0.5))
elevatedUrgency = (lowStock AND criticalQuantity) OR urgentRisk
depletionRisk = NOT lowStock AND depletionRiskPrediction
```

Risk level is `critical` for low stock with elevated urgency, `high` for low
stock or depletion risk, and `stable` otherwise.

Risk score is:

```text
(critical items * 100)
+ (high-risk items * 25)
+ (low-stock items * 20)
+ (projected alerts * 10)
+ (projected recommendations * 5)
```

Priority is `CRITICAL` for critical exposure or the critical threshold,
`HIGH` for low/high exposure or the high threshold, and `MEDIUM` otherwise.

Alerts are non-persistent projections: `LOW_STOCK` for low stock and
`DEPLETION_RISK` for non-low-stock depletion risk. Recommendations are also
non-persistent. Low stock first seeks tenant-scoped surplus in another
warehouse; if it can cover the shortfall the type is `TRANSFER_STOCK`, else
the policy selects `REORDER_STOCK` or `REORDER_URGENTLY`.

## 5. Deterministic Fixture

The Phase 2 tests use the `STARTER-OPS` H2 test tenant:

| SKU | Warehouse | Available | Threshold | Unit price |
|---|---|---:|---:|---:|
| `SKU-FLX-100` | `WH-NORTH` | `28` | `20` | `95.00` |

Coast Flux is available `22` with threshold `14`, so its tenant-scoped
transferable surplus is `8`. The boundary cases do not depend on seeded recent
orders.

## 6. Healthy Preview

Test: `scenarioPhaseTwoHealthyPreviewPersistsEvidenceWithoutOperationalSideEffects`.

Input is North Flux quantity `2` at `95.00`. Observed values match:

| Result | Value |
|---|---:|
| Projected order value | `190.00` |
| Total units | `2` |
| Projected available | `26` |
| Low stock / risk level | `false` / `stable` |
| Projected alerts / recommendations | `0` / `0` |
| Review priority / score | `MEDIUM` / `0` |

The persisted run is tenant `STARTER-OPS`, warehouse `WH-NORTH`, approval
status/stage `NOT_REQUIRED`, with no requester, reviewer, or final owner.

## 7. Low-Stock Preview

Input is North Flux quantity `9`:

```text
projected available = 28 - 9 = 19
```

Observed: `lowStock=true`, risk `high`, one `LOW_STOCK` alert with `HIGH`
severity, and one `TRANSFER_STOCK` recommendation moving `1` unit from Coast.
The persisted score is `60` and priority is `HIGH`:

```text
25 high-risk + 20 low-stock + 10 alert + 5 recommendation = 60
```

## 8. Critical Preview

Input is North Flux quantity `18`:

```text
projected available = 28 - 18 = 10
critical boundary = max(1, round(20 * 0.5)) = 10
```

Observed: `lowStock=true`, risk `critical`, one `LOW_STOCK` alert with
`CRITICAL` severity, and `REORDER_URGENTLY` because Coast surplus `8` cannot
cover the `10`-unit shortfall. The score is `135` and priority is `CRITICAL`:

```text
100 critical + 20 low-stock + 10 alert + 5 recommendation = 135
```

## 9. Multi-Line and Comparison

Existing MvpFlow coverage projects North Flux quantity `9` plus Orb quantity
`3`: value `1215.00`, total units `12`, Flux available `19` and low stock, Orb
available `29` and not low stock. No live order is created.

Existing comparison coverage compares Conservative Flux quantity `2` with
Aggressive quantity `9`, recommends Conservative because its score is lower,
and stores only comparison planning evidence. Comparison does not execute an
order.

## 10. Invalid and Insufficient Inputs

Test: `scenarioPhaseTwoRejectsInsufficientInventoryWithoutPlanningEvidence`.
It requests one unit more than current availability and observes `400` with an
insufficient-inventory message, unchanged inventory, and no new Scenario run.

Existing access-boundary coverage also verifies missing warehouse (`400`),
missing product (`4xx`), zero quantity (`400`), and anonymous preview (`403`).

## 11. Side-Effect Boundary

The healthy-preview test captures counts before and after the request. Customer
orders, inventory quantity, fulfillment tasks, dispatch work items, persisted
alerts, and persisted recommendations remain unchanged. Exactly one PREVIEW
history row and one `SCENARIO_ANALYZED` business event are added. That is the
precise meaning of no live side effects: planning memory and evidence are
allowed, operational execution state is not mutated.

## 12. Direct PREVIEW Attacks

Existing test: `scenarioPreviewCannotBeExecutedDirectlyIntoLiveOrderFlow`.
After creating a PREVIEW, it posts to `/api/scenarios/{id}/execute` with Review
Owner authority. The backend returns `400` containing `approved saved plans`;
orders and inventory are unchanged, history reports `executable=false`, and no
`SCENARIO_EXECUTED` event exists.

Phase 2 adds the approval attack:

```text
POST /api/scenarios/{previewId}/approve
```

The backend returns `400` containing `Only saved plans require approval`. The
PREVIEW remains non-governed and no approval event is created. Approval is not a
backdoor into PREVIEW execution.

## 13. Warehouse and Tenant Isolation

`PlatformTenantAccessBoundaryIntegrationTest.scenarioCreationRejectsAnonymousInvalidAndWrongWarehouseRequests`
proves that a North-scoped operator can preview North, receives `403` for Coast,
and receives `403` when comparing North to Coast. The selected warehouses are
checked before projection.

`SecurityVerificationIntegrationTest.tenantScopedApisAndReplayEndpointsDoNotLeakAcrossTenants`
proves that cross-tenant products, inventory, orders, alerts, recommendations,
replay, connectors, dashboard, runtime, users, events, audit, and Scenario
history are not exposed. A Scenario request for the other tenant returns `404`.

The projection service itself uses tenant-scoped warehouse/product queries and
relationship guards; it does not rely on frontend filtering.

## 14. Frontend and Runtime Alignment

The Scenario Planner consumes warehouse, projected inventory, projected alerts,
projected recommendations, value, and units as planning output. Requested By is
session-derived and read-only; reviewer selection applies to saved governed
plans, not PREVIEW. Backend validation, scope, governance, and execution
eligibility remain authoritative.

PREVIEW records `SCENARIO_ANALYZED` planning evidence and history. It does not
claim to create an operational alert or order realtime update. The existing
dashboard/realtime system remains a separate operational-event concern. No new
frontend or realtime behavior was introduced in this phase.

## 15. Verification Results

Focused backend run after correcting the expectation:

```text
MvpFlowIntegrationTest#scenarioPhaseTwoHealthyPreviewPersistsEvidenceWithoutOperationalSideEffects
MvpFlowIntegrationTest#scenarioPhaseTwoRiskBoundariesMatchPolicyAndProjectedRecommendations
MvpFlowIntegrationTest#scenarioPhaseTwoRejectsInsufficientInventoryWithoutPlanningEvidence
MvpFlowIntegrationTest#scenarioPreviewCannotBeExecutedDirectlyIntoLiveOrderFlow
Tests run: 1 per focused invocation; all four passed
Failures: 0
Errors: 0
Skipped: 0
```

The focused Phase 2 checks also include
`scenarioPhaseTwoRejectsInsufficientInventoryWithoutPlanningEvidence`; the
final four-test focused run passed. The full backend suite then passed with 160
tests run, 0 failures, 0 errors, and 0 skipped tests, including the Phase 2
additions. Required final checks also include frontend lint/build/verify, diff
check, secret scan, and documentation link check.

Frontend verification was rerun and passed:

- `npm.cmd run lint`
- `npm.cmd run build`
- `npm.cmd run verify`

The frontend launch-readiness check inspected 72 source files and verified the
proof-critical labels and frontend documentation references. No frontend
runtime source changed in Phase 2.

No hosted proof is required for test-only changes. Existing hosted proof is
prior deployed evidence, not new Phase 2 live evidence.

## 16. Defects and Fixes

The first Phase 2 run found one test expectation defect: the initial expected
high-risk score omitted the `high-risk items * 25` term. The running code
correctly returned `60`; the expectation was corrected. This was not a product
defect.

Phase 2 changes modify only integration-test coverage and this evidence record.
They do not modify production controllers, services, entities, repositories,
API contracts, frontend behavior, authorization rules, or migrations.

## 17. Owner Verification Checklist

Owner verification should use a fresh synthetic tenant and must not record
passwords, tokens, cookies, or raw payloads. Confirm in the deployed revision:

- healthy PREVIEW shows value, units, inventory, risk, alerts, and recommendations;
- low-stock PREVIEW shows the expected warehouse-aware recommendation;
- critical PREVIEW shows critical state and urgent reorder when transfer surplus is insufficient;
- insufficient inventory is rejected without history or operational mutation;
- multi-line and comparison projections match item-level calculations;
- wrong-warehouse and cross-tenant requests are denied;
- direct PREVIEW execute and approve are denied;
- history reports PREVIEW as non-executable;
- no order, inventory, fulfillment, dispatch, approval, or execution mutation occurs;
- the Scenario UI does not present PREVIEW as a governed executable plan.

## 18. Blockers and Verdict

**Critical blockers:** 0 identified in repository-backed Phase 2 evidence.
**High blockers:** 0 identified in repository-backed Phase 2 evidence.
**Medium follow-up:** owner verification in the deployed environment remains
pending; comparison history intentionally stores summary planning evidence rather
than an executable request payload; realtime behavior for planning events remains
a separate operational verification concern.
**Production behavior changed:** No.
**Deployment required:** No, because only tests and evidence documentation changed.
**Phase 3 started:** No.

**Verdict:** `PHASE 2 READY FOR OWNER VERIFICATION`

Phase 3 must not begin until the owner confirms deployed projection,
intelligence, isolation, and PREVIEW non-execution behavior.
