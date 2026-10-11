# Dashboard snapshot authentication query bound, October 8

## Boundary and observed baseline

This is H2 query-amplification work within M1, not a resolution of warm hosted
latency or historical Hikari starvation. On `main` at `a28c0ba`, source
inspection showed that snapshot composition calls `getCurrentOperator()` and
tenant-context resolution across many sections. Each non-strict
`resolveAuthenticatedSession()` reloaded `access_users` to check the active
user, session version and current tenant policy.

A one-request local H2 test-profile probe used a trusted starter-tenant
session, MockMvc `GET /api/dashboard/snapshot`, and Hibernate's
`StatementInspector`. The inspector counters were reset immediately before
the request; SQL containing `access_users` was counted separately from all
statements. Before the change, the request returned 200 and prepared **78 SQL
statements, including 21 touching `access_users`**. Stack inspection assigned
one to the strict workspace authorization and 20 to non-strict session
resolution. This is one local fixture, not a hosted query or latency sample.

## Narrow correction

The controller still performs its strict workspace authorization first. Only
while it reads the dashboard snapshot does `AuthSessionService` retain the
first validated non-strict identity on the servlet request. The scope is
removed in `finally`; a different session cannot reuse that value. A failed
identity resolution is never cached. The controller rechecks current authority
after composition and refuses the response if the tenant/actor/roles/scopes changed
during the read. Early `RequestTraceFilter` lookups and subsequent HTTP
requests remain outside the scope. No session-wide cache, timeout, pool,
schema or infrastructure setting changed.

The final authority comparison is source-level protection, not yet a
deterministic mid-request revocation race proof. The test below verifies
revocation between separate requests.

In the same local fixture after the initial scoped correction, a trace run
returned 200 with **47 total SQL statements and four touching
`access_users`**. The four were two request-trace lookups, one strict
authorization, and one lookup inside the snapshot scope. With the final
authority recheck in place, the focused test measured **53 total SQL
statements and five touching `access_users`** in the same local fixture. The
permanent integration test sets a conservative ceiling of five auth-user
reads and 60 total SQL statements for this fixture,
then repeats the request and changes the user session version. The next
request is denied with 403, proving the scope does not persist across
requests. A focused final-code run passed the new query-bound test, the
snapshot-reuse test, seven security tests and three Replay/Order connection
demand repetitions (12 tests total, zero failures/errors). Two direct
controller tests passed: an unchanged actor receives the snapshot, while a
scope change during composition returns 403. PR review exposed that identical
actor names and grants in different tenants must not compare equal. The actor
context now carries tenant code, and a third direct test checks that a tenant
switch during composition returns 403. The focused H2 query-bound, seven
security and three controller tests passed on this revision. A complete
backend run on the revised production code passed **72 reports/409 tests,
zero failures, errors or skips**. The earlier 406-test full run and exact-SHA
CI run `37808651263` passed on commit `9dfd2e8`, before the tenant-context
follow-up. Exact-SHA PR CI `38132661078` passed both jobs for `c2eaf32`;
PR #4 merged as `117cdb1d4b8d6e6f8b5295d5c7845798c6b95e7f`, and merged-main CI
`38133025146` passed both `verify` and `dispatch-postgres` jobs.

An earlier full-backend attempt, started before the final authority recheck,
**failed** in `ReplayOrderConnectionDemandIntegrationTest`: repetition 2
could not align its 10-transaction barrier, and repetition 3 returned nine
HTTP 500 statuses rather than nine 201s. That test does not invoke the
snapshot endpoint. Its subsequent focused 3/3 pass does not resolve the
intermittency under the full-suite workload. The barrier waits 10 seconds;
whether the failure reflects test scheduling, resource pressure or an
application contention path is not established. The full final-code suite
was repeated and passed, including this test's three
repetitions. The preceding intermittent failure remains unexplained; H3/H6
and M1 must not be marked closed based on a later green run.

This supports a local query-amplification reduction, not a claim of equivalent
PostgreSQL timings, real tenant-scale behavior, connection-hold reduction, or
hosted HTTP improvement. In particular, the older sampled
`AuthSessionService#resolveUser` frame was one instant in a multi-second
request and did not establish how much wall time this path consumed. The
V15 direct audit SQL measurement is a separate boundary.

## Bounded hosted after-sample, October 11

Render showed merge `117cdb1` Live as deploy `dep-db5mh26k1f9s73egsdfg`
before the probe. The frontend, backend health, readiness, liveness, anonymous
auth session and SockJS checks all returned 200; the six-flag gate was true.
Existing synthetic tenant `HOSTED-PROOF-5-20260902-144935` was used for
read-only requests with one authenticated login. Run from `frontend/` using
`node tests/measure-hosted-snapshot.mjs`. No broad E2E or data mutation ran.
The probe emitted
only UTC, endpoint, status, duration and request ID, not credentials or body.

The readiness and anonymous session preflight took 933 and 1,102 ms. Login
took 5,896 ms; the initial summary and snapshot took 4,264 and 7,604 ms.
Those initial calls are **warm-up/cutover-adjacent**, not included in the
five-sample warm range. `WARM_BASELINE_UTC=2026-10-11T10:51:12.357Z`.

| Warm sample | Summary client ms | Snapshot client ms | Snapshot request ID |
| --- | ---: | ---: | --- |
| 1 | 895 | 3,794 | `6b9d94a6-f653-4d6a-8ae0-b6e43d3c9c69` |
| 2 | 708 | 3,803 | `80c44aa2-0bbd-4954-a5d5-807a8f35abd3` |
| 3 | 1,054 | 3,078 | `c1ef66ff-182e-460c-a3f7-104b2b991c03` |
| 4 | 675 | 2,982 | `75d79bab-dc45-4b48-a4b7-4e6055e26dfc` |
| 5 | 1,103 | 3,342 | `6585be39-11c2-45c2-b006-bf0f04996c1a` |

All ten warm reads returned 200. Summary range: 675-1,103 ms; snapshot
range: 2,982-3,803 ms. These are serial Playwright API client durations,
not browser-render time or a load/soak distribution. Render application logs
for instance `7zzr9` matched the exact snapshot request IDs for samples 3-5:

| ID suffix | HTTP thread | Composition wall / thread CPU ms | Largest sections | Sampled wait |
| --- | --- | ---: | --- | --- |
| `c1ef66ff` | `exec-9` | 2,091 / 99 | fulfillment 303, inventory 295, connectors 205 | one PostgreSQL JDBC frame, one Java/other frame |
| `75d79bab` | `exec-7` | 2,087 / 104 | fulfillment 305, inventory 303, connectors 205 | one PostgreSQL JDBC frame, one Java/other frame |
| `6585be39` | `exec-5` | 2,006 / 94 | fulfillment 406, inventory 305, connectors 299 | two Java/other frames |

Each wait category is a sparse stack sample, not measured SQL duration,
connection-hold time or proof of a PostgreSQL lock. The difference between
client duration and composition wall time includes work outside that stage and
network overhead; it has not been apportioned. Nearby operational-dispatch
start/completion logs on the same instance showed Hikari `total=10`, typically
`active=0-1`, `idle=9-10`, `waiting=0`, and `processed=0`. Those are sampled
scheduler boundaries, **not** the Hikari state inside each HTTP request.
No request-to-PostgreSQL-PID/SQL timeline or synchronized host CPU/heap chart
was captured.

Historical hosted snapshots around 5-19 seconds were taken on other revisions,
tenants and load windows. The present 3.0-3.8 second range is a bounded current
after-sample, **not** a like-for-like causal improvement estimate for the
identity-reuse change. The older 2.0-2.9 second ordered audit SQL baseline and
V15's 0.048-0.061 ms direct SQL after-sample describe a different query;
neither can substitute for snapshot HTTP or Platform Activity measurements.
Warm snapshot latency remains material, and historical Hikari `10/10` holder
identity remains unknown. M1/H1/H2/H7/H12 stay open.

## Exit still required

- Preserve the passed full backend and exact-SHA CI checks; investigate the
  intermittent Replay/Order connection-demand failure rather than treating
  the later green run as closure.
- Obtain a comparable before/after hosted window or another attribution method
  for the specific auth-query change. Measure PostgreSQL/connection hold and
  host resource context instead of inferring it from sparse stack samples.
- Preserve tenant/role/scope and revocation behavior in hosted verification.
  A local H2 query count cannot close H1, H2, H7, H12 or M1.
