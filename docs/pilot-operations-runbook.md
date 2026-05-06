# Pilot Operations Runbook

This runbook is the strict operating guide for controlled SynapseCore production use. It assumes the hosted proof is already green and focuses on repeatable operation, rollback, and recovery.

## Supported Pilot Scope

SynapseCore is ready for controlled production use for the currently proven surface:

- tenant-explicit auth and session control
- tenant-scoped catalog onboarding
- order ingestion through webhook, CSV, and scheduled pull
- inventory reservation and low-stock monitoring
- alerts, recommendations, approvals, replay, runtime, and audit visibility
- tenant-scoped realtime with `REDIS_PUBSUB`

Current scope boundaries still matter:

- connector breadth is intentionally narrow
- Redis pub/sub is the current distributed realtime topology

## Required Deployment Truth

- `SPRING_PROFILES_ACTIVE=prod`
- `SPRING_JPA_HIBERNATE_DDL_AUTO=validate`
- `SPRING_FLYWAY_ENABLED=true`
- `ALLOW_HEADER_FALLBACK=false`
- `SESSION_COOKIE_SECURE=true`
- `SYNAPSECORE_REALTIME_BROKER_MODE=REDIS_PUBSUB`
- `SYNAPSECORE_RATE_LIMIT_ENABLED=true`
- `SYNAPSECORE_ALERT_HOOK_ENABLED` matches the current operator webhook posture

## Pilot Start Procedure

1. Validate env files and secrets.
2. Run Flyway validation before deployment.
3. Deploy backend and confirm `/`, `/actuator/health/liveness`, and `/actuator/health/readiness`.
4. Deploy frontend and verify sign-in loads.
5. Run hosted proof preparation:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\prepare-hosted-proof.ps1
```

6. Run hosted browser proof:

```powershell
npm.cmd run test:e2e:prod
```

7. Review `/api/system/runtime`, `/api/system/incidents`, and `/actuator/prometheus`.
8. Probe a wrong-password login and expect a fast `401`, not a long-running request.

The pilot is not considered live unless all eight steps pass.

## Daily Operating Checks

- no unexplained active incidents
- replay backlog is stable or empty
- disabled connectors are intentional
- dispatch queue backlog is not growing unexpectedly
- runtime broker mode still reports `REDIS_PUBSUB`
- wrong-password login still rejects quickly
- the latest hosted-proof tenant can still sign in and load dashboard, runtime, catalog, integrations, and replay surfaces

## Replay And Recovery Rules

Current operational rules:

- disabled connector CSV failures create replay records immediately
- those records remain visible for manual recovery
- automated replay does not steal or mutate manual-only disabled-connector records while the connector is still disabled
- manual replay becomes the intended ownership path once the connector is repaired
- dead-lettered or orphaned records should be investigated, not silently ignored

## Recovery Procedure

Use recovery when the deployment is correct but operations are degraded.

- replay backlog growth:
  - confirm the connector is enabled when automated replay is expected
  - inspect replay queue in the UI
  - replay eligible records manually when the business wants intentional operator control
  - investigate dead-lettered records before clearing them
- realtime degradation:
  - confirm broker mode in runtime
  - verify Redis availability
  - if the UI falls back to degraded snapshot polling, treat that as degraded service, not normal steady state
- inventory contention:
  - check recent lock-conflict surfaces
  - confirm no oversell occurred
  - reduce import or replay pressure before resuming high-volume operations if contention spikes

## Rollback Procedure

Use rollback when a deployment is healthy enough to answer traffic but no longer safe to trust.

1. Stop further rollout and freeze connector changes.
2. Identify the last known good frontend and backend build fingerprints.
3. Redeploy the last known good versions.
4. Re-run:
   - `/`
   - `/actuator/health/liveness`
   - `/actuator/health/readiness`
   - `powershell -ExecutionPolicy Bypass -File scripts\prepare-hosted-proof.ps1`
   - `npm.cmd run test:e2e:prod`
5. If rollback does not restore integrity and the issue is data or schema related, restore PostgreSQL from the most recent good backup.

Do not attempt to repair forward blindly if runtime, replay, or catalog writes are untrustworthy.

## Security Handling

- keep bootstrap and platform-admin tokens out of the repo
- inject tokens only through deployment env configuration
- rotate tenant user passwords through the real access APIs
- keep production header fallback disabled
- keep session cookies secure on HTTPS
- keep rate limiting enabled for auth, password change, tenant bootstrap, tenant-admin mutation, policy mutation, and integration mutation endpoints
- treat tenant-code mismatches, connector token failures, and unexpected platform-admin use as security incidents

## Monitoring Expectations

Operators should watch:

- `/api/system/runtime`
- `/api/system/incidents`
- `/actuator/prometheus`
- hosted proof success on the current deployment
- auth failure, rate-limit rejection, catalog write, replay, realtime publish, dispatch, alert-hook failure, integration failure, and inventory lock-conflict metrics

If runtime truth and hosted proof disagree, trust hosted proof and investigate immediately.

## Operational Noise Classification

`Broken pipe` and `ClientAbortException` logs are operational noise when they happen during browser disconnects or navigation teardown and there is no matching failing proof step. Treat them as incidents only when they line up with real user-visible errors or requestId-backed server failures.
