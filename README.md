# SynapseCore

SynapseCore is a multi-tenant operational SaaS platform. It sits above company systems, receives operational activity, updates live state, detects risk, recommends action, routes approvals, exposes runtime trust signals, and keeps recovery paths visible.

It is not a demo dashboard anymore. The repo is now aligned around a real product foundation with a few clearly isolated hardening gaps.

## Current Product Label

SynapseCore is currently a **production-ready SaaS foundation candidate for its supported operational scope**.

What is real today:

- tenant-explicit sign-in, session, and workspace administration
- tenant bootstrap and platform-admin provisioning lanes
- tenant-owned product catalog, warehouses, inventory, orders, order items, scenarios, alerts, and recommendations
- order lifecycle and reservation-aware inventory updates
- policy-backed alerts and recommendations
- approvals, escalations, and scenario execution
- webhook, CSV, and scheduled pull order ingestion
- replay and recovery queues
- runtime, incidents, audit, metrics, and tenant-scoped realtime
- a modular frontend with real product pages for dashboard, catalog, integrations, runtime, replay, approvals, audit, and admin surfaces

What is bounded today:

- integrations are real but intentionally narrow
- webhook, CSV, and scheduled pull are the supported ingestion lanes
- Redis pub/sub is the current distributed realtime posture; STOMP relay remains an optional later topology, not a missing requirement

## Product Shape

SynapseCore is meant to behave like one operating loop:

1. receive operational activity
2. update internal state
3. evaluate risk and pressure
4. generate alerts and recommendations
5. route approvals or escalations when needed
6. expose the result live across dashboard, audit, runtime, and recovery surfaces

That loop now exists in the backend and the frontend is organized around it.

## Core Product Surfaces

- Public experience: product narrative and sign-in
- Dashboard: act-now queue, risk signals, health overview, live activity, and what changed
- Catalog: create, edit, and import tenant-owned products
- Orders and Inventory: live operational state and stock posture
- Fulfillment: backlog, dispatch pressure, delays, and lane risk
- Scenarios, History, Approvals, Escalations: controlled decision and execution flow
- Integrations: supported connector status, sync telemetry, failures, and recovery actions
- Replay: failed inbound recovery and operator replay
- Runtime and Audit: trust, incidents, queue posture, and traceability
- Users, Settings, Tenants, Platform Admin, Releases: SaaS administration and rollout posture

## Repo Layout

- `backend/`: Spring Boot operational platform
- `frontend/`: React control center
- `docs/`: product, deployment, verification, and architecture docs
- `infrastructure/`: Docker Compose and edge files
- `scripts/`: verification and operating helpers
- `render.yaml`: current Render topology

## Local Development

Start the local stack:

```bash
cd infrastructure
docker compose up --build
```

Useful local checks:

```powershell
cd frontend
npm.cmd run build
cd ..
cmd /c mvnw.cmd test
```

Development-only tooling is still available, but it is explicitly isolated from the live product path. Examples include local reseed and local starter baselines for developer convenience. They are not part of production posture.

## Production And Render

Current live Render services:

- frontend: [https://synapscore-frontend-3.onrender.com](https://synapscore-frontend-3.onrender.com)
- backend: [https://synapscore-3.onrender.com](https://synapscore-3.onrender.com)

Important current production truths:

- `SPRING_PROFILES_ACTIVE=prod`
- `ALLOW_HEADER_FALLBACK=false`
- `SYNAPSECORE_REALTIME_BROKER_MODE=REDIS_PUBSUB`
- `SPRING_JPA_HIBERNATE_DDL_AUTO=validate`
- `SYNAPSECORE_RATE_LIMIT_ENABLED=true`
- `SYNAPSECORE_ALERT_HOOK_ENABLED` controls whether operational failures emit to an external webhook

See:

- [docs/deployment.md](docs/deployment.md)
- [docs/render-deployment.md](docs/render-deployment.md)
- [docs/live-deployment-runbook.md](docs/live-deployment-runbook.md)
- [docs/pilot-operations-runbook.md](docs/pilot-operations-runbook.md)
- [docs/schema-migration-roadmap.md](docs/schema-migration-roadmap.md)

## Hosted Proof

Hosted proof must use a real tenant and real accounts created through production APIs. It must not rely on `SYNAPSE-DEMO`, hidden seed users, or manual database edits.

Hosted proof now classifies product-write conflicts more precisely. Duplicate or legacy-hidden catalog rows should surface as specific product conflicts, while failures in `business_events`, `audit_logs`, or `operational_dispatch_work_items` should surface as explicit repair-needed write-path failures instead of the old generic 409.

Prepare a hosted proof tenant with:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\prepare-hosted-proof.ps1
```

Then run the browser proof:

```powershell
cd frontend
npm.cmd run test:e2e:prod
```

Current hosted-proof status:

- proof pack exists
- tenant/user preparation works
- live hosted proof passed end to end on Render
- wrong-password sign-in now returns a fast structured `401` on the deployed backend
- rerun the full browser proof after any future auth/security deploy before calling the deployment finally signed off

## Supported Integration Surface

The live supported connector surface is intentionally narrow and truthful:

- webhook order ingestion
- CSV order import
- scheduled pull for order ingestion

SynapseCore does not currently claim broad ERP coverage or arbitrary connector breadth. The integrations page and runtime docs should be read as support for those implemented order-ingestion lanes only.

## Realtime Truth

Realtime is tenant-scoped and supports distributed fanout through Redis pub/sub.

Current mode:

- development: simple in-memory STOMP broker
- current Render: Redis pub/sub backed fanout
- available for later rollout if infrastructure demands it: STOMP relay mode

## Verification And Status

Read the strict current status in:

- [docs/verification-status.md](docs/verification-status.md)

That document now distinguishes:

- what is locally proven
- what is live and working
- what is supported in production today
- what is intentionally out of current scope

## Architecture References

- [docs/architecture.md](docs/architecture.md)
- [docs/system-flow.md](docs/system-flow.md)
- [docs/api-spec.md](docs/api-spec.md)

## Honest Bottom Line

SynapseCore is no longer presenting itself like a cleaned-up demo. It is a real multi-tenant SaaS foundation with operational logic, control surfaces, tenant administration, and recovery mechanics already in place.

The remaining work is no longer about core platform safety. It is about later scope expansion and scale choices:

- broader connector breadth beyond the current supported ingestion lanes
- optional STOMP relay rollout if Redis pub/sub is no longer the preferred topology
- larger-volume deployment tuning once real company load characteristics are known
