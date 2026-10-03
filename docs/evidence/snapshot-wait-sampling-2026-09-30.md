# Slow snapshot wait sampling - 2026-09-30

## Established boundary

The exact-revision [CPU/wall trace](snapshot-cpu-wall-observability-2026-09-30.md)
showed a warm Dashboard snapshot taking 18,799 ms on the HTTP thread with
only 559 ms of current-thread CPU. Snapshot composition accounted for
14,803 ms wall and 415 ms CPU. Existing section timings identify where wall
time accumulates but not whether a section is acquiring Hikari, using
PostgreSQL JDBC, waiting on Redis/Java, or doing other work. The pool sample
at request completion does not establish its state during those 14.8 seconds.

## Bounded diagnostic

`SlowThreadWaitSampler` schedules no stack read until a snapshot composition
has passed the existing five-second slow threshold. It then samples the
builder thread once per second, capped at 60 samples. On completion the task
is canceled; a daemon executor removes canceled tasks. Only a slow snapshot
logs one aggregate with category counts and representative class/method
frames. Categories distinguish Hikari `getConnection`/pool borrow,
PostgreSQL JDBC, Lettuce Redis client, Java monitor/wait, and other Java
activity. Hikari statement-proxy frames alone are **not** classified as
pool acquisition. No SQL text, parameters, request payloads, or credentials
are logged.

Sampling is approximate. A `POSTGRES_JDBC` sample says the thread was inside
the driver, not that PostgreSQL was blocked or that the sampled interval was
entirely SQL execution. A one-second sampler can miss shorter events and
cannot identify a PostgreSQL PID, complete query sequence, exact acquisition
or hold time, or an outer `@Transactional` owner. Use the existing request ID
and section timing to correlate a hosted slow request with PostgreSQL and
Java evidence before applying a fix. The diagnostic does not change Hikari,
transactions, scheduler frequency, API responses, or infrastructure.

## Verification and next gate

Focused classification and snapshot tests passed on the final files (four
tests, zero failures/errors). `backend/mvnw.cmd -q test` also exited 0 on
those files; Surefire reports 69 suites, 397 tests, zero failures, errors,
or skips. [Exact-SHA CI for `50fca48`](https://github.com/kobanihlulani77-maker/SynapsCore/actions/runs/36721471571)
completed successfully on September 30. On October 3, Render showed
`50fca4862c87f3a8d8e61606d53cca98cd28433b` as the last successfully
deployed commit and the service as Live (deploy `dep-dauh5etg1s2s73cu4qp0`).

One read-only authenticated warm control on that deployment returned HTTP 200
for readiness (506 ms, request `2c9cf298-009a-4909-9108-865116d28c40`),
login (1,194 ms, `5b101a74-7c47-4a7d-8020-cce015f0ab47`), summary
(1,415 ms, `a84e2412-81e2-4fbc-adb0-fd1c405357aa`), and snapshot
(4,007 ms, `29f055fa-a12e-4fba-8dd0-4bdfd77cf1b6`). The snapshot began
at `2026-10-03T12:35:21.0098602Z`. Render's last-hour search showed no
matching slow-composition log before this control; the 4,007 ms client
duration does not itself prove composition crossed the five-second sampler
threshold. No sampled wait category or request-ID-matched stack was captured.
An earlier probe's output was accidentally swallowed by its PowerShell wrapper
and is excluded from timing evidence. This is a healthy control, not proof that
the intermittent slow path is resolved or that Hikari holders are identified.

At the next naturally occurring warm snapshot above the slow threshold,
capture its request-ID-matched Render aggregate and classify the dominant path.
If Hikari acquisition dominates, capture pool overlap and holders; if JDBC
dominates, inspect PostgreSQL state and query timing; if another category
dominates, trace that service path. Do not infer a plan change from low
current-thread CPU or a single stack sample. H1/H7 and M1 remain open.
