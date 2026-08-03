# Environment Reference

This document explains the environment files and environment variables that shape SynapseCore behavior across local development, Docker infrastructure, self-hosted production-style runs, and Render-style deployments.

It is a reference guide, not a secrets file.

Do not commit real secrets into any of the environment files discussed here.

## Environment Layers

SynapseCore uses several environment layers depending on how the system is being run.

### Frontend Local Override

Typical file:

- `frontend/.env.local`

Purpose:

- local host frontend pointing to local or chosen backend

Common keys:

- `VITE_API_URL`
- `VITE_WS_URL`

Status:

- local-only
- should not be committed

### Frontend Local Example

Typical file:

- `frontend/.env.local.example`

Purpose:

- local setup example for host frontend runs

Status:

- local guidance only
- currently treated as local-only in this repo

### Backend Local Example

Typical file:

- `backend/.env.local.example`

Purpose:

- local backend guidance for host-based startup

Status:

- local guidance only
- currently treated as local-only in this repo

### Infrastructure Env Templates

Files:

- `infrastructure/env/backend.env`
- `infrastructure/env/frontend.env`
- `infrastructure/env/backend.prod.env`
- `infrastructure/env/frontend.prod.env`
- `infrastructure/env/backend.prod.example.env`
- `infrastructure/env/frontend.prod.example.env`
- `infrastructure/env/backend.prod.selfhost.env`
- `infrastructure/env/frontend.prod.selfhost.env`
- `infrastructure/env/edge.prod.env`
- `infrastructure/env/edge.prod.example.env`

Purpose:

- Docker Compose and deployment-oriented environment wiring

Status:

- operationally sensitive
- may contain templates or local deployment defaults
- should be handled carefully

## Frontend Variables

### `VITE_API_URL`

Purpose:

- base URL for frontend API calls

Examples:

- local host UI to local backend: `http://localhost:8080`
- hosted frontend to hosted backend: `https://synapscore-3.onrender.com`

### `VITE_WS_URL`

Purpose:

- websocket/SockJS endpoint root used by the frontend

Examples:

- local: `http://localhost:8080/ws`
- hosted: `https://synapscore-3.onrender.com/ws`

Important:

- this should point to the `/ws` endpoint, not just the backend root

### `VITE_APP_BUILD_VERSION`

Purpose:

- frontend build fingerprint

### `VITE_APP_BUILD_COMMIT`

Purpose:

- commit identity exposed in runtime build metadata

### `VITE_APP_BUILD_TIME`

Purpose:

- build timestamp for traceability

## Backend Variables

### Boot And Profile

Common keys:

- `SPRING_PROFILES_ACTIVE`
- `SERVER_PORT`
- `SERVER_ADDRESS`

Purpose:

- choose profile and bind address/port behavior

Common values:

- `dev`
- `local`
- `prod`

### Database

Common keys:

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `DATABASE_URL`
- `DB_HOST`
- `DB_PORT`
- `DB_NAME`
- `DB_USER`
- `DB_PASSWORD`

Purpose:

- configure PostgreSQL connectivity

Important:

- production-oriented config expects explicit DB connectivity
- backend readiness and startup truth depend heavily on correct DB values

### Redis And Session

Common keys:

- `SPRING_DATA_REDIS_URL`
- `REDIS_HOST`
- `REDIS_PORT`
- `SPRING_SESSION_REDIS_NAMESPACE`

Purpose:

- Redis connectivity
- production session store
- distributed realtime posture

### CORS And Session Security

Common keys:

- `CORS_ALLOWED_ORIGINS`
- `SESSION_COOKIE_SECURE`
- `SESSION_COOKIE_SAME_SITE`
- `ALLOW_HEADER_FALLBACK`

Purpose:

- frontend/backend trust posture
- browser session behavior
- header fallback rules

Important production truth:

- `ALLOW_HEADER_FALLBACK` should be `false`
- `SESSION_COOKIE_SECURE` should align with HTTPS posture

### JPA And Flyway

Common keys:

- `SPRING_JPA_HIBERNATE_DDL_AUTO`
- `JPA_DDL_AUTO`
- `SPRING_FLYWAY_ENABLED`

Purpose:

- startup schema behavior
- migration discipline

Important:

- production posture should remain validation and migration based
- do not casually reintroduce implicit schema mutation in prod

### Realtime And Runtime

Common keys:

- `SYNAPSECORE_REALTIME_BROKER_MODE`
- `DASHBOARD_CACHE_ENABLED`
- `SIMULATION_INTERVAL_MS`

Purpose:

- live update behavior
- dashboard cache behavior
- simulation cadence where enabled

### Build Fingerprint

Common keys:

- `SYNAPSECORE_BUILD_VERSION`
- `SYNAPSECORE_BUILD_COMMIT`
- `SYNAPSECORE_BUILD_TIME`

Purpose:

- runtime traceability
- release identity

### Tenant Bootstrap And Provisioning

Optional keys:

- `SYNAPSECORE_BOOTSTRAP_INITIAL_TOKEN`
- `SYNAPSECORE_PLATFORM_ADMIN_TOKEN`

Purpose:

- first production tenant bootstrap
- later production tenant provisioning

Important:

- these are sensitive values
- they should not be committed as real tokens

### Hosted Proof Preparation Values

Authoritative generated values:

- `PLAYWRIGHT_BASE_URL`
- `PLAYWRIGHT_API_BASE_URL`
- `PLAYWRIGHT_TENANT_CODE`
- `PLAYWRIGHT_TENANT_NAME`
- `PLAYWRIGHT_PROOF_PRODUCT_SKU`
- `PLAYWRIGHT_TENANT_ADMIN_USERNAME`
- `PLAYWRIGHT_TENANT_ADMIN_PASSWORD`
- `PLAYWRIGHT_PLANNER_USERNAME`
- `PLAYWRIGHT_PLANNER_PASSWORD`
- `PLAYWRIGHT_INTEGRATION_ADMIN_USERNAME`
- `PLAYWRIGHT_INTEGRATION_ADMIN_PASSWORD`

Source:

- generated or reused by `scripts\prepare-hosted-proof.ps1`
- persisted locally in ignored state at `frontend\test-results\hosted-proof-state.json`
- read automatically by the Playwright hosted proof when shell env vars are absent

Not generated:

- `SYNAPSECORE_BOOTSTRAP_INITIAL_TOKEN`

That token is a private backend/Render secret. It is required only when the target production database has zero tenants and the first tenant must be created through the supported bootstrap API.

## Profile Notes

### `dev`

Typical use:

- host or Docker-assisted local development

Common posture:

- Postgres-backed
- Redis-backed
- header fallback often enabled
- local browser-friendly cookie settings

### `local`

Typical use:

- lightweight local fallback path

Common posture:

- may use simpler local assumptions
- not the main production-like path

### `prod`

Typical use:

- Render or production-style deployment

Common posture:

- strict readiness expectations
- Redis-backed sessions
- DB validation posture
- header fallback disabled

## Local Environment Patterns

### Frontend On Host, Backend On Host

Typical frontend local values:

```text
VITE_API_URL=http://localhost:8080
VITE_WS_URL=http://localhost:8080/ws
```

### Frontend On Host, Backend In Docker On Port 8080

Typical frontend local values remain:

```text
VITE_API_URL=http://localhost:8080
VITE_WS_URL=http://localhost:8080/ws
```

### Docker Compose Infra Only

Typical env sources:

- `infrastructure/env/backend.env`
- `infrastructure/env/frontend.env`

## Production-Style Environment Patterns

Typical frontend hosted values:

```text
VITE_API_URL=https://synapscore-3.onrender.com
VITE_WS_URL=https://synapscore-3.onrender.com/ws
```

Typical backend hosted values include:

- `SPRING_PROFILES_ACTIVE=prod`
- production DB URL or DB host/user/password
- Redis URL
- production CORS origins
- secure cookie posture
- build fingerprint values

## Common Mistakes

- pointing `VITE_API_URL` at the frontend instead of the backend
- pointing `VITE_WS_URL` at the backend root instead of `/ws`
- leaving placeholder build commit/time values in production envs
- enabling header fallback in production
- mixing local Postgres assumptions with Docker Postgres assumptions
- treating `.env.local` files as commit-worthy source

## Related Docs

- [local-runbook.md](local-runbook.md)
- [render-ops-runbook.md](render-ops-runbook.md)
- [database-and-migrations.md](database-and-migrations.md)
- [release-process.md](release-process.md)
- [troubleshooting-index.md](troubleshooting-index.md)

## Bottom Line

The environment layer is part of the platform, not just setup trivia.

In SynapseCore, incorrect environment posture can directly affect:

- readiness
- auth/session
- websocket trust
- proof validity
- operator-visible runtime confidence
