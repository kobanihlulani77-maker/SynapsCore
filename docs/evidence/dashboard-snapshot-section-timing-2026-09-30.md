# Dashboard snapshot section timing - 2026-09-30

## Boundary

The warm [September 27 hosted trace](warm-runtime-pool-pressure-2026-09-27.md)
records slow dashboard reads, but the HTTP filter's handler duration cannot
identify which part of `OperationalViewService.buildSnapshot()` took the time.
The snapshot assembles multiple domains serially and coalesces equivalent
in-flight requests. A slow waiter need not own a database connection.

## Bounded diagnostic

`OperationalViewService` now measures monotonic elapsed time for the owner
composition and its individual sections, and separately measures total time
through the in-flight coordinator. Only requests taking at least five seconds
emit a warning. The composition log contains section names and elapsed
milliseconds, not tenant data, payloads, credentials, or SQL text. Existing
request ID and thread log context can link a warning to the HTTP trace.

An owner composition warning identifies an expensive section. A slow request
warning without a corresponding composition warning may represent an in-flight
waiter, contention before composition, or another boundary; it must not be
classified as database time from these logs alone. Section elapsed time includes
Java work, connection acquisition, and SQL; it does not separate them. Neither
warning identifies one of the historical ten Hikari holders or proves a fix.

## Verification and next capture

The focused `OperationalViewServiceSnapshotReuseTest` and
`InFlightRequestCoordinatorTest` passed (3 tests, 0 failures/errors/skips).
The complete backend `mvnw.cmd test` run passed 393 tests across 67 reports,
with 0 failures, 0 errors, and 0 skips. The docs link check was clean (916
links, none missing). CI, deployment, and hosted timing results must be
recorded separately; local tests do not substitute for them.

On a confirmed deployment, capture one warm slow snapshot with its request ID,
request versus composition durations, section timings, Hikari sample, and
PostgreSQL activity from the same UTC window. If the request is slow but
composition is fast, investigate coalesced waiting and request-side boundaries.
If one section dominates, inspect only that section's service/transaction path.
Do not raise a timeout or alter a database/backend plan from the timing alone.
