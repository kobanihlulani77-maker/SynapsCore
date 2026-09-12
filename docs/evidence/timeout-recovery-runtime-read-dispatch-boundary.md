# Timeout Recovery - Runtime Read Dispatch Boundary

## Bounded Question

May `GET /api/system/runtime` synchronously drain deferred operational fan-out,
or must Runtime remain an observation-only read while the existing after-commit
and scheduled workers own dispatch execution?

## Repository Truth

The architecture defines operational dispatch as persisted, deferred fan-out.
`OperationalStateChangeListener` drains after commit on the operational executor,
and `OperationalDispatchQueueService.drainOnSchedule()` provides a scheduled
recovery path. Runtime's additional drainer was introduced by commit `1474f298`
under local full-stack verification stabilization.

Both `SystemRuntimeService.getRuntimeStatus()` and
`getTenantRuntimeStatus()` called a private retry loop before building the read
response. That loop could call the global drainer five times, query queue state
after every attempt, and sleep four times for 100 ms. Queue processing can also
refresh dashboard state and publish realtime updates for pending tenants.

## Direct Red Proof

The test profile disables scheduling. A focused integration test persists one
`PENDING` dispatch item, calls `GET /api/system/runtime`, and reads the item back.
Against unchanged production: **1 test, 1 failure, 0 errors/skips**. The expected
status was `PENDING`; Runtime changed it to `COMPLETED`. Log:
`backend/target/runtime-read-dispatch-red.log`.

This proves a Runtime HTTP read executed deferred queue work. It does not prove
that the request held a JDBC connection continuously or that this path alone
caused historical ten-of-ten Hikari starvation.

## Correction and Verification

The two Runtime entrypoints no longer call the inline drain helper, and that
helper was removed. The after-commit listener and scheduled drainer are
unchanged. Runtime still reports pending, processing, failed, oldest-pending,
and last-completed queue posture from authoritative repository state.

Focused verification: **6 tests passed, 0 failures/errors/skips**. This covers
three Runtime endpoint cases, both queue service cases, and the state-change
listener. Log: `backend/target/runtime-read-dispatch-focused.log`.

Full backend verification: **353 tests passed, 0 failures/errors/skips**,
BUILD SUCCESS in 10m18s, completed `2026-09-12T12:11:19Z`. Log:
`backend/target/runtime-read-dispatch-full-suite.log`. Backend package
verification also passed; log: `backend/target/runtime-read-dispatch-package.log`.

CI and hosted served-revision verification remain separate gates at this
pre-push checkpoint.

## Boundary

This removes synchronous deferred work and bounded sleeps from Runtime reads.
It does not change Hikari size, timeouts, scheduler count/frequency, queue batch
size, retry policy, persistence, realtime payloads, or operational authority.
Hosted request-duration improvement and exact historical holder attribution
remain deployment-time evidence gates.
