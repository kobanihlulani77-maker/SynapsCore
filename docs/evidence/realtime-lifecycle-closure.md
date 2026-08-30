# Realtime Lifecycle Closure Evidence

## 1. Scope and Starting Point

This bounded Domain 17 cycle covers the current SockJS/STOMP realtime path,
tenant and warehouse authority enforcement, frontend convergence, and proof
classification. It does not reopen the closed platform-owner, tenant, scenario,
warehouse, activity/audit, or runtime domains. It does not introduce a durable
event log, event versioning, distributed workers, a new broker, Kubernetes, or
high-availability infrastructure.

Starting repository HEAD immediately before this cycle:

`ef3a514fe7ba00967c31e6b1b831bc1df8dfa377`

Unrelated worktree changes present before this cycle remain outside the change:

- `frontend/Dockerfile`
- `.gitattributes`
- `docs/evidence/scenario-lifecycle-phase-0-authority-census.md`
- `docs/evidence/scenario-lifecycle-phase-10-source-observation.md`

## 2. Realtime Contract

SynapseCore uses REST and persisted database state as the authoritative source.
SockJS/STOMP is a delivery and freshness channel, not an authority channel.
The backend publishes tenant-prefixed topics from committed operational state;
the frontend merges valid messages and can re-read the authoritative snapshot.

### Server-to-client flow

```text
committed mutation
  -> operational dispatch work item / event
  -> RealtimeService
  -> local broker or Redis Pub/Sub
  -> tenant-prefixed STOMP topic
  -> current-authority outbound interceptor
  -> authorized recipient only
  -> frontend merge
  -> REST reconciliation if needed
```

### Client-to-server flow

```text
browser CONNECT
  -> HTTP session and current auth validation
  -> server binds tenant, roles, and warehouse scope
  -> browser SUBSCRIBE to one known tenant topic
  -> server revalidates authority at subscription time
  -> server revalidates authority at delivery time

browser SEND
  -> always rejected
  -> no websocket command handler exists in the current product contract
```

## 3. Focused Changes

### 3.1 Client STOMP SEND is closed

The server now rejects every client `SEND`, including a known tenant topic,
unknown topic, cross-tenant topic, and application destination. This is a
server-side rule and does not depend on hidden frontend controls. Current
application behavior has no legitimate browser-to-broker command handler.

### 3.2 Unknown destinations fail closed

Subscriptions and messages must remain under the signed-in tenant prefix and
must use the explicit known tenant topic suffix set in
`WebSocketConfig.TenantSubscriptionChannelInterceptor`. A random topic,
platform-looking topic, application destination, or unknown tenant topic is
rejected for inbound subscription traffic. Unknown or unauthorized outbound
messages are dropped for that recipient.

### 3.3 Outbound isolation is recipient-local

An unauthorized outbound `MESSAGE` returns `null` for the unauthorized
recipient rather than throwing an exception that can interfere with delivery
to authorized subscribers. This preserves the existing tenant and warehouse
scope policy while preventing one unauthorized subscription from blocking the
same broadcast for an authorized recipient.

### 3.4 Frontend convergence and malformed payload safety

The workspace realtime hook now:

- parses message bodies through one safe JSON wrapper;
- ignores malformed payloads without throwing through the message callback;
- records bounded diagnostic metadata (topic, size, error text, timestamp), not
  the raw payload;
- requests an authoritative snapshot after malformed input;
- refreshes the authoritative snapshot every 60 seconds while the socket is
  live;
- stops the live reconciliation timer when transport leaves the live state or
  the hook is disposed;
- retains the existing 15-second degraded refresh behavior when live delivery
  is unavailable.

This is bounded convergence, not a guarantee that every transient websocket
message is replayed. The database and REST snapshot remain the source of truth.

### 3.5 Hosted-render correction discovered during closure

The first post-deployment hosted proof exposed a separate shared-context render
defect before the realtime assertions could run: the Settings context referenced
workspace warehouse state and administration handlers that were not extracted
from the existing state/action objects. A clean browser reproduced a blank
screen with a `ReferenceError`; this was not hidden or worked around in proof.

The smallest corrections restored the existing state/action wiring in
`useWorkspacePageContexts.js`. The deployed bundle then rendered the sign-in
shell correctly, and the subsequent clean hosted run passed all six tests.

The hosted Alert-page attempt also exposed an older stale-topic race: a queued
realtime alert payload could overwrite a newer REST snapshot with an older
recommended action. Tenant-wide alert and recommendation topics now trigger the
existing authoritative REST convergence path without directly merging the
topic payload. This preserves the realtime notification while keeping REST as
the current-state authority.

## 4. Authority and Topic Policy

The known tenant suffixes are:

```text
/dashboard.summary
/alerts
/alerts.changed
/recommendations
/recommendations.changed
/inventory
/fulfillment.overview
/orders.recent
/events.recent
/audit.recent
/system.incidents
/integrations.connectors
/integrations.imports
/integrations.replay
/integrations.changed
/scenarios.notifications
/scenarios.escalated
```

The current policy remains:

- the authenticated HTTP session establishes the websocket identity;
- tenant code is normalized and bound to the websocket session;
- a destination outside the current tenant prefix is denied;
- a destination inside the tenant prefix but outside the known set is denied;
- integration topics require `INTEGRATION_ADMIN` or
  `INTEGRATION_OPERATOR`;
- raw tenant-wide operational topics are not available to warehouse-scoped
  sessions;
- scoped operators continue to use filtered REST data and permitted scoped
  delivery only;
- current session, active-user, active-operator, tenant-policy, role, and
  warehouse scope state is revalidated for relevant websocket traffic;
- logout, expiry, disabled-user state, role removal, and scope removal remain
  authority transitions handled by the existing session/authority model.

No client-provided tenant header or websocket frame can grant authority.

## 5. Evidence Matrix

The following numbered record is the complete bounded-cycle closure checklist.
Each result distinguishes direct focused evidence from existing or deferred
operational evidence. No secrets are recorded.

1. **Starting HEAD:** `ef3a514fe7ba00967c31e6b1b831bc1df8dfa377`.
2. **Final contract:** REST/database state is authoritative; STOMP delivers tenant-scoped freshness.
3. **Client SEND:** rejected server-side for known tenant destinations.
4. **SEND fix:** unconditional `SEND` rejection is implemented in the inbound interceptor.
5. **Unknown destination:** rejected rather than passed through.
6. **Known destination policy:** explicit tenant suffix allow-list is enforced.
7. **Tenant isolation:** cross-tenant destinations remain denied.
8. **Platform separation:** platform-looking destinations are not tenant destinations and are denied by the tenant interceptor.
9. **Anonymous handshake:** existing HTTP-session/authentication handshake remains required.
10. **Tenant-wide subscription:** known tenant topics remain available to authorized tenant-wide sessions.
11. **Scoped raw denial:** warehouse-scoped sessions cannot subscribe to tenant-wide raw operational topics.
12. **Integration authority:** integration topics remain limited to integration roles.
13. **Outbound recipient isolation:** unauthorized recipient delivery is dropped locally; authorized recipients still receive the message.
14. **Logout while connected:** existing current-session revalidation denies later protected websocket traffic after logout.
15. **Session expiry:** existing session validation prevents continued authority after expiry.
16. **Disabled user:** existing session validation denies disabled-user authority.
17. **Role removal:** current authority is re-read before relevant websocket access and does not rely only on CONNECT-time roles.
18. **Scope removal:** current warehouse scope is revalidated for relevant access.
19. **Final-scope removal:** a user with no remaining permitted scope cannot regain access from stale websocket attributes.
20. **Role/scope addition:** new authority is evaluated through the current session model; it is not granted by a client frame.
21. **Tenant/user switch:** the websocket remains bound to its authenticated session tenant; a client cannot switch it with a destination.
22. **Connection cleanup:** existing disconnect handling removes tracked session bindings.
23. **Registry cleanup:** the current registry cleanup behavior remains bounded to disconnect lifecycle; long-lived registry/TTL hardening is future work.
24. **Reconnect:** frontend transport reconnect behavior remains enabled with the existing bounded delay.
25. **Reconnect after logout:** reconnect cannot establish protected authority from a logged-out session.
26. **Missed live message:** a missed message is not treated as durable replay; REST refresh is the recovery source.
27. **Automatic live reconciliation:** frontend requests an authoritative snapshot every 60 seconds while connection mode is live.
28. **Duplicate message:** existing merge/refresh behavior remains idempotent at the UI snapshot boundary; no duplicate-event protocol was introduced.
29. **Out-of-order message:** REST remains authoritative; no ordering guarantee is claimed for the current broker path.
30. **REST/realtime race:** a REST snapshot can correct a stale or raced websocket merge.
31. **Malformed payload:** safe parser ignores malformed JSON, records bounded diagnostics, and requests snapshot recovery.
32. **Payload secret safety:** malformed diagnostics do not include the raw body; no secret is intentionally logged by the wrapper.
33. **Message size:** size is recorded as metadata only; a separate hard payload-size protocol is future hardening.
34. **Redis loopback:** existing publisher origin metadata prevents the publishing node from re-delivering its own Redis event.
35. **Redis outage:** live cross-node delivery can degrade; REST and committed database state remain authoritative.
36. **Redis recovery:** reconnect/reconciliation behavior is bounded by the existing transport and snapshot paths; durable replay is not claimed.
37. **Server restart:** clients reconnect through the existing SockJS/STOMP lifecycle and refresh from REST.
38. **No-realtime mode:** degraded refresh is the existing fallback; the UI must not present degraded transport as live.
39. **Refresh coalescing:** existing decision-surface refresh coalescing remains in place.
40. **Burst traffic:** no new burst protocol was introduced; bounded executor/dispatch behavior remains the current capacity boundary.
41. **Activity/Audit parity:** realtime does not create a second authority or historical record; existing REST/activity paths remain authoritative.
42. **Alerts parity:** alert payload delivery does not bypass the existing tenant/role rules.
43. **Recommendations parity:** recommendation payload delivery does not bypass the existing tenant/role rules.
44. **Integration/Replay parity:** integration and replay topics retain integration-role and tenant checks.
45. **Dashboard seam:** dashboard summary is a known tenant topic and remains reconciled from REST.
46. **Scenario seam:** scenario notification/escalation topics are known tenant topics and remain subject to current authority.
47. **Runtime seam:** system incident delivery is a known tenant topic; runtime REST state remains authoritative.
48. **After-commit behavior:** existing operational dispatch is initiated from committed application state.
49. **Rollback behavior:** failed transactions do not become authoritative realtime business state through the existing transaction boundary.
50. **Dispatch failure recovery:** current failed dispatch behavior remains visible through existing operational evidence; automatic durable retry is not claimed.
51. **Delivery semantics:** current path is best-effort live delivery plus REST convergence, not exactly-once delivery.
52. **Ordering semantics:** no global ordering guarantee is claimed across nodes or reconnects.
53. **Production defects:** the bounded source/test review found no new backend product defect; the hosted run exposed one existing frontend render seam and one stale-topic convergence seam.
54. **Frontend defects:** malformed callback handling, permanent-live staleness, missing workspace context wiring, and stale tenant-wide alert/recommendation merges were addressed at the smallest frontend seams.
55. **Security defects:** SEND fail-open and unknown destination pass-through were closed; outbound unauthorized recipient throwing was replaced with per-recipient suppression.
56. **Fix set:** `WebSocketConfig.java`, `WebSocketAccessBoundaryTest.java`, `useWorkspaceRealtime.js`, and the minimal existing context wiring in `useWorkspacePageContexts.js` are the intended implementation files.
57. **Classification:** no known Classification A blocker; residual evidence items are Classification C unless a later live run demonstrates otherwise.
58. **Focused backend result:** `WebSocketAccessBoundaryTest`: `10` tests, `0` failures, `0` errors.
59. **Full backend result:** `268` tests, `0` failures, `0` errors, `BUILD SUCCESS`.
60. **Frontend verification:** `npm.cmd run verify` passed, including lint/check, source checks, proof-label checks, and Vite build; `npm.cmd run build` passed.
61. **Documentation/diff checks:** `git diff --check` passed; docs link check passed after the evidence document was added.
62. **CI result:** no new CI workflow was introduced in this bounded cycle.
63. **Hosted proof:** after deployment and `PROOF_ALLOWED=True`, the complete existing hosted proof passed `6/6` in `3.6m` on the final deployed frontend bundle.
64. **Hosted failure classification:** the first attempt failed on the missing workspace context state wiring; after the first correction it exposed the paired missing action wiring; after those were corrected, the Alert-page mismatch was classified as a stale realtime payload/convergence defect and fixed by REST-authoritative topic handling. No proof assertion was weakened.
65. **Deployment commits:** `a187a37` realtime boundary/convergence, `8301cd4` unauthenticated render state wiring, `de89b0e` paired setter wiring, `2c0a727` workspace administration handler wiring, and `562897c` REST-authoritative feed convergence.
66. **Critical blockers:** `0` after focused, full, and hosted verification.
67. **High blockers:** `0` after focused, full, and hosted verification.
68. **Classification A remaining:** `0` currently identified.
69. **Owner/live evidence:** owner walkthrough is not required for this bounded technical closure; the hosted browser/API proof supplied the required technical live evidence.
70. **Realtime final readiness:** final deployed connection check and hosted proof are green; unsupported exactly-once, ordering, and durable websocket replay guarantees remain explicitly unclaimed.
71. **Final verdict:** `REALTIME LIFECYCLE VERIFIED AND OPERATIONALLY COMPLETE FOR CONTROLLED B2B PILOT — OWNER LIVE WALKTHROUGH DEFERRED`.

## 6. Test Details

### Focused backend test

```text
cd backend
cmd /c mvnw.cmd -Dtest=com.synapsecore.config.WebSocketAccessBoundaryTest test
Tests run: 10, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

The added assertions cover:

- known and unknown client `SEND` rejection;
- unknown and non-tenant subscription rejection;
- unauthorized outbound recipient suppression without blocking an authorized
  recipient.

### Full backend suite

```text
cd backend
cmd /c mvnw.cmd test
Tests run: 268, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Expected test logging includes intentional invalid-login, disabled-connector,
missing-product, CORS test-controller, and Redis-fallback warnings. They were
asserted test paths, not suite failures.

### Frontend checks

```text
cd frontend
npm.cmd run verify
npm.cmd run build
```

Both completed successfully after the realtime hook changes.

The clean-browser route smoke check also confirmed that an unauthenticated
`/dashboard` request redirects to `/sign-in`, renders the sign-in shell, and
produces no console or page errors on the final deployed bundle.

## 7. Hosted Validation Gate

The code change affects the backend websocket interceptor and the frontend
realtime hook, so hosted validation is required after the intended commit is
deployed. The safe order is:

```text
git push origin main
wait for the Render backend and frontend revisions to deploy
powershell -ExecutionPolicy Bypass -File scripts\check-live-connections.ps1
```

Continue only when the connection check reports:

```text
FRONTEND_UP=True
BACKEND_UP=True
DB_READY=True
AUTH_READY=True
WS_READY=True
PROOF_ALLOWED=True
```

Then use the existing ignored proof state and supported preparation flow:

```text
powershell -ExecutionPolicy Bypass -File scripts\prepare-hosted-proof.ps1
cd frontend
npm.cmd run test:e2e:prod
```

Do not print or commit proof passwords, tokens, state files, reports containing
secrets, or local environment files. Do not run hosted proof while readiness,
authentication, websocket, or database health is unavailable.

Hosted proof result on the final deployed bundle:

```text
6 passed (3.6m)
1. auth flow and the full authenticated page system render cleanly in a browser
2. product catalog onboarding works through tenant-scoped API and browser surface
3. dashboard summary updates live without a browser refresh
4. replay recovery, scenario approval, execution, and browser role gating work through the UI
5. alerts, recommendations, orders, inventory, integrations, users, profile, and settings stay connected
6. frontend surfaces backend auth rate limiting without getting stuck in a loading state
```

The final run produced no test failure, skipped test, browser console error,
page error, or failed resource. The only process warnings were Node notices
that `NO_COLOR` was ignored because `FORCE_COLOR` was set. Hosted proof
confirms the existing supported product lanes and the dashboard realtime update
path. It does not prove exactly-once delivery, total event ordering,
multi-region failover, durable websocket replay, or arbitrary custom STOMP
destinations.

## 8. Residual Limitations

These are not silently promoted to blockers:

- the current realtime registry is lifecycle-cleaned on disconnect but does not
  have a separately proven TTL/size policy;
- Redis Pub/Sub is not a durable message queue;
- missed messages are repaired by REST convergence rather than replayed from a
  websocket offset;
- delivery ordering and exactly-once semantics are not claimed;
- the 60-second live reconciliation bounds stale state but does not eliminate
  the interval;
- durable dispatch retry and multi-node scheduler coordination remain outside
  this cycle;
- hosted proof uses the existing ignored proof state; preparation that requires
  a private bootstrap/platform-admin token remains operator-gated and does not
  change the technical result when an already prepared proof state is valid.

## 9. Closure Rule

The bounded cycle may be closed as technically verified only after the intended
revision is pushed, deployed, connection-gated, and the existing hosted proof
passes without unrelated failures. If any hosted failure occurs, classify it as
fixture defect, proof expectation drift, product defect, or deployment/
environment issue before changing anything.

Final closure commits for this bounded cycle:

- `a187a37` — `Harden realtime delivery and convergence`
- `8301cd4` — `Fix unauthenticated workspace render`
- `de89b0e` — `Complete workspace context state wiring`
- `2c0a727` — `Wire workspace administration actions`
- `562897c` — `Prefer REST convergence for realtime feeds`

Required final verdict after the clean hosted run:

`REALTIME LIFECYCLE VERIFIED AND OPERATIONALLY COMPLETE FOR CONTROLLED B2B PILOT — OWNER LIVE WALKTHROUGH DEFERRED`
