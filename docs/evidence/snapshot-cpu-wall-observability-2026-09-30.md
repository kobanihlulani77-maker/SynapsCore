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

## Verification and hosted result

`RequestTraceFilterTest` asserts that the slow log contains bounded route,
wall, CPU, and Hikari values without request query secrets. The two existing
`OperationalViewService` snapshot tests exercise the changed composition path.
Focused tests passed locally. `backend/mvnw.cmd -q test` exited 0; current
Surefire reports from 14:28-14:39 SAST contain 68 suites, 396 tests, zero
failures, zero errors, and zero skips. GitHub Actions run `36716270938`
succeeded for exact SHA `487b012213aed3cbecc5641a5a22ab23f2359429`.
Render deploy `dep-daug6d7f3r2c73etcb30` was observed Live for that SHA,
after startup reported Flyway V14 current.

One read-only warm hosted sample on that deployment had readiness HTTP 200
in 861 ms, authenticated login HTTP 200 in 6,200 ms, and Dashboard snapshot
HTTP 200 in 19,217 ms at the client. The snapshot contained 58 connector
summaries. Its request ID was `6f84aece-1e5d-4323-a121-5cd32f62c0ee`.
The matching Render trace on one HTTP thread recorded:

| Span | Wall | Current-thread CPU |
| --- | ---: | ---: |
| Snapshot composition | 14,803 ms | 415 ms |
| Complete slow HTTP request | 18,799 ms | 559 ms |

The composition trace included `audit` 1,699/18 ms wall/CPU,
`connectors` 906/32, `scenarioNotifications` 1,199/28,
`incidents` 1,298/30, `fulfillment` 1,300/40,
`recommendations` 998/16, `summary` 1,701/54, and `sla` 902/32.
Other sections also consumed wall time; no single section explains the
remaining duration. At HTTP completion, Hikari showed total 10, active 1,
idle 9, waiting 0. That is a completion-time sample, not the pool's state
throughout the request.

**Classification:** the slow request is not explained by CPU execution on
its own HTTP thread. The gap is waiting or off-thread time, but these logs do
not distinguish connection acquisition, SQL/database wait, network, another
thread, or host scheduling. They do not identify a JDBC holder or prove that
the Free plan is the causal limit. Historical Hikari starvation remains
separate and proven only in its captured windows. This sample neither closes
H1/H7 nor authorizes a plan change under the
[cross-phase infrastructure rule](../SYNAPSCORE-MASTER-ENGINEERING-READINESS-MAP.md#infrastructure-and-evidence-policy).

## Next gate

The next bounded diagnostic must attribute the 18.8-second wall interval:
measure JDBC acquisition and hold, PostgreSQL state, network and thread
stacks in the same UTC/request window. If normal representative work then
proves resource-limited, preserve this Free-tier sample and compare the
smallest owner-approved capacity change with unchanged workload and gates.
Do not enlarge Hikari, raise timeouts, or change the plan from CPU timing
alone. H1/H7 and M1 remain open.
