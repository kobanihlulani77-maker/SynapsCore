# Deployment Guide

This guide describes the current real deployment posture for SynapseCore after the final hosted-proof hardening pass.

## Current Production Truth

Production posture today:

- profile: `prod`
- schema posture: Flyway at startup with Hibernate `ddl-auto=validate`
- realtime mode on the current live Render deployment: `REDIS_PUBSUB`
- browser sessions in production: Redis-backed
- header fallback in production: disabled by default
- tenant provisioning after first bootstrap: platform-admin token only
- hosted proof: passed end to end twice in a row against the live Render deployment

That means SynapseCore is operating as a real SaaS platform for its supported scope, not as a localhost-only demo or a one-off seeded proof.

## Backend Environment

Important production variables:

- `SPRING_PROFILES_ACTIVE=prod`
- `DATABASE_URL` or `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `SPRING_DATA_REDIS_URL`
- `SPRING_SESSION_REDIS_NAMESPACE`
- `CORS_ALLOWED_ORIGINS`
- `SESSION_COOKIE_SECURE=true`
- `SESSION_COOKIE_SAME_SITE=None` when frontend and backend use different hosted origins
- `ALLOW_HEADER_FALLBACK=false`
- `SYNAPSECORE_REALTIME_BROKER_MODE=REDIS_PUBSUB`
- `SPRING_JPA_HIBERNATE_DDL_AUTO=validate`
- `SYNAPSECORE_BOOTSTRAP_INITIAL_TOKEN` only for the first tenant on an empty production database
- `SYNAPSECORE_PLATFORM_ADMIN_TOKEN` for later production tenant provisioning
- `SYNAPSECORE_RATE_LIMIT_ENABLED=true`
- `SYNAPSECORE_RATE_LIMIT_AUTH_LOGIN_MAX_ATTEMPTS`
- `SYNAPSECORE_RATE_LIMIT_AUTH_PASSWORD_MAX_ATTEMPTS`
- `SYNAPSECORE_RATE_LIMIT_TENANT_ONBOARDING_MAX_ATTEMPTS`
- `SYNAPSECORE_RATE_LIMIT_ACCESS_ADMIN_MUTATION_MAX_ATTEMPTS`
- `SYNAPSECORE_RATE_LIMIT_INTEGRATION_MUTATION_MAX_ATTEMPTS`
- `SYNAPSECORE_ALERT_HOOK_ENABLED`
- `SYNAPSECORE_ALERT_HOOK_WEBHOOK_URL`
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

Hosted Render should use the SockJS endpoint URL for `VITE_WS_URL`:

- `https://<backend-origin>/ws`

Do not point hosted Render at a raw `wss://.../ws` broker URL. The supported hosted path is STOMP over SockJS, with websocket, xhr-streaming, and xhr-polling available as transport options.

## Current Supported Integration Scope

The live supported integration surface is intentionally narrow:

- webhook order ingestion
- CSV order import
- scheduled pull order ingestion

Deployment docs should not imply broader connector breadth than that current scope.

## Realtime Truth

Supported realtime modes:

- `SIMPLE_IN_MEMORY`
- `REDIS_PUBSUB`
- `STOMP_RELAY`

Current live truth:

- Render is running `REDIS_PUBSUB`
- this provides distributed fanout beyond single-node simple-broker mode
- the hosted proof now waits for the dashboard to reach a live connection state before realtime mutation begins

## Deterministic Replay And Recovery Truth

Current replay contract:

- disabled connector CSV imports return structured failed rows with `failureCode=CONNECTOR_DISABLED`
- those failed rows create inbound and replay records immediately
- replay records are tenant-bound scalar data, not lazy tenant proxies
- automated replay skips manual-only disabled-connector replay failures
- manual replay records stay visible until an operator intentionally repairs the connector and replays the record
- manual replay and automated replay use locking and eligibility rules so they do not double-process the same record

This is the product rule now, not a proof-only exception.

## Security And Rate-Limit Posture

Production posture today:

- secure cookies are required on HTTPS
- header fallback remains disabled
- auth and mutation rate limits stay enabled
- the hosted proof intentionally exercises auth rate limiting and waits for cooldown automatically on the next run
- wrong-password login is expected to return a fast structured `401`, not a long-hanging request

## Runtime Identity And Observability

Operators should treat these surfaces as authoritative:

- `/`
- `/actuator/health/liveness`
- `/actuator/health/readiness`
- `/api/system/runtime`
- `/api/system/incidents`

Anonymous production actuator exposure is limited to health, liveness, and readiness. Metrics/prometheus scraping requires a controlled monitoring path.

Important runtime truths:

- backend build identity can resolve from `RENDER_GIT_COMMIT`
- readiness includes `db`, `redis`, `ping`, and `readinessState`
- connector, replay, dispatch, and incident posture are exposed through runtime and incident APIs

Operational noise classification:

- isolated `Broken pipe` and `ClientAbortException` logs caused by browser disconnects or navigation aborts are treated as client noise
- those lines should not be read as product failure unless they line up with a real failing request or hosted-proof step

## Hosted Proof Contract

Hosted proof must use:

- a real tenant
- real tenant users
- production APIs only

Do not use:

- `SYNAPSE-DEMO`
- hidden seed users
- manual database edits

Official order:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\prepare-hosted-proof.ps1
cd frontend
npm.cmd run test:e2e:prod
```

Current hosted-proof truth:

- tenant and user setup is real
- dashboard, replay, approvals, realtime, integrations, and auth rate limiting are browser-proven live
- the full hosted proof passed twice consecutively on Render
- the proof path is now deterministic for the currently supported scope

## Related Docs

- [render-deployment.md](render-deployment.md)
- [live-deployment-runbook.md](live-deployment-runbook.md)
- [hosted-proof.md](hosted-proof.md)
- [replay-recovery.md](replay-recovery.md)
- [runtime-observability.md](runtime-observability.md)
- [integration-operations.md](integration-operations.md)
- [verification-status.md](verification-status.md)
