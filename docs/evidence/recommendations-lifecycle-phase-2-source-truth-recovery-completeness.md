# Recommendations Lifecycle Phase 2

## Closure Scope

Phase 2 verifies that Recommendations remain derived advisory guidance rather
than authoritative operational truth. Authoritative inventory, fulfillment,
orders, integrations, and scenario projections retain their own boundaries.
Recommendation evaluation may calculate a decision from those sources, but it
does not execute a transfer, create an order, mutate inventory, or turn a
Scenario projection into a live alert or recommendation.

## 63-Item Closure Report

1. The authoritative source for inventory recommendations is persisted Inventory
   plus its Product and Warehouse relationships.
2. The authoritative source for fulfillment recommendations is persisted active
   FulfillmentTask state plus its CustomerOrder and Warehouse relationships.
3. Recommendation rows identify their tenant and primary warehouse.
4. Inventory recommendations identify their product.
5. Transfer recommendations identify both source and destination warehouses.
6. Fulfillment recommendations are warehouse-level advisory conditions.
7. `CURRENT` means the source condition still justifies the advice.
8. `RETIRED` means the source condition no longer justifies the advice.
9. Recommendation age is not treated as source truth.
10. A condition key, not display text, controls current-row identity.
11. Inventory identity is `INVENTORY|product-id|warehouse-id`.
12. Fulfillment identity is `FULFILLMENT|warehouse-id`.
13. Inventory pressure without a transfer plan produces `REORDER_STOCK`.
14. Critical low inventory without a transfer plan produces `REORDER_URGENTLY`.
15. Low destination inventory with safe network surplus produces `TRANSFER_STOCK`.
16. Fulfillment backlog produces `PRIORITIZE_FULFILLMENT`.
17. Delivery-delay pressure produces `ESCALATE_LOGISTICS`.
18. Fulfillment anomaly pressure produces `INVESTIGATE_LOGISTICS_ANOMALY`.
19. Fulfillment anomaly has precedence over delivery-delay guidance.
20. Delivery-delay guidance has precedence over ordinary backlog guidance.
21. The Phase 2 integration test exercises all six recommendation types.
22. Re-evaluating a persistent inventory condition refreshes the same row.
23. Re-evaluating a persistent fulfillment condition refreshes the same row.
24. Priority and type changes do not create duplicate current conditions.
25. Clearing an inventory condition retires its current recommendation.
26. Clearing a fulfillment condition retires its current recommendation.
27. A later recurrence creates a new current lifecycle row.
28. Transfer advice carries explicit source and destination identity.
29. Transfer advice carries a suggested quantity derived from threshold shortfall.
30. Transfer calculation does not create a CustomerOrder.
31. Transfer calculation does not change source inventory.
32. Transfer calculation does not change destination inventory.
33. Transfer calculation does not execute fulfillment or integration work.
34. A changed transfer source is reevaluated against the destination's current
   shortfall and network surplus.
35. A transfer is retired when safe source surplus no longer exists.
36. A still-valid transfer is refreshed in place rather than duplicated.
37. Replenishment and source changes therefore converge transfer advice toward
   committed inventory state.
38. Inventory monitoring calculates prediction and insight before advisory
   persistence.
39. Inventory monitoring catches a recommendation evaluation runtime failure.
40. Inventory monitoring continues to AlertService with a null recommendation
   when advisory evaluation fails.
41. The direct failure-isolation test verifies this continuation without using
   Mockito or changing the build dependency set.
42. Fulfillment evaluation uses the same bounded null-advisory fallback.
43. Fulfillment alert evaluation therefore remains a separate source-derived
   path when recommendation persistence is unavailable.
44. Advisory failure is logged with tenant, warehouse, source, and operational
   identity context without logging secrets or payloads.
45. A scheduled reconciliation service re-evaluates committed inventory rows.
46. The scheduled reconciliation service re-evaluates active fulfillment tasks.
47. Reconciliation retires current fulfillment advice when no active task remains
   for its warehouse.
48. Reconciliation catches errors per inventory or task so one item does not stop
   the remaining pass.
49. Reconciliation defaults to enabled with a 60-second initial delay.
50. Reconciliation defaults to a 60-second fixed delay.
51. The inventory reconciliation query eagerly loads product and tenant ownership
   required by the detached scheduled evaluation path.
52. The active fulfillment query eagerly loads tenant, order, and warehouse
   ownership required by reconciliation.
53. The test profile disables scheduling, so focused tests invoke reconciliation
   deterministically rather than waiting for a timer.
54. The reconciliation integration test creates advice from low committed stock.
55. The same test raises committed stock and verifies the advice becomes retired.
56. Normal source writes and recommendation writes remain in the existing source
   transaction; ordinary rollback preserves atomic source behavior.
57. Reconciliation reads committed source state and does not manufacture source
   records.
58. Scenario preview remains projection-only and does not call live recommendation
   persistence.
59. Scenario projected warnings and recommendations remain hypothetical and do
   not mutate live alert or recommendation state.
60. Alerts and Recommendations remain separate lifecycles; alert action text is
   advisory text, not an executable recommendation command.
61. REST, dashboard snapshots, and realtime feeds continue to consume the current
   scoped recommendation set established in Phase 1.
62. Same-process condition locks, pessimistic current-row locking, and the
   database current-condition invariant remain in place for race protection.
63. Full PostgreSQL fault injection, multi-node concurrency, and a fresh hosted
   owner walkthrough remain evidence gaps rather than being represented as
   passed in this local phase.

## Source Truth and Failure Boundary

The production seam is intentionally small. `InventoryMonitoringService` and
`FulfillmentService` treat recommendation persistence as advisory evaluation:
they log a runtime failure and continue to the corresponding alert evaluation
with no Recommendation object. `RecommendationReconciliationService` provides
bounded source-driven convergence on the next pass for records that could not
be created, refreshed, or retired.

This does not claim that a database outage can be hidden from the transaction
manager. A persistence failure that marks the surrounding source transaction
rollback-only remains an infrastructure/transaction failure and must be
visible. The new contract covers ordinary evaluation exceptions and provides a
committed-state repair path; PostgreSQL fault injection is still required to
prove every driver-specific failure mode.

## Test Evidence

Focused command:

```powershell
cd backend
cmd /c "mvnw.cmd -q -Dtest=RecommendationLifecyclePhase2IntegrationTest,InventoryMonitoringServiceTest test"
```

Result: **4 tests, 0 failures, 0 errors, 0 skipped**.

Covered directly:

- all six recommendation types;
- anomaly > delivery delay > backlog precedence;
- current-row update, retirement, and recurrence;
- transfer non-execution and source-driven transfer retirement;
- committed inventory reconciliation;
- inventory recommendation failure isolation.

Existing Phase 1 coverage continues to cover scoped REST/dashboard/realtime
currentness, tenant and warehouse identity, same-process locking, and raw-topic
protection. Existing Scenario lifecycle coverage continues to cover projection
separation from live operational alerts and Recommendations.

## Classification

| Area | Classification | Result |
| --- | --- | --- |
| Six recommendation type mapping | A | Passed in focused integration coverage. |
| Inventory source truth | A | Derived from persisted inventory and reconciled. |
| Fulfillment source truth | A | Derived from active fulfillment state and reconciled. |
| Precedence | A | Anomaly, delay, then backlog precedence is tested. |
| Current-row refresh | A | Persistent conditions update in place. |
| Retirement and recurrence | A | Clear retires; recurrence creates a new lifecycle. |
| Transfer advisory boundary | A | No order, fulfillment, or inventory side effect. |
| Transfer source refresh | A | Validity and quantity refresh or retirement are implemented. |
| Advisory runtime failure isolation | A | Inventory path directly tested; fulfillment path implemented symmetrically. |
| Committed-state reconciliation | A | Deterministic local integration test passed. |
| Scenario projection boundary | B | Existing projection-only tests preserved. |
| Alert lifecycle separation | B | Existing alert lifecycle remains independent. |
| REST/dashboard/realtime currentness | B | Established by Phase 1; no contract change in Phase 2. |
| Normal transaction rollback | B | Existing transaction model retained; source and advice are atomic on normal rollback. |
| PostgreSQL driver fault injection | C | Not run in this local H2 phase. |
| Multi-node recommendation race proof | C | Database invariant and locks exist; distributed execution proof remains later work. |
| Hosted owner/live recommendation walkthrough | C | Not rerun because this phase did not require hosted proof. |
| Recommendation history/search | D | Outside the current advisory-feed scope. |
| Recommendation execution controls | D | Intentionally unsupported; Recommendations remain advisory. |

Classification A remaining: **0**.

## Phase 2 Verdict

**RECOMMENDATIONS LIFECYCLE SOURCE-TRUTH, FAILURE-RECOVERY, AND COMPLETENESS
VERIFIED LOCALLY**

Critical blockers: **0**

High blockers: **0**

Phase 3 readiness: **Ready after repository-wide verification and commit**.

The C classifications are explicit evidence boundaries, not passed claims.
