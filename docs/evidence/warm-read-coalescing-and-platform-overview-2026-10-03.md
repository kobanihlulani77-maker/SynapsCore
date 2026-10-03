# Warm read latency attribution, October 3, 2026

## Scope and observed deployment

This is a bounded H1/H2 diagnostic, not an M1 closure or a performance fix. Render
application logs were read in the owner's Chrome session on October 3. The two
requests below came from instance `vf7td`; the later live-tail instance was
`kvgb4`. The log search proves these requests occurred, not that either
instance was serving the current local HEAD. No new hosted proof has run against
the diagnostic changes in this record.

| UTC completion | Request ID | Route | Wall / thread CPU | Completion-time pool |
| --- | --- | --- | --- | --- |
| 12:34:42.980 | `45289aae-46dc-4319-82dc-4b4de82ef6d2` | `GET /api/dashboard/snapshot`, 200 | 6,624 / 296 ms | total 10, active 0, idle 10, waiting 0 |
| 13:15:47.945 | `532af59e-19f1-456b-a00d-6db28e0f910e` | `GET /api/platform/overview`, 200 | 5,406 / 7 ms | total 10, active 1, idle 9, waiting 0 |

For the first request, `OperationalViewService` also logged `Slow dashboard
snapshot request totalMs=5003` at 12:34:41.381 UTC. Searching that request ID
returned no snapshot *composition* line. The source shows why this is
ambiguous: [the snapshot coordinator](../../backend/src/main/java/com/synapsecore/domain/service/InFlightRequestCoordinator.java)
can make a concurrent caller wait on another caller's composition. The 5,003 ms
is not proof of 5,003 ms of SQL, Java composition, or connection retention by
this request. The extra approximately 1.6 seconds between that method's log and
the HTTP completion also remains unattributed.

The platform overview composes runtime, tenant summaries, and activity in
sequence. Its 7 ms of HTTP-thread CPU rules down sustained execution on that
thread for this one request. The completion-time pool observation does not rule
out an earlier acquisition delay. The route had no section timings, so the
5.4 seconds could not be assigned to one of those three reads.

Neither captured request shows Hikari starvation at completion. Neither has a
PostgreSQL PID, SQL timeline, transaction age, or host CPU/GC correlation.
Therefore the historical `10/10` pool incident remains separate and its holders
remain unknown. No infrastructure plan, pool size, timeout, or scheduler setting
was changed in response.

## Bounded diagnostic change

- Slow snapshot-request logs now distinguish the request that composed the
  response from a caller that waited on an identical in-flight request.
  An exception before that classification is reported as unresolved, not as a
  composing request.
  [Coordinator](../../backend/src/main/java/com/synapsecore/domain/service/InFlightRequestCoordinator.java)
  behavior and result reuse are unchanged.
- A slow platform overview now records wall and current-thread CPU time for
  `runtime`, `tenants`, and `activity` separately. Its response contract and
  query path are unchanged.
- The outer slow snapshot request retains its five-second threshold. Snapshot
  composition and platform overview section breakdown use a two-second
  diagnostic threshold, so a near-five-second method can still be correlated
  to a slow HTTP request. The logs use the request ID already in MDC. No
  payload, credential, tenant data, or normal-path detail is logged.

Focused local checks: `InFlightRequestCoordinatorTest` (3) and
`PlatformTenantAccessBoundaryIntegrationTest` (36) passed with zero failures or
errors on the diagnostic source. These tests exercise concurrency attribution
and existing platform authority behavior; they are not hosted latency proof.

On exact-SHA CI-green deployment `9974937`, Render marked the backend Live at
14:34 UTC and the six-flag readiness gate returned true. A naturally occurring
`GET /api/platform/overview` completed at 14:35:48.855 UTC under request ID
`ea2d17d9-22a5-4a0e-9856-062a2b47ad85`: status 200, 5,115 ms wall, 30 ms
current-thread CPU, Hikari total 10 / active 2 / idle 8 / waiting 0 at
completion. Its method did not emit a five-second section log. This does not
prove the delay was outside that method: the method could have run just under
five seconds, with the remaining time in authentication, filter, controller,
or response processing. That observation justified lowering only the internal
breakdown threshold. No new breakdown has yet been observed after this
threshold correction.

The first local rerun of the threshold correction returned 38/39: all 36
platform boundary tests passed, but the new coordinator test failed once.
Its fixed 100 ms test release sometimes ran before the waiter joined, so the
test observed its own supplier result. The test now waits until the waiter
thread is actually blocked before releasing the composer. The coordinator
suite then passed twice consecutively (3/3 each run). This is a test
synchronization correction, not evidence of a production coalescing defect.
Full exact-SHA CI for this follow-up is pending.

## Next evidence needed

After exact-SHA CI and deployment confirmation, use the next naturally slow
warm request rather than broad E2E to force one. For a snapshot, correlate the
`coalescedWait` flag and any composing request's section/wait-sampler logs with
the same UTC window. For platform overview, identify the slow section, then
correlate pool acquisition, PostgreSQL sessions and Java stack/transaction
ownership. Measure HTTP-to-method and method-to-response gaps separately.
These logs alone still cannot map Hikari connections to PostgreSQL PIDs.

H1, H2, H7 and M1 remain **OPEN** until the relevant hosted workload,
transaction ownership, resource envelope and repeatability gates are proven.
