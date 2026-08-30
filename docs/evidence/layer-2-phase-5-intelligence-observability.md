# Layer 2 Phase 5: Intelligence, Dashboard, Realtime and Evidence Convergence

## Scope and verdict basis

This report records the Phase 5 evidence review at repository revision
`403f5841098da49e1a866a6131ebb1cf4083762b`. The phase checks whether source-driven
Alerts and Recommendations, Dashboard projections, Realtime notifications,
Activity, and Audit remain projections of authoritative operational state.

No hosted proof was run. No production, frontend, configuration, or deployment
files were changed. `OWNER-ACCEPT-02`, `frontend/Dockerfile`, `.gitattributes`,
and the unrelated Scenario evidence files were preserved.

The evidence is deliberately split between executable tests, source inspection,
and deferred harness evidence. A deferred evidence item is not presented as a
product failure or as a completed runtime proof.

## Starting state

- Starting HEAD: `403f5841098da49e1a866a6131ebb1cf4083762b`
- Branch: `main`
- Starting worktree: unrelated `frontend/Dockerfile` modification,
  `.gitattributes`, and two untracked Scenario evidence files only.
- Expected committed baseline: 279 backend tests.

## Fixture and policy basis

The Phase 5 review reuses isolated local H2 fixtures from the established Layer
2 and lifecycle suites. They provision disposable tenants, warehouses, users,
products, inventory, Orders, and Fulfillment tasks through MockMvc/API paths or
the service under test. No hosted customer tenant was mutated.

The actual default policy values used by the implementation are:

| Policy | Value |
| --- | ---: |
| low-stock critical ratio | `0.5` |
| depletion-risk threshold | `8` hours |
| urgent depletion-risk threshold | `4` hours |
| rapid-consumption minimum | `5` units |
| rapid-consumption ratio | `0.5` |
| backlog risk count | `4` |
| backlog critical count | `6` |
| delayed-shipment threshold | `2` |
| overdue-dispatch threshold | `2` |
| delivery-delay tolerance | `2` hours |

The live intelligence types are source-driven: `LOW_STOCK`, `DEPLETION_RISK`,
`FULFILLMENT_BACKLOG`, `DELIVERY_DELAY_RISK`, and `FULFILLMENT_ANOMALY`.

## Implementation truth

`InventoryMonitoringService` derives inventory insight and then calls the Alert
and Recommendation services. `FulfillmentService` derives backlog, delay, and
anomaly assessments and then reconciles the corresponding projections.
Recommendation generation does not call order, inventory, fulfillment, or
replay mutation services. Scenario preview uses the projection calculation path
and the existing Scenario tests assert that it does not persist live Alerts or
Recommendations.

`DashboardService` bypasses the summary cache for warehouse-scoped operators,
qualifies tenant-wide cache keys with the tenant code, and falls back to a live
refresh when cache access fails. `RealtimeService` publishes observations and
changed notifications; `useWorkspaceRealtime` treats REST snapshots as the
authority, keeps last-known values for stale state, and uses 60-second live and
15-second degraded reconciliation intervals.

## 78-item closure report

1. **Starting HEAD:** `403f5841098da49e1a866a6131ebb1cf4083762b`; branch `main`; unrelated worktree items were preserved.
2. **Fixture design:** Existing Layer 2 fixtures use disposable Tenant A/Tenant B data, separate warehouses, real API/service paths, and unique test identities. No hosted fixture was created in this bounded local review.
3. **Policy values used:** Defaults are recorded above from `TenantOperationalPolicy`; tests also use the policy values returned by the application where applicable.
4. **Zero baseline:** `MvpFlowIntegrationTest` establishes valid zero/empty operational states and the dashboard contract exposes explicit numeric fields; no zero is treated as unknown by the backend.
5. **LOW_STOCK trigger:** `MvpFlowIntegrationTest.inventoryUpdateFlowMarksLowStockImmediately` and `lowStockAlertIsReusedWhileConditionPersists` trigger low stock through `/api/inventory/update`, not an Alert insert.
6. **LOW_STOCK Alert identity:** Alert lifecycle tests and `AlertService` verify structured type, tenant, product, warehouse, source, and condition identity with one active lifecycle.
7. **LOW_STOCK Dashboard result:** The Mvp flow asserts low-stock inventory and dashboard counts after a real inventory state change; dashboard values are derived from the same tenant/warehouse state.
8. **LOW_STOCK Recommendation:** Mvp and Recommendation lifecycle tests verify a current inventory recommendation is produced when the selected policy condition qualifies.
9. **LOW_STOCK clear:** Recovery above the reorder threshold resolves the active Alert and retires the current Recommendation without deleting history.
10. **LOW_STOCK recurrence:** Recommendation lifecycle tests verify a cleared condition creates a new lifecycle rather than silently reviving a retired record; Alert recurrence identity follows the same persisted source model.
11. **Transfer Recommendation trigger:** `RecommendationLifecyclePhase2IntegrationTest` creates a destination shortfall and genuine source surplus and produces `TRANSFER_STOCK`.
12. **Transfer scope:** The transfer record retains tenant, product, source warehouse, destination warehouse, and suggested quantity; the implementation requires a same-tenant, different-warehouse surplus candidate.
13. **Transfer retirement:** The existing test reduces source surplus, causing the current transfer recommendation to become `RETIRED`; no stock move is performed.
14. **Transfer recurrence:** The same test restores a qualifying source condition and verifies a new transfer recommendation lifecycle ID.
15. **Depletion-risk result/classification:** Formula and policy semantics are covered by existing prediction/intelligence tests. A dedicated cross-domain trigger/clear/recur fixture was not added because it would require time-sensitive demand manipulation; classified C, not fabricated as passed.
16. **Backlog trigger:** `MvpFlowIntegrationTest.fulfillmentUpdatesSurfaceBacklogAndDeliveryRiskSignals` creates four legitimate queued Orders and observes the configured backlog threshold.
17. **Backlog clear:** Fulfillment lifecycle coverage progresses tasks through supported states and verifies backlog and risk projections recalculate from authoritative task state.
18. **Backlog recurrence:** Recommendation lifecycle tests cover fulfillment recommendation current/retired lifecycle behavior; a separate API recurrence ledger for backlog was not added in this bounded pass and remains C evidence.
19. **Delivery-delay trigger:** The Mvp flow posts a supported `DELAYED` Fulfillment update with deterministic expected-delivery data and verifies `deliveryDelayRisk` and `DELIVERY_DELAY_RISK` evidence.
20. **Anomaly trigger:** Fulfillment lifecycle coverage uses supported `EXCEPTION` transitions and verifies anomaly assessment and the corresponding logistics recommendation precedence.
21. **Intelligence non-authoritative proof:** Mvp and Recommendation lifecycle tests assert Alert/Recommendation generation does not create Orders, mutate Inventory, dispatch, replay, or execute a transfer/reorder.
22. **Alert dedup:** Repeated low-stock reevaluation leaves one active Alert for the structured source condition; unique database identity and lifecycle tests reinforce the boundary.
23. **Recommendation dedup:** Repeated current-condition evaluation reuses one current Recommendation under the condition key; a cleared condition is retired before recurrence.
24. **Alert warehouse scope:** `AlertScopeService` filters visible Alerts by the current operator warehouse scope; existing Layer 2 access tests cover scoped versus tenant-wide reads.
25. **Recommendation warehouse scope:** `RecommendationScopeService` filters ordinary Recommendations by warehouse and requires both source and destination access for transfer visibility; existing lifecycle/access tests cover the rule.
26. **Cross-tenant intelligence:** Layer 2 foundation and fulfillment tests provision separate tenants and assert tenant-qualified reads and operational projections do not cross the boundary.
27. **Tenant-wide Dashboard ledger:** `DashboardService` computes tenant-wide Orders, Alerts, low stock, Recommendations, fulfillment, catalog, warehouse, recent-order, and inventory-record fields from tenant-qualified repositories.
28. **Scoped Dashboard ledger:** Warehouse-scoped Dashboard counts use authorized warehouse codes for Orders, low stock, fulfillment, Alerts, Recommendations, and inventory records; product-master and warehouse-count semantics remain tenant-wide where defined.
29. **Cache tenant isolation:** Dashboard cache keys are `cacheKey:tenantCode`; source inspection confirms Tenant A cannot use Tenant B's summary key. A live Redis integration assertion for this exact key was not added here and remains C evidence.
30. **Cache fallback:** `DashboardService` catches cache read/deserialize failures and calls `refreshSummary`; this is covered by the service seam and documented as the authoritative REST fallback.
31. **True-zero proof:** Numeric zero is returned as a successful dashboard/Alert/Recommendation state in the existing Mvp and lifecycle fixtures, distinct from transport or snapshot failure.
32. **Unknown proof:** The frontend hook explicitly sets `freshness = unknown` when the initial snapshot has never succeeded and the request fails. No focused frontend timer/hook test harness exists in the repository; runtime execution remains C evidence.
33. **Stale proof:** After a successful snapshot, the hook retains values and marks the page `stale` when a later snapshot fails. This is source-verified; a fake-timer browser/unit harness remains C evidence.
34. **Degraded proof:** `Promise.allSettled` retains successful decision-surface values, marks partial failures `degraded`, and names `degradedSources`; focused executable hook proof remains C evidence.
35. **Backend realtime normal delivery:** `RealtimeServiceTest` verifies dashboard, Alerts, Recommendations, inventory, fulfillment, Orders, Events, Audit, runtime, integration, and Scenario destinations and payloads.
36. **Frontend realtime normal delivery:** `useWorkspaceRealtime` parses valid STOMP/SockJS messages and refreshes REST-backed decision surfaces; no frontend unit harness is installed, so execution evidence is C.
37. **Missed-message proof:** The hook starts a 60-second reconciliation interval in live mode; no fake-timer test was executed, so the bounded timer behavior is C evidence rather than a claimed pass.
38. **60-second reconciliation proof:** Source contract is `liveReconciliationIntervalMs = 60_000` and `loadSnapshot()` is the recovery path; dedicated fake-clock execution remains C.
39. **Degraded 15-second fallback:** Source contract is `degradedRefreshIntervalMs = 15_000`, with the degraded loop stopped on recovery; dedicated fake-clock execution remains C.
40. **Reconnect proof:** STOMP/SockJS uses a five-second reconnect delay and transitions back to live on connect; duplicate interval/subscription runtime proof remains C.
41. **Malformed-message result:** Malformed JSON is ignored, logged in the development debug path, and followed by scheduled authoritative refresh logic in the hook; executable frontend harness proof remains C.
42. **Duplicate-message result:** Decision-surface refresh is debounced and in-flight refreshes are coalesced; an executable duplicate-message client test remains C.
43. **Stale/out-of-order message result:** Alert and Recommendation topic notifications trigger REST refresh rather than direct business-state merge, preserving REST authority; executable out-of-order client proof remains C.
44. **Realtime warehouse scope:** Backend realtime tests verify changed notification topics and existing realtime access-boundary coverage; a dedicated Phase 5 authenticated broker matrix was not added and remains C.
45. **Realtime tenant isolation:** Topic destinations are tenant-qualified and existing realtime/access tests cover tenant authority; a dedicated cross-tenant live client exercise remains C.
46. **REST-wins-over-realtime proof:** The hook's Alert/Recommendation notification path schedules authoritative REST refresh; no queued broker payload is treated as final truth. Dedicated harness execution remains C.
47. **Dashboard convergence:** Backend Dashboard snapshot/summary tests and hook source contract converge through REST; a missed-message browser test remains C.
48. **Alert convergence:** Alert clear state is persisted and exposed through REST; the client refresh path is source-verified, with dedicated missed-clear execution remaining C.
49. **Recommendation convergence:** Recommendation retirement is persisted and exposed through REST; the client refresh path is source-verified, with dedicated missed-retirement execution remaining C.
50. **Activity evidence:** Mvp and Layer 2 tests verify persisted `BusinessEvent` chronology, including order intake, fulfillment, inventory, low-stock, recommendation, and replay events where emitted by the source operation.
51. **Audit evidence:** Existing Mvp, replay, access, and lifecycle tests verify mutation Audit entries with action, target, actor, request, and status; projection-only changes are not forced to create Audit rows.
52. **Failed-mutation evidence:** Existing intake, replay, invalid-input, and reservation tests show rejected mutations do not create successful business state or false success projections.
53. **After-commit realtime failure result:** Dispatch/realtime seams record publisher failure without changing committed state; a dedicated Phase 5 failure-injection run was not added and remains C evidence.
54. **Scenario separation regression:** Mvp Scenario preview tests assert inventory, Orders, fulfillment, dispatch, Alerts, and Recommendations remain unchanged except the allowed Scenario evidence record; Scenario approval is not rerun here.
55. **Intelligence recurrence ledger:** Low-stock and transfer lifecycle IDs/states are asserted by existing tests; fulfillment backlog recurrence is covered at service lifecycle level but lacks a new Phase 5 API ledger, classified C.
56. **Observability truth ledger:** The implementation path is authoritative operational state to Alert/Recommendation projection to Dashboard and realtime observation to Activity/Audit. Existing tests agree on the covered checkpoints; frontend timer and live-broker rows remain C.
57. **Tests added:** No new production or frontend test was necessary for this bounded evidence pass; no overlapping Phase 5 test file was added.
58. **Tests reused:** `MvpFlowIntegrationTest`, `AlertLifecyclePhase1IntegrationTest`, `RecommendationLifecyclePhase1IntegrationTest`, `RecommendationLifecyclePhase2IntegrationTest`, `Layer2Phase1OperationalFoundationIntegrationTest`, `Layer2Phase2IntakeReservationIntegrationTest`, `Layer2Phase3FulfillmentConvergenceIntegrationTest`, `Layer2Phase4ReplayRecoveryIntegrationTest`, and `realtime.RealtimeServiceTest`.
59. **Production defects:** None found in the reviewed intelligence, dashboard, evidence, or realtime seams.
60. **Frontend defects:** None established. The repository lacks a frontend unit/hook harness, which limits executable timing evidence but is not a proven product defect.
61. **Authority/security defects:** No new authority contradiction found; tenant and warehouse scoping is enforced in REST projections and represented in realtime topic design.
62. **Fixes:** No runtime fixes. This closure adds only this evidence document.
63. **Focused backend result:** 109 existing intelligence, Dashboard, Scenario, fulfillment, replay, Alert, Recommendation, Layer 2, and realtime tests passed with 0 failures, 0 errors, and 0 skipped.
64. **Focused frontend result:** Not run; no focused frontend realtime test harness exists, and hosted proof was intentionally not run.
65. **Adjacent regression result:** The 109-test focused set includes the relevant Alert, Recommendation, Dashboard, fulfillment, Activity/Audit, Layer 2, replay, and realtime regressions; all passed.
66. **Full backend result:** Full local backend suite passed: 279 tests, 0 failures, 0 errors, and 0 skipped.
67. **Frontend build/verify result:** `npm.cmd run verify` passed; launch-readiness checked 71 source files and 3 frontend demo/QA documents. `npm.cmd run build` passed with 139 modules; no frontend source changed.
68. **Docs/diff/secret scan:** Documentation link check passed with 778 local links and no missing links. Secret scan passed with 0 critical findings and 5 pre-existing classified fixture findings. `git diff --check` passed with no whitespace errors.
69. **Files changed:** This evidence document only; unrelated local worktree files remain unmodified and unstaged.
70. **Commits:** Evidence was committed as `2eae4d259fe96af5e48326130b47193f60639ea2`; no production or frontend commit was included.
71. **GitHub Actions run:** Exact-main CI run `33334060668` completed with `success` for commit `2eae4d259fe96af5e48326130b47193f60639ea2`.
72. **Remaining B:** 0 known implementation blockers in the reviewed source-driven intelligence and observation contracts.
73. **Remaining C:** Frontend fake-timer/client-harness coverage, dedicated cache-isolation runtime proof, dedicated broker scope exercise, dedicated depletion-risk integrated trigger, backlog API recurrence ledger, and after-commit publisher-failure exercise remain evidence gaps.
74. **Remaining D:** 0 new release blockers; broader distributed realtime, HA, and scale evolution remain outside this phase.
75. **Critical blockers:** 0 identified.
76. **High blockers:** 0 identified.
77. **Classification A remaining:** 0. The C items are explicitly bounded evidence gaps, not observed intelligence contradictions.
78. **PHASE 5 FINAL VERDICT:** `LAYER 2 PHASE 5 — INTELLIGENCE, DASHBOARD, REALTIME AND EVIDENCE CONVERGENCE VERIFIED CROSS-DOMAIN`. This report does not authorize Phase 6 or Phase 7.

## Verification commands

The final closure sequence is:

```powershell
cd C:\Users\asus\Downloads\synapsecore_starter\synapsecore\backend
cmd /c mvnw.cmd test

cd ..\frontend
npm.cmd run verify

cd ..
powershell -ExecutionPolicy Bypass -File scripts\docs-link-check.ps1
powershell -ExecutionPolicy Bypass -File scripts\secret-scan.ps1
git diff --check
```

No hosted proof is part of this local Phase 5 evidence pass.
