# Timeout Recovery - Runtime Inline Dispatch Context

## Bounded Question

When an operational dispatch batch runs inline on an existing request thread,
does it preserve the request's correlation identity after publishing the queued
work? This is an observability ownership boundary, not proof that dispatch held
a JDBC connection or caused historical Hikari starvation.

## Direct Red Proof

`SystemRuntimeService.getRuntimeStatus()` and `getTenantRuntimeStatus()` invoke
the dispatch drainer synchronously. `OperationalDispatchQueueService` replaced
the current RequestTraceContext and MDC with the queued item's request, actor,
and tenant, then unconditionally cleared both in `finally`.

The focused test begins with Runtime caller identity, processes one real queue
batch through the service, verifies that downstream fan-out sees the queued
identity, and then verifies the original caller identity. Against unchanged
production it reported **2 tests, 1 failure, 0 errors/skips**: after dispatch,
the expected caller request `runtime-request` was `system-no-request`. Log:
`backend/target/runtime-inline-dispatch-context-red.log`.

## Correction

Each batch now snapshots the caller's RequestTraceContext and complete MDC map,
runs fan-out under the queued item's identity, and restores the snapshot in the
existing `finally` boundary. A scheduler thread with no caller context is still
restored to an empty context. Queue selection, claims, status transitions,
batching, failure handling, metrics, transactions, and realtime publication are
unchanged.

Focused verification: **15 tests passed, 0 failures/errors/skips**, covering
dispatch processing, asynchronous state-change handling, and the request trace
filter. Log: `backend/target/runtime-inline-dispatch-context-focused.log`.

Full backend verification: **352 tests passed, 0 failures/errors/skips**,
BUILD SUCCESS in 11m11s, completed `2026-09-12T11:52:06Z`. Log:
`backend/target/runtime-inline-dispatch-context-full-suite.log`. Backend package
verification also passed; log:
`backend/target/runtime-inline-dispatch-context-package.log`.

CI and hosted served-revision verification remain separate gates at this
pre-push checkpoint.

## Limits and Next Boundary

This correction prevents correlation loss and tenant/actor misattribution after
inline dispatch. It does not shorten dispatch work, change connection demand,
or prove that Runtime should execute the drainer synchronously. Whether a
read-only Runtime request may perform deferred global fan-out is the next
separate boundary; it requires its own red proof before production is changed.
