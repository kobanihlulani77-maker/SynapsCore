# Platform activity read boundary, October 7, 2026

## Observed hosted request

Render identified `f7cd9b6518de79861c99748568a121d7b0b64b28` as the last
successfully deployed backend revision and marked it Live. Its application log
on instance `x4bnj` recorded this naturally occurring platform overview read:

| UTC | Request ID | Thread | Composition | Sections | Section CPU |
| --- | --- | --- | --- | --- | --- |
| 2026-10-07 10:32:31.439 | `d03b4073-de48-46b8-8a2f-03b5e540c0af` | `http-nio-0.0.0.0-10000-exec-9` | 3,980 ms | runtime 8 ms, tenants 571 ms, activity 3,399 ms | runtime 1 ms, tenants 8 ms, activity 2 ms |

The log's timestamp is when the section warning was emitted, not a measured
browser request start or completion time. A request-ID search returned this
section warning but no outer five-second slow-request line. Nearby idle
operational-dispatch samples showed Hikari active 0-2, idle 8-10, waiting 0;
they are point samples, not a continuous acquisition or transaction trace.
The 3,399 ms activity section is the dominant observed component of this one
overview. The 2 ms current-thread CPU rules down sustained execution on that
thread for this section, but does not distinguish SQL, JDBC acquisition,
network, scheduling, or other wait. It does not explain the historical 10/10
Hikari incident.

## Exact-revision activity and PostgreSQL timing

Commit `74fa2d26a243faa2def0afe99dc129ae0b911752` passed [exact-SHA CI
run 37610152541](https://github.com/kobanihlulani77-maker/SynapsCore/actions/runs/37610152541):
both `verify` and disposable-PostgreSQL `dispatch-postgres` succeeded. Render
then showed that SHA as its last successfully deployed, Live backend. The
six-flag read-only connection gate passed afterward. This confirms the
diagnostic is running, not that activity latency is fixed.

The newly separated stage timing and the Render PostgreSQL duration log
captured repeated naturally slow reads on instance `rmdbp`:

| App warning UTC | Request ID | Activity / events / audits / merge wall (ms) | Audits CPU (ms) | Nearby PostgreSQL audit SELECT duration (ms) |
| --- | --- | --- | --- | --- |
| 10:56:32.498 | `77462c56-fa40-4b16-8805-9f9a91ec47fd` | 3,197 / 198 / 2,943 / 55 | 5 | 2,936.438 |
| 10:56:33.701 | `5f6203b3-f27c-4d20-8587-05275e66fd64` | 2,201 / 99 / 2,100 / 1 | 4 | 2,039.839 |
| 10:56:39.039 | `f4336521-025f-42f7-8439-b576f343187b` | 2,237 / 96 / 2,140 / 0 | 3 | 2,037.253 |
| 10:56:40.136 | `5a13ea33-4650-493b-93fc-3ac21d7f549e` | 2,337 / 103 / 2,233 / 0 | 3 | 2,199.968 |

The PostgreSQL log names the executed SQL: `SELECT ... FROM audit_logs
ORDER BY created_at DESC FETCH FIRST $1 ROWS ONLY`. Its duration closely
accounts for the Java `audits` wall time in this window. The PostgreSQL log
does not carry the HTTP request ID, so the matching rows are temporal and
query-shape correlations, not a proven one-to-one JDBC PID mapping. No
EXPLAIN plan, blocker trace, or table-row count is available; the Render
database Metrics view reported no data for the recent window. These samples
attribute the slow section primarily to PostgreSQL execution of the audit
read, not to the Java merge or the historical ten-connection holder event.

## Source boundary and targeted correction

`PlatformControlPlaneService.getOverview()` composes Runtime, tenant summaries
and activity sequentially without an outer transaction. Its same-class call
to `getActivity()` does not pass through the Spring transactional proxy.
`getActivity()` reads the newest 20 business events, then the newest 20 audit
logs, maps them to metadata-only responses, sorts, and returns 20. The schema
baseline and mapped entity contain no `created_at` index on `audit_logs`.
The fast event stage does not justify changing `business_events`. An unindexed
sort/scan is the leading explanation for the measured audit SQL time, but its
physical plan and the contribution of database resource pressure are unproven.

The smallest justified correction is a PostgreSQL-only concurrent
index on `audit_logs(created_at DESC)` in V15, with a disposable-PostgreSQL CI
proof that the migrated index is valid and supports the actual order/limit
query. This avoids a write-blocking index build and leaves application query,
response, transaction, Hikari, scheduler and infrastructure behavior unchanged.
The migration was merged in PR #1. A failed
concurrent build must be diagnosed; an invalid leftover index must not be
silently accepted as success. After a safe merge/deploy, compare the same
activity/SQL timings and check for fresh pool or startup failures. Do not
claim M1 or H1 closure from faster platform activity alone.

The first disposable-PostgreSQL branch run, [CI 37611684372](https://github.com/kobanihlulani77-maker/SynapsCore/actions/runs/37611684372),
passed `verify` but remained at the V15 non-transactional migration line for
over six minutes. It was canceled rather than treating the stalled migration
as a pass. Flyway's default PostgreSQL transactional advisory lock is
incompatible with `CREATE INDEX CONCURRENTLY` ([Flyway PostgreSQL driver
documentation](https://documentation.red-gate.com/flyway/reference/database-driver-reference/postgresql-database)).
The branch configured Flyway's PostgreSQL session-level advisory lock and
asserted that setting in the PostgreSQL proof. This is a migration coordination
setting, not an application/Hikari transaction change. Its exact-SHA branch CI
passed before merge. Merged-main [CI run 37613626785](https://github.com/kobanihlulani77-maker/SynapsCore/actions/runs/37613626785)
then passed both `verify` and `dispatch-postgres` jobs at
`5c321c1d638cd8ceb87b55d3f77f982f76b660bc`.

## Hosted V15 rollout, October 7

Render deployment `dep-db32mr6q1p3s73f13400` showed the exact merged SHA
`5c321c1d638cd8ceb87b55d3f77f982f76b660bc` as **Live** after a 5m15s
deploy. Instance `57mtj` logged the non-transactional migration start at
11:24:56.881 UTC and `Successfully applied 1 migration ... now at version v15`
at 11:24:59.381 UTC; Flyway reported 2.278 seconds of execution. The first
six-flag connection gate ran across the cutover and reported health/readiness/
liveness timeouts, so it was not treated as a pass. A fresh gate after Render
showed Live passed all six flags: frontend, backend, database readiness, auth,
WebSocket and proof allowed. An authenticated Chrome Platform Activity page
then rendered 20 metadata-only signals on the new revision.

This confirms deployment and functional rendering, **not** the magnitude or
repeatability of the index's latency effect. No post-index browser request
duration, PostgreSQL audit SELECT duration, hosted EXPLAIN, or concurrency
sample was captured in a way suitable for a before/after comparison. The
browser log view became unavailable during that comparison. Do not infer that
the old 2.0-2.9 second query is eliminated merely because no slow warning was
seen. The next bounded check is to time the same authenticated activity read
and ordered audit SQL on this Live revision, with warm baseline and concurrent
load noted. H1 historical Hikari starvation remains a separate open question.

## After-measurement continuation

On October 7, a fresh Render Deploys inspection again showed
`5c321c1d638cd8ceb87b55d3f77f982f76b660bc` as the last successfully
deployed, **Live** backend revision (`dep-db32mr6q1p3s73f13400`). Local
`main` and `origin/main` were `f2a0eff6c16c4f8fa73af420ab2b8893e425ecd7`;
that later commit changed evidence and the readiness map, not the deployed
application code. The signed-in Chrome Platform Activity page displayed 20
signals. This is a fresh functional observation, not a client timing sample.

The Render PostgreSQL log search for `audit_logs` showed the earlier 2-3
second ordered reads and the V15 index build; it did not provide a duration
for a successful post-index read. The log threshold means absence of a new
duration entry cannot be converted into a numerical query result. Browser
automation exposed the rendered page but not resource-timing entries, and its
debugging connection later became unavailable. No request ID was mapped to a
post-index PostgreSQL PID. The Live instance observed in application logs was
`57mtj`; a fast request cannot be assigned to that instance from page render
alone.

The bounded alternative is [the secure local Activity probe](../../scripts/measure-platform-activity.ps1).
It records readiness and unauthenticated-session warm checks, prompts locally
for Platform Owner credentials, then times authenticated login, warm-up, and
five serial overview/Activity pairs. Output contains UTC start/end, endpoint,
status, client duration, request ID and Activity count, but no credential,
cookie or response body. Login/logout still create their normal audit events.
The probe is not a PostgreSQL query timer, load test, or proof of the historical
Hikari holders. **No probe result has been supplied yet, so the hosted
after-measurement and before/after comparison remain OPEN.** If the read
remains slow, capture the hosted plan and wait/resource state before another
production correction. If it is fast, measure the same ordered SQL with a
bounded read-only method before claiming a database-duration comparison.

The first attempted probe login received SynapseCore Platform Owner HTTP 429.
Do not repeat login attempts to force a measurement or reinterpret the rate
limit as Activity latency. The Chrome application tab was signed out; the
signed-in Render database Metrics page exposed no recent query-duration data,
and its Connect menu provided connection strings rather than a SQL console.
The supported alternative is [the read-only audit SQL probe](../../scripts/measure-platform-audit-sql.ps1):
it prompts locally for the Render External Database URL, uses a temporary
PostgreSQL client, verifies the V15 index metadata, then executes one warmup
and five bounded `EXPLAIN (ANALYZE, BUFFERS)` samples of the exact platform
audit-order SELECT. It neither logs in to SynapseCore nor returns audit rows.
Its natural planner choice and PostgreSQL execution times can establish a
database-side after-sample without consuming the application login bucket.
**The SQL probe has not yet been run on hosted PostgreSQL.** It cannot measure
HTTP request/Activity composition duration or establish request-to-PID
identity; those remain open until an existing authenticated session or a
normal, non-rate-limited login is available. Do not compare a direct SQL
measurement with the earlier Java `audits` wall time as if they were the same
metric. Compare direct SQL only with the pre-index PostgreSQL SQL durations,
noting that workload and cache state differ.

## Verification and limit

The existing `PlatformTenantAccessBoundaryIntegrationTest` exercises platform
overview and activity response/authority, including metadata-only output.
The focused local run passed 36 tests, zero failures/errors/skips, with H2 in
the production Spring profile. The subsequent full `mvnw test` run completed;
its 70 Surefire XML reports record 404 tests, zero failures, zero errors, and
zero skips. The diagnostic's exact-SHA CI, Live deployment and six-flag gate
then passed as recorded above. The V15 branch passed the 36-test focused
H2-backed platform boundary suite with zero failures/errors/skips and Maven
exit 0 after the migration was added. That checks the H2 no-op path and
response/authority behavior, not PostgreSQL concurrent-index creation.
Corrected disposable-PostgreSQL CI and safe deployment passed as above. A
repeated hosted before/after latency comparison remains pending. H1/H2/H7/H12
and M1 remain OPEN.
