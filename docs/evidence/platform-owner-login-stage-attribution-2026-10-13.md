# Platform Owner sign-in stage attribution, October 13, 2026

## Boundary and reason for this diagnostic

M1 H1/H2/H7 remain open. On the Live `8f5c7a9` application, three bounded
Platform Owner probe logins returned HTTP 200 but took 3,696.7, 2,390.3 and
2,342.6 ms at the client. The last began at 09:23:33.886 UTC with request ID
`a752e2ee-1f63-4be3-bb93-124feeb01b4d`. Its same-ID Render application
lines on instance `wttn5`, HTTP thread `http-nio-0.0.0.0-10000-exec-4`, show
`PlatformRealtimeNotificationService` start at 09:23:36.067 UTC and completion
at 09:23:36.069 UTC (`elapsedMs=1`). That logged stage cannot account for the
client wall time. It does not prove what occupied the earlier interval.

The source path is `POST /api/platform/session/login` ->
`PlatformOwnerSessionService.signIn()`: configuration and credential check
(including password-hash verification on a matching username), prior-session
invalidation and new session setup, then `AuditLogPersistenceService`'s
platform login audit. With no ambient transaction, the audit uses a
`REQUIRES_NEW` transaction through `CoreIdentityWriteIsolationService`.
Afterward its metadata event can publish a realtime notification. The prior
outer slow-HTTP threshold was 5 seconds, so none of the three client timings
required an outer warning. No server-side full request duration, Hikari
acquisition, audit transaction time, SQL PID, host CPU or routing time was
recorded for them. Do not label BCrypt, audit, Redis, PostgreSQL or Render Free
as the cause from the current evidence.

## Scoped candidate instrumentation

This branch lowers the outer `RequestTraceFilter` warning threshold to two
seconds for **Platform Owner login only**. The existing safe warning fields
include total HTTP wall/CPU, identity versus handler wall/CPU, final Hikari
snapshot, status, and route. Other API routes retain their existing threshold.
For a successful sign-in whose **service** time reaches two seconds,
`PlatformOwnerSessionService` logs total service time and credential, session,
and audit-stage wall durations. No username, password, hash, cookie, session
identifier, request/response body, or tenant payload is logged. Failed
credential attempts keep their existing authentication, audit and rate-limit
behavior. These changes do not alter authentication results, transactions,
pool size, timeouts, schedulers or infrastructure.

The audit-stage timer spans the audit persistence call, commit, event
publication and any synchronous after-commit work. It is **not** a PostgreSQL
SQL timer. The Hikari numbers are a response-end point sample, not an
in-flight connection history. If the client is slow but the server warning is
absent, a sufficiently complete log search can bound server time below the
two-second threshold; it cannot by itself divide routing, network and client
time. If both warning families appear, match their request ID and thread,
then attribute the measured stage before proposing a product fix.

## Verification and next gate

The focused local `RequestTraceFilterTest,PlatformOwnerSessionServiceTest` run
passed 17 tests with zero failures/errors/skips. It covers the two-second
threshold, unchanged five-second threshold for unrelated routes and tenant login, stage
arithmetic and exclusion of a synthetic secret-bearing query string from the
outer log. The full local backend `mvnw test` run before the additional
tenant-login regression completed with 413 tests across 73 Surefire reports,
zero failures/errors/skips. Exact-head CI must
still pass before merge. This is local proof of diagnostic control flow,
**not** hosted attribution.

After an approved merge, confirm the exact Render Live SHA and six-flag gate.
Wait for a normal Platform Owner sign-in or one bounded, non-rate-limited
probe; do not create repeated login traffic to force a warning. Match one
slow client request ID to its outer HTTP and service-stage logs, then use
PostgreSQL/Redis/resource evidence only for the stage actually found slow.
If no qualifying event occurs, keep H1/H2/H7 and M1 open; a quiet log is not
proof that intermittent latency was fixed. Historical Hikari 10/10 starvation
remains a separate unresolved holder question.
