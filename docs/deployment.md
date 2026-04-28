# Deployment Guide

This guide describes the current real deployment posture for SynapseCore.

## Current Production Truth

Production posture today:

- profile: `prod`
- session cookies: secure by default
- header fallback: disabled by default
- tenant provisioning after first bootstrap: platform-admin token only
- realtime mode on current live Render: `REDIS_PUBSUB`
- schema validation on current live Render: Hibernate `ddl-auto=validate`

That means SynapseCore is running as a real SaaS platform with validate-only schema startup and distributed Redis-backed realtime.

## Backend Environment

Important production variables:

- `SPRING_PROFILES_ACTIVE=prod`
- `DATABASE_URL` or `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `SPRING_DATA_REDIS_URL`
- `CORS_ALLOWED_ORIGINS`
- `SESSION_COOKIE_SECURE=true`
- `SESSION_COOKIE_SAME_SITE=None` when frontend and backend are on different hosted origins
- `ALLOW_HEADER_FALLBACK=false`
- `SYNAPSECORE_REALTIME_BROKER_MODE=REDIS_PUBSUB`
- `SPRING_JPA_HIBERNATE_DDL_AUTO=validate`
- `SYNAPSECORE_BOOTSTRAP_INITIAL_TOKEN` only for first-tenant bootstrap on an empty production database
- `SYNAPSECORE_PLATFORM_ADMIN_TOKEN` for later production tenant provisioning
- `PUBLIC_APP_URL`
- `PUBLIC_API_URL`
- `SYNAPSECORE_BUILD_VERSION`
- `SYNAPSECORE_BUILD_COMMIT`
- `SYNAPSECORE_BUILD_TIME`

## Frontend Runtime Configuration

The frontend reads runtime config from `/runtime-config.js` at startup.

Important variables:

- `VITE_API_URL`
- `VITE_WS_URL`
- `VITE_APP_BUILD_VERSION`
- `VITE_APP_BUILD_COMMIT`
- `VITE_APP_BUILD_TIME`

## Realtime Truth

Supported realtime modes:

- `SIMPLE_IN_MEMORY`
- `REDIS_PUBSUB`
- `STOMP_RELAY`

Current live truth:

- Render is running `REDIS_PUBSUB`
- this provides distributed fanout beyond single-node simple-broker mode
- distributed publisher fanout is covered by automated proof in the backend suite

Optional later for different scale-out topology:

- multi-node deployment proof with Redis pub/sub or STOMP relay
- optional `SYNAPSECORE_REALTIME_BROKER_MODE=STOMP_RELAY`
- relay host, port, login, and passcode configuration when using relay mode

## Integration Truth

The live supported integration surface is intentionally narrow:

- webhook order ingestion
- CSV order import
- scheduled pull for order ingestion

Deployment docs should not imply broader connector support than that.

## Schema Migration Truth

Current state:

- production now starts with `SPRING_JPA_HIBERNATE_DDL_AUTO=validate`
- Flyway is active at startup
- baseline coverage exists for the current managed schema
- production-hardening tests run on validation posture

Read the exact migration path in:

- [schema-migration-roadmap.md](schema-migration-roadmap.md)

## Production Checklist

1. Set backend production env values.
2. Set frontend runtime env values.
3. Run the backend with `SPRING_PROFILES_ACTIVE=prod`.
4. Keep `ALLOW_HEADER_FALLBACK=false`.
5. Narrow `CORS_ALLOWED_ORIGINS` to the deployed frontend origin only.
6. Use `SESSION_COOKIE_SECURE=true` behind HTTPS.
7. If the database is empty, use `SYNAPSECORE_BOOTSTRAP_INITIAL_TOKEN` once, then remove it.
8. Use `SYNAPSECORE_PLATFORM_ADMIN_TOKEN` for later tenant provisioning.
9. Verify `/actuator/health` and `/actuator/health/readiness`.
10. Verify dashboard REST and realtime connections from the deployed frontend.
11. Treat `ddl-auto=validate` plus Flyway validation as required production posture.

## Hosted Proof

Hosted proof must use:

- a real tenant
- real tenant users
- production APIs only

Do not use:

- `SYNAPSE-DEMO`
- hidden seed users
- manual database edits

Proof preparation:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\prepare-hosted-proof.ps1
```

Browser proof:

```powershell
cd frontend
npm.cmd run test:e2e:prod
```

Current hosted-proof truth:

- tenant/user setup is real
- the hosted proof passed end to end on Render
- remaining work is scope expansion and scale tuning, not proof-path repair

## Related Docs

- [render-deployment.md](render-deployment.md)
- [live-deployment-runbook.md](live-deployment-runbook.md)
- [verification-status.md](verification-status.md)
