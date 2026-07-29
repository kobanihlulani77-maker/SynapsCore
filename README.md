# SynapseCore

SynapseCore is a real-time Operations Control System delivered as a multi-tenant SaaS platform.

It gives operations teams one governed place to see live state, recover failed inbound work, route approvals, track incidents, and keep tenant-scoped operational truth visible across orders, inventory, integrations, replay, alerts, recommendations, and runtime trust surfaces.

This is not being presented as a demo dashboard. The platform has been proven live through the hosted proof path on Render.

## What SynapseCore Is

SynapseCore is designed to sit above fragmented company systems and create one operational control loop:

1. receive operational activity
2. update internal state
3. surface risk and pressure
4. generate alerts and recommendations
5. route approvals or escalations when needed
6. keep replay, runtime, audit, and incident truth visible

Current live product surfaces include:

- tenant-explicit auth, session, and workspace administration
- catalog and warehouse-aware inventory control
- order ingestion through webhook, CSV, and scheduled pull
- alerts, recommendations, scenarios, approvals, and escalations
- integration replay and recovery
- runtime, incidents, audit, and business-event tracing
- tenant-scoped realtime updates
- users, settings, and SaaS administration surfaces

## Who It Is For

SynapseCore is a good fit for operations-heavy organizations such as:

- logistics companies
- warehouses and 3PL environments
- retail chains
- ecommerce fulfillment teams
- distributors
- manufacturers
- procurement-heavy businesses
- operations centers
- supply chain coordinators
- multi-warehouse businesses
- transport and fleet operations
- field operations
- enterprise admin teams supporting multiple business units or clients

## What Problems It Solves

SynapseCore is strongest when a company has real operational pressure but weak operational coherence.

Typical pain points:

- fragmented systems across spreadsheets, portals, exports, and support channels
- failed integrations that disappear into inboxes or manual re-entry
- delayed approvals with weak ownership and no shared review lane
- inventory and order mismatch across warehouses, planning, and fulfillment
- no visible replay or recovery path for failed inbound work
- poor operational visibility into connector failures, backlogs, or stock pressure
- weak audit traceability after incidents or risky decisions

## What Is Proven

The current supported scope is not theoretical. It is proven.

Hosted proof evidence:

- full hosted proof passed twice consecutively on Render
- run 1: `6 passed (6.3m)`
- run 2: `6 passed (4.3m)`

What those runs proved live:

- frontend and backend connection is real
- auth flow and session behavior are real
- tenant-scoped catalog onboarding is real
- orders and inventory surfaces are real
- realtime dashboard updates work without refresh
- replay recovery is deterministic for the supported disabled-connector recovery flow
- scenario approval, execution, and browser role gating are real
- runtime, integrations, users, settings, alerts, and recommendations are real
- frontend-visible auth rate limiting is real

## Current Live Status

The most important current truth is:

- historical hosted proof evidence exists
- the frontend deployment is reachable
- hosted proof is currently paused whenever live backend, DB, readiness, auth, or websocket trust are unavailable

That means historical proof success should be read together with the current recovery and runtime posture, not as a permanent guarantee that the live backend is healthy right now.

## Current Supported Scope

SynapseCore is fully real for its current supported scope.

That scope is intentionally honest:

- connector breadth is currently limited to webhook, CSV, and scheduled pull order ingestion
- this is not being claimed as a broad ERP connector marketplace yet
- Redis pub/sub is the current distributed realtime posture on Render
- STOMP relay and larger horizontal-scale topologies remain future infrastructure hardening choices, not missing proof gaps

## Local Development

Recommended local path:

```bash
cd infrastructure
docker compose up --build
```

Local default endpoints:

- frontend: `http://localhost:5173`
- backend: `http://localhost:8080`

Local env files used by the compose stack:

- `infrastructure/env/backend.env`
- `infrastructure/env/frontend.env`

Useful local checks:

```powershell
cd frontend
npm.cmd run build

cd ..\backend
cmd /c mvnw.cmd test
```

Frontend development commands:

```powershell
cd frontend
npm.cmd install
npm.cmd run lint
npm.cmd run verify
npm.cmd run dev
```

Frontend demo/readiness docs:

- [docs/frontend-demo-guide.md](docs/frontend-demo-guide.md)
- [docs/frontend-demo-mode.md](docs/frontend-demo-mode.md)
- [docs/frontend-qa-checklist.md](docs/frontend-qa-checklist.md)

Windows local host helpers are also available in the backend folder:

- `backend\start-local-demo.cmd`
- `backend\start-local-prod.cmd`

## Deployment

Current live Render services:

- frontend: [https://synapscore-frontend-3.onrender.com](https://synapscore-frontend-3.onrender.com)
- backend: [https://synapscore-3.onrender.com](https://synapscore-3.onrender.com)

Important deployment truths:

- backend profile: `prod`
- schema posture: Flyway-backed startup with Hibernate `ddl-auto=validate`
- realtime mode on current Render: `REDIS_PUBSUB`
- production browser sessions: Redis-backed
- header fallback in production: disabled
- health checks should use liveness, while proof traffic should wait for readiness

Key trust endpoints:

- `/`
- `/actuator/health/liveness`
- `/actuator/health/readiness`
- `/api/system/runtime`
- `/api/system/incidents`
- `/actuator/prometheus`

## Final Hosted Proof Flow

Official order:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\prepare-hosted-proof.ps1

cd frontend
npm.cmd run test:e2e:prod
```

Hosted proof uses:

- a real tenant
- real tenant users
- a real proof product SKU
- production APIs only

It does not rely on:

- `SYNAPSE-DEMO`
- hidden seed users
- manual database edits

## Company-Fit Analyzer

The repo now includes a real company-fit and operational pain analyzer grounded in the implemented platform scope.

It can generate company-specific reports in markdown, HTML, or JSON for:

- logistics
- warehousing
- retail
- ecommerce fulfillment
- distribution
- manufacturing
- procurement-heavy operations
- operations centers
- fleet and field operations
- enterprise administration

PowerShell wrapper:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\explain-company-fit.ps1 --company-type warehouses --format markdown
```

Shell wrapper:

```bash
bash scripts/explain-company-fit.sh --company-type logistics-companies --format markdown
```

Direct generator examples:

```powershell
node scripts\generate-company-fit-report.mjs --list
node scripts\generate-company-fit-report.mjs --company-type logistics-companies --format markdown
node scripts\generate-company-fit-report.mjs --company-type retail-chains,ecommerce-fulfillment --format html --output docs\generated\commerce-fit.html
node scripts\generate-company-fit-report.mjs --all --format json --output docs\generated\company-fit-report.json
```

Generated showcase:

- [docs/generated/company-fit-showcase.html](docs/generated/company-fit-showcase.html)

## Important Docs

- [docs/documentation-map.md](docs/documentation-map.md)
- [docs/repository-review-report.md](docs/repository-review-report.md)
- [docs/scripts-reference.md](docs/scripts-reference.md)
- [docs/INDEX.md](docs/INDEX.md)
- [docs/environment-reference.md](docs/environment-reference.md)
- [docs/release-process.md](docs/release-process.md)
- [docs/troubleshooting-index.md](docs/troubleshooting-index.md)
- [docs/hosted-proof.md](docs/hosted-proof.md)
- [docs/company-fit-playbook.md](docs/company-fit-playbook.md)
- [docs/replay-recovery.md](docs/replay-recovery.md)
- [docs/runtime-observability.md](docs/runtime-observability.md)
- [docs/integration-operations.md](docs/integration-operations.md)
- [docs/onboarding-playbook.md](docs/onboarding-playbook.md)
- [docs/live-deployment-runbook.md](docs/live-deployment-runbook.md)
- [docs/deployment.md](docs/deployment.md)
- [docs/render-deployment.md](docs/render-deployment.md)
- [docs/verification-status.md](docs/verification-status.md)
- [docs/api-spec.md](docs/api-spec.md)

## Repo Health

Safe repo and docs checks:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\repo-health.ps1
powershell -ExecutionPolicy Bypass -File scripts\project-map.ps1
powershell -ExecutionPolicy Bypass -File scripts\explain-infrastructure.ps1
powershell -ExecutionPolicy Bypass -File scripts\env-sanity-check.ps1
powershell -ExecutionPolicy Bypass -File scripts\docs-link-check.ps1
powershell -ExecutionPolicy Bypass -File scripts\script-help.ps1
```

These commands are informational only. They do not modify runtime behavior or delete files.

## Honest Bottom Line

SynapseCore should now be read as a real SaaS operations platform with live hosted proof, not as a code repo looking for a story.

The platform is fully real for its current supported scope.

Current live proof readiness, however, still depends on backend and dependency health. If readiness, auth, websocket trust, or DB availability are down, hosted proof should pause and the repo should describe that honestly.

What remains from here is not proof-path repair. It is:

- broader connector breadth if the product expands
- future infrastructure choices if a larger horizontal scale pattern is needed
- continued operations polish, positioning, and company-specific deployment work
