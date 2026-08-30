# Auth / Sessions Phase 1 Evidence

## Scope

This evidence record covers Domain 12 Phase 1: session trust, authority
currentness, and invalidation. It does not reopen completed product domains and
does not cover the later Redis, browser, CORS, or hosted-proof phases.

Starting repository revision: `4ba4a1cb1e2db1eed0ee4258b8b43c51546c4dc8`.

## Session Trust Contract

An authenticated workspace session is trusted only when its persisted identity
and current database state agree. The validation path now requires:

- a present tenant and username;
- a valid, non-future `authenticatedAt` value;
- a matching active user in the same tenant;
- a matching active tenant;
- a matching user `sessionVersion`;
- a matching tenant security-policy version; and
- an identity that has not been disabled or otherwise invalidated.

Malformed, missing, or future trust attributes invalidate the session rather
than receiving a new implicit timestamp. Invalid sessions fail closed and must
sign in again.

## Temporary Password Contract

Users created with `passwordChangeRequired=true` may inspect the session, change
their password through the supported password endpoint, or sign out. Shared
authenticated workspace actions are blocked until the password is rotated.
Successful rotation clears the temporary-password requirement and establishes
a fresh authenticated session state.

The frontend routes a successful login that still requires rotation to the
Profile surface instead of the operational dashboard. This changes navigation
only; it does not change authentication or authorization rules.

## Realtime Currentness Contract

The websocket handshake records the originating HTTP session. On CONNECT and
before tenant-scoped SUBSCRIBE, SEND, or MESSAGE processing, the backend
re-resolves the current HTTP session and refreshes tenant, role, and warehouse
authority. If the session is revoked, the realtime operation is rejected and
revoked outbound messages are dropped.

This prevents a websocket from retaining handshake-time authority after a user
is disabled, a role is removed, or a warehouse scope changes. Existing tenant,
role, topic, and warehouse checks remain in force.

## Focused Verification

Command run from `backend`:

```text
cmd /c mvnw.cmd test "-Dtest=SecurityVerificationIntegrationTest,PlatformTenantAccessBoundaryIntegrationTest,WebSocketAccessBoundaryTest"
```

Result:

```text
Tests run: 47, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

The focused coverage includes:

- malformed, missing, and future session trust timestamps;
- malformed session-version trust;
- temporary-password restriction and supported password rotation;
- logout identity clearing;
- tenant and role authorization boundaries;
- disabled-user and changed-scope invalidation behavior;
- tenant-wide versus warehouse-scoped authority;
- cross-tenant and restricted realtime destinations;
- current HTTP-session authority before realtime use; and
- rejection of realtime use after HTTP-session authority revocation.

The existing platform/tenant boundary suite also retains coverage for role
session separation, warehouse filtering, platform isolation, scenario
governance, replay visibility, and runtime/activity boundaries.

## Classification

| Area | Classification | Evidence |
| --- | --- | --- |
| Session trust validation | A: implemented and locally verified | `SecurityVerificationIntegrationTest` passed, including malformed trust cases |
| Temporary-password restriction | A: implemented and locally verified | password-rotation test passed; role fixture now rotates through the supported API |
| Current role and warehouse authority | A: implemented and locally verified | platform/tenant boundary suite passed; websocket interceptor re-resolves authority |
| Realtime revocation behavior | A: implemented and locally verified | `WebSocketAccessBoundaryTest` passed with current-session and revoked-session cases |
| Redis-backed session behavior | C: deferred verification | requires the deployed/runtime Redis environment and later phase scope |
| Browser password-rotation UX | C: deferred verification | frontend routing is changed; browser proof belongs to the later frontend/hosted phase |
| Hosted CORS and deployed websocket behavior | C: deferred verification | no hosted proof was run in this phase by design |

## Files Changed For This Phase

- `backend/src/main/java/com/synapsecore/auth/AuthSessionService.java`
- `backend/src/main/java/com/synapsecore/config/WebSocketConfig.java`
- `frontend/src/hooks/useWorkspaceSessionActions.js`
- `backend/src/test/java/com/synapsecore/SecurityVerificationIntegrationTest.java`
- `backend/src/test/java/com/synapsecore/PlatformTenantAccessBoundaryIntegrationTest.java`
- `backend/src/test/java/com/synapsecore/config/WebSocketAccessBoundaryTest.java`

The unrelated local changes to `frontend/Dockerfile`, `.gitattributes`, and the
two pre-existing scenario evidence files remain outside this phase.

## Limitations And Next Gate

This phase proves the application-layer session validator and websocket
currentness behavior against the local Spring test context. It does not claim
that Redis failover, browser-level cookie behavior, deployed CORS, or hosted
realtime behavior have been re-proven here. Those checks remain subsequent
verification work. No hosted proof was run because this phase introduced no
hosted deployment change and explicitly stops before the later phases.
