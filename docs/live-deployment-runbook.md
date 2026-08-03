# Live Deployment Runbook

This runbook is the practical operating path for deploying SynapseCore onto a real host and keeping the hosted proof deterministic.

## Recommended Baseline

- Ubuntu `24.04 LTS`
- `2 vCPU / 4 GB RAM` minimum for a single-node rollout
- separate public app and API origins
- Redis available for realtime fanout and browser session storage

## Environment Preparation

Generate env targets:

```bash
bash scripts/prepare-prod-envs.sh
```

Key backend truths:

- `SPRING_PROFILES_ACTIVE=prod`
- `ALLOW_HEADER_FALLBACK=false`
- `SESSION_COOKIE_SECURE=true`
- authenticated browser sessions should be Redis-backed in production so node restarts do not drop live operator access
- `SYNAPSECORE_REALTIME_BROKER_MODE=REDIS_PUBSUB`
- `SPRING_JPA_HIBERNATE_DDL_AUTO=validate`; Flyway startup validation is part of the production posture

Key frontend truths:

- `VITE_API_URL` must point at the real API origin
- `VITE_WS_URL` must point at the real `/ws` SockJS endpoint

## Tenant Provisioning Truth

Production tenant creation is intentionally strict:

- first tenant on an empty production database: bootstrap token lane
- later tenant creation: platform-admin token lane
- signed-in tenant admins do not create new tenant workspaces in production

## Hosted Proof Credential Flow

Hosted browser proof must use real tenant accounts created or reset through production APIs.

The current proof preparation lane is authoritative for proof tenant and operator values. It resolves values in this order:

1. script parameters
2. shell environment overrides
3. ignored proof state at `frontend\.hosted-proof\hosted-proof-state.json`
4. generated safe defaults

For a new empty production database, the only value the script cannot safely invent is the private backend bootstrap token.

Empty database bootstrap:

```powershell
$env:SYNAPSECORE_BOOTSTRAP_INITIAL_TOKEN="<private Render backend bootstrap secret>"
powershell -ExecutionPolicy Bypass -File scripts\prepare-hosted-proof.ps1
```

Preparation command after proof state exists or the bootstrap token is already loaded:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\prepare-hosted-proof.ps1
```

Do not commit or print `frontend\.hosted-proof\hosted-proof-state.json`; it contains generated proof passwords for the local proof runner.

Browser proof:

```powershell
cd frontend
npm.cmd run test:e2e:prod
```

Current truth:

- the proof pack is real
- user provisioning is real
- the hosted proof passed twice end to end on Render

## Verification Order

1. Warm the live deployment first:
   - `GET /`
   - `GET /actuator/health/liveness`
   - `GET /actuator/health/readiness`
   - `GET /api/auth/session`
   - `GET /ws/info`
   - open `/sign-in` on the frontend origin
2. Run hosted proof preparation.
3. Run the browser proof.
4. Verify runtime, incidents, replay, and audit trust surfaces.

## Cold Start And Warm-up

Render free-instance cold starts can accept traffic before the whole proof lane is stable enough for a first browser pass.

The repo now hardens the official order in two places:

- [prepare-hosted-proof.ps1](../scripts/prepare-hosted-proof.ps1) waits for backend readiness, unauthenticated session bootstrap, realtime SockJS availability, the frontend sign-in shell, and then verifies authenticated dashboard, runtime, and snapshot traffic
- the Playwright hosted proof has a global warm-up gate before test `1` starts and will not continue until the authenticated dashboard lane is reachable

## Replay And Recovery Rules

Current replay eligibility rules matter operationally:

- a disabled connector CSV import must return `CONNECTOR_DISABLED` instead of a generic `500`
- that failed import must create a replay record immediately
- automated replay does not own or mutate manual-only disabled-connector records while the connector is still disabled
- the replay record stays visible to the UI
- an operator can enable the connector and perform manual replay intentionally
- locking prevents manual and automated replay from double-processing the same record

If these rules stop being true, treat the deployment as untrustworthy even if health checks still say `UP`.

## Daily Operating Checks

- no unexplained active incidents
- replay backlog is stable or empty
- disabled connectors are intentional
- dispatch queue backlog is not growing unexpectedly
- runtime broker mode still reports `REDIS_PUBSUB`
- wrong-password login still rejects quickly
- the latest hosted-proof tenant can still sign in and load dashboard, runtime, catalog, integrations, and replay surfaces

## Recovery Procedure

Use recovery when the deployment is correct but operations are degraded.

- replay backlog growth:
  - confirm connector is enabled
  - confirm the record is eligible for manual or automated recovery
  - inspect replay queue in the UI
  - replay eligible records
  - investigate dead-lettered or orphaned records before clearing them
- realtime degradation:
  - confirm broker mode in runtime
  - verify Redis availability
  - if the UI falls back to degraded snapshot polling, treat that as degraded service, not normal steady state
- inventory contention:
  - check recent lock-conflict surfaces
  - confirm no oversell occurred
  - reduce import or replay pressure before resuming high-volume operations if contention spikes

## Rollback Procedure

Use rollback when the deployment is healthy enough to answer traffic but no longer safe to trust.

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

## Operational Noise Classification

`Broken pipe` and `ClientAbortException` lines are operational noise when:

- they happen during browser navigation or teardown
- they do not line up with a failing hosted-proof step
- there is no matching requestId-backed application failure

Treat them as real incidents only when they cluster around user-visible failures or concrete API errors.

## Bottom Line

Use this runbook as a real operational deployment guide, not a demo walkthrough.

For exact pilot operating, rollback, recovery, and security handling, use:

- [pilot-operations-runbook.md](pilot-operations-runbook.md)
- [hosted-proof.md](hosted-proof.md)
- [replay-recovery.md](replay-recovery.md)
