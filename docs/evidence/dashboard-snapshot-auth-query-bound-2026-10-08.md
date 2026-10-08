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
after composition and refuses the response if the actor/roles/scopes changed
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
scope change during composition returns 403. A complete backend run against
the final production code passed **71 reports/406 tests, zero failures,
errors or skips**. The two direct controller tests were added after that
run's test compilation and passed separately. Exact-revision CI and hosted
results are pending.

An earlier full-backend attempt, started before the final authority recheck,
**failed** in `ReplayOrderConnectionDemandIntegrationTest`: repetition 2
could not align its 10-transaction barrier, and repetition 3 returned nine
HTTP 500 statuses rather than nine 201s. That test does not invoke the
snapshot endpoint. Its subsequent focused 3/3 pass does not resolve the
intermittency under the full-suite workload. The barrier waits 10 seconds;
whether the failure reflects test scheduling, resource pressure or an
application contention path is not established. The full final-code suite
was repeated with a retained log and passed, including this test's three
repetitions. The preceding intermittent failure remains unexplained; H3/H6
and M1 must not be marked closed based on a later green run.

This supports a local query-amplification reduction, not a claim of equivalent
PostgreSQL timings, real tenant-scale behavior, connection-hold reduction, or
hosted HTTP improvement. In particular, the older sampled
`AuthSessionService#resolveUser` frame was one instant in a multi-second
request and did not establish how much wall time this path consumed. The
V15 direct audit SQL measurement is a separate boundary.

## Exit still required

- Pass the full backend regression suite and exact-SHA CI, including the
  existing PostgreSQL lane; preserve any failure evidence.
- After an approved merge and confirmed new Live revision, compare repeated
  warm authenticated snapshot HTTP timings and section breakdowns with a
  comparable earlier window. Capture request IDs and resource/pool context.
- Preserve tenant/role/scope and revocation behavior in hosted verification.
  A local H2 query count cannot close H1, H2, H7, H12 or M1.
