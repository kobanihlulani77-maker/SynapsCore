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

Operational dispatch emits the boundary only when pending work exists. Replay
and pull keep their existing enabled checks and schedules. The diagnostics read
the existing Hikari MXBean and do not request a JDBC connection. Exceptions are
logged and rethrown unchanged.

## Direct Proof

`ScheduledTaskExecutionDiagnosticsTest` holds the only live connection in a real
Hikari/H2 pool, executes the observer, and verifies that telemetry reports
`hikariTotal=1 hikariActive=1 hikariIdle=0 hikariWaiting=0` while the pool remains
at one connection. It also verifies that failure identity is emitted and the
original exception is propagated.

`ScheduledWorkerDiagnosticsWiringTest` invokes the real scheduled entrypoints
and verifies the exact Replay and pull task labels. Existing operational dispatch
tests exercise the updated constructor and unchanged queue behavior.

Focused verification: **10 tests passed, 0 failures/errors/skips**.

Full backend verification: **367 tests passed, 0 failures/errors/skips**.

Backend package: **BUILD SUCCESS**.

## Classification and Next Gate

`SCHEDULER_OWNER_OBSERVABILITY_GAP = CLOSED LOCALLY`

This is not a timeout fix and does not claim that a scheduler owns the historical
ten Hikari holders. After CI succeeds and the exact commit is confirmed live,
the next action is one measured warm baseline while correlating
`Scheduled work task=` records with HTTP request IDs, `HHH000444`, durations,
and Hikari counters. Broad hosted E2E remains blocked until that baseline is
healthy. If it is slow, the overlapping task record identifies the next bounded
owner investigation.
