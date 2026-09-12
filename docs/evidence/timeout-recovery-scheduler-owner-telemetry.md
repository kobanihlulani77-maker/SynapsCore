# Timeout Recovery - Scheduler Owner Telemetry

## Bounded Question

Which scheduled worker owns a long-running interval on the shared main scheduler,
and what Hikari headroom exists at the start and end of that interval?

Hosted logs previously identified only the shared `SynapseScheduled-1` thread.
That was insufficient to distinguish automated Replay from scheduled pull work,
while operational dispatch used its own executor. This phase closes that
observability gap without changing scheduler frequency, concurrency, transaction
boundaries, Hikari configuration, timeouts, or infrastructure.

## Live Evidence Requiring the Seam

The exact `60e4646` deployment was live when a measured authenticated warm
baseline returned HTTP 200 but remained operationally slow:

- dashboard snapshot: **50,100 ms**, request ID
  `4acf8b2e-2b11-4bb1-af54-e5ec3ebcd2c9`
- Runtime: **29,634 ms**, request ID
  `112d5ceb-77f8-4eb3-8f3f-8aa33efd6ab1`
- no current Hikari acquisition-timeout record was captured in that window
- `HHH000444` follow-on-locking warnings repeatedly appeared on
  `SynapseScheduled-1`

The thread name proved scheduler overlap but not the concrete worker. For the
observed tenant, all 54 connectors were batch-file-drop connectors, with no
scheduled-pull connector. Its 12 pending Replay records were
`CONNECTOR_DISABLED`, which automated Replay excludes before record locking.
Those tenant-local exclusions did not identify work in other active tenants.

## Production Seam

`ScheduledTaskExecutionDiagnostics` wraps an existing scheduled work supplier
and emits structured `START`, `COMPLETE`, or `FAILED` records containing:

- stable task name;
- actual executing thread;
- elapsed milliseconds;
- processed count on success or failure type on failure;
- Hikari total, active, idle, and waiting counts at the boundary.

The following stable task names are wired:

- `integration-replay-automation`
- `integration-scheduled-pull`
- `operational-dispatch`

Operational dispatch now emits the boundary for every scheduled invocation,
including its initial queue selection and an empty-queue result. Direct and
asynchronous calls to `processPendingWork()` are not mislabeled as scheduled
work. Replay and pull keep their existing enabled checks and schedules. The
diagnostics read the existing Hikari MXBean and do not request a JDBC
connection. Exceptions are logged and rethrown unchanged.

## Direct Proof

`ScheduledTaskExecutionDiagnosticsTest` holds the only live connection in a real
Hikari/H2 pool, executes the observer, and verifies that telemetry reports
`hikariTotal=1 hikariActive=1 hikariIdle=0 hikariWaiting=0` while the pool remains
at one connection. It also verifies that failure identity is emitted and the
original exception is propagated.

`ScheduledWorkerDiagnosticsWiringTest` invokes the real scheduled entrypoints
and verifies the exact Replay and pull task labels.
`OperationalDispatchQueueServiceTest` proves that the scheduled boundary starts
before queue selection and completes after an empty result, while preserving the
existing queue behavior.

Focused verification after the full dispatch-boundary correction: **7 tests
passed, 0 failures/errors/skips**.

Full backend verification: **368 tests passed, 0 failures/errors/skips**.

Backend package: **BUILD SUCCESS**.

## Classification and Next Gate

The exact `b79c0e7` deployment was then confirmed live. A second authenticated
warm baseline ran from `2026-09-12T14:59:32.5700987Z` through
`2026-09-12T15:00:09.2418124Z`. All requests returned HTTP 200, but dashboard
snapshot took **18,016 ms** (`433271cf-c63c-4872-8728-b53affcb394c`) and Runtime
took **9,906 ms** (`52c337da-6bae-495d-8457-a12d6128a083`).

Replay completed at `14:58:18.048Z`; the next scheduled pull and Replay records
did not start until `15:00:21.246Z` and `15:00:21.248Z`. This approximately
123-second shared-main-scheduler blackout fully contained the slow baseline.
The surrounding Replay and pull boundaries showed `hikariActive=0`,
`hikariIdle=10`, and `hikariWaiting=0`. Dense `HHH000444` follow-on-locking
warnings appeared on `SynapseScheduled-1`, but no `operational-dispatch`
boundary existed because the first implementation began observation only after
queue selection and only for a non-empty result. That absence was an
instrumentation blind spot, not proof that dispatch owned the interval.

The dispatch correction therefore moves the observer to `drainOnSchedule()` and
wraps the complete scheduled invocation. It changes no domain behavior or
scheduler setting.

`OPERATIONAL_DISPATCH_SCHEDULE_BOUNDARY_GAP = CLOSED LOCALLY`

This is not a timeout fix and does not claim that dispatch or another scheduler
owns the historical ten Hikari holders. After CI succeeds and the exact
correction commit is confirmed live, the next action is one measured warm
baseline while correlating the complete `operational-dispatch` boundary with
HTTP request IDs, `HHH000444`, duration, and Hikari counters. Broad hosted E2E
remains blocked until that bounded correlation is complete.
