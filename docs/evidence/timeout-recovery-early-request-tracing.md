# Timeout Recovery - Early Request Tracing

## Scope and Starting Point

Date: 2026-09-06. Starting HEAD:
`7c2fc17b21d6fa93c2b37d404624e88737cb2a58`.

This is the bounded measurement correction prepared in the
[recovery map](../hosted-timeout-recovery-map.md) and
[SLA race checkpoint](timeout-recovery-sla-escalation-race.md).
It does not claim to identify the historical ten Hikari holders or to fix
the 5836 ms hosted login observed at 2026-09-06T11:47:22.721Z.

## Repository Truth and Red Proof

`RequestTraceFilter.doFilterInternal` resolved actor and tenant before starting
its timer, setting the response request ID/MDC, or entering its try/finally.
Each resolution can call `AuthSessionService.resolveAuthenticatedSession`,
whose user lookup reaches persistence. Therefore connection acquisition and
session-query time could be absent from the application request timer, and
early failure could escape correlation and request metrics entirely.

Metric recording also preceded cleanup in the same finally block. A recording
exception could leave request identity in ThreadLocal/MDC on a reused thread.

The new `RequestTraceFilterTest` ran against unchanged production source:
**12 tests, 5 failures, 0 errors, 0 skipped**. The failures were:

- Early lookup saw `system-no-request` instead of the incoming request ID.
- The measured lookup window was 142723300 ns (142.7233 ms), but recorded request duration was only 65700 ns (0.0657 ms).
- Failure in either the first or second identity lookup left the response request ID absent (two cases).
- A metric-recording exception left `first-request` in the request context.

Red run log: `backend/target/request-trace-red.log` (local, ignored artifact).
The timing test models delayed session persistence with two short pauses and
compares measured monotonic durations; it does not change production timeouts.

## Minimal Correction

Only `RequestTraceFilter` production behavior changed:

1. Start the timer on filter entry and establish request ID/MDC before session resolution.
2. Begin with anonymous/missing-tenant context, then populate only resolved identity inside the measured try/finally.
3. Include lookup failures in the existing error accounting and guarantee cleanup even if metric recording throws.

No session result is cached; both existing validation calls remain. Header
fallback policy, login bypass, authority/session validation, downstream status
handling, and client-disconnect handling remain covered by direct tests.
No SQL, transaction annotation, Hikari setting, scheduler, timeout, frontend,
dependency, or infrastructure configuration changed.

## Verification

Focused command (from `backend`):

```text
mvnw.cmd -q -Dtest=RequestTraceFilterTest,CoreIdentityConnectionDemandTest,CoreIdentityWriteIsolationServiceTest,ScenarioSlaEscalationConcurrencyIntegrationTest test
```

Result: **22 tests, 0 failures, 0 errors, 0 skipped**. This includes all 12 new
tracing cases and all ten identity-demand/SLA concurrency regression cases.
Log: `backend/target/request-trace-focused.log`.

Full backend command: `mvnw.cmd test`.
Result: **338 tests, 0 failures, 0 errors, 0 skipped**, BUILD SUCCESS in
7m40s, completed **2026-09-06T12:06:00Z**. The existing 34-test
`PlatformTenantAccessBoundaryIntegrationTest` also passed unchanged.
Log: `backend/target/request-trace-full-suite.log`.

Backend package: `mvnw.cmd -q -DskipTests package`, exit 0.
Artifact: `backend/target/backend-0.0.1-SNAPSHOT.jar`, 76186906 bytes,
last written **2026-09-06T12:06:35Z**.
Log: `backend/target/request-trace-package.log`.

Documentation link check: **788 links, no missing local links**.
`git diff --check`: clean. Unrelated worktree files are excluded from this change.
This is the local verification checkpoint; CI and hosted acceptance are separate
release gates, not results claimed by these local tests.

## Limits and Next Action

- Login POSTs deliberately skip the early session lookups. This measurement correction is not an explanation for the observed slow login.
- The timer measures the synchronous application filter path, not Render routing, full browser round-trip time, WebSocket lifetime, or individual connection-hold/SQL duration.
- Request IDs during early lookup improve log correlation but do not by themselves map a Hikari connection to a PostgreSQL PID or prove the ten historical owners.
- No new hosted E2E or repeated database sampling was performed for this correction. The last served revision and complete warm baseline remain unconfirmed.
- The existing Render tab was checked again during this correction; Chrome still returned `Debugger unattached`. No Render deployment status or server logs were obtained through that attempt.
- Recommendation reconciliation remains ruled down for its captured window. The healthy control remains valid.

After local/CI verification and push, confirm the actual served revision after
deployment before any hosted proof. Correlate the already captured slow-login
request `70482131-7817-48b4-8f66-a63b3fdb89da` with Render logs when available;
do not repeat a broad test merely to create another timeout. The next prepared
local owner analysis is the product/import outer-transaction path calling the
independent identity-sequence transaction, separate from the already corrected
independent-write helper.

Overall timeout recovery and Consistency Phase 5.1 remain OPEN. Whole-system
repeatability and pilot readiness are not established by this instrumentation fix.
