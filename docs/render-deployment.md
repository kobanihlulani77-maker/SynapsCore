# Render Deployment Guide

This guide prepares SynapseCore for Render with a backend Docker web service, a frontend static site, a managed Postgres database, and a managed Redis key-value store.

## Blueprint

Use the repo root `render.yaml`. It defines:

- `synapsecore-backend` (Docker web service)
- `synapsecore-frontend` (static site)
- `synapsecore-postgres` (managed Postgres)
- `synapsecore-redis` (managed Redis)

## Backend Service (Docker)

Render uses the Dockerfile in `backend/Dockerfile`.

Build command (Render-managed):

```bash
docker build -t synapsecore-backend .
```

Start command (from Dockerfile):

```bash
java -jar /app/app.jar
```

Health check path:

```
/actuator/health
```

### Backend env vars to set (Render)

Paste or confirm these in the backend service:

```
SPRING_PROFILES_ACTIVE=prod
SERVER_ADDRESS=0.0.0.0
DATABASE_URL=<from synapsecore-postgres connection string>
REDIS_URL=<from synapsecore-redis connection string>
CORS_ALLOWED_ORIGINS=https://<your-frontend-domain>
SESSION_COOKIE_SECURE=true
SESSION_COOKIE_SAME_SITE=None
ALLOW_HEADER_FALLBACK=false
JPA_DDL_AUTO=validate
PUBLIC_APP_URL=https://<your-frontend-domain>
PUBLIC_API_URL=https://<your-backend-domain>
SYNAPSECORE_BUILD_VERSION=0.0.1
SYNAPSECORE_BUILD_COMMIT=render-deploy
SYNAPSECORE_BUILD_TIME=2026-04-10T00:00:00Z
```

Notes:

- Render supplies `PORT`; the backend binds to it automatically.
- Keep `SESSION_COOKIE_SECURE=true` and `SESSION_COOKIE_SAME_SITE=None` for HTTPS and cross-subdomain cookies.

## Frontend Service (Static Site)

Render builds the frontend from `frontend/`.

Build command:

```bash
npm ci && npm run build
```

Publish directory:

```
frontend/dist
```

### Frontend env vars to set (Render)

```
VITE_API_URL=https://<your-backend-domain>
VITE_WS_URL=wss://<your-backend-domain>/ws
VITE_APP_BUILD_VERSION=0.0.1
VITE_APP_BUILD_COMMIT=render-deploy
VITE_APP_BUILD_TIME=2026-04-10T00:00:00Z
```

## Postgres and Redis

Render creates both from the blueprint. Copy the connection strings into:

- `DATABASE_URL` for Postgres
- `REDIS_URL` for Redis

## Post-deploy checks

1. Backend health: `https://<your-backend-domain>/actuator/health`
2. Frontend loads: `https://<your-frontend-domain>`
3. Sign in with demo credentials.
4. Verify dashboard summary updates and WebSocket live status.
