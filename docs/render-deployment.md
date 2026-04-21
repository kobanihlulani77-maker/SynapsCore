# Render Deployment Guide

This guide reflects the current live Render setup for SynapsCore.

## Live Render Topology

The repo blueprint in `render.yaml` defines:

- `synapscore-3` as the backend Docker web service
- `synapscore-frontend-3` as the frontend static site
- `synapscore-postgres` as the managed Postgres database
- `synapscore-redis` as the managed Redis service

## Exact Live Public URLs

- Frontend: `https://synapscore-frontend-3.onrender.com`
- Backend: `https://synapscore-3.onrender.com`
- Backend health: `https://synapscore-3.onrender.com/actuator/health`

## Backend Service

Render builds the backend from:

- Root Directory: `backend`
- Dockerfile Path: `Dockerfile`
- Docker Environment: `docker`
- Health Check Path: `/actuator/health`

Render injects `PORT`. Do not hardcode it in the Render service.

### Exact backend env keys

Use these backend env vars on Render:

```text
SPRING_PROFILES_ACTIVE=prod
SERVER_ADDRESS=0.0.0.0
DATABASE_URL=<Render Postgres internal connection string>
SPRING_DATA_REDIS_URL=<Render Redis internal connection string>
CORS_ALLOWED_ORIGINS=https://synapscore-frontend-3.onrender.com
SESSION_COOKIE_SECURE=true
SESSION_COOKIE_SAME_SITE=None
ALLOW_HEADER_FALLBACK=false
SYNAPSECORE_REALTIME_BROKER_MODE=SIMPLE_IN_MEMORY
SYNAPSECORE_INTEGRATION_PULL_WORKER_ENABLED=true
SYNAPSECORE_INTEGRATION_PULL_WORKER_INTERVAL_MS=60000
SYNAPSECORE_INTEGRATION_PULL_WORKER_BATCH_SIZE=10
SPRING_JPA_HIBERNATE_DDL_AUTO=update
PUBLIC_APP_URL=https://synapscore-frontend-3.onrender.com
PUBLIC_API_URL=https://synapscore-3.onrender.com
SYNAPSECORE_BUILD_VERSION=0.0.1
SYNAPSECORE_BUILD_COMMIT=render-deploy
SYNAPSECORE_BUILD_TIME=2026-04-10T00:00:00Z
```

Optional for a brand-new empty production database only:

```text
SYNAPSECORE_BOOTSTRAP_INITIAL_TOKEN=<one-time-bootstrap-secret>
SYNAPSECORE_PLATFORM_ADMIN_TOKEN=<rotated-platform-admin-secret>
```

### Keys to remove

Do not use these old keys for the Render backend:

```text
REDIS_URL
REDIS_HOST
REDIS_PORT
JPA_DDL_AUTO
```

Notes:

- `DATABASE_URL` is supported because the backend normalizes Render `postgres://` / `postgresql://` connection strings into JDBC at startup.
- `SPRING_DATA_REDIS_URL` must point at the internal Render Redis URL and must not be blank in production.
- `SPRING_JPA_HIBERNATE_DDL_AUTO=update` is the first-deploy default for a fresh Render database.
- `SYNAPSECORE_BOOTSTRAP_INITIAL_TOKEN` is only for the first tenant creation on an empty production environment. Remove it after the first workspace is created.
- `SYNAPSECORE_PLATFORM_ADMIN_TOKEN` is the dedicated production tenant-provisioning lane after bootstrap. Use it through the `X-Synapse-Platform-Admin-Token` header and rotate it like any other privileged secret.
- Signed-in tenant admins still cannot create additional tenant workspaces in production.
- `SYNAPSECORE_REALTIME_BROKER_MODE=SIMPLE_IN_MEMORY` is correct for the current single-node Render backend. Switch it to `EXTERNAL_BROKER` only after provisioning an external STOMP broker and setting the relay host, port, login, and passcode env vars.
- `SYNAPSECORE_INTEGRATION_PULL_WORKER_ENABLED=true` enables real scheduled order API pulls for connectors configured with `syncMode=SCHEDULED_PULL` and `pullEndpointUrl`.

## Frontend Service

Render builds the frontend from:

- Root Directory: `frontend`
- Build Command: `npm ci && npm run build`
- Publish Directory: `dist`

The static service must keep the SPA rewrite:

```yaml
routes:
  - type: rewrite
    source: /*
    destination: /index.html
```

### Exact frontend env keys

```text
VITE_API_URL=https://synapscore-3.onrender.com
VITE_WS_URL=https://synapscore-3.onrender.com/ws
VITE_APP_BUILD_VERSION=0.0.1
VITE_APP_BUILD_COMMIT=render-deploy
VITE_APP_BUILD_TIME=2026-04-10T00:00:00Z
```

`VITE_WS_URL` uses the public `/ws` endpoint. The frontend normalizes it for both native WebSocket and SockJS transport usage.

## Final Render Setup Summary

Backend:

- service name: `synapscore-3`
- root directory: `backend`
- dockerfile path: `Dockerfile`
- health check: `/actuator/health`
- required envs: the backend list above

Frontend:

- service name: `synapscore-frontend-3`
- root directory: `frontend`
- build command: `npm ci && npm run build`
- static publish path: `dist`
- SPA rewrite: `/* -> /index.html`
- required envs: the frontend list above

## Post-Deploy Checks

1. Open `https://synapscore-frontend-3.onrender.com`
2. Verify direct deep links like `/sign-in` and `/dashboard` resolve through the SPA rewrite
3. Verify `https://synapscore-3.onrender.com/actuator/health`
4. Verify sign-in loads tenant directory successfully
5. Verify login sets a secure session cookie and redirects into `/dashboard`
6. Verify dashboard, integrations, runtime, and audit pages load without API or CORS failures
