# Render Deployment Guide

This guide reflects the current live Render setup for SynapseCore.

## Live Render Topology

Defined in `render.yaml`:

- backend web service: `synapscore-3`
- frontend static site: `synapscore-frontend-3`
- managed Postgres: `synapscore-postgres`
- managed Redis: `synapscore-redis`

Live URLs:

- frontend: `https://synapscore-frontend-3.onrender.com`
- backend: `https://synapscore-3.onrender.com`
- backend health: `https://synapscore-3.onrender.com/actuator/health`

## Backend Render Environment

Required keys:

```text
SPRING_PROFILES_ACTIVE=prod
SERVER_ADDRESS=0.0.0.0
DATABASE_URL=<Render Postgres internal connection string>
SPRING_DATA_REDIS_URL=<Render Redis internal connection string>
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
SYNAPSECORE_BUILD_COMMIT=<git-sha>
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

## Frontend Render Environment

```text
VITE_API_URL=https://synapscore-3.onrender.com
VITE_WS_URL=https://synapscore-3.onrender.com/ws
VITE_APP_BUILD_VERSION=<release-version>
VITE_APP_BUILD_COMMIT=<git-sha>
VITE_APP_BUILD_TIME=<utc-timestamp>
```

The frontend service must keep SPA rewrite routing:

```yaml
routes:
  - type: rewrite
    source: /*
    destination: /index.html
```

## Current Render Scope

These are truthful current Render boundaries:

- schema startup is validate-only with Flyway-backed baseline coverage
- realtime is broker-backed through `REDIS_PUBSUB`
- integration breadth is intentionally narrow

## Supported Integration Breadth On Render

Render deployment currently supports the implemented lanes only:

- webhook order ingestion
- CSV order import
- scheduled pull order ingestion

Do not describe the platform on Render as having broad connector coverage beyond those lanes.

## Post-Deploy Checks

1. Open the frontend URL.
2. Verify deep links such as `/sign-in` and `/dashboard`.
3. Verify backend health and readiness.
4. Verify sign-in loads tenant directory correctly.
5. Verify session cookies and redirect behavior.
6. Verify dashboard, integrations, runtime, and audit load without CORS failures.
7. Verify realtime works with the current Redis pub/sub posture in mind.
8. Verify a wrong-password `POST /api/auth/session/login` returns a fast `401`.
9. Run hosted proof preparation and browser proof and expect the Render proof pack to pass.

## Related Docs

- [deployment.md](deployment.md)
- [live-deployment-runbook.md](live-deployment-runbook.md)
- [schema-migration-roadmap.md](schema-migration-roadmap.md)
- [verification-status.md](verification-status.md)
