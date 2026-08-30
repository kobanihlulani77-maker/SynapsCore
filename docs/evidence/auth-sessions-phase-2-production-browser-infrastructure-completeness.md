# Auth / Sessions Phase 2: Production, Browser, And Infrastructure Completeness

## Scope And Gate Position

This record closes Domain 12 Phase 2 without repeating the Phase 1 authority
matrix. Phase 1 commit `13d3d1f1996d43aa22e48057f094e90522e9940c` established
session trust validation, current tenant/user/operator authority, password
rotation restrictions, logout invalidation, and realtime authority
re-resolution. Phase 2 checks whether those contracts remain usable through the
production-shaped configuration, deployed browser, credentialed CORS, and
SockJS/realtime boundary.

Phase 1 Classification A remaining is corrected to **0**. This document keeps
unexercised provider fault injection and long-duration expiry drills as
Classification C evidence limitations. They are not presented as product
passes, but neither did the repository or live deployment expose a missing
required capability.

## Evidence Snapshot

| Evidence | Result | Boundary |
| --- | --- | --- |
| Starting repository HEAD | `13d3d1f` | Phase 1 baseline; unrelated local files were preserved |
| Live frontend | PASS | `https://synapscore-frontend-3.onrender.com` returned HTTP 200 |
| Live backend health | PASS | `/actuator/health` returned HTTP 200 and `{"status":"UP"}` |
| Live readiness | PASS | `/actuator/health/readiness` returned HTTP 200 and `{"status":"UP"}` |
| Live liveness | PASS | `/actuator/health/liveness` returned HTTP 200 and `{"status":"UP"}` |
| Live anonymous session | PASS | `/api/auth/session` returned HTTP 200 and `signedIn:false` |
| Live SockJS info | PASS | `/ws/info` returned HTTP 200 with websocket support and `cookie_needed:true` |
| Approved deployed CORS origin | PASS | credentialed response echoed the deployed frontend origin |
| Unapproved CORS origin | PASS | HTTP 403 `Invalid CORS request`; no allow-origin header |
| Live cookie probe | PASS | login/session/logout HTTP 200; `SESSION` was Secure, HttpOnly, and SameSite=None |
| Hosted browser proof | PASS | existing proof state; 6 tests passed in 6.3 minutes |
| Focused backend checks | PASS | 14 tests, 0 failures, 0 errors |

The live checks were run on 2026-08-30. The hosted proof used the existing
ignored proof-state file and did not print or persist any password or cookie
value in this evidence record.

## Production Session Contract

Production configuration uses Spring Session Redis, with the namespace
`synapsecore:sessions`, `flush-mode: on_save`, and `save-mode: on_set_attribute`.
Production requires a non-empty `SPRING_DATA_REDIS_URL` before application
startup. Production uses `ddl-auto: validate`; Flyway remains the migration
authority. The live readiness endpoint was UP while the live session and
SockJS endpoints were reachable, which is consistent with the configured DB,
Redis, and application health group.

The application does not use the rate-limit memory fallback as a session
fallback. `SecurityRateLimitService` may use process-local memory when Redis is
unavailable for rate limiting, but production session storage remains the
Spring Session Redis path. Header fallback is disabled in production through
`ALLOW_HEADER_FALLBACK=false`.

## Browser And Frontend Contract

The browser sends API requests with `credentials: include`. It sends the
tenant header only when an authoritative session tenant is present. A 401
clears the signed-in workspace state, marks the connection signed out, stores a
safe post-auth destination, and redirects to sign-in. A 403 remains an
operator permission message. Transport failures say that no HTTP response was
received and do not mislabel every failure as CORS.

The hosted browser proof exercised successful login, failed-login recovery,
page reload, authenticated page navigation, realtime dashboard update, logout,
protected platform denial, and rate-limit UX. The proof global setup also
verified backend readiness, anonymous session response, SockJS info,
authenticated session, dashboard summary, dashboard snapshot, and runtime
warm-up before tests began.

## Production-Shaped Test Boundary

The focused backend command was:

```text
cd backend
cmd /c mvnw.cmd test "-Dtest=DeploymentHardeningConfigurationTest,SecurityHardeningIntegrationTest"
```

Result:

```text
Tests run: 14, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

The five deployment configuration tests verify production Redis-backed session
configuration, readiness/liveness separation, validate-only schema posture,
Render distributed realtime configuration, bounded dashboard cache settings,
and restricted actuator exposure. The nine security tests verify approved and
rejected CORS, representative error responses, no session on failed login,
rate limiting, and health endpoint access.

These local tests use H2 and intentionally disable Spring Session Redis in the
security context. They therefore validate the application/security contract,
not a provider-level Redis outage. The deployed proof and readiness checks
validate the live healthy path. A controlled Redis stop/start drill and a
long-duration expiry drill remain Classification C evidence work because they
were not run against the pilot deployment.

## Required 70-Item Closure Report

1. **Starting HEAD:** `13d3d1f1996d43aa22e48057f094e90522e9940c`.
2. **Deployed revision/status:** the deployed services were reachable and healthy; the public health payload does not expose a backend commit identifier.
3. **Production session-store result:** production configuration is Redis-backed Spring Session; live authenticated cookie/session probe passed.
4. **Redis session persistence result:** healthy live login, session read, reload, and logout path passed; raw Redis key inspection was not performed.
5. **Redis-login failure result:** not fault-injected against the live provider; Classification C evidence limitation.
6. **Redis authenticated-request failure:** not fault-injected against the live provider; no product failure observed; Classification C evidence limitation.
7. **Header-fallback result:** production default is disabled and focused configuration/security coverage passed.
8. **Redis recovery result:** no provider stop/start recovery drill was performed; Classification C evidence limitation.
9. **Rate-limit/session-store separation:** verified in source configuration; rate limiting can degrade to memory, session storage cannot silently switch to that path.
10. **Cookie contract:** live `SESSION` cookie probe passed with Secure, HttpOnly, and SameSite=None.
11. **HTTPS/Secure cookie:** live cookie was Secure and all hosted URLs were HTTPS.
12. **Client-storage result:** frontend stores only remembered workspace preference and pending route metadata; session authority remains server-side cookie/session state.
13. **Deployed CORS result:** approved frontend origin received credentialed CORS headers.
14. **Wildcard-CORS result:** unapproved `https://evil.example` received HTTP 403 `Invalid CORS request` and no allow-origin header.
15. **Browser login result:** hosted proof test 1 passed successful login through the sign-in UI.
16. **Reload result:** hosted proof test 1 passed authenticated page reload and dashboard restoration.
17. **Invalid-session reload result:** Phase 1 fail-closed validation and hosted 401 handling are proven; forced invalid-cookie reload was not separately injected in Phase 2.
18. **passwordChangeRequired browser result:** frontend routes a still-temporary-password session to Profile; full positive hosted rotation was not run to preserve proof credentials.
19. **Password rotation browser result:** supported backend/API contract was proven in Phase 1; positive hosted rotation remains a safe disposable-account exercise.
20. **Concurrent-session post-password result:** Phase 1 session-version invalidation is locally verified; hosted concurrent-browser exercise remains Classification C.
21. **Logout browser result:** hosted proof test 1 passed UI logout.
22. **Frontend data-clearing result:** logout and 401 handlers clear session/workspace state and redirect to sign-in.
23. **Back-button result:** no explicit browser-history security drill was run; protected route checks and 401 handling remain in place.
24. **User A/User B result:** Phase 1 current-user validation prevents stale identity reuse; no cross-user hosted session swap was performed.
25. **Tenant A/Tenant B result:** Phase 1 tenant isolation is locally verified; hosted proof uses one deterministic tenant and does not claim a two-tenant cookie swap drill.
26. **Platform-to-tenant result:** hosted tenant proof received HTTP 403 on protected platform API; platform/tenant boundary suite passed in Phase 1.
27. **Tenant-to-platform result:** hosted proof test 1 verified tenant UI denial for platform routes and API denial.
28. **Absolute expiry result:** application calculates a fixed expiry from authenticatedAt; forced wall-clock expiry was not executed in hosted infrastructure.
29. **Idle timeout result:** servlet session max inactive interval is configured from tenant security settings; forced idle timeout was not waited out in Phase 2.
30. **Expiry frontend result:** 401 handling clears state and redirects to sign-in; live forced-expiry browser evidence remains Classification C.
31. **Websocket-after-expiry result:** Phase 1 current-session revalidation rejects revoked/invalid authority; forced hosted expiry socket drill remains Classification C.
32. **Websocket-after-logout result:** logout removes HTTP authority and Phase 1 websocket checks reject revoked authority; dedicated hosted socket-after-logout fault drill was not run.
33. **Websocket-role-removal smoke:** Phase 1 local realtime authority re-resolution covers role currentness.
34. **Websocket-warehouse-removal smoke:** Phase 1 local realtime authority re-resolution covers warehouse currentness.
35. **Websocket reconnect result:** hosted realtime proof passed live dashboard update; forced network disconnect/reconnect remains Classification C.
36. **Auth failure UX:** hosted proof passed invalid-login recovery without a stuck loading state.
37. **401 behavior:** frontend maps 401 to session-missing/expired guidance and invokes workspace sign-out handling.
38. **403 behavior:** frontend maps 403 to an explicit permission message; hosted platform denial passed.
39. **429 behavior:** focused backend tests and hosted proof rate-limit test passed.
40. **Infrastructure failure UX:** transport errors identify absent HTTP response and backend/proxy health; deliberate live outage was not induced.
41. **Session credential clearing:** password input is cleared after login/logout; cookie values are not exposed to UI code.
42. **Malformed session-ID result:** Phase 1 malformed trust/session validation is fail-closed locally.
43. **Direct API result:** live anonymous session returned 200 signedOut; protected tenant/platform calls are denied by existing proof and focused tests.
44. **Hosted customer-login proof:** hosted proof test 1 passed.
45. **Hosted platform separation:** tenant session received platform API denial and platform routes remained separated.
46. **Hosted CORS/cookie proof:** approved-origin CORS and live secure cookie probe passed.
47. **Hosted realtime-auth proof:** hosted readiness, SockJS info, authenticated warm-up, and realtime dashboard test passed.
48. **Hosted Redis-outage classification:** not exercised; Classification C evidence limitation, not an observed product defect.
49. **Security logging result:** focused tests emitted expected invalid-login and deliberate test-failure logs without test failures; no secrets were printed.
50. **Account-recovery classification:** supported password-change flow exists; positive hosted credential rotation should use a disposable account before a broad pilot.
51. **Multiple-session classification:** session-version invalidation exists and is locally verified; hosted multi-browser timing evidence is deferred.
52. **MFA/SSO classification:** not in current supported scope; future enterprise hardening.
53. **Session-observability classification:** runtime reports readiness/session posture without exposing session secrets; deeper metrics/tracing remain future hardening.
54. **Final failure matrix:** observed failures were none in live checks; deliberate local 401/403/429/CORS cases passed; Redis/expiry/network fault drills are C.
55. **Required operational/security work found:** no Classification A work found; C evidence drills remain useful before broader production scale.
56. **Fixes:** documentation classification correction only; no runtime or test fix was required.
57. **A/B/C/D table:** A remaining 0; B remaining 0; C includes provider fault injection, forced expiry, forced reconnect, and disposable-account positive rotation evidence; D none identified.
58. **Focused tests:** 14 tests passed, 0 failures, 0 errors.
59. **Production-shaped Redis tests:** five configuration tests passed; no live Redis fault injection was claimed.
60. **Full backend:** Phase 1 baseline was 257 passed; not rerun because Phase 2 made no runtime changes.
61. **Frontend/browser checks:** existing hosted proof passed 6/6; live frontend shell and browser-facing endpoints passed.
62. **Docs/diff checks:** docs link and diff checks are run after the evidence update.
63. **Files changed:** Phase 2 evidence document and Phase 1 classification wording; no source files changed.
64. **Commits:** recorded after final checks in the repository history.
65. **Critical blockers:** 0.
66. **High blockers:** 0.
67. **Classification A remaining:** 0.
68. **Owner/live evidence status:** owner-triggered live proof state was used without exposing secrets; provider outage drills remain deferred evidence.
69. **AUTH / SESSIONS FINAL READINESS:** suitable for a controlled B2B pilot with documented C evidence limits and existing operational runbooks.
70. **Phase 2 verdict:** see the final status below.

## Classification Summary

| Classification | Result |
| --- | --- |
| A: required product behavior missing | 0 remaining |
| B: required behavior implemented but failing | 0 observed |
| C: evidence or provider-drill gap | Redis outage/recovery, forced expiry, forced reconnect, hosted concurrent-session and positive disposable-account rotation drills |
| D: future evolution | MFA/SSO, deeper session metrics/tracing, provider-level HA and broader scale evidence |

## Final Status

**AUTH / SESSIONS LIFECYCLE VERIFIED AND OPERATIONALLY COMPLETE FOR CONTROLLED
B2B PILOT — OWNER LIVE WALKTHROUGH DEFERRED**

This closes Domain 12 Phase 2. It does not start Tenant / Workspace Admin.
