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
clean apart from line-ending notices.

Commit `638536a316d6f6b1f18cfab9a3855099637d410f` passed SynapseCore CI run
400 in 5m28s. Render then exposed that exact revision as Live before hosted
measurement began.

The first completed post-deploy reconciliation took 80,097 ms. The next three
completed reconciliations took 26,705 ms, 21,495 ms, and 19,577 ms. All four
runs preserved Inventory 155/155, Fulfillment 1/1, and zero failures. The three
steady-state runs after the initial pass average 22,592 ms, compared with the
prior 70,903 ms and 84,698 ms live checkpoint.

The first bounded authenticated sample overlapped the 80,097 ms reconciliation
and measured 46,718 ms for Dashboard snapshot and 18,486 ms for Runtime. A
second sample after reconciliation completed measured 10,945 ms and 7,944 ms
respectively. Render contained no grouped-demand fallback warning and no Hikari
acquisition timeout for the deployed process. Adjacent operational dispatch,
Replay, and pull telemetry showed `hikariActive=0`, `hikariIdle=10`, and
`hikariWaiting=0`.

The grouped path is active and the exact per-Inventory demand-query
amplification is verified removed in production. This does not close Inventory
latency: the first post-deploy pass remained slow and authenticated reads remain
above the desired active-runtime threshold. The next bounded source seam is the
Recommendation and Alert no-op transactional work repeated for each stable
Inventory record.

`RECOMMENDATION_INVENTORY_DEMAND_QUERY_AMPLIFICATION = VERIFIED LIVE`

`RECOMMENDATION_INVENTORY_LATENCY = STILL OPEN`

## Stable Inventory Advisory No-op Correction

The next bounded source map showed that healthy Inventory rows with no current
advisory state still entered the Recommendation transaction and both Alert
condition-lock transactions during every scheduled pass. With 155 Inventory
rows, this produced hundreds of lookups and advisory-lock calls that could not
change persisted state.

Scheduled reconciliation now loads current Inventory Recommendations and active
Alert condition keys once, then supplies that snapshot to the existing
Inventory monitoring path. It skips a persistence service only for a healthy
row with no state owned by that service. Current Recommendations, active
low-stock or depletion Alerts, and transfer Recommendations that reference the
row as their source remain on the original repair path. Any active calculated
condition also remains on that path. Event-driven Inventory evaluation is
unchanged and still performs full synchronization.

Recommendation and Alert condition identities are generated by shared methods
used by both the snapshot and the authoritative services. Snapshot query
failure is fail-safe: the pass logs the failure and performs full per-Inventory
synchronization. No advisory rule, persistence behavior for active state,
transaction boundary, Hikari setting, timeout, scheduler setting, database, or
infrastructure setting changes.

The focused gate passed 10 tests, the expanded Alert/Inventory/Recommendation
gate passed 42 tests, and the complete backend suite passed 379 tests, all with
zero failures, errors, and skips. Production packaging also succeeded. The
direct tests prove healthy no-state skipping, persisted-state repair, active
condition handling, transfer-source repair, and fail-safe full synchronization.

Exact-revision CI, deployment confirmation, and bounded hosted measurement are
still required before this seam can be classified as verified live.

`RECOMMENDATION_INVENTORY_STABLE_NOOP_AMPLIFICATION = CORRECTED LOCALLY`

`RECOMMENDATION_INVENTORY_LATENCY = STILL OPEN`

## Stable Inventory Advisory No-op Live Result

Commit `d7a622989c745fb3b7387dcaabab7127f652c1b8` passed SynapseCore CI
run 402 in 4m46s. Render deployed that exact revision in 4m45s and showed it as
Live before the bounded hosted proof began. The six required live-connection
flags were all true.

The first three completed reconciliation events observed on the exact deployed
revision were:

- `c6bccc04-ca91-4464-9242-865d85e00eae`: 10,398 ms;
- `56cba2b6-596d-49b7-823c-1b5379286f43`: 4,777 ms;
- `b11f0160-9291-44ad-8652-e9fc97bb069e`: 2,600 ms.

Every run reported Inventory 155 attempted, 155 succeeded, zero failed;
Fulfillment one attempted, one succeeded, zero failed; and no retirements or
run failure. The average duration was 5,925 ms, versus 22,592 ms for the prior
three steady-state grouped-demand runs.

Render contained no `Recommendation reconciliation could not load current
inventory advisory state` warning and no Hikari connection-acquisition timeout
for the inspected process window. Adjacent scheduled-work telemetry showed zero
Hikari waiters and seven to ten idle connections, including while authenticated
HTTP traffic overlapped recommendation and dispatch work. Repeated Hibernate
follow-on-locking warnings and platform notification activity remained visible
on the recommendation thread, but they did not coincide with pool starvation or
failed work in this proof and are not classified as a new defect from this
evidence alone.

Bounded authenticated reads still varied materially. Dashboard snapshot was
24,890 ms, 18,118 ms, 9,634 ms, and 10,527 ms across four samples; Runtime was
12,710 ms, 7,922 ms, 4,756 ms, and 6,100 ms. All returned HTTP 200. The stable
advisory no-op seam is live-verified, while Dashboard snapshot and Runtime
composition remain the next bounded latency target.

`RECOMMENDATION_INVENTORY_STABLE_NOOP_AMPLIFICATION = VERIFIED LIVE`

`RECOMMENDATION_INVENTORY_LATENCY = MATERIALLY REDUCED`

`AUTHENTICATED_DASHBOARD_RUNTIME_LATENCY = STILL OPEN`

## Dashboard Inventory Projection Batching

After the stable Inventory advisory no-op closure, the bounded source map moved
to authenticated read composition. `OperationalViewService.getInventoryOverview()`
loaded the visible Inventory set once but calculated every row through APIs that
each resolved tenant policy and queried recent demand independently. The
snapshot path consequently repeated one recent-demand aggregate query and at
least two policy lookups for every Inventory row.

The Dashboard path now uses the existing grouped-demand reader once and caches
one operational policy per tenant for the request. Prediction and intelligence
receive that same policy and the exact grouped units. A failed grouped read
retains correctness by logging
`Dashboard snapshot could not batch recent inventory demand` and using the
original per-row demand query path; policy lookup remains request-scoped. No
Inventory rule, response contract, scope boundary, persistence behavior,
transaction boundary, scheduler, Hikari setting, timeout, schema, frontend, or
infrastructure setting changes.

Two direct tests prove the grouped path and fail-safe fallback. The expanded
Stock Prediction, Operational View, and Realtime unit gate passed seven tests,
and the Spring/JPA Dashboard snapshot integration test passed. The full backend
suite passed 381 tests with no failures, errors, or skips. Production packaging
also succeeded.

The seam remains locally verified pending exact-revision CI and hosted proof.
The production proof must establish the exact deployed revision, all six live
flags, absence of the grouped-read fallback warning and Hikari acquisition
timeouts, and repeated bounded snapshot/runtime timings against the current
snapshot baseline of 9,634-24,890 ms.

`DASHBOARD_SNAPSHOT_INVENTORY_QUERY_AMPLIFICATION = CORRECTED LOCALLY`

`AUTHENTICATED_DASHBOARD_RUNTIME_LATENCY = STILL OPEN`

## Dashboard Inventory Projection Batching Live Result

Commit `63deb26339b84a346278b91267fafb8bcf5f3a73` passed SynapseCore CI
run 404 in 5m11s. Render's first auto-deploy was canceled after 11m24s at the
internal health-check gate. A manual deployment of the same commit then became
Live, and the six-flag connection gate was fully green.

One bounded authenticated session produced the following exact timings:

- login: 4,063 ms;
- Dashboard summary: 2,462 ms, 1,981 ms, and 8,264 ms;
- Dashboard snapshot: 31,097 ms, 25,642 ms, and 21,088 ms;
- Runtime: 31,481 ms, 20,694 ms, and 15,130 ms;
- logout: 464 ms.

Every snapshot returned HTTP 200 and all 155 Inventory rows. Render had no
`Dashboard snapshot could not batch recent inventory demand` warning and no
Hikari connection-acquisition timeout in the inspected process window. The
grouped read is therefore active without correctness fallback, and the former
per-Inventory demand-query amplification is verified removed in production.

The endpoint remains unacceptably slow. This proof does not support another
Dashboard Inventory batching change or any pool, timeout, database, frontend,
or infrastructure adjustment. It moves the bounded investigation to the
serial Runtime composition path, where repeated repository reads remain
visible in source.

`DASHBOARD_SNAPSHOT_INVENTORY_QUERY_AMPLIFICATION = VERIFIED LIVE`

`AUTHENTICATED_DASHBOARD_RUNTIME_LATENCY = STILL OPEN`

## Authenticated Composition Snapshot Reuse

The bounded follow-up mapped duplicate repository work shared by Dashboard
snapshot and Runtime. Dashboard snapshot collected audit, connector, Replay,
and Scenario-notification data for its response before incident composition
loaded the same four sources again. Runtime separately loaded rich connector
telemetry for connector diagnostics and then loaded it again through incident
composition. It also repeated pending and failed dispatch counts already
present in its backbone summary.

The composition services now reuse exact request-local source snapshots:
Dashboard passes its four source lists to incident composition, and Runtime
passes one connector snapshot while reusing one backbone summary for telemetry.
`SystemIncidentService` resolves the current operator once per composition and
retains its no-argument path for independent callers. Filtering, incident
identity, priority, ordering, response limits, tenant and warehouse authority,
and response fields are unchanged.

This is a read-composition correction only. It does not change transaction
boundaries, Hikari, timeouts, scheduler behavior, database schema, frontend
behavior, or infrastructure. Six direct and batching-regression tests passed,
and the expanded Realtime/MVP/production-hardening gate passed 124 tests with
zero failures, errors, or skips. The complete backend suite passed 385 tests
with zero failures, errors, or skips, and production packaging succeeded.

`AUTHENTICATED_COMPOSITION_DUPLICATE_READS = CORRECTED LOCALLY`

`AUTHENTICATED_DASHBOARD_RUNTIME_LATENCY = STILL OPEN`

## Authenticated Composition Snapshot Reuse Live Result

Commit `0d1e56e5308351a6464af3d8edcd0895b0602deb` passed SynapseCore CI
run 406 in 5m07s and became Live through an exact Render auto-deploy lasting
5m23s. The six-flag live gate was fully green before the bounded authenticated
proof began at `2026-09-19T12:24:27.4281088Z`.

The proof logged in once in 6,634 ms, then measured three sequential samples.
Dashboard summary completed in 5,413 ms, 1,975 ms, and 487 ms. Dashboard
snapshot completed in 27,385 ms, 11,795 ms, and 8,016 ms. Runtime completed in
10,633 ms, 9,443 ms, and 7,722 ms. Logout completed in 625 ms. Every request
returned HTTP 200, and all three snapshots retained all 155 visible Inventory
rows.

Compared with the prior exact-deployment proof, snapshot average improved from
25,942 ms to 15,732 ms, while Runtime average improved from 22,435 ms to 9,266
ms. Render showed no Hikari connection-acquisition timeout in the inspected
window. Adjacent post-proof scheduler telemetry retained ten idle connections
and zero waiters during dispatch, Replay automation, and scheduled-pull work.

This verifies the request-local snapshot reuse in production and materially
reduces the shared authenticated composition latency. The endpoint family is
not yet fully closed because the first snapshot remained 27,385 ms. That
remaining serial composition cost stays the next bounded source target; this
result does not justify Hikari, timeout, database, frontend, scheduler, or
infrastructure changes, nor a broad E2E run.

`AUTHENTICATED_COMPOSITION_DUPLICATE_READS = VERIFIED LIVE`

`AUTHENTICATED_DASHBOARD_RUNTIME_LATENCY = MATERIALLY REDUCED BUT STILL OPEN`

## Dashboard Fulfillment Snapshot Reuse

The next bounded source map found one remaining cache-miss duplication inside
Dashboard snapshot composition. `OperationalViewService` composed the complete
Fulfillment overview for the response, while a cache-miss
`DashboardService.getSummary()` independently composed the same overview again
for its backlog, delay, and risk counts.

Dashboard snapshot now composes one request-local Fulfillment overview, supplies
that exact object to Dashboard summary refresh when the summary cache misses,
and returns the same object in the response. Standalone summary reads preserve
their original no-argument path and continue composing Fulfillment when no
snapshot is supplied. Summary caching, response fields, tenant and warehouse
authority, transaction boundaries, Hikari settings, timeouts, schedulers,
schema, frontend behavior, and infrastructure are unchanged.

Four focused snapshot-reuse and batching tests passed. The expanded Realtime,
MVP-flow, and production-hardening gate passed 118 tests with zero failures,
errors, or skips. The complete backend suite passed 386 tests across 66 reports
with zero failures, errors, or skips, and production packaging succeeded.

Commit `3bb4a08cb1c0cbb7e3348bab62fbe8cdcd14c461` passed SynapseCore CI
run 408 and Render auto-deployed that exact commit in 4m43s. All six live-
connection flags were green before authenticated measurement began at
`2026-09-19T12:56:39.4388720Z`.

One bounded authenticated session produced the following exact timings:

- login: 4,519 ms;
- Dashboard summary: 3,866 ms, 1,207 ms, and 964 ms;
- Dashboard snapshot: 23,322 ms, 13,311 ms, and 13,121 ms;
- Runtime: 12,765 ms, 10,490 ms, and 13,679 ms;
- logout: 727 ms.

Every request returned HTTP 200, and every Dashboard snapshot contained all 155
visible Inventory rows. Because the sequence intentionally measured Summary
before Snapshot, it warmed the 30-second summary cache and is not sufficient by
itself to prove the cache-miss branch timing. After the cache TTL had elapsed,
one isolated authenticated Snapshot with no preceding Summary call in its
session returned HTTP 200 with all 155 Inventory rows in 13,272 ms.

Render contained no Hikari connection-acquisition timeout and no Dashboard
Inventory batching fallback warning in the inspected one-hour window. Adjacent
post-proof dispatch, Replay-automation, and scheduled-pull telemetry reported
ten total connections, zero active, ten idle, and zero waiters.

The duplicate Fulfillment composition is removed in the exact live revision and
the bounded live contract remains correct. Endpoint latency is not closed: the
three-Snapshot average was 16,585 ms versus 15,732 ms in the preceding proof,
and Runtime averaged 12,311 ms versus 9,266 ms. The first Snapshot improved from
27,385 ms to 23,322 ms, while the isolated cache-miss-oriented Snapshot completed
in 13,272 ms, but the aggregate variation does not justify claiming a general
performance closure. The next source map must continue through remaining serial
Snapshot composition rather than changing Hikari, timeouts, database resources,
frontend behavior, or infrastructure.

`DASHBOARD_SNAPSHOT_DUPLICATE_FULFILLMENT_COMPOSITION = CORRECTED, CI-GREEN, AND LIVE-CONTRACT VERIFIED`

`AUTHENTICATED_DASHBOARD_RUNTIME_LATENCY = STILL OPEN`

## Direct Recommendation Lock Live Closure

Repository inspection traced the repeated PostgreSQL/Hibernate follow-on-lock
warning to `RecommendationService.createForInventory()` and
`RecommendationRepository.findByTenantCodeAndConditionKeyForUpdate()`. The
transactional service intentionally requires a pessimistic row lock, but the
locked repository method also requested an association `EntityGraph`. Hibernate
could not apply the PostgreSQL lock directly to that joined result and emitted
`HHH000444` before issuing a separate locking select.

Commit `23cb441fe05836b9b2a7ef07517374bcb9f3ff79` removed only that
`EntityGraph`. It retained `PESSIMISTIC_WRITE`, the 1,000 ms lock timeout, the
same condition identity, and all transaction and authority boundaries. A direct
reflection test protects that lock strategy. Seventeen focused tests, 123
expanded tests, and the complete 388-test backend suite passed with zero
failures, errors, or skips; packaging also succeeded. GitHub Actions run
`35883689356` was successful.

Render deployed the exact commit in 5m02s. The retired `[r4cht]` process emitted
its final `HHH000444` warning at `2026-09-23T15:48:42.148Z`; replacement process
`[2248c]` completed application startup at `2026-09-23T15:48:45.376Z`. The six-
flag live gate was fully green. New-process recommendation cycles were visible
at approximately `15:50:16Z` and `15:51:17Z-15:51:35Z`, including realtime
publication stages completing in 2-192 ms. No `HHH000444` warning and no Hikari
connection-acquisition timeout appeared on `[2248c]` after the cutover.

This closes the warning and direct-lock seam only. It does not reopen the prior
healthy recommendation connection-retention capture and does not close the
separate historical Hikari starvation or authenticated endpoint-latency work.

`RECOMMENDATION_FOLLOW_ON_LOCKING = VERIFIED REMOVED ON THE LIVE REPLACEMENT INSTANCE`
