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

## Source boundary and next measurement

`PlatformControlPlaneService.getOverview()` composes Runtime, tenant summaries
and activity sequentially without an outer transaction. Its same-class call
to `getActivity()` does not pass through the Spring transactional proxy.
`getActivity()` reads the newest 20 business events, then the newest 20 audit
logs, maps them to metadata-only responses, sorts, and returns 20. The schema
baseline contains no `created_at` index on those two tables. A scan/sort at
large row counts is a plausible cause, not a proven query plan or reason for
this 3.4-second observation. No PostgreSQL PID, EXPLAIN plan, table sizes, or
per-query timing was captured in the hosted window.

The smallest next diagnostic separates `events`, `audits`, and `merge` wall and
current-thread CPU durations when activity itself exceeds two seconds. It
does not change the queries, transaction boundary, response, Hikari settings,
or infrastructure. At the next naturally slow exact-revision request, identify
which read consumed the time and correlate a safe PostgreSQL EXPLAIN/ANALYZE or
session sample before proposing an index or other production correction.

## Verification and limit

The existing `PlatformTenantAccessBoundaryIntegrationTest` exercises platform
overview and activity response/authority, including metadata-only output.
The focused local run passed 36 tests, zero failures/errors/skips, with H2 in
the production Spring profile. The subsequent full `mvnw test` run completed;
its 70 Surefire XML reports record 404 tests, zero failures, zero errors, and
zero skips. These are local behavior checks, not a PostgreSQL query-plan or
hosted latency proof. PostgreSQL query-plan proof, exact-revision CI, and
deployment of this diagnostic remain pending. H1/H2/H7/H12 and M1 remain OPEN.
