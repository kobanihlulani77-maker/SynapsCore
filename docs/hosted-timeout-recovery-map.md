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
failed an existing Scenario SLA event-count assertion (322 tests, one failure);
the discrepancy is unresolved, not dismissed as a flake. A bounded hosted
readback received readiness and login responses, then timed out on runtime
before confirming the served revision. Traffic stopped. CI and hosted
acceptance remain blocked as recorded in the evidence document.

This is partial progress through the holder analysis and correction phases,
not closure of all timeout mechanisms. The next hosted action remains
**Phase 1 - Establish a Warm Baseline**, after the exact deployed revision is
confirmed. Broad hosted E2E is still gated by the failure classification and
the bounded correction's verification.
