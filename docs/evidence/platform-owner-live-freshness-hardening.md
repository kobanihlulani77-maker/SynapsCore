# Platform Owner Live Freshness Hardening

Status: implemented locally; hosted two-browser acceptance remains a required manual gate.

## Scope

This correction closes the stale-control-plane gap identified during Owner Manual Acceptance. An already-open Platform Owner browser can now learn that safe platform metadata changed without a manual page reload.

The signal is an invalidation notification, not a pushed data feed:

```text
authoritative audit/business persistence
        -> after-commit safe platform notification
        -> Platform Owner-only STOMP subscription
        -> authenticated REST refresh
        -> /api/platform/activity and /api/platform/overview
```

REST remains authoritative. The platform WebSocket carries only a minimal metadata signal and never carries customer orders, inventory values, inbound payloads, replay payloads, connector secrets, credentials, passwords, tokens, or audit details.

## Implemented Behavior

### Safe evidence notification

The platform topic is `/topic/platform/activity.changed`. Its payload is limited to:

```json
{
  "type": "PLATFORM_ACTIVITY_CHANGED",
  "occurredAt": "<instant>",
  "scope": "PLATFORM"
}
```

The notification is emitted after an authoritative audit/business write. Broker delivery failure is logged and does not undo committed truth; REST reconciliation remains the recovery path.

The central audit persistence writer is used for successful tenant login evidence and Platform Owner login/logout evidence. Successful tenant login records the tenant code, actor, action, source, target, status, request ID, and safe details only. No session identifier, cookie, authorization header, password, password hash, or token is persisted.

### Explicit realtime authority

The existing `/ws` handshake accepts either:

- a current tenant session, marked `TENANT` and retaining the existing tenant topic and warehouse-scope rules; or
- a current Platform Owner session, marked `PLATFORM` with no tenant identity or tenant role.

Only a current Platform Owner session may subscribe to `/topic/platform/activity.changed`. Tenant, anonymous, expired, replaced, or otherwise invalidated platform authority is rejected. Client STOMP `SEND` remains denied.

### Frontend convergence

The Platform Owner application uses a focused realtime hook. On a valid platform change signal it coalesces refresh work and reloads `/api/platform/activity` and `/api/platform/overview`; it does not append arbitrary WebSocket payloads to React state.

When the connection is live, REST reconciliation runs every 60 seconds. When the connection is degraded or reconnecting, reconciliation runs every 15 seconds. Malformed notifications are ignored, and the platform activity surface exposes the current realtime state without changing the existing page composition.

## Local Evidence

Focused backend proof:

```text
WebSocketAccessBoundaryTest: 13 tests, 0 failures, 0 errors
PlatformRealtimeNotificationServiceTest: 2 tests, 0 failures, 0 errors
Focused total: 15 tests, 0 failures, 0 errors
```

The focused proof covers safe payload publication, broker-failure isolation, Platform Owner subscription, tenant and anonymous rejection, and rejection after platform-session invalidation.

Full backend verification:

```text
285 tests, 0 failures, 0 errors
```

Frontend verification completed with the launch-readiness check and production build:

```text
Frontend launch-readiness check passed.
vite production build passed.
```

Expected negative-path database constraint messages remain present in existing concurrency tests; their assertions passed and they are not failures in this change.

## Remaining Manual Acceptance

Do not treat local tests as hosted proof. After deployment, the authorized manual acceptance should use two browser sessions:

1. Keep Browser A signed in as Platform Owner on Platform Activity.
2. In Browser B, sign in successfully as a legitimate tenant operator.
3. Confirm Browser A receives the safe platform signal and refreshes activity without F5.
4. Confirm Platform Overview and other metadata-backed surfaces converge through REST.
5. Confirm no customer business payload is visible in the platform stream.
6. Confirm logout, session replacement, and expiry prevent further platform delivery.

Hosted proof is intentionally not run by this local implementation pass. It should resume only after deployment and readiness checks are healthy, using the repository's existing proof preparation and evidence rules.

## Files in This Change

- `backend/src/main/java/com/synapsecore/audit/AuditLogPersistenceService.java`
- `backend/src/main/java/com/synapsecore/audit/AuditLogService.java`
- `backend/src/main/java/com/synapsecore/auth/AuthSessionService.java`
- `backend/src/main/java/com/synapsecore/config/WebSocketConfig.java`
- `backend/src/main/java/com/synapsecore/event/BusinessEventService.java`
- `backend/src/main/java/com/synapsecore/platform/PlatformActivityChangedNotification.java`
- `backend/src/main/java/com/synapsecore/platform/PlatformMetadataChangedEvent.java`
- `backend/src/main/java/com/synapsecore/platform/PlatformOwnerSessionService.java`
- `backend/src/main/java/com/synapsecore/realtime/PlatformRealtimeNotificationService.java`
- `backend/src/test/java/com/synapsecore/config/WebSocketAccessBoundaryTest.java`
- `backend/src/test/java/com/synapsecore/realtime/PlatformRealtimeNotificationServiceTest.java`
- `frontend/src/components/PlatformApplication.jsx`
- `frontend/src/hooks/usePlatformRealtime.js`
- `frontend/src/pages/PlatformActivity.jsx`
