# SynapseCore Hosted Timeout Recovery Map

## Purpose

This map is the single forward path for the intermittent hosted timeout problem.
It is evidence-first, phase-gated, and intentionally sequential. We do not chase
the numbered E2E test that happens to fail, and we do not change several runtime
layers at once.

The primary failure under investigation is:

```text
application connection retention or overlap
  -> Hikari total=10, active=10, idle=0, waiters>0
  -> new database work waits for a connection
  -> authenticated requests and health checks time out
  -> Render marks the backend unhealthy
  -> browser pages fail as downstream symptoms
```

## Locked Truth Before Starting

### Proven

- Historical Hikari starvation occurred with all ten application connections active and waiters present.
- Historical health failure followed Hikari acquisition timeout.
- Historical backend HTTP latency reached tens of seconds while the instance was already active.
- Historical memory-limit failure occurred and is a possible amplifier.
- Historical PostgreSQL lock and long-transaction evidence exists.
- A 35-minute healthy PostgreSQL control window had no blockers, no long transaction, and at most three simultaneous open transactions.
- Recommendation reconciliation was ruled down as the holder for its captured window.
- Cold start is a separate proven failure mode and must not be used to explain an active-run failure.
- A frontend convergence issue is separately credible when HTTP is fast and authoritative data is present but the page remains stale.

### Not proven

- The exact ten historical Hikari holders.
- The exact Java method owning every historical PostgreSQL PID.
- Whether the historical incident was caused by one holder family or a combination of long transactions, lock waits, background overlap, and resource pressure.

### Locked exclusions

- Do not increase Hikari capacity as the first response.
- Do not increase Playwright timeouts or retries to hide the condition.
- Do not upgrade Render or PostgreSQL before the application holder is understood.
- Do not disable schedulers.
- Do not refactor transaction boundaries without an identified owner.
- Do not reopen recommendation reconciliation unless new contradictory evidence appears.
- Do not treat a healthy control window as proof that the historical failure did not exist.

## Operating Rules

1. One phase at a time.
2. Every phase has one question, one evidence gate, and one exit condition.
3. A failed phase stops the program; it does not trigger unrelated work.
4. Preserve the current healthy control capture and all historical failure evidence.
5. If code changes, use one bounded change set, commit and push it, wait for the new Render revision to deploy and become ready, then verify the served revision before testing.
6. Stop at the first new failure and preserve its exact time window.
7. Keep `CHROME_HTTP_SLOW` separate from `HTTP_FAST_BUT_UI_STALE`.
8. Warm-baseline and live-capture gates govern hosted work. Source inspection and bounded local reproductions may proceed using existing evidence without waiting for another hosted failure.
9. When a cause is reproduced, apply Phases 9-10 to that cause before attacking the next unresolved family. A healthy capture alone neither clears a family nor requires another identical capture.
10. Before another hosted capture, verify that available diagnostics can answer the missing ownership question. PostgreSQL samples and timestamps alone do not establish a Java owner or exact cumulative SQL duration.

## Phase 0 - Freeze the Truth Map

**Question:** Are we investigating one shared timeout chain rather than separate page defects?

**Work:** Record the historical Hikari incident, the healthy PostgreSQL control, the known cold-start behavior, the warm HTTP latency evidence, and the frontend convergence evidence.

**Pass gate:** The evidence distinguishes proven, suspected, ruled-down, and unavailable data.

**Stop condition:** Any new claim contradicts the locked evidence without a new artifact.

**Exit:** The shared timeout chain remains the primary target.

## Phase 1 - Establish a Warm Baseline

**Question:** Was the system already warm before the failure began?

**Work:** Before any hosted proof, record UTC and durations for readiness, liveness, unauthenticated session, authenticated login, dashboard summary, dashboard snapshot, runtime, and SockJS/WebSocket readiness.

**Classifications:**

- `COLD_START_ENVIRONMENT`: failure occurs before the warm baseline.
- `ACTIVE_RUNTIME`: baseline is healthy and the failure begins afterward.

**Pass gate:** All baseline requests are fast and successful, with a recorded `WARM_BASELINE_UTC`.

**Stop condition:** Baseline is not warm. Do not interpret later behavior as an active-runtime failure.

**Exit:** Only an active-runtime failure may proceed to Phase 2.

## Phase 2 - Capture the First Unhealthy Transition

**Question:** Does the healthy system transition into Hikari starvation or warm backend latency?

**Work:** Use one synchronized observation window with Chrome Network timing, Render logs, application pool telemetry where available, and the one-second PostgreSQL monitor. Do not run broad E2E merely to force a failure.

**Trigger A:** `Hikari total=10, active=10, idle=0, waiting>0`.

**Trigger B:** A warm Chrome request exceeds five seconds while Hikari and PostgreSQL remain healthy.

**Pass gate:** The first failure boundary has an exact UTC window and request or scheduler context.

**Stop condition:** Either trigger appears. Freeze the window immediately; do not continue generating traffic.

**Exit:** Classify the failure as pool starvation or warm backend latency.

## Phase 3 - Map Hikari Holders to PostgreSQL and Java

**Question:** What owns the connections when the pool is exhausted?

**Work:** For every active PostgreSQL PID in the frozen window, record transaction start/end, transaction age, active SQL age, state, wait event, blockers, query sequence, and commit/rollback. Correlate the same UTC window to request ID, HTTP thread, scheduler thread, tenant, and Java service.

**Required chain:**

```text
Chrome or scheduler
  -> request ID / thread
  -> Hikari holder
  -> PostgreSQL PID
  -> SQL sequence
  -> Java service
  -> exact @Transactional owner
  -> Java time between SQL calls
  -> commit or rollback
```

**Pass gate:** At least one holder is mapped end to end, and the overlap explains the Hikari count.

**Stop condition:** The evidence only shows PostgreSQL `ClientRead` or short SQL without proving what Java is doing. Classify it as incomplete rather than calling it a lock.

**Exit:** One or more concrete holder families are named and ranked.

## Phase 4 - Clear Background Holder Families

**Question:** Are scheduled workers consuming connections concurrently with HTTP traffic?

**Work:** Inspect and correlate these families separately:

- `SynapseScheduled-*` operational dispatch.
- Replay automation and integration pull workers.
- Fulfillment reconciliation.
- `SynapseRecommendationScheduled-*` recommendation reconciliation.

Recommendation reconciliation remains ruled down for its completed healthy capture unless new evidence contradicts it.

**Pass gate:** Each active scheduler is shown as short-lived, bounded, and below the available connection headroom, or an exact holder is identified.

**Stop condition:** A scheduler owns a long transaction or overlaps with HTTP to exhaust the pool.

**Exit:** Background contention is cleared or one scheduler is selected as the only next fix target.

## Phase 5 - Clear HTTP Transaction Holders

**Question:** Does a request keep a connection while doing non-SQL work or nested domain work?

**Work:** Trace the main candidates one at a time:

- Order creation loops over lines, reserves inventory, reevaluates signals, initializes fulfillment, records events, and audits inside a transaction.
- Fulfillment initialization and reconciliation evaluate alerts and recommendations inside transactional methods.
- Replay and integration ingestion can call order creation and inherit its transaction shape.
- Product/import writes invoke sequence synchronization and core identity repair.

Measure total transaction time against active SQL time and Java/non-SQL time.

**Pass gate:** Each candidate is either shown to release promptly or has a measured retention mechanism.

**Stop condition:** A candidate holds a connection materially longer than its SQL work and overlaps with other holders.

**Exit:** One HTTP transaction owner is selected for the smallest justified fix, or HTTP holders are cleared.

## Phase 6 - Clear Advisory Locks and Database Waits

**Question:** Is PostgreSQL itself delaying work, or is Java holding connections while waiting elsewhere?

**Work:** Separate true database lock waits from `ClientRead`, idle-in-transaction, CPU, and application waits. Inspect the `synapsecore.core-identity-writes` advisory-lock path and tenant-row pessimistic waits only when they appear in the frozen failure window.

**Pass gate:** Blockers, lock duration, owning PID, and releasing transaction are known.

**Stop condition:** A real lock convoy or advisory-lock convoy is proven. Do not mask it by increasing pool size.

**Exit:** PostgreSQL wait is cleared, or the exact lock owner is selected for the smallest fix.

## Phase 7 - Clear Resource Amplifiers

**Question:** Is the application resource envelope amplifying otherwise bounded work?

**Work:** Correlate Render memory, CPU, thread pressure, restart events, garbage collection where available, Hikari metrics, and request latency with the failure window.

**Pass gate:** Resource pressure is either absent during the failure or tied to the holder family already identified.

**Stop condition:** Memory or CPU pressure independently explains request starvation or instance failure.

**Exit:** Resource pressure is classified as cause, amplifier, or unrelated.

## Phase 8 - Separate Frontend Convergence

**Question:** Was the backend slow, or did the browser fail to converge after a timely response?

**Work:** Use Chrome Network as the authority for request duration and response content. Classify separately:

- `CHROME_HTTP_SLOW`: the browser waits for the backend response.
- `HTTP_FAST_BUT_UI_STALE`: the response is timely and authoritative, but the page does not render it.

**Pass gate:** The browser classification has request timing, response status, request ID, and page state.

**Stop condition:** Do not change frontend rendering while the HTTP request itself is slow.

**Exit:** Frontend convergence is either cleared or becomes its own bounded follow-up.

## Phase 9 - Apply the Smallest Production Fix

**Question:** What single change removes the proven retention or contention seam?

**Work:** Change only the owner identified by Phases 3-7. Add or update a direct regression test. Do not combine unrelated scheduler, pool, database, and frontend changes.

**Pass gate:** Focused local tests pass and the diff addresses the measured owner.

**Deployment gate:** Commit and push, wait for the new Render deployment to finish, confirm the served revision and readiness, then begin hosted verification. A run started before the new revision is live is invalid evidence.

**Exit:** The new revision is confirmed live and ready.

## Phase 10 - Warm Verification and Repeatability

**Question:** Did the fix remove the transition, not merely make one run pass?

**Work:** Establish the warm baseline, run the affected focused proof twice, then run the full hosted E2E twice. Keep Chrome, Render logs, and database monitoring aligned. Stop on the first failure.

**Pass gate:** No Hikari starvation, no unexplained warm HTTP latency, no PostgreSQL lock convoy, and no authoritative-state/UI divergence.

**Exit:** The same conditions pass repeatedly with evidence.

## Phase 11 - Closure

Close only when all of the following are true:

- Hikari starvation is either eliminated or its bounded, accepted operating envelope is proven.
- No unowned long transaction or lock convoy remains.
- Warm baseline is repeatable.
- Focused and full hosted proofs repeat successfully.
- Chrome classifies HTTP and UI behavior consistently.
- No unrelated defect is hidden behind retries or increased timeouts.

Required closure statement:

```text
SYNAPSCORE TIMEOUT RECOVERY COMPLETE - WARM HOSTED RUNTIME IS REPEATABLE
```

Until then:

```text
SYNAPSCORE TIMEOUT RECOVERY OPEN - EXACT HOLDER MAPPING OR FIX VERIFICATION REMAINS
```

## Current Starting Point

As of 2026-09-06, the local source/reproduction pass has identified the first
concrete cause. See
[Identity repair connection-demand evidence](evidence/timeout-recovery-identity-repair-connection-demand.md).

Ten independent repairs reproduced `active=10, idle=0` with ten acquisition
waiters using the real Spring JPA transaction proxy. The redundant repair
wrapper is corrected locally: six focused tests and the full 322-test backend
suite pass, and the backend package builds. The correction was pushed in
`cda37614259fc36b8495ecde315b33b63434dd97`. CI passed the six focused tests but
failed an existing Scenario SLA event-count assertion (322 tests, one failure).
The subsequent documentation-only CI run passed, but a controlled three-reader
test reproduced three SLA events instead of one. The bounded atomic transition
correction and its verification are recorded in
[SLA escalation race evidence](evidence/timeout-recovery-sla-escalation-race.md).
A bounded hosted
readback received readiness and login responses, then timed out on runtime
before confirming the served revision. Traffic stopped. CI and hosted
acceptance require the corrected commit's verification as recorded in the evidence documents.

Correction `196bfd7d12584e47508e0623af4f93378e8dc9ec` subsequently passed all
326 backend tests locally and in CI, plus CI frontend/Compose checks. Its
bounded hosted baseline stopped at a successful but slow login (5836 ms,
2026-09-06T11:47:22.721Z, request
`70482131-7817-48b4-8f66-a63b3fdb89da`). No further hosted requests or E2E
followed. The served revision and complete warm baseline remain unconfirmed.
The early session-resolution timing/correlation gap was subsequently reproduced:
five direct checks failed against unchanged production source. The bounded
filter correction passes all 22 focused tracing/identity/SLA tests, all 338
backend tests, and the package build. Local verification is recorded in
[Early request tracing evidence](evidence/timeout-recovery-early-request-tracing.md).
This is a measurement correction, not a proven cause of the slow login, whose
path skips those early session lookups. The next prepared local holder family
is the product/import outer-transaction path and its independent identity repair.

CI for tracing commit `62eeb41914e0b936e525432962ab02714d6adbd5` then failed
the original SLA count assertion (2 events instead of 1). The controlled
three-reader test still passed, but direct tests of Runtime incident and
workspace support composition reproduced a separate ambient read-only
transaction defect. Verification of the caller-boundary correction is tracked in
[SLA read-only caller evidence](evidence/timeout-recovery-sla-readonly-callers.md).
It removes the two read-only wrappers and explicitly fetches connector tenants
for detached workspace read composition. All 66 focused tests and 342 full
backend tests pass, and packaging succeeds (2026-09-08). CI and served-revision
verification remain separate gates before claiming hosted closure. Correction
`0d9711bd253fbc8d85300b117754b3d7cf7d07d9` subsequently passed CI run
`34228050405`: all 342 backend tests, frontend build, and both Compose checks.
The local/CI regression gate is complete. Product/import connection demand is
the next prepared local holder investigation; hosted served-revision and warm
baseline verification are still unconfirmed.

This is partial progress through the holder analysis and correction phases,
not closure of all timeout mechanisms. The next hosted action remains
**Phase 1 - Establish a Warm Baseline**, after the exact deployed revision is
confirmed. Broad hosted E2E is still gated by the failure classification and
the bounded correction's verification.

## Product/Import Holder Checkpoint - 2026-09-08

The next HTTP holder reproduction identified both product entrypoints borrowing
an outer catalog connection before independent sequence preflight. Ten aligned
requests reproduced ten retained connections and ten waiters. Preflight now
finishes before the top-level catalog transaction begins. All 49 focused tests
and 348 full backend tests pass. Details, explicit outer-caller limitations,
and hosted gates are in
[Product preflight evidence](evidence/timeout-recovery-product-preflight-connection-demand.md).
This removes a proven local double-borrow deadlock, not all hosted latency or
all transient queueing. The product watchdog's scheduler selection is the next
bounded diagnostics check; no further scheduler behavior change is included.

Product correction `6cb6ddbf755f664b3fb1ae21d9fc46f0207404b1` passed CI run
`34230885610`: 348 backend tests, frontend build, and both Compose checks.
Ten-request tests reached all ten catalog boundaries; transient acquisition
waiters still occurred, but the circular double-borrow deadlock did not.

## Product Watchdog Checkpoint - 2026-09-08

The actual two-scheduler Spring configuration reproduced the product watchdog
silently returning NO_OP. Its optional provider is now qualified to the existing
main scheduler, without changing scheduler count, frequency, or pool capacity.
Verification and limitations are recorded in
[Product watchdog wiring evidence](evidence/timeout-recovery-product-watchdog-wiring.md).
This restores a diagnostic, not proof that the historical ten holders are known.

## Next Holder Work - Prepared, Not Cleared

The following source map is a work queue, not measured connection-hold evidence.
Keep the completed healthy recommendation capture locked unless contradictory
runtime evidence appears.

| Order | Entry and ownership boundary | Next bounded proof |
| --- | --- | --- |
| 1 | `SystemRuntimeService.getRuntimeStatus/getTenantRuntimeStatus` calls `drainOperationalDispatchQueue`, which may invoke global `OperationalDispatchQueueService.processPendingWork` five times. The queue has no outer transaction and an instance-local draining guard. | Separate inline dispatch latency from database retention; determine whether HTTP trace/tenant context survives a dispatched batch. Measure repository/downstream transaction boundaries rather than treating the whole drain as one connection hold. |
| 2 | `IntegrationReplayAutomationService` calls per-record `IntegrationReplayService` TransactionTemplate attempts. Order creation joins the attempt; failed-attempt recording occurs after rollback. | Measure one attempt and overlapping HTTP work. Preserve proven replay atomicity; do not turn the whole batch into one transaction or infer its duration from scheduler duration. |
| 3 | `OrderService.createOrderForTenant` is transactional and includes line reservation, signal reevaluation, Fulfillment initialization, event/audit persistence, and dispatch publication. | Distinguish aggregate SQL time from work between calls within that required atomic write. Do not split inventory/order atomicity without a demonstrated seam. |
| 4 | `IntegrationScheduledPullWorkerService` has no outer transaction on its scheduled entrypoint; it performs external fetch/body reads before ingestion calls. | Check external response/body delay separately from JDBC retention and main-scheduler delay. Absence of an annotation alone is not measured proof of zero borrowed connections. |

No duration, exact historical PID, Hikari headroom, or hosted causal attribution
is claimed for these four source-mapped families. The next local bounded phase
is Runtime inline-dispatch ownership; hosted progression still requires the
exact served revision and a complete warm baseline, not elapsed deploy time.

## Runtime Inline Dispatch Context Checkpoint - 2026-09-12

The first Runtime ownership proof reproduced caller-correlation loss after an
inline queue batch: queued fan-out used the correct queued identity, but the
Runtime request's original RequestTraceContext and MDC were cleared afterward.
The batch now restores the caller snapshot from its existing `finally` boundary.
See [Runtime inline dispatch context evidence](evidence/timeout-recovery-runtime-inline-dispatch-context.md).
This is a trace-ownership correction, not a Hikari starvation fix. The separate
read-side question of whether Runtime should synchronously drain deferred global
work remains open until its direct side-effect/latency proof is complete.

## Runtime Read Dispatch Boundary Checkpoint - 2026-09-12

With test scheduling disabled, a single Runtime GET changed a persisted dispatch
item from `PENDING` to `COMPLETED`. Runtime was therefore executing deferred
global fan-out, repository calls, and bounded sleeps before returning an
observability response. The two Runtime entrypoints no longer invoke that
drainer; after-commit and scheduled dispatch ownership are unchanged. See
[Runtime read dispatch evidence](evidence/timeout-recovery-runtime-read-dispatch-boundary.md).
This removes one proven HTTP-thread workload mechanism. It does not establish
continuous JDBC hold time or identify all historical Hikari holders.

## Background Holder Budget Checkpoint - 2026-09-12

Default one-instance executor wiring and direct focused tests bound meaningful
background concurrency to one main scheduled job, one recommendation job, and
one active dispatch drain. Fifteen focused tests pass. See
[Background holder budget evidence](evidence/timeout-recovery-background-holder-budget.md).
Background executors alone are ruled down as the source of ten simultaneous
long-running holders on one default instance. Overlap with HTTP work, multiple
instances, and per-operation nested connection demand remain separate questions.

## Order/Fulfillment Assessment Scope Checkpoint - 2026-09-12

The required atomic Order path was not found to make external calls, sleep, or
open an ordinary nested independent transaction. It did, however, load every
active Fulfillment task and Order line for the tenant before filtering to the
Order's warehouse in Java. A direct transaction-scoped proof reproduced 119
entity loads with 24 unrelated Coast tasks. Fulfillment assessment now queries
by tenant, warehouse, and active status at the repository boundary. See
[Order/Fulfillment assessment scope evidence](evidence/timeout-recovery-order-fulfillment-assessment-scope.md).

Twenty-three focused tests and all 354 backend tests pass, and backend packaging
succeeds. This removes unrelated cross-warehouse hydration from the connection
hold window; it does not identify all historical holders or prove hosted pool
headroom. The next bounded phase is concurrent Order/Fulfillment transaction-
demand proof before any broad hosted E2E.

## Order Connection Demand Checkpoint - 2026-09-12

Ten distinct real Order requests were aligned after product resolution while
their required Order transactions held all ten Hikari connections. Across three
repetitions, all 30 requests returned 201, committed the required Inventory,
Order, and Fulfillment state, and fully released the pool. Waiter samples stayed
bounded at zero or one in both the focused and full-suite executions; no waiter
prevented progress, and all were gone after release. See
[Order connection demand evidence](evidence/timeout-recovery-order-connection-demand.md).

The normal distinct Order path is therefore ruled down locally as a circular
same-request double-borrow mechanism. This is not hosted PostgreSQL capacity
proof and does not identify the historical ten holders. The next bounded holder
phase is Replay/Order overlap, specifically the per-record Replay attempt and
post-rollback failure-recording boundaries.

## Replay/Order Connection Demand Checkpoint - 2026-09-12

One failed automated Replay attempt and nine independent HTTP Orders were
aligned while their transactions occupied all ten Hikari connections. Across
three repetitions, all 27 HTTP Orders completed, the Replay attempt rolled back
without operational side effects, and its durable failure transaction began
only after the initial transaction completed. Waiter samples stayed bounded at
zero or one, and the pool fully released. See
[Replay/Order connection demand evidence](evidence/timeout-recovery-replay-order-connection-demand.md).

Replay/Order overlap is ruled down locally as a nested same-thread connection-
borrow mechanism. The next bounded holder family is scheduled pull, separating
external response/body delay from JDBC-backed ingestion and measuring its
overlap with HTTP demand.

## Scheduled Pull External Delay Checkpoint - 2026-09-12

A real scheduled-pull connector was held inside a deliberately slow external
response-body read. Hikari reported zero active connections throughout that
delay. While the pull remained blocked, ten independent HTTP Orders occupied all
ten connections and completed in each of three repetitions; connector success
was persisted after the body was released. See
[Scheduled pull external-delay evidence](evidence/timeout-recovery-scheduled-pull-external-delay.md).

Scheduled-pull external latency is ruled down locally as a JDBC-retention holder.
The four prepared holder families now have bounded evidence. The next phase is
consolidation and CI/deployed-revision/warm-baseline gating, not a speculative
transaction, timeout, pool-size, or infrastructure change.

## Scheduler Owner Telemetry Checkpoint - 2026-09-12

The exact `60e4646` hosted revision produced a slow authenticated baseline while
the shared main scheduler emitted repeated follow-on-locking warnings. Existing
logs exposed only `SynapseScheduled-1`, so they could not distinguish automated
Replay from scheduled pull ownership across all active tenants. Stable task
identity, duration, outcome, and Hikari boundary counters are now emitted for
automated Replay, scheduled pull, and operational dispatch runs. See
[Scheduler owner telemetry evidence](evidence/timeout-recovery-scheduler-owner-telemetry.md).

Ten focused tests and all 367 backend tests pass, and backend packaging succeeds.
The telemetry reads the Hikari MXBean without borrowing a connection and changes
no schedule, concurrency, transaction, timeout, pool, or infrastructure setting.
The observability gap is closed locally; the historical timeout owner remains
open until CI passes, the exact revision is live, and one measured warm baseline
is correlated with the new records. Broad hosted E2E remains blocked until then.

## Operational Dispatch Boundary Coverage Checkpoint - 2026-09-12

The exact `b79c0e7` deployment was live for an authenticated warm baseline from
`2026-09-12T14:59:32.5700987Z` to `2026-09-12T15:00:09.2418124Z`. Dashboard
snapshot took 18,016 ms and Runtime took 9,906 ms, both HTTP 200. The complete
window fell inside a shared-main-scheduler telemetry gap from Replay completion
at `14:58:18.048Z` until scheduled pull and Replay restarted at
`15:00:21.246Z`/`15:00:21.248Z`. Surrounding Replay and pull boundaries reported
zero active Hikari connections, ten idle, and zero waiters; repeated `HHH000444`
warnings identified only the shared scheduler thread.

The first telemetry seam wrapped operational dispatch only after its initial
queue query and only when work was present. Consequently, the lack of a dispatch
record inside the gap could not rule dispatch in or out. The observer now wraps
the entire `drainOnSchedule()` invocation, including queue selection and empty
results, without labeling direct or async drains as scheduled work. Seven
focused tests and all 368 backend tests pass; backend packaging succeeds.

`OPERATIONAL_DISPATCH_SCHEDULE_BOUNDARY_GAP = CLOSED LOCALLY`

This remains diagnostic-only. The next bounded gate is CI, exact served-revision
confirmation, and one warm-baseline correlation against the complete dispatch
boundary. No broad hosted E2E, pool increase, timeout increase, scheduler change,
or infrastructure change is justified.

## Recommendation Scheduler Routing Root Cause - 2026-09-12

The exact `498d9a5` deployment produced another authenticated warm baseline from
`2026-09-12T15:23:21.9879163Z` through `2026-09-12T15:24:50.1912751Z`.
Dashboard snapshot took 48,454 ms and Runtime took 26,553 ms. Complete dispatch
telemetry proved the main scheduler stopped between `15:21:09.265Z` and
`15:25:09.888Z`, while dispatch itself completed normally at both boundaries.

Tenant audit evidence identified the exact owner: recommendation reconciliation
run `4fac31f3-4abb-4642-86ca-db7e732993ac` ran from
`15:21:10.191674147Z` through `15:25:08.891581758Z` for 238,699 ms, evaluating
155 Inventory records and 96 Fulfillment tasks. Its interval matches the main-
scheduler blackout and dense `HHH000444` warnings on `SynapseScheduled-1`.

The dedicated scheduler annotation was present but ineffective. The custom
`SchedulingConfigurer` replaced Spring's qualifier-aware `TaskSchedulerRouter`
with the main scheduler, forcing every annotation-driven task onto that one
executor. The correction removes only that override and aliases the existing
main scheduler as `taskScheduler`, allowing unqualified tasks to retain their
current executor while the recommendation qualifier routes to
`synapseRecommendationTaskScheduler`. See
[Recommendation scheduler routing evidence](evidence/timeout-recovery-recommendation-scheduler-routing.md).

`RECOMMENDATION_SCHEDULER_ROUTING_DEFECT = PROVEN, CORRECTED, AND VERIFIED LOCALLY`

This does not yet claim the historical ten-connection Hikari starvation is
closed. The focused runtime routing proof, 25-test recommendation gate, full
369-test backend suite, package, documentation, and diff checks are green. The
remaining gates are CI, exact deployed-revision confirmation, then one warm
baseline to verify thread ownership and latency before any broad hosted E2E.
