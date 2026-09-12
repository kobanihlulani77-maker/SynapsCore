# Timeout Recovery - Recommendation Scheduler Routing

## Bounded Question

Why did a warm hosted request remain slow while the shared main scheduler stopped
running dispatch, Replay, and pull work, even though recommendation reconciliation
declared its own scheduler qualifier?

## Exact Live Correlation

The exact `498d9a5` deployment was live. An authenticated baseline ran from
`2026-09-12T15:23:21.9879163Z` through `2026-09-12T15:24:50.1912751Z` and every
request returned HTTP 200. Dashboard snapshot required **48,454 ms** and Runtime
required **26,553 ms**, so this was active warm-runtime latency rather than an
instance cold start.

Complete scheduler boundaries showed:

- operational dispatch completed at `15:21:09.265Z` in 5 ms with zero active
  Hikari connections, ten idle, and zero waiters;
- no dispatch, automated Replay, or scheduled pull boundary ran during the slow
  HTTP window;
- dispatch resumed at `15:25:09.888Z` and completed in 2 ms;
- Replay and pull immediately followed, completing in 98 ms and 2 ms with zero
  active Hikari connections, ten idle, and zero waiters.

Tenant audit evidence supplied the missing work identity. Recommendation run
`4fac31f3-4abb-4642-86ca-db7e732993ac` started at
`2026-09-12T15:21:10.191674147Z` and completed at
`2026-09-12T15:25:08.891581758Z`: **238,699 ms**. It successfully evaluated 155
Inventory records and 96 Fulfillment tasks. Dense Hibernate `HHH000444`
follow-on-locking warnings appeared on `SynapseScheduled-1` during that exact
interval.

Operational dispatch, automated Replay, scheduled pull, and the product-write
watchdog are ruled out for this interval by their complete boundaries or absent
activation marker. Recommendation reconciliation is the exact main-scheduler
owner.

## Root Cause

`RecommendationReconciliationService.reconcileOnSchedule()` correctly specified
`scheduler = "synapseRecommendationTaskScheduler"`. However,
`SchedulingConfig` implemented `SchedulingConfigurer` and called
`taskRegistrar.setTaskScheduler(synapseScheduledTaskScheduler())`.

Spring normally installs a qualifier-aware `TaskSchedulerRouter`; the qualifier
stored on each scheduled method is resolved by that router. Replacing the router
with one concrete scheduler forced all annotation-driven tasks onto the main
executor, so the recommendation annotation was present but ineffective. The
previous test checked only the annotation string and the dedicated pool size; it
did not execute annotation-driven work and therefore could not detect the
routing defect.

## Smallest Production Correction

The explicit registrar override is removed. The existing main scheduler is
aliased as both `taskScheduler` and `synapseScheduledTaskScheduler`, allowing
Spring's router to select it for unqualified work while preserving the existing
name used by the product contention watchdog. Qualified recommendation work can
now resolve the existing `synapseRecommendationTaskScheduler` bean.

No schedule, pool size, transaction boundary, timeout, domain algorithm, Hikari
setting, or infrastructure setting changes.

## Local Verification

The runtime Spring-context test proves an unqualified scheduled method executes
on `SynapseScheduled-*` while a qualified method executes on
`SynapseRecommendationScheduled-*`.

- focused scheduler and recommendation gate: 25 tests, 0 failures, 0 errors;
- full backend suite: 369 tests, 0 failures, 0 errors, 0 skipped;
- backend package: success;
- documentation link check: 807 links, none missing;
- `git diff --check`: clean, with line-ending notices only.

CI, exact deployed-revision confirmation, and one bounded warm baseline remain
required before hosted closure. That baseline must verify the same ownership
split in Render logs and tenant audit evidence.

`RECOMMENDATION_SCHEDULER_ROUTING_DEFECT = PROVEN, CORRECTED, AND VERIFIED LOCALLY`

Historical Hikari starvation remains a separate proven phenomenon until the
post-deploy warm proof establishes whether this routing correction removes the
observed latency and preserves pool headroom.

## Post-Deploy Runtime Result

Commit `dfc0df3e0a7f5cb35c8a7f70f05bdf8c2b997fc6` passed CI run 395 and was
confirmed as Render's last successfully deployed commit. Render logs showed
recommendation work on `SynapseRecommendationScheduled-1` while dispatch
continued independently on `SynapseScheduled-1`, proving the routing correction
is effective in production.

The bounded authenticated baseline from `2026-09-12T15:57:50.6880303Z` through
`2026-09-12T15:59:17.6603581Z` still measured 47,520 ms for Dashboard snapshot
and 25,261 ms for Runtime. During that window, Hikari generally reported one
active connection, nine idle, and zero waiters, with a brief maximum of two
active and eight idle. This is not a current pool-acquisition starvation event.

Tenant audit evidence correlated the window with recommendation run
`b1314b57-d36a-4ff1-8c01-9864f2a7db80`, which ran from
`2026-09-12T15:56:23.492608293Z` through `2026-09-12T16:00:04.892467469Z` for
221,399 ms. It evaluated 155 Inventory records and 96 active Fulfillment tasks.
Nearby recommendation-only runs completed in approximately 39-42 seconds.

Source inspection found that fulfillment Recommendations and all three
fulfillment Alert types use warehouse-level condition identities. Despite that,
scheduled reconciliation rebuilt the same warehouse assessment and rewrote the
same warehouse-level Recommendation and Alerts once for every active task. The
task list is newest-first, so the oldest task in each warehouse produced the
final persisted state after all repeated calls.

The bounded amplification correction selects one task per active warehouse,
preserving that same final-state representative while removing duplicate
warehouse-level transactions. Event-driven evaluation after a real Fulfillment
change is untouched. Reconciliation evidence now counts actual warehouse work
units rather than repeated active-task rewrites.

Local correction verification:

- direct final-state representative and Spring/JPA work-unit gate: 11 tests,
  0 failures, 0 errors, 0 skipped;
- expanded Recommendation, Alert, Fulfillment, scheduler, and connection gate:
  49 tests, 0 failures, 0 errors, 0 skipped;
- full backend suite: 371 tests, 0 failures, 0 errors, 0 skipped;
- backend production package: success;
- documentation link check: 808 links, none missing;
- `git diff --check`: clean, with line-ending notices only.

Commit `1811332e0ccd1b34b21b6b01ff00183ea620b7be` passed CI run 396 in
4m33s and Render deployed that exact revision in 4m37s. A bounded authenticated
baseline ran from `2026-09-12T16:35:04.8121709Z` through
`2026-09-12T16:36:20.7409387Z`, with no broad E2E traffic:

- readiness: 1,343 ms;
- liveness: 399 ms;
- unauthenticated session: 617 ms;
- login: 3,457 ms;
- authenticated session: 1,294 ms;
- Dashboard summary: 2,480 ms;
- Dashboard snapshot: 39,526 ms;
- Runtime: 23,483 ms;
- SockJS info: 1,224 ms;
- audit read: 1,250 ms;
- logout: 772 ms.

Tenant audit correlated the immediately preceding reconciliation run
`aaf5fc6f-5634-4b28-bc08-f1bf144624b7`. It completed in 83,203 ms with
155 successful Inventory work units and exactly one successful Fulfillment
warehouse work unit. This proves the prior 96 repeated task evaluations
collapsed to the active-warehouse count while preserving successful completion.

Render logs contained no Hikari acquisition timeout for the deployed revision.
Immediately after the baseline, scheduled dispatch, replay, and pull telemetry
showed zero waiters and eight to ten idle connections, with dispatch work
completing in 2-94 ms. The observed request latency is therefore not a reproduced
10/10 pool-starvation event. Historical Hikari starvation remains proven, but
this fulfillment amplification seam is closed.

The remaining latency signal is the 155-item Inventory reconciliation path. It
must be diagnosed as the next bounded work family rather than inferred from a
numbered browser test.

`RECOMMENDATION_SCHEDULER_ROUTING = VERIFIED LIVE`

`RECOMMENDATION_FULFILLMENT_WAREHOUSE_AMPLIFICATION = VERIFIED LIVE`

## Inventory Policy Lookup Amplification Correction

The first post-warehouse-bounding reconciliation still took 83,203 ms while
successfully evaluating 155 distinct Inventory records. Source tracing found a
second bounded amplification seam in each Inventory evaluation:

- `StockPredictionService.estimate(Inventory)` loaded the tenant operational
  policy;
- `InventoryIntelligenceService.evaluate(Inventory, StockPrediction)` loaded
  the same tenant operational policy again;
- both lookups called the transactional policy service and therefore performed
  at least 310 policy repository calls across the 155-item pass, repeatedly
  resolving the same policy for records belonging to the same tenant.

Scheduled reconciliation now loads one policy when it first encounters each
distinct Inventory tenant and supplies it to prediction and intelligence through
explicit overloads. The cache lives only for that run and policy resolution
remains inside the existing per-item failure boundary, so one tenant lookup
cannot abort the whole pass. The existing methods remain unchanged for
event-driven Inventory evaluation, all 155 Inventory records are still
evaluated, and Recommendation/Alert persistence and condition-lock semantics
are unchanged.

Direct tests prove that three Inventory records across two tenants perform two
policy loads and that the scheduled monitoring overload does not call either
legacy policy-loading method. Local verification:

- focused reconciliation and inventory-intelligence gate: 15 tests, 0
  failures, 0 errors, 0 skipped;
- expanded Recommendation, Alert, Fulfillment, scheduler, and connection gate:
  69 tests, 0 failures, 0 errors, 0 skipped;
- the unchanged scheduled-pull external-delay test had one missed 10-second
  local-server latch during the first combined run, then passed all three
  repetitions alone and all three repetitions in the clean expanded rerun;
- full backend suite: 373 tests, 0 failures, 0 errors, 0 skipped.

Production packaging, documentation, CI, exact deployed-revision confirmation,
and one bounded hosted reconciliation measurement remain required. No Hikari,
timeout, scheduler, database, or infrastructure setting changes.

`RECOMMENDATION_INVENTORY_POLICY_LOOKUP_AMPLIFICATION = CORRECTED LOCALLY`

## Inventory Policy Lookup Live Result

Commit `f70fb9fcb4ac62a49d66a4efe3c6bbe096ca4a99` passed SynapseCore CI run
398 in 4m36s. Render then auto-deployed that exact revision in 4m55s and exposed
it as the last successfully deployed commit before hosted measurement began.

The first two completed post-deploy reconciliation runs preserved all work:

- run `47f58e29-aa94-4126-946a-d159d933b752`: 70,903 ms, Inventory
  155/155, Fulfillment 1/1, failures 0;
- run `9730ea9e-456c-4127-913c-c7d6c354435f`: 84,698 ms, Inventory
  155/155, Fulfillment 1/1, failures 0.

The corrected authenticated baseline ran from
`2026-09-12T17:29:20.5510758Z` through `2026-09-12T17:30:18.5918331Z`:

- readiness: 1,021 ms;
- liveness: 788 ms;
- unauthenticated session: 1,054 ms;
- login: 1,667 ms;
- authenticated session: 500 ms;
- Dashboard summary: 1,677 ms;
- Dashboard snapshot: 31,085 ms;
- Runtime: 17,584 ms;
- SockJS info: 540 ms;
- audit read: 1,311 ms;
- logout: 676 ms.

The prior baseline was 83,203 ms for reconciliation, 39,526 ms for Dashboard
snapshot, and 23,483 ms for Runtime. The first new reconciliation improved, but
the second varied above the old scheduler duration; the read endpoints improved
directionally but remain slow. Therefore the exact query-work reduction is
verified live, while deterministic latency closure is not claimed.

Render's one-hour Hikari search contained no connection-acquisition timeout for
the new process. Adjacent scheduler telemetry reported zero waiters and nine to
ten idle connections, with operational dispatch completing in 1-81 ms and
automated Replay in 4 ms. This is not a reproduced 10/10 pool-starvation window.

The next bounded Inventory target is the per-record predictive demand query and
Recommendation/Alert no-op transaction work. Diagnose and reduce only proven
repeated work; do not skip Inventory evaluation or alter policy, condition-lock,
pool, timeout, scheduler, database, or infrastructure behavior.

`RECOMMENDATION_INVENTORY_POLICY_LOOKUP_AMPLIFICATION = VERIFIED LIVE`

`RECOMMENDATION_INVENTORY_LATENCY = IMPROVED BUT STILL OPEN`

## Inventory Demand Query Amplification Correction

The remaining stable-row source path issued
`sumRecentQuantityByProductAndWarehouse` once for every Inventory record. At
the observed 155-record hosted volume, this produced 155 serial aggregate
queries with the same one-hour demand shape.

The scheduled pass now obtains those totals through one grouped product-and-
warehouse query and supplies each exact total to the existing prediction rules.
Missing grouped rows map to zero, matching the previous aggregate result. The
event-driven Inventory entrypoint keeps its single-record query, while a failed
scheduled batch read falls back to that original path rather than aborting the
pass.

The change does not skip Inventory evaluation or alter intelligence rules,
Recommendation/Alert persistence, condition locks, transactions, Hikari,
timeouts, scheduler settings, or infrastructure. The focused gate passed seven
tests, the expanded Alert/Inventory/Recommendation gate passed 40 tests, and the
full backend suite passed 375 tests, all with no failures, errors, or skips.
This includes a Spring/JPA integration execution of the grouped query and
preserved per-item failure accounting. Production packaging succeeded, the
documentation check found all 808 local links valid, and `git diff --check` was
clean apart from line-ending notices. CI and exact-deployment gates remain
before live measurement.

`RECOMMENDATION_INVENTORY_DEMAND_QUERY_AMPLIFICATION = CORRECTED LOCALLY`
