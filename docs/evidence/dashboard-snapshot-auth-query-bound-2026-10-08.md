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
The same-ID Render log for login `a776eeea-338b-43ba-8acf-8128ee38e8a9`
reported 5,100 ms HTTP wall, 215 ms thread CPU and Hikari 1 active/9 idle/0
waiting at completion. The initial snapshot
`4cdd4877-e4de-4237-b7ea-cfec38e54a8a` reported 6,996 ms HTTP wall,
4,795 ms composition, 215 ms HTTP-thread CPU, and Hikari 0 active/10 idle/0
waiting at completion. Its four sparse composition stack samples included
two PostgreSQL JDBC, one Redis client and one Java/other. Client minus server
wall was approximately 608 ms; server wall minus composition was 2,201 ms.
Those differences are arithmetic boundaries, not an attribution of the
unmeasured stages. The completion-time pool snapshot does not prove that the
pool was uncongested throughout the request.

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

## Snapshot HTTP boundary diagnostic and Live after-sample, October 11

The five warm client reads above fell mostly below the existing five-second
`RequestTraceFilter` warning threshold. Three composition logs were captured,
but the matching request-level identity/handler wall and CPU fields were not.
This prevents a same-ID split of server time from client/network time for
ordinary 3-second snapshots. A candidate narrows the **logging threshold only**
for `GET /api/dashboard/snapshot` to two seconds; all other API routes keep
five seconds. The request ID, matched route, phase timings and pool snapshot
are already part of the existing bounded log format. No timeout, pool,
scheduler, transaction, response or authorization behavior changes. The
completion-time Hikari sample remains only a point-in-time observation, not
connection-holder proof. The lower threshold may increase warning volume if
snapshots are routinely slow; hosted log volume must be observed before this
diagnostic is treated as an acceptable permanent setting.

The focused `RequestTraceFilterTest` passed 14/14 locally, including the
snapshot threshold boundary, unchanged five-second threshold on another
route, and payload omission. The complete local backend command exited zero;
reports written in this run totalled **72 suites/410 tests, zero failures,
errors or skips**. `ReplayOrderConnectionDemandIntegrationTest` passed its
three repetitions in this run; that does not explain its earlier intermittent
full-suite failure. A stale failed Surefire report from October 8 remains on
disk and is excluded by report modification time rather than hidden as a
current failure. Exact-head PR runs `38136231545` and `38136213070` passed
both `verify` and `dispatch-postgres`. PR #6 merged as
`62b5d681d7eb3500876e3fa9e52f82eb79b01799`; merged-main CI
`38136507060` passed both jobs. Render showed that exact SHA Live after a
4m56s auto-deploy. The post-cutover six-flag gate returned true.

On Live instance `msjqm`, the same read-only synthetic-tenant probe used one
login and five serial warm summary/snapshot pairs. Readiness and anonymous
session preflight returned 200 in 622 and 435 ms. Login returned 200 in
2,845 ms; initial summary and snapshot returned 200 in 2,557 and 4,688 ms.
Those calls establish warm-up, not the warm sample range.
`WARM_BASELINE_UTC=2026-10-11T11:48:25.380Z`.

| Warm sample | Snapshot UTC start | Summary client ms | Snapshot client ms | Snapshot ID suffix | Same-ID HTTP wall / CPU ms | Same-ID composition wall / CPU ms |
| --- | --- | ---: | ---: | --- | ---: | ---: |
| 1 | 11:48:26.069 | 688 | 3,510 | `f6b71e37` | 3,059 / 142 | 2,292 / 100 |
| 2 | 11:48:30.571 | 992 | 3,234 | `f24c0a77` | 2,647 / 120 | not captured |
| 3 | 11:48:34.679 | 873 | 3,000 | `e49d8786` | 2,514 / 134 | not captured |
| 4 | 11:48:38.282 | 603 | 3,014 | `dd4864a2` | 2,612 / 112 | 2,097 / 85 |
| 5 | 11:48:42.205 | 908 | 3,008 | `6f0debc4` | 2,516 / 112 | not captured |

All ten warm reads returned 200. The unrounded client snapshot range was
2,999.5-3,510.0 ms; same-ID server HTTP range was 2,514-3,059 ms. Client
minus server wall was approximately 402-587 ms. For the two warm requests
with composition logs, server wall minus composition was 767 and 515 ms.
These differences bound unmeasured work; they do not identify network,
serialization, acquisition or a particular SQL statement. The initial
snapshot `ea3a0d6a-77c7-43ef-9e84-8a1a06b14e0d` had 4,307 ms HTTP wall,
3,195 ms composition and 203 ms HTTP-thread CPU. It is excluded from the
warm range.

All six snapshot HTTP logs were emitted by the new 2-second threshold and
reported Hikari `total=10 active=0 idle=10 waiting=0` **at response
completion**. They do not establish pool state during each request, nor
explain historical `10/10` starvation. The five warm HTTP-thread CPU times
were 112-142 ms against 2.5-3.1 seconds wall. Three composition logs were
captured in the six-snapshot probe; the others must not be treated as zero
composition time. Two sparse warm composition samplers landed in PostgreSQL
JDBC frames, including fulfillment assessment and alert visibility paths.
They are not measured SQL duration, PostgreSQL PID/lock evidence or a
connection-hold timeline. Nearby operational-dispatch scans processed zero
work and sampled zero waiters; this is not a representative busy-worker test.
The new threshold produced six HTTP warnings plus three composition warnings
in this bounded run. Log volume under sustained operator traffic is still
unmeasured.

This closes the **missing same-request HTTP-versus-composition observation**
for this warm control, not warm-latency root cause or M1. The similar older
3.0-3.8 second warm-client range was on a different deployment/instance and
is not a like-for-like causal improvement comparison. The current data place
most client wall inside the server handler, with low current-thread CPU;
they do not distinguish PostgreSQL round trips, Java off-CPU waits, host
throttling or pool acquisition during the request. Platform Owner Activity
HTTP after-measurement remains separate and unavailable from this tenant
probe. No performance fix was applied.

## In-flight pool observation candidate, October 11

The evidence-only PR #7 merged as `0000a9fe2513ce0a67fb1dae389a5adf8a60a23f`;
both merged-main CI jobs passed. Render's manual deploy of that exact SHA became
Live after 5m04s. Its startup validated all 15 migrations, reported schema
version 15 with no migration necessary, and the post-cutover six-flag gate
returned true. This deployment changed documents only, not the runtime code
that produced the warm trace above.

Source inspection found no enclosing `@Transactional` boundary on the
snapshot composer. Its sections call separate services, including a
read-only transaction for alert visibility. The completion-time pool readings
therefore cannot be read as a single connection held throughout composition.

A **not-yet-deployed diagnostic candidate** extends the existing one-second,
60-sample snapshot composition stack sampler to also capture the maximum
global Hikari active/waiting counts and minimum idle count while it samples.
The values are logged only with a slow composition. A missing/non-Hikari pool
is reported as unknown (`-1`), not as healthy zero. The sampling performs no
database query, changes no transaction, pool size or timeout, and records no
tenant payload or SQL text. Focused sampler/snapshot/realtime tests passed,
followed by a complete local H2-profile backend run: 72 Surefire reports,
411 tests, zero failures, errors or skips. The docs link check found 1,020
local links and no missing target. This is not PostgreSQL, CI or hosted proof.
It may miss sub-second pressure, its pool counts
are global and non-atomic, and it cannot map a pool connection to a PostgreSQL
PID or identify a transactional owner. CI and hosted results for this
candidate are still missing.

## Exit still required

- Preserve the passed full backend and exact-SHA CI checks; investigate the
  intermittent Replay/Order connection-demand failure rather than treating
  the later green run as closure.
- Obtain a comparable before/after hosted window or another attribution method
  for the specific auth-query change. Measure PostgreSQL/connection hold and
  host resource context instead of inferring it from sparse stack samples.
- Preserve tenant/role/scope and revocation behavior in hosted verification.
  A local H2 query count cannot close H1, H2, H7, H12 or M1.
- If the in-flight pool diagnostic is deployed, correlate its samples with
  the same request ID and section timing; obtain separate PostgreSQL PID/SQL
  and Java transaction evidence before attributing a holder or applying a fix.
