# Render Deployment Guide

This guide reflects the current live Render setup for SynapseCore and the operational realities that matter for hosted proof.

## Live Render Topology

Defined in `render.yaml`:

- backend web service: `synapscore-3`
- frontend static site: `synapscore-frontend-3`
- managed Postgres: `synapscore-postgres`
- managed Redis: `synapscore-redis`

Live URLs:

- frontend: `https://synapscore-frontend-3.onrender.com`
- backend: `https://synapscore-3.onrender.com`
- backend liveness: `https://synapscore-3.onrender.com/actuator/health/liveness`
- backend readiness: `https://synapscore-3.onrender.com/actuator/health/readiness`

## Backend Render Environment

Required keys:

```text
SPRING_PROFILES_ACTIVE=prod
SERVER_ADDRESS=0.0.0.0
DATABASE_URL=<Render Postgres internal connection string>
SPRING_DATA_REDIS_URL=<Render Redis internal connection string>
SPRING_SESSION_REDIS_NAMESPACE=synapsecore:sessions
CORS_ALLOWED_ORIGINS=https://synapscore-frontend-3.onrender.com
SESSION_COOKIE_SECURE=true
SESSION_COOKIE_SAME_SITE=None
ALLOW_HEADER_FALLBACK=false
SYNAPSECORE_REALTIME_BROKER_MODE=REDIS_PUBSUB
SYNAPSECORE_INTEGRATION_PULL_WORKER_ENABLED=true
SYNAPSECORE_INTEGRATION_PULL_WORKER_INTERVAL_MS=60000
SYNAPSECORE_INTEGRATION_PULL_WORKER_BATCH_SIZE=10
SPRING_JPA_HIBERNATE_DDL_AUTO=validate
PUBLIC_APP_URL=https://synapscore-frontend-3.onrender.com
PUBLIC_API_URL=https://synapscore-3.onrender.com
SYNAPSECORE_BUILD_VERSION=<release-version>
SYNAPSECORE_BUILD_TIME=<utc-timestamp>
```

Optional:

```text
SYNAPSECORE_BOOTSTRAP_INITIAL_TOKEN=<one-time bootstrap token>
SYNAPSECORE_PLATFORM_ADMIN_TOKEN=<production tenant provisioning token>
SYNAPSECORE_ALERT_HOOK_ENABLED=<true|false>
SYNAPSECORE_ALERT_HOOK_WEBHOOK_URL=<operator webhook endpoint>
SYNAPSECORE_RATE_LIMIT_AUTH_LOGIN_MAX_ATTEMPTS=<count per window>
SYNAPSECORE_RATE_LIMIT_AUTH_PASSWORD_MAX_ATTEMPTS=<count per window>
SYNAPSECORE_RATE_LIMIT_TENANT_ONBOARDING_MAX_ATTEMPTS=<count per window>
SYNAPSECORE_RATE_LIMIT_ACCESS_ADMIN_MUTATION_MAX_ATTEMPTS=<count per window>
SYNAPSECORE_RATE_LIMIT_INTEGRATION_MUTATION_MAX_ATTEMPTS=<count per window>
```

Important runtime identity truth:

- the backend runtime build commit can resolve from Render's `RENDER_GIT_COMMIT`
- `/api/system/runtime` is the authoritative place to confirm the live backend fingerprint

## Frontend Render Environment

```text
VITE_API_URL=https://synapscore-3.onrender.com
VITE_WS_URL=https://synapscore-3.onrender.com/ws
VITE_APP_BUILD_VERSION=<release-version>
VITE_APP_BUILD_COMMIT=<release-fingerprint>
VITE_APP_BUILD_TIME=<utc-timestamp>
```

Hosted Render should use the SockJS endpoint URL above, not a raw `wss://.../ws` broker URL.
The frontend uses STOMP over SockJS and allows websocket, xhr-streaming, and xhr-polling transports so realtime can still recover on constrained or slow proxy paths.

The frontend service must keep SPA rewrite routing:

```yaml
routes:
  - type: rewrite
    source: /*
    destination: /index.html
```

## Current Render Scope

These are the truthful current Render boundaries:

- schema startup is validate-only with Flyway-backed baseline coverage
- realtime is broker-backed through `REDIS_PUBSUB`
- authenticated browser sessions are Redis-backed so a backend recycle does not strand signed-in operators
- integration breadth is intentionally narrow

### Supported Integration Breadth On Render

Render deployment currently supports the implemented lanes only:

- webhook order ingestion
- CSV order import
- scheduled pull order ingestion

Do not describe the platform on Render as having broad connector coverage beyond those lanes.

## Replay And Recovery Truth On Render

The current live replay contract is:

- disabled connector CSV imports return structured `CONNECTOR_DISABLED` failed rows
- those failures create replay records immediately
- disabled-connector replay records are manual-only while the connector remains disabled
- automated replay does not steal those proof or operator-owned records
- operators can enable the connector and recover the record intentionally through manual replay

This matters because the hosted proof now depends on that behavior deterministically.

## Root, Runtime, And Health Truth

The backend root `/` should return a safe service-status response.

The production trust surfaces are:

- `/`
- `/actuator/health/liveness`
- `/actuator/health/readiness`
- `/api/system/runtime`
- `/api/system/incidents`
- `/actuator/prometheus`

## Post-Deploy Checks

1. Verify `GET /` returns a safe response.
2. Verify `GET /actuator/health/liveness` is up.
3. Verify `GET /actuator/health/readiness` is up.
4. Verify `GET /api/auth/session` answers cleanly before browser proof starts.
5. Verify `GET /ws/info` answers cleanly before realtime proof starts.
6. Open the frontend URL and verify deep links such as `/sign-in` and `/dashboard`.
7. Verify sign-in loads the tenant directory correctly.
8. Verify session cookies and redirect behavior.
9. Verify dashboard, integrations, runtime, and audit load without CORS failures.
10. Verify a wrong-password `POST /api/auth/session/login` returns a fast `401`.
11. Run hosted proof preparation and browser proof and expect the full Render proof pack to pass.

## Cold Start And Warm-up

Render free instances can be slow on the first request after idle time. Treat hosted proof as a three-step sequence instead of a blind browser run:

1. readiness warm-up
2. hosted proof prep
3. Playwright E2E proof

The repo now supports that sequence directly:

- [prepare-hosted-proof.ps1](../scripts/prepare-hosted-proof.ps1) performs readiness and proof-state preparation
- [prepare-hosted-proof.ps1](../scripts/prepare-hosted-proof.ps1) verifies authenticated dashboard, runtime, and snapshot traffic after tenant prep
- `npm.cmd run test:e2e:prod` runs a Playwright global warm-up gate before test `1`, including an authenticated dashboard snapshot check

This matters even more on free-tier Render because the final proof intentionally exercises auth rate limiting. The next run must start after that bucket cools down, and the hosted proof now waits for that cooldown automatically.

## Operational Noise Classification

`Broken pipe` and `ClientAbortException` lines are expected noise when a browser disconnects or a Playwright navigation tears down a request early. Treat them as real incidents only when they line up with a failed proof step or a real requestId-backed server error.

## Related Docs

- [deployment.md](deployment.md)
- [live-deployment-runbook.md](live-deployment-runbook.md)
- [hosted-proof.md](hosted-proof.md)
- [runtime-observability.md](runtime-observability.md)
- [schema-migration-roadmap.md](schema-migration-roadmap.md)
- [verification-status.md](verification-status.md)
