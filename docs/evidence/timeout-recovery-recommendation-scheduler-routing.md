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
