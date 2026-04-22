# Deployment Guide

SynapseCore now separates local development behavior from production-oriented runtime behavior.

## Profiles

- `dev`
  - default profile
  - local-friendly datasource and Redis defaults
  - session cookie secure flag defaults to `false`
  - header fallback for protected actions is allowed by default for scripts and test tooling
- `prod`
  - intended for deployed environments
  - expects datasource and Redis connection settings from environment variables
  - session cookie secure flag defaults to `true`
- protected-action header fallback is disabled by default
- JPA defaults to `update` for first deployment unless overridden through `SPRING_JPA_HIBERNATE_DDL_AUTO`
- tenant-admin tenant onboarding is disabled by default after bootstrap
- tenant provisioning after bootstrap requires the platform-admin token lane

## Backend Environment

Local Docker Compose defaults live in:

- `infrastructure/env/backend.env`

Production starting template lives in:

- `infrastructure/env/backend.prod.example.env`

Self-hosted production-style default env file lives in:

- `infrastructure/env/backend.prod.selfhost.env`

Important production variables:

- `SPRING_PROFILES_ACTIVE=prod`
- `DATABASE_URL` or `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME` when using `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_PASSWORD` when using `SPRING_DATASOURCE_URL`
- `SPRING_DATA_REDIS_URL`
- `CORS_ALLOWED_ORIGINS`
- `SESSION_COOKIE_SECURE`
- `SESSION_COOKIE_SAME_SITE`
- `ALLOW_HEADER_FALLBACK=false`
- `SYNAPSECORE_REALTIME_BROKER_MODE=SIMPLE_IN_MEMORY` for the current single-node deployment, or `EXTERNAL_BROKER` with relay env vars for horizontal websocket scale-out
- `SYNAPSECORE_INTEGRATION_PULL_WORKER_ENABLED=true`
- `SYNAPSECORE_INTEGRATION_PULL_WORKER_INTERVAL_MS=60000`
- `SYNAPSECORE_INTEGRATION_PULL_WORKER_BATCH_SIZE=10`
- `SPRING_JPA_HIBERNATE_DDL_AUTO=update`
- `SYNAPSECORE_BOOTSTRAP_INITIAL_TOKEN` only for the first tenant bootstrap on an empty production environment
- `SYNAPSECORE_PLATFORM_ADMIN_TOKEN` for ongoing production tenant provisioning after bootstrap
- `PUBLIC_APP_URL`
- `PUBLIC_API_URL`
- `SYNAPSECORE_BUILD_VERSION`
- `SYNAPSECORE_BUILD_COMMIT`
- `SYNAPSECORE_BUILD_TIME`

## Frontend Runtime Configuration

The frontend no longer depends on build-time-only API URLs inside the Docker image.

At container startup, the nginx image renders `/runtime-config.js` from environment variables:

- `VITE_API_URL`
- `VITE_WS_URL`
- `VITE_APP_BUILD_VERSION`
- `VITE_APP_BUILD_COMMIT`
- `VITE_APP_BUILD_TIME`

Production starting template lives in:

- `infrastructure/env/frontend.prod.example.env`

Self-hosted production-style default env file lives in:

- `infrastructure/env/frontend.prod.selfhost.env`

This means the same built frontend image can be pointed at different backend URLs without rebuilding the bundle.

## Public Domains And HTTPS

SynapseCore now also includes:

- `infrastructure/docker-compose.public.yml` for a public self-hosted rollout
- `infrastructure/Caddyfile` for edge routing and automatic HTTPS
- `infrastructure/env/edge.prod.example.env` as the starting template for app domain, API domain, and ACME email

Generate the editable target file with:

```bash
bash scripts/prepare-prod-envs.sh
```

or on Windows:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\prepare-prod-envs.ps1
```

That also creates:

- `infrastructure/env/edge.prod.env`

Important edge variables:

- `SYNAPSECORE_APP_DOMAIN`
- `SYNAPSECORE_API_DOMAIN`
- `SYNAPSECORE_ACME_EMAIL`

## Self-Hosted Production Compose

SynapseCore now includes:

- `infrastructure/docker-compose.yml` for local development
- `infrastructure/docker-compose.prod.yml` as a production-shaped self-hosted stack

Bring up the self-hosted production-style stack with:

```bash
bash scripts/start-prod.sh
```

Windows PowerShell equivalent:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\start-prod.ps1
```

What the production override changes:

- backend uses `backend.prod.selfhost.env` by default
- frontend uses `frontend.prod.selfhost.env` by default
- frontend is exposed on `80`
- backend remains exposed on `8080`
- Postgres and Redis are no longer exposed directly to the host
- services restart automatically unless stopped
- Postgres, Redis, backend, and frontend all use health checks
- backend waits for healthy Postgres and Redis
- frontend waits for a healthy backend

Override the env files when you want to point the production-shaped stack at different real env files:

```bash
BACKEND_ENV_FILE=./env/backend.prod.env FRONTEND_ENV_FILE=./env/frontend.prod.env bash scripts/start-prod.sh
```

Windows PowerShell equivalent:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\start-prod.ps1 -BackendEnvFile ./env/backend.prod.env -FrontendEnvFile ./env/frontend.prod.env
```

Before a real deployment, copy and replace the example values in:

- `infrastructure/env/backend.prod.example.env`
- `infrastructure/env/frontend.prod.example.env`

with your real deployed domains, credentials, and connection strings.

Render-specific production defaults now live in:

- `render.yaml`
- `docs/render-deployment.md`

## Public HTTPS Compose

When you want SynapseCore exposed on real public domains with HTTPS, use:

```bash
BACKEND_ENV_FILE=./env/backend.prod.env FRONTEND_ENV_FILE=./env/frontend.prod.env EDGE_ENV_FILE=./env/edge.prod.env bash scripts/start-public-prod.sh
```

Windows PowerShell equivalent:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\start-public-prod.ps1 -BackendEnvFile ./env/backend.prod.env -FrontendEnvFile ./env/frontend.prod.env -EdgeEnvFile ./env/edge.prod.env
```

What this public compose changes:

- adds Caddy as the edge service
- terminates HTTPS automatically
- serves the frontend on `https://$SYNAPSECORE_APP_DOMAIN`
- serves the backend on `https://$SYNAPSECORE_API_DOMAIN`
- keeps backend and frontend internal to the Docker network
- still preserves health-aware startup and schema bootstrap

For local Windows prep, SynapseCore also includes PowerShell equivalents for the release scripts:

- `scripts\prepare-prod-envs.ps1`
- `scripts\check-prod-config.ps1`
- `scripts\release-readiness.ps1`
- `scripts\verify-deployment.ps1`

## Protected Actions

Protected review and integration actions should use signed-in sessions in deployed environments.

Header-based fallback with `X-Synapse-Actor` and `X-Synapse-Roles` is intended for:

- tests
- scripts
- non-UI tooling

In the production profile it is disabled by default.

## Deployment Checklist

1. Set backend environment variables from `backend.prod.example.env`.
2. Set frontend runtime variables from `frontend.prod.example.env`.
3. Use `backend.prod.selfhost.env` and `frontend.prod.selfhost.env` only for local self-hosted prod-style runs.
4. Run `bash scripts/check-prod-config.sh` against the real env files before launch.
   On Windows, use `powershell -ExecutionPolicy Bypass -File scripts\check-prod-config.ps1 -BackendEnvFile ./env/backend.prod.env -FrontendEnvFile ./env/frontend.prod.env`.
5. Run the backend with `SPRING_PROFILES_ACTIVE=prod`.
6. Verify `CORS_ALLOWED_ORIGINS` points only at the deployed frontend origin.
7. Verify `SESSION_COOKIE_SECURE=true` behind HTTPS.
8. Verify protected actions work through signed-in sessions.
9. If the production database is empty, set `SYNAPSECORE_BOOTSTRAP_INITIAL_TOKEN`, create the first tenant once, then remove the token.
10. Set `SYNAPSECORE_PLATFORM_ADMIN_TOKEN` for any later production tenant provisioning and use it through `X-Synapse-Platform-Admin-Token`.
11. Do not rely on tenant-admin sessions to create additional tenant workspaces in production. That path remains intentionally blocked.
12. Verify `/actuator/health` and `/actuator/health/readiness`.
13. Verify dashboard REST and WebSocket connections from the deployed frontend.
13. Verify `/actuator/prometheus` if you plan to scrape SynapseCore metrics.

## Smoke Verification

After bringing up a deployed or self-hosted stack, run:

```bash
bash scripts/verify-deployment.sh
```

Windows PowerShell equivalent:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\verify-deployment.ps1 -FrontendUrl https://app.example.com -BackendUrl https://api.example.com
```

For direct browser proof on the local production-shaped stack, also run:

```powershell
cd frontend
npm.cmd run test:e2e:prod

cd ..
powershell -ExecutionPolicy Bypass -File scripts\verify-realtime.ps1 -FrontendUrl http://localhost -BackendUrl http://localhost:8080
```

For hosted Render proof, use the real non-demo credential model in `frontend/prod-proof.env.example`:

```powershell
cd frontend
$env:PLAYWRIGHT_FRONTEND_URL="https://synapscore-frontend-3.onrender.com"
$env:PLAYWRIGHT_BACKEND_URL="https://synapscore-3.onrender.com"
$env:PLAYWRIGHT_TENANT_CODE="<real-company-tenant-code>"
$env:PLAYWRIGHT_OPERATIONS_LEAD_USERNAME="<tenant-admin-username>"
$env:PLAYWRIGHT_OPERATIONS_LEAD_PASSWORD="<tenant-admin-password>"
$env:PLAYWRIGHT_OPERATIONS_PLANNER_USERNAME="<planner-username>"
$env:PLAYWRIGHT_OPERATIONS_PLANNER_PASSWORD="<planner-password>"
$env:PLAYWRIGHT_INTEGRATION_LEAD_USERNAME="<integration-admin-username>"
$env:PLAYWRIGHT_INTEGRATION_LEAD_PASSWORD="<integration-admin-password>"
npm.cmd run test:e2e:prod
```

The live proof expects one tenant-admin operator, one planner/operator account, and one integration-admin operator created through the production tenant bootstrap/admin flow. It does not rely on hidden seed users.

`scripts/start-prod.sh` runs the same smoke verification automatically unless you pass `--skip-verify`.

Before startup, `scripts/start-prod.sh` also runs:

```bash
bash scripts/check-prod-config.sh
```

That release gate validates:

- required backend and frontend prod env values exist
- `SPRING_PROFILES_ACTIVE=prod`
- `ALLOW_HEADER_FALLBACK=false`
- `SPRING_JPA_HIBERNATE_DDL_AUTO=update`
- secure-cookie posture matches HTTPS origins
- frontend WebSocket target ends with `/ws`
- backend and frontend build fingerprint values are present
- placeholder domains and passwords are blocked unless explicitly allowed

For a pre-rollout handoff summary, run:

```bash
bash scripts/release-readiness.sh
```

Windows PowerShell equivalent:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\release-readiness.ps1 -BackendEnvFile ./env/backend.prod.env -FrontendEnvFile ./env/frontend.prod.env
```

That report combines:

- the prod-config release gate
- production compose validation
- backend and frontend build fingerprints
- the exact start, verify, browser-proof, backup, and restore commands for the selected env files

To create editable target env files from the example templates, run:

```bash
bash scripts/prepare-prod-envs.sh
```

Windows PowerShell equivalent:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\prepare-prod-envs.ps1
```

For the full VPS flow and rollout checklist, see:

- [Live Deployment Runbook](live-deployment-runbook.md)
- [Go-Live Checklist](go-live-checklist.md)

Default checks:

- backend health
- backend readiness
- backend Prometheus metrics
- dashboard summary
- system runtime endpoint
- system incident inbox
- frontend `/healthz`
- frontend `/runtime-config.js`

Override targets when validating a real deployed environment:

```bash
FRONTEND_URL=https://app.example.com BACKEND_URL=https://api.example.com bash scripts/verify-deployment.sh
```

## Backup And Recovery

SynapseCore now includes a simple self-hosted Postgres recovery lane:

```bash
bash scripts/backup-postgres.sh
```

That creates a timestamped plain SQL backup under `backups/` by default.

To restore a backup into the production-shaped self-hosted stack:

```bash
bash scripts/restore-postgres.sh --file backups/synapsecore-postgres-YYYYMMDD-HHMMSS.sql --yes
```

Windows PowerShell equivalents:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\backup-postgres.ps1
powershell -ExecutionPolicy Bypass -File scripts\restore-postgres.ps1 -BackupFile backups\synapsecore-postgres-YYYYMMDD-HHMMSS.sql -Yes
powershell -ExecutionPolicy Bypass -File scripts\verify-restore-drill.ps1
```

Important restore note:

- `restore-postgres.sh` resets the target database schema before replaying the backup
- use it only when you intend to replace the current database contents
- point `COMPOSE_FILE` at a real deployed compose file if you are not using `docker-compose.prod.yml`

Useful overrides:

```bash
BACKUP_DIR=/var/backups/synapsecore bash scripts/backup-postgres.sh
COMPOSE_FILE=./infrastructure/docker-compose.prod.yml bash scripts/backup-postgres.sh
COMPOSE_FILE=./infrastructure/docker-compose.prod.yml bash scripts/restore-postgres.sh --file /var/backups/synapsecore/latest.sql --yes
```

## Log Correlation

SynapseCore console logs now include:

- request ID correlation from `X-Request-Id`
- resolved actor name from the signed-in session, or the declared actor header when header fallback is enabled

This means you can move from:

- an API error payload
- an audit log row
- a system incident card

to the matching backend logs using the same request ID and actor context.

## Deployment Fingerprint

The dashboard system-status panel now exposes both backend and frontend build metadata:

- version
- commit
- build time

That information comes from:

- backend env values `SYNAPSECORE_BUILD_VERSION`, `SYNAPSECORE_BUILD_COMMIT`, `SYNAPSECORE_BUILD_TIME`
- frontend runtime-config values `VITE_APP_BUILD_VERSION`, `VITE_APP_BUILD_COMMIT`, `VITE_APP_BUILD_TIME`

## Metrics And Queue Backbone

SynapseCore now includes a lightweight internal queue backbone for operational fan-out:

- write-side services persist `OperationalDispatchWorkItem` records
- a scheduled worker drains those records in batches
- failed queue work surfaces in `GET /api/system/incidents`
- runtime queue posture is visible in `GET /api/system/runtime`

For deeper observability, the backend also exposes `GET /actuator/prometheus`, including:

- alert backlog gauges
- fulfillment backlog and delay gauges
- replay backlog gauges
- dispatch queue backlog and failed-work gauges
- tenant-tagged counters for orders, fulfillment updates, imports, replay attempts, and dispatch processing

Use those values to confirm exactly which release is running after a self-hosted rollout or a production update.
