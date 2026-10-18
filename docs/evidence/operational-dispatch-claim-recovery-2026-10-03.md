# Operational dispatch claim recovery - 2026-10-03

## Proven gap

The dispatch worker persisted each notification item as `PROCESSING` before
refreshing the Dashboard and broadcasting tenant-scoped realtime state. Its
scheduled scan selected only `PENDING`. A process stop after the claim but
before `COMPLETED` or `FAILED` therefore left the item outside every later
drain. The entity had no `@Version` field, so catching optimistic-lock
exceptions around ordinary `save` did not establish a cross-instance claim
or terminal-transition guard. This is a source-code lifecycle gap, not a
claim that a particular hosted item was observed stranded.

## Bounded correction

The scan now includes `PENDING` plus `PROCESSING` items whose `updatedAt` is
older than a five-minute configurable lease (`processing-lease-ms`, with a
one-minute floor). Claim, completion, and failure are conditional database
updates. The claim increments the existing `attemptCount`; terminal updates
require that exact attempt. A late old worker cannot mark a reclaimed item
complete or failed. A competing worker receives zero updated rows and does
not broadcast. No schema migration, Hikari change, scheduler-frequency change,
or operational order/inventory mutation is involved.

Recovery republishes a tenant-scoped notification from authoritative REST
state. It is **at least once**, not exactly once: an unusually slow worker
that survives past the lease can overlap a reclaim and emit a duplicate
signal. The attempt guard protects the durable queue state, not duplicate
publication. `FAILED` remains excluded from automatic recovery and visible
through the existing incident path. A database outage while recording the
terminal state leaves `PROCESSING` eligible for a later lease-based retry.

## Follow-up: successful broadcast with failed terminal recording

A later source inspection found that the worker's single `try/catch` covered
both realtime publication and the subsequent `COMPLETED` database update.
If publication succeeded but the terminal write threw, that catch called
`failDispatch` and could durably label an already-published item `FAILED`.
This is a code-proven failure boundary, not an observed hosted incident.

A regression test reproduced it before the correction: one broadcast occurred,
the completion write threw, and the item became `FAILED` instead of remaining
`PROCESSING`. The worker now handles publication and terminal-recording errors
separately. A publication error still marks the claim `FAILED`; a terminal
recording error propagates so scheduler diagnostics report `FAILED` and the
drain stops; it leaves unfinished claims under the existing lease/reclaim path.
Successfully recorded items in a partly completed batch remain `COMPLETED`.
Re-publication after a lease may be a
duplicate notification; authoritative REST state remains the source of truth.

The corrected focused queue/claim tests passed locally. The complete backend
suite on the changed files passed 70 suites and 403 tests, zero failures,
errors or skips, using the H2 test profile. Production packaging with tests
skipped exited zero. This does not prove an injected PostgreSQL terminal-write
failure, hosted recovery after process interruption, or dispatch headroom under
backlog. For final production revision `a123bca595643cb4800b7981ae38d33924ea5e46`,
[CI run `37128570340`](https://github.com/kobanihlulani77-maker/SynapsCore/actions/runs/37128570340)
completed both `verify` and `dispatch-postgres` successfully. The PostgreSQL
job repeats claim/reclaim tests; it does not inject a terminal-write failure.
Render showed that exact SHA as the last successful deployment and marked
`dep-db0gogg473hc738514sg` Live. The six-flag read-only connection gate
reported frontend, backend, database readiness, auth, WebSocket and proof
allowance true. Visible post-deploy dispatch logs showed only idle scans with
`processed=0` and `hikariWaiting=0` in the inspected slice. These establish
build/deployment health, not broadcast-to-terminal-write recovery or sustained
headroom. H4 and M1 remain OPEN.

## Verification boundary

The final focused local run passed seven tests, zero failures/errors: the
service tests cover stale versus fresh work, `FAILED` exclusion, fanout, and
trace restoration; JPA integration tests cover reclaim, old-attempt rejection,
and simultaneous single-winner claims. The complete backend suite on the same
production code passed 70 suites and 401 tests with zero failures, errors,
or skips. The last subsequent edit only strengthened the stale-versus-fresh
test; its focused rerun passed. Production packaging also exited zero.

The local run used H2, not PostgreSQL. Docker Desktop's daemon was unavailable
locally on October 3, so no local PostgreSQL container was used. [Exact-SHA CI for
`a172a68`](https://github.com/kobanihlulani77-maker/SynapsCore/actions/runs/37125342253)
completed successfully. Render subsequently showed
`a172a68ecbc460729b9beafbaeba8111426e7072` as the last successfully
deployed commit and marked [deploy `dep-db0ftvojo6nc739jer90`](https://dashboard.render.com/web/srv-d7d2s41j2pic73fakrag/deploys/dep-db0ftvojo6nc739jer90) Live. Recent
post-deploy `operational-dispatch` scans completed in 3-98 ms with
`processed=0` and `hikariWaiting=0` in the observed log slice. That is an
idle scheduler control, not a recovery exercise or a sustained headroom
measurement.

The follow-up [CI run for `30a6dc1`](https://github.com/kobanihlulani77-maker/SynapsCore/actions/runs/37126166095)
completed both `verify` and `dispatch-postgres` successfully. The focused job
used a disposable PostgreSQL 17.11 service, applied Flyway migrations V1-V14,
and passed both repository integration tests with zero failures or errors.
This proves the conditional claim, stale reclaim, and old-attempt rejection
on PostgreSQL, including two simultaneous claim callers with one winner.
Those callers were threads in one test process, not separate application
instances.

Controlled process-stop timing, multi-instance drain behavior, backlog recovery time,
pool headroom under actual dispatch work, and duplicate-signal effects remain
to verify. H4 and M1 stay open. The next production-shaped proof should run
on isolated PostgreSQL data or a safe disposable tenant, compare queue
status/attempts before and after a controlled worker interruption, and
preserve both authoritative state and realtime readback without mutating
customer operations.

## PostgreSQL terminal-write failure proof, October 8

[PR #2](https://github.com/kobanihlulani77-maker/SynapsCore/pull/2) merged as
`16f44753f1cc90c21d3dbe6accc59a51d575c798`. It adds one focused test
and includes that test in the existing disposable-PostgreSQL CI job; it does
not change production code, schema, scheduler settings or infrastructure.
The test creates a uniquely named pending dispatch item and a PostgreSQL
trigger that rejects its `COMPLETED` update after a recorded realtime
broadcast. It asserts that the processing attempt propagates the PostgreSQL
failure, does not falsely become `FAILED`, and releases the drain guard. It
then removes the trigger, ages the claim beyond the lease, and asserts that a
later drain reclaims and completes the item on attempt 2. The recording
realtime service sees two broadcasts, making the at-least-once consequence
explicit rather than claiming exactly-once publication.

The first branch CI attempt exposed a test-fixture error: a static bean
factory could not supply the realtime `ObjectProvider`. Replacing it with a
registered singleton in a `DefaultListableBeanFactory` fixed only that
fixture. The focused H2 local queue suite passed 7/7. At exact head
`bdf46bd99e66e2ffa738de2fe7717ade6fcee453`, both
[CI 37791142973](https://github.com/kobanihlulani77-maker/SynapsCore/actions/runs/37791142973)
and [CI 37791136167](https://github.com/kobanihlulani77-maker/SynapsCore/actions/runs/37791136167)
passed `verify` and `dispatch-postgres`. The merged-main
[CI 37793495899](https://github.com/kobanihlulani77-maker/SynapsCore/actions/runs/37793495899)
completed successfully at exact SHA `16f4475`: both `dispatch-postgres`
and `verify` passed. This checks the disposable-PostgreSQL failure/reclaim
path and the normal CI regression lanes, not an actual hosted worker stop.

This is disposable-PostgreSQL transaction/reclaim proof under a test-owned
trigger, not a process kill, independent JVM, hosted delivery, duplicate
consumer, sustained backlog or resource-headroom proof. H4 and M1 remain
**OPEN**.

## Fresh-worker abort simulation candidate, October 13

A test-only candidate extends the existing disposable-PostgreSQL proof class.
It commits a `PROCESSING` claim, then throws a deliberate `Error` immediately
before the integration notification is published. This bypasses the worker's
ordinary `RuntimeException` failure handler, modeling loss of that worker
between claim and publication. A newly constructed queue service sees the
unexpired claim as ineligible. After the test ages the claim beyond the lease,
that fresh service reclaims it, publishes once, and records `COMPLETED` on
attempt 2. The assertions cover the durable status, attempt count, processed
timestamp, and notification count on isolated test data.

The changed test class compiled and the local focused queue/claim suite passed
9/9. Docker Desktop's daemon was unavailable locally, so the new test did not
run against local PostgreSQL. On candidate `98feffb`, the
[dispatch-postgres CI job](https://github.com/kobanihlulani77-maker/SynapsCore/actions/runs/38711425142)
used disposable PostgreSQL 17 and passed 5/5 selected tests, including both
methods in `OperationalDispatchTerminalWritePostgresProof`. The general `verify`
job was still running when this evidence was updated. The final-head
[CI run `38711698411`](https://github.com/kobanihlulani77-maker/SynapsCore/actions/runs/38711698411)
subsequently passed both `verify` and `dispatch-postgres` at `515c9b3`; the
PostgreSQL job passed 5/5 selected tests. [PR #15](https://github.com/kobanihlulani77-maker/SynapsCore/pull/15)
merged as `a17fd6141b0f71da385078f56dcbe0353ddf2414`. On October 15,
Render Deploys showed that exact merge Live after 5m12s, and the post-deploy
six-flag connection gate passed. This proves startup reachability, not hosted
recovery. An exception in one JVM followed by a fresh service object is **not** an actual
process kill, separate JVM, multi-instance race, hosted delivery, lease
overlap, or backlog/headroom proof. H4/H11 and M1 remain OPEN regardless of
this candidate's CI result.

## Separate-JVM process-stop proof, October 18

The next isolated test/CI candidate uses the existing disposable PostgreSQL
job rather than production data. Its first guarded Maven test starts a worker,
persists a `PROCESSING` claim, and writes that test JVM's PID only after the
claim returns and publication is entered. The CI shell sends an OS `SIGKILL`
to that PID, requires the first Maven invocation to fail, and starts a second
Maven/JVM process against the same PostgreSQL database. The fresh worker must
observe attempt 1 still `PROCESSING`, refuse to reclaim before lease expiry,
then reclaim after the test ages only its own row and complete attempt 2 with
one recorded broadcast. The test class is disabled unless the dedicated
process-kill system property is set; it cannot terminate a normal local test
run or hosted process. The adjacent local queue/claim suite passed 9/9. The
complete local backend run exited zero with 414 reported tests across 73
Surefire reports, zero failures/errors and two intentional guarded skips. A
focused recompile after the PID-comparison edit again skipped both guarded
methods. These local runs prove compilation and guard behavior, **not** the
kill/reclaim path.

At exact head `8a823e0f72de3d0c6f9385c00c23197049df5af1`, [CI run
39120367583](https://github.com/kobanihlulani77-maker/SynapsCore/actions/runs/39120367583)
passed both the disposable-PostgreSQL `dispatch-postgres` job and the general
`verify` job. The CI shell sent `SIGKILL` to the marker PID after the committed
claim; the first Maven invocation exited 137. A second Maven/JVM invocation
read the same PostgreSQL row, confirmed attempt 1 was still `PROCESSING`,
refused early reclaim, aged only that row's lease, then completed attempt 2
with one test-recorded integration broadcast (`1/1` selected test passed).
The adjacent PostgreSQL claim, terminal-write and Activity tests passed `5/5`.
This is a real process boundary in an isolated CI service, not merely an
in-process exception or fresh object. The test does not measure production
WebSocket delivery or prove exactly-once publication.

PR #24 merged as `f29bd85c7341c80feb016efb335989618eefa09a`.
[Merged-main CI run 39121078371](https://github.com/kobanihlulani77-maker/SynapsCore/actions/runs/39121078371)
passed both the disposable-PostgreSQL and general jobs, including a third
isolated OS-kill/fresh-JVM recovery execution. The three passing executions
are CI repeatability for this specific crash boundary, not a hosted service
recovery measurement.

This proof does not establish multi-node coordination, a live Render restart,
backlog fairness, duplicate-signal effects after a post-broadcast crash,
or pool headroom under dispatch load. H4/H11 and M1 remain OPEN.
