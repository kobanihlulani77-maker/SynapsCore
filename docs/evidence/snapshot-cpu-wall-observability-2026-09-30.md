# Slow snapshot CPU-versus-wall timing - 2026-09-30

## Why this seam

The exact-revision [connector batching proof](connector-telemetry-batching-2026-09-30.md)
reduced the 58-connector snapshot section to 499 ms, but the full warm snapshot
still took 13,806 ms in composition and 16,875 ms in the HTTP handler. Multiple
other sections each took hundreds to roughly two thousand milliseconds. The
completion-time Hikari sample was 0 active/10 idle/0 waiting. None of these
measurements distinguishes CPU execution from JDBC/network/lock/pool waiting,
or identifies the historical ten connection holders.

## Bounded instrumentation

For snapshots crossing the existing five-second slow threshold,
`OperationalViewService` now logs current-thread CPU milliseconds beside the
existing wall milliseconds for composition and each section. For slow API
requests, `RequestTraceFilter` logs current-thread CPU for the whole filter,
identity resolution, and the remaining handler interval beside existing wall
and completion-time Hikari numbers. A value of `-1` means the JVM does not
support or enable current-thread CPU timing. The timer does not enable JVM-wide
profiling, log payloads/credentials, alter transactions, or change the API.

Current-thread CPU time excludes time the thread is blocked or waiting, but is
not process-wide CPU use and cannot by itself locate JDBC acquisition, SQL,
serialization, another worker, or a PostgreSQL PID. Low CPU relative to wall
is a direction for the next correlated trace, not proof that the Free instance
is throttled. A Hikari value at response completion remains a point sample.

## Verification and next gate

`RequestTraceFilterTest` asserts that the slow log contains bounded route,
wall, CPU, and Hikari values without request query secrets. The two existing
`OperationalViewService` snapshot tests exercise the changed composition path.
Focused tests passed locally. `backend/mvnw.cmd -q test` exited 0; current
Surefire reports from 14:28-14:39 SAST contain 68 suites, 396 tests, zero
failures, zero errors, and zero skips. Exact-SHA CI/deployment and a single
warm hosted snapshot trace are pending for this instrumentation.

On the deployed revision, compare section CPU with wall, and total HTTP CPU
with handler wall for the same request ID. If CPU approximates wall, investigate
CPU demand and the measured instance envelope. If wall greatly exceeds CPU,
capture acquisition/hold, PostgreSQL state, network and thread stack for the
same UTC window. Do not enlarge Hikari, raise timeouts, or change the plan
from CPU timing alone. H1/H7 and M1 remain open.
