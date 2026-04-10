# SynapseCore

SynapseCore is a real-time operational intelligence platform that sits above existing company systems and turns operational activity into live understanding, future risk visibility, and immediate recommended action.

The product is not a static dashboard. It is a continuously running decision engine.

## MVP Promise

The first MVP proves one sharp loop:

`order comes in -> inventory changes -> low stock is detected -> alert and recommendation appear live`

If that flow works end to end, SynapseCore feels alive.

## What The MVP Includes

- order ingestion through `POST /api/orders`
- fulfillment and logistics updates through `POST /api/fulfillment/updates`
- inventory updates through `POST /api/inventory/update`
- low-stock detection using reorder thresholds
- basic depletion prediction using recent usage
- early depletion-risk detection when demand is burning through stock faster than the current buffer can absorb
- structured alerts with impact and recommended action
- rule-based recommendations for replenishment urgency
- a dedicated fulfillment lane for every order so warehouse backlog and delivery pressure become first-class operational state
- backlog, dispatch-SLA, delivery-delay, and logistics-anomaly modeling beyond pure inventory pressure
- fulfillment-specific alerts and recommendations for backlog prioritization, logistics escalation, and anomaly investigation
- transfer-stock recommendations when another warehouse can cover a shortage faster than a new order
- multi-line what-if order impact analysis before inventory is actually committed
- head-to-head comparison between two proposed order plans before choosing one
- recent scenario history so planning activity becomes part of the operational memory
- one-click execution of preview scenarios into the live order flow
- reload of preview scenarios back into the planner for quick iteration
- named scenario plans that can be saved, reloaded, and executed later
- lightweight approval flow for saved plans before live execution
- automatic review priority and risk scoring for saved plans based on projected operational impact
- high-risk review queue shortcuts so leads can jump straight to the most urgent pending plans
- escalated approval policy for critical plans so the riskiest proposed actions require the assigned review owner and an approval note before live execution
- staged final approval for escalated plans so the highest-risk proposals move through owner review before a second distinct approver can release them
- automatic final-approval routing for escalated plans so critical proposals land with a specific final approver and queue
- session-backed user sign-in for protected control actions so the dashboard behaves like a real signed-in control center
- tenant-aware workspace onboarding through `POST /api/access/tenants` so a tenant admin can bootstrap a new company workspace with users, operators, warehouses, inventory, and connectors
- warehouse-scoped operator lanes so tenant roles can be limited to specific warehouses for review, replay, connector management, and scoped operational views
- tenant admin support tooling for creating, updating, deactivating, and recovering operator lanes, tenant user accounts, workspace settings, warehouse metadata, and connector support ownership inside the current workspace
- tenant security policy controls for password rotation windows, tenant session timeouts, and forced invalidation of other active tenant sessions
- tenant support diagnostics so workspace admins can see password hygiene pressure, blocked access lanes, active tenant incidents, and recent admin/security activity in one support console
- self-service password change for signed-in users so password rotation can happen inside the control center instead of only through admin resets
- backend-enforced lightweight access control for scenario review, connector management, and replay operations, with explicit actor headers kept as a fallback for test and non-UI flows
- a seeded operator directory so protected control actions map to known SynapseCore operators instead of free-typed powerful identities
- stage-aware approval due times and overdue queues so pending plans can be triaged when review SLAs slip
- automatic SLA escalation for overdue critical plans so stalled final approvals reroute to an executive fallback approver and leave an event trail
- a dedicated scenario notification feed so escalation reroutes and ownership acknowledgments show up as first-class operational notices
- a live SLA escalation inbox so rerouted critical approvals surface immediately in the control center and over WebSockets
- a unified system incident inbox so audit failures, replay backlog, disabled connectors, and action-required control notices converge into one trust monitor
- escalation acknowledgment so rerouted critical approvals can move from “new urgent issue” into an owned operational handoff
- live dashboard updates over tenant-scoped WebSocket topics
- a live operational timeline for business events and audit activity
- a live fulfillment operations lane that shows backlog, overdue dispatch work, delayed shipments, and route pressure in the control center
- request tracing and audit logging for enterprise-style operational traceability
- an internal operational dispatch queue that decouples write-side processing from dashboard/realtime fan-out and keeps failed fan-out work visible for support follow-up
- Prometheus-ready observability through `/actuator/prometheus` plus tenant-scoped runtime counters for orders, fulfillment updates, imports, replay attempts, and dispatch processing
- simulation mode that generates fake orders through the same business services
- a first real integration path through `POST /api/integrations/orders/webhook`
- a second real integration path through CSV batch order import at `POST /api/integrations/orders/csv-import`
- connector configuration and enable/disable management through `POST /api/integrations/orders/connectors`
- connector sync, validation, transformation, and warehouse-fallback policy controls so inbound lanes can be tuned without changing code
- recent integration run history through `GET /api/integrations/orders/imports/recent`
- a live integration operations lane in the control center so connector status and recent inbound runs stay visible without leaving the dashboard
- a failed inbound replay queue so rejected webhook or grouped CSV orders can be retried into the live flow after the operational issue is fixed

## Stack

- Backend: Java 21 + Spring Boot
- Database: PostgreSQL
- Cache / fast state: Redis
- Realtime: Spring WebSockets (STOMP + SockJS)
- Frontend: React + Vite
- Infrastructure: Docker Compose

## Repo Layout

- `backend/` Spring Boot operational brain
- `frontend/` live operational dashboard
- `infrastructure/` Docker Compose and environment files
- `docs/` architecture, flow, and API reference
- `scripts/` helper commands and project explainers
- `AGENTS.md` project guidance for future Codex tasks

## Guided Repo Tour

If you are onboarding to SynapseCore, run the explanation script first before
starting services or reading code in detail:

```bash
bash scripts/explain-project.sh
```

If you want the full platform explanation split into dedicated lanes, use:

```bash
bash scripts/explain-all.sh
bash scripts/explain-product.sh
bash scripts/explain-architecture.sh
bash scripts/explain-connections.sh
bash scripts/explain-usage.sh
bash scripts/explain-api.sh
bash scripts/explain-pages.sh
bash scripts/explain-onboarding.sh
bash scripts/explain-testing.sh
bash scripts/explain-deployment-operations.sh
bash scripts/explain-company-fit.sh
```

On Windows PowerShell, use:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\explain-all.ps1
powershell -ExecutionPolicy Bypass -File scripts\explain-product.ps1
powershell -ExecutionPolicy Bypass -File scripts\explain-architecture.ps1
powershell -ExecutionPolicy Bypass -File scripts\explain-connections.ps1
powershell -ExecutionPolicy Bypass -File scripts\explain-usage.ps1
powershell -ExecutionPolicy Bypass -File scripts\explain-api.ps1
powershell -ExecutionPolicy Bypass -File scripts\explain-pages.ps1
powershell -ExecutionPolicy Bypass -File scripts\explain-onboarding.ps1
powershell -ExecutionPolicy Bypass -File scripts\explain-testing.ps1
powershell -ExecutionPolicy Bypass -File scripts\explain-deployment-operations.ps1
powershell -ExecutionPolicy Bypass -File scripts\explain-company-fit.ps1
```

Then run the repo map helper:

```bash
bash scripts/project-tree.sh
```

To view full project structure, run:

```bash
bash scripts/full-structure.sh
```

`explain-project.sh` gives a documentation-style walkthrough of the product,
architecture, folders, backend packages, runtime flow, and key files.

`project-tree.sh` prints the important structure of the repo and labels each
section so the system is easier to navigate quickly.

`full-structure.sh` prints the full repository tree. Use
`bash scripts/full-structure.sh --full` if you want to include generated and
dependency-heavy folders such as `node_modules`, `target`, and `dist`.

For a release-style verification sweep across backend tests, frontend build,
and Docker Compose validation, run:

```bash
bash scripts/ci-verify.sh
```

For a focused production-config release gate, run:

```bash
bash scripts/check-prod-config.sh
```

On Windows PowerShell, you can run the equivalent check with:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\check-prod-config.ps1
```

The release gate also checks that backend and frontend build fingerprint values are present before launch.

For a rollout handoff summary, run:

```bash
bash scripts/release-readiness.sh
```

On Windows PowerShell, use:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\release-readiness.ps1
```

To generate real deployment env targets from the example files, run:

```bash
bash scripts/prepare-prod-envs.sh
```

On Windows PowerShell, use:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\prepare-prod-envs.ps1
```

The same prep now also creates:

- [edge.prod.env](infrastructure/env/edge.prod.env) from [edge.prod.example.env](infrastructure/env/edge.prod.example.env)


For self-hosted Postgres backup and recovery helpers, use:

```bash
bash scripts/backup-postgres.sh
bash scripts/restore-postgres.sh --file backups/synapsecore-postgres-YYYYMMDD-HHMMSS.sql --yes
```

On Windows PowerShell, use:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\backup-postgres.ps1
powershell -ExecutionPolicy Bypass -File scripts\restore-postgres.ps1 -BackupFile backups\synapsecore-postgres-YYYYMMDD-HHMMSS.sql -Yes
powershell -ExecutionPolicy Bypass -File scripts\verify-restore-drill.ps1
```

To regenerate the latest verification record from the current local environment, run:

```bash
bash scripts/generate-verification-report.sh
```

On Windows/WSL, the bash entrypoint automatically delegates to the PowerShell verifier so localhost checks still run against the live Windows-served stack.

Or on Windows PowerShell:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\generate-verification-report.ps1
```

For direct browser proof against the production-shaped stack, also run:

```powershell
cd frontend
npm.cmd run test:e2e:prod

cd ..
powershell -ExecutionPolicy Bypass -File scripts\verify-realtime.ps1 -FrontendUrl http://localhost -BackendUrl http://localhost:8080
```

For a zero-domain-cost public test deployment on a free VM, generate ready-to-use env files with:

```bash
bash scripts/prepare-free-test-envs.sh --public-ip YOUR_SERVER_IP --email you@example.com
```

Or on Windows PowerShell:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\prepare-free-test-envs.ps1 -PublicIp YOUR_SERVER_IP -AcmeEmail you@example.com
```

## Quick Start

### Full stack with Docker

```bash
cd infrastructure
docker compose up --build
```

Then open:

- frontend: `http://localhost:5173`
- backend API: `http://localhost:8080`
- websocket endpoint: `http://localhost:8080/ws`

### Production-shaped self-hosted stack

SynapseCore now includes a production override compose file for a self-hosted release-style stack:

```bash
bash scripts/start-prod.sh
```

On Windows PowerShell:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\start-prod.ps1
```

This override:

- switches backend to the `prod` profile self-hosted env file
- switches frontend to the production runtime self-hosted env file
- removes direct host exposure for Postgres and Redis
- exposes the frontend on port `80`
- keeps Postgres, Redis, backend, and frontend health-aware
- waits for healthy dependencies before bringing up downstream services
- sets services to `restart: unless-stopped`

`scripts/start-prod.sh` now runs the production-config safety gate first and then the deployment smoke checks automatically.

If you want to point the prod compose file at different real env files, override them like this:

```bash
BACKEND_ENV_FILE=./env/backend.prod.env FRONTEND_ENV_FILE=./env/frontend.prod.env bash scripts/start-prod.sh
```

For a local self-hosted prod-style run, the defaults are:

- [backend.prod.selfhost.env](infrastructure/env/backend.prod.selfhost.env)
- [frontend.prod.selfhost.env](infrastructure/env/frontend.prod.selfhost.env)

For a real deployed environment, start from:

- [backend.prod.example.env](infrastructure/env/backend.prod.example.env)
- [frontend.prod.example.env](infrastructure/env/frontend.prod.example.env)
- [edge.prod.example.env](infrastructure/env/edge.prod.example.env)

### Public company deployment with HTTPS and domains

SynapseCore now also includes a public-facing deployment compose file with Caddy:

```bash
BACKEND_ENV_FILE=./env/backend.prod.env FRONTEND_ENV_FILE=./env/frontend.prod.env EDGE_ENV_FILE=./env/edge.prod.env bash scripts/start-public-prod.sh
```

On Windows PowerShell:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\start-public-prod.ps1 -BackendEnvFile ./env/backend.prod.env -FrontendEnvFile ./env/frontend.prod.env -EdgeEnvFile ./env/edge.prod.env
```

This public stack:

- uses [docker-compose.public.yml](infrastructure/docker-compose.public.yml)
- terminates HTTPS with [Caddyfile](infrastructure/Caddyfile)
- serves the frontend on your app domain
- serves the backend on your API domain
- keeps backend and frontend on the internal Docker network instead of exposing them directly as host ports

You can still override the verification targets if needed:

```bash
FRONTEND_URL=https://app.example.com BACKEND_URL=https://api.example.com bash scripts/verify-deployment.sh
```

Or from Windows PowerShell:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\verify-deployment.ps1 -FrontendUrl https://app.example.com -BackendUrl https://api.example.com
```

For self-hosted recovery operations, SynapseCore also includes:

- [backup-postgres.sh](scripts/backup-postgres.sh) for timestamped Postgres backups
- [restore-postgres.sh](scripts/restore-postgres.sh) for guarded schema-reset restores into the production-shaped stack

### Local development

1. Start infrastructure:

```bash
cd infrastructure
docker compose up -d postgres redis
```

2. Run backend:

```bash
cd backend
./mvnw spring-boot:run
```

On Windows PowerShell, use `mvnw.cmd spring-boot:run`.

3. Run frontend:

```bash
cd frontend
npm install
npm run dev
```

If you are using PowerShell and `npm` is blocked by execution policy, use `npm.cmd`.

## Configuration Profiles

SynapseCore now separates local and deployment-oriented runtime behavior:

- `dev`
  - default profile
  - local datasource and Redis defaults
  - header fallback allowed for protected test/script flows
  - non-secure session cookie default for local HTTP
- `prod`
  - deployment-oriented profile
  - session cookie secure flag defaults to `true`
  - protected header fallback is disabled by default
  - JPA defaults to `validate`

For local Docker Compose, use:

- [backend.env](infrastructure/env/backend.env)
- [frontend.env](infrastructure/env/frontend.env)

For production starting templates, use:

- [backend.prod.example.env](infrastructure/env/backend.prod.example.env)
- [frontend.prod.example.env](infrastructure/env/frontend.prod.example.env)

The frontend Docker image now reads `VITE_API_URL` and `VITE_WS_URL` at container startup through `/runtime-config.js`, so deployed API and WebSocket endpoints no longer need to be baked into a rebuilt bundle.

## Simulation

Seed data is created automatically when the backend starts for the first time.

If you want to restore the project to a clean, known demo baseline later, run:

```bash
bash scripts/seed.sh
```

To reseed and immediately begin live activity:

```bash
bash scripts/seed.sh --with-simulation
```

To make the system feel alive, start simulation mode:

```bash
curl -X POST http://localhost:8080/api/simulation/start
```

Simulation creates real orders through the same business flow as manual ingestion, which means it also:

- deducts inventory
- runs low-stock checks
- generates alerts
- generates recommendations
- pushes fresh live updates over the operational WebSocket topics

## Core Endpoints

- `POST /api/orders`
- `POST /api/integrations/orders/webhook`
- `POST /api/inventory/update`
- `GET /api/dashboard/summary`
- `GET /api/dashboard/snapshot`
- `GET /api/audit/recent`
- `POST /api/scenarios/order-impact`
- `POST /api/scenarios/order-impact/compare`
- `POST /api/scenarios/save`
- `GET /api/scenarios/history`
- `GET /api/scenarios/notifications`
- `GET /api/scenarios/{scenarioRunId}/request`
- `POST /api/scenarios/{scenarioRunId}/approve`
- `POST /api/scenarios/{scenarioRunId}/reject`
- `POST /api/scenarios/{scenarioRunId}/acknowledge-escalation`
- `POST /api/scenarios/{scenarioRunId}/execute`
- `GET /api/auth/session`
- `POST /api/auth/session/login`
- `POST /api/auth/session/password`
- `POST /api/auth/session/logout`
- `GET /api/system/runtime`
- `GET /api/system/incidents`
- `POST /api/integrations/orders/csv-import`
- `GET /api/integrations/orders/connectors`
- `POST /api/integrations/orders/connectors`
- `GET /api/integrations/orders/imports/recent`
- `GET /api/integrations/orders/replay-queue`
- `POST /api/integrations/orders/replay/{replayRecordId}`
- `GET /api/alerts`
- `GET /api/recommendations`
- `GET /api/inventory`
- `GET /api/products`
- `GET /api/warehouses`
- `GET /api/orders/recent`
- `GET /api/events/recent`
- `GET /api/access/tenants`
- `GET /api/access/operators`
- `POST /api/access/tenants`
- `GET /api/access/admin/operators`
- `POST /api/access/admin/operators`
- `PUT /api/access/admin/operators/{operatorId}`
- `GET /api/access/admin/workspace`
- `PUT /api/access/admin/workspace`
- `PUT /api/access/admin/workspace/security`
- `PUT /api/access/admin/workspace/warehouses/{warehouseId}`
- `PUT /api/access/admin/workspace/connectors/{connectorId}`
- `GET /api/access/admin/users`
- `POST /api/access/admin/users`
- `PUT /api/access/admin/users/{userId}`
- `POST /api/access/admin/users/{userId}/reset-password`
- `POST /api/simulation/start`
- `POST /api/simulation/stop`

The dashboard now uses a lightweight signed-in user session for protected control actions:

- sign in with `POST /api/auth/session/login`
- inspect the current session with `GET /api/auth/session`
- rotate the current signed-in password with `POST /api/auth/session/password`
- sign out with `POST /api/auth/session/logout`
- sign-in now uses a seeded user account with tenant code, username, and password
- signed-in session payloads now include `sessionExpiresAt`, `passwordExpiresAt`, `passwordChangeRequired`, `passwordRotationRequired`, `sessionTimeoutMinutes`, and `passwordRotationDays`
- each signed-in user is mapped to a known operator from the seeded operator directory returned by `GET /api/access/operators`
- the active tenant directory is available from `GET /api/access/tenants`
- tenant admins can create a new tenant workspace with `POST /api/access/tenants`
- signed-in sessions and operator-directory responses now include `warehouseScopes`
- tenant admins can manage current-tenant operators, user accounts, workspace metadata, tenant security settings, warehouse labels/locations, and connector support ownership through `/api/access/admin/...`, including deactivation, remapping, password resets, and cross-session invalidation
- tenant admins can also manage connector sync mode, validation policy, transformation policy, sync cadence, and default-warehouse fallback behavior through the workspace connector lane
- tenant workspace responses now also include `supportDiagnostics`, `supportIncidents`, and `recentSupportActivity` so tenant admins can work from a support-ready control lane instead of only a settings form

The system runtime surface at `GET /api/system/runtime` now also exposes lightweight control-center telemetry for:

- disabled connectors
- replay queue depth
- recent import issues
- recent audit failures
- active alert count
- fulfillment backlog and delay pressure
- dispatch queue depth and failed dispatch count
- dispatch cadence, queue batch size, and most recent dispatch completion
- cumulative tenant metrics for orders, fulfillment updates, imports, replay attempts, and dispatch processing
- recent business-event diagnostics over a rolling window
- latest business-event and latest failure-audit timestamps
- backend build version, commit, and build time metadata

The system incident inbox at `GET /api/system/incidents` now exposes the control-center incident lane directly from the backend instead of reconstructing it only in the browser, including failed internal dispatch work when the queued fan-out backbone needs attention.

The frontend runtime config now also carries frontend build version, commit, and build time so the system-status panel shows a full deployment fingerprint.

Backend console logs now include the current `X-Request-Id` correlation value plus the resolved tenant code and actor name when available, so incident cards, audit records, and server logs can be traced together during support work.

For external metrics scraping, the backend also exposes:

- `GET /actuator/prometheus`

The deployment smoke script now verifies both `GET /api/system/runtime` and
`GET /api/system/incidents`, and the repo includes a GitHub Actions workflow at
`.github/workflows/ci.yml` so backend tests, frontend build, and Compose
configuration validation run as a repeatable release gate. The repo now also includes `scripts/release-readiness.sh` so the selected prod env files, compose target, release fingerprint, and rollout commands can be summarized in one place before launch.

Explicit actor headers remain available as a backend fallback for tests, scripts, and non-UI flows:

- `X-Synapse-Actor`: acting operator name
- `X-Synapse-Tenant`: optional acting tenant code for fallback tooling
- `X-Synapse-Roles`: comma-separated control roles
- header fallback is enabled by default in `dev` and disabled by default in `prod`
- the actor must still exist in the seeded operator directory returned by `GET /api/access/operators`
- declared header roles must still already belong to that operator
- tenant onboarding requires `TENANT_ADMIN`
- operators may also be scoped to one or more warehouse codes; an empty scope means tenant-wide access
- scenario review actions require one of `REVIEW_OWNER`, `FINAL_APPROVER`, or `ESCALATION_OWNER`
- connector updates require `INTEGRATION_ADMIN`
- replaying failed inbound orders requires `INTEGRATION_OPERATOR` or `INTEGRATION_ADMIN`
- `POST /api/dev/reseed` development-only demo reset helper

Scoped operators now affect both actions and visibility:

- `GET /api/warehouses` returns only the warehouses in the current operator's assigned lane when a scoped session or fallback actor is present
- `GET /api/integrations/orders/connectors` and `GET /api/integrations/orders/replay-queue` honor the same warehouse lane
- scenario review and escalation actions are rejected when the acting operator is outside the scenario warehouse scope

Tenant admins can now create access lanes directly from the current tenant:

```bash
curl -X POST http://localhost:8080/api/access/admin/operators \
  -H "Content-Type: application/json" \
  -H "X-Synapse-Actor: Operations Lead" \
  -H "X-Synapse-Tenant: SYNAPSE-DEMO" \
  -H "X-Synapse-Roles: TENANT_ADMIN" \
  -d '{
    "actorName": "North Review Manager",
    "displayName": "North Review Manager",
    "description": "Warehouse-scoped review owner for north operations",
    "active": true,
    "roles": ["REVIEW_OWNER"],
    "warehouseScopes": ["WH-NORTH"]
  }'
```

Then issue a user account for that operator:

```bash
curl -X POST http://localhost:8080/api/access/admin/users \
  -H "Content-Type: application/json" \
  -H "X-Synapse-Actor: Operations Lead" \
  -H "X-Synapse-Tenant: SYNAPSE-DEMO" \
  -H "X-Synapse-Roles: TENANT_ADMIN" \
  -d '{
    "username": "north.review.manager",
    "fullName": "North Review Manager",
    "password": "north-lane-2026",
    "operatorActorName": "North Review Manager"
  }'
```

Update or deactivate a tenant user account:

```bash
curl -X PUT http://localhost:8080/api/access/admin/users/7 \
  -H "Content-Type: application/json" \
  -H "X-Synapse-Actor: Operations Lead" \
  -H "X-Synapse-Tenant: SYNAPSE-DEMO" \
  -H "X-Synapse-Roles: TENANT_ADMIN" \
  -d '{
    "fullName": "North Review Manager",
    "active": true,
    "operatorActorName": "North Final Manager"
  }'
```

Reset that tenant user password:

```bash
curl -X POST http://localhost:8080/api/access/admin/users/7/reset-password \
  -H "Content-Type: application/json" \
  -H "X-Synapse-Actor: Operations Lead" \
  -H "X-Synapse-Tenant: SYNAPSE-DEMO" \
  -H "X-Synapse-Roles: TENANT_ADMIN" \
  -d '{
    "password": "north-reset-2026"
  }'
```

Workspace metadata and support ownership can now be managed from the same tenant-admin lane:

```bash
curl -X PUT http://localhost:8080/api/access/admin/workspace \
  -H "Content-Type: application/json" \
  -H "X-Synapse-Actor: Operations Lead" \
  -H "X-Synapse-Tenant: SYNAPSE-DEMO" \
  -H "X-Synapse-Roles: TENANT_ADMIN" \
  -d '{
    "tenantName": "Synapse Demo Company Updated",
    "description": "Tenant admin support lane updated through workspace settings."
  }'

curl -X PUT http://localhost:8080/api/access/admin/workspace/warehouses/1 \
  -H "Content-Type: application/json" \
  -H "X-Synapse-Actor: Operations Lead" \
  -H "X-Synapse-Tenant: SYNAPSE-DEMO" \
  -H "X-Synapse-Roles: TENANT_ADMIN" \
  -d '{
    "name": "Warehouse North Prime",
    "location": "Johannesburg Prime"
  }'

curl -X PUT http://localhost:8080/api/access/admin/workspace/connectors/1 \
  -H "Content-Type: application/json" \
  -H "X-Synapse-Actor: Operations Lead" \
  -H "X-Synapse-Tenant: SYNAPSE-DEMO" \
  -H "X-Synapse-Roles: TENANT_ADMIN" \
  -d '{
    "supportOwnerActorName": "North Operations Director",
    "notes": "North operations director now owns webhook support."
  }'
```

Tenant workspace bootstrap example:

```bash
curl -X POST http://localhost:8080/api/access/tenants \
  -H "Content-Type: application/json" \
  -H "X-Synapse-Actor: Operations Lead" \
  -H "X-Synapse-Tenant: SYNAPSE-DEMO" \
  -H "X-Synapse-Roles: TENANT_ADMIN" \
  -d '{
    "tenantCode": "ACME-OPS",
    "tenantName": "Acme Operations",
    "description": "Starter workspace for a new operating team",
    "adminFullName": "Amina Dlamini",
    "adminUsername": "amina.admin",
    "adminPassword": "launchpad-2026",
    "primaryLocation": "Johannesburg",
    "secondaryLocation": "Cape Town"
  }'
```

The onboarding response includes the new tenant identity, the bootstrap admin username, the generated executive approver credentials, and the starter warehouse codes.

### Demo User Accounts

Use these seeded demo accounts when testing protected control actions:

Default tenant:

- `SYNAPSE-DEMO` -> `Synapse Demo Company`

- `operations.lead` / `lead-2026` -> `Operations Lead`
- `naledi.lead` / `naledi-2026` -> `Naledi Lead`
- `integration.lead` / `integration-admin-2026` -> `Integration Lead`
- `integration.operator` / `integration-ops-2026` -> `Integration Operator`
- `executive.ops.director` / `executive-approve-2026` -> `Executive Operations Director`

## Testing

Backend integration coverage focuses on the MVP proof loop:

- order ingestion
- inventory deduction
- low-stock detection
- alert generation
- recommendation generation
- simulation start and stop
- request tracing and audit log coverage

Run tests with:

```bash
cd backend
./mvnw test
```

On Windows PowerShell, use `mvnw.cmd test`.

## Demo Flow

1. Open the dashboard at `http://localhost:5173`
2. Use the hero-panel demo runway to reset the local baseline or reset and immediately start a live walkthrough
3. Verify seeded products and inventory are present
4. Post an order to `POST /api/orders`, send an external order to `POST /api/integrations/orders/webhook`, or start simulation
5. Use the what-if planner to preview risk before committing an order, compare two possible plans, and watch recent scenario history become part of the control center
6. Save a strong Scenario A as a named plan when you want to keep it for later
7. Approve a saved plan when it is ready to move from planning into action, or reject it with a review note when it needs revision
8. For escalated plans, complete owner review first and then send the plan to its assigned final approver before it becomes executable
9. Filter planning history by run type, approval state, approval policy, approval stage, warehouse, requester, review owner, assigned final approver, minimum review priority, overdue status, or SLA-escalated status when operators need ownership-focused or SLA-focused views
10. Reload a useful preview or rejected saved plan back into Scenario A when you want to refine or rerun the same idea
11. Save a rejected plan as a tracked revision so the next review cycle keeps its lineage visible in planning memory
12. Use the planner quick actions to jump straight to your request history, your pending review queue, your high-risk review queue, your escalated review queue, the final approval queue, your own final-approval queue, the overdue queue, or the SLA-escalated queue
13. Promote a saved preview or approved named plan into a live order directly from planning history when the projected option looks right
14. Watch the SLA escalation inbox when critical approvals miss their due time and get rerouted to a fallback executive approver
15. Use the scenario notifications panel to see reroutes and ownership acknowledgments as dedicated operational notices
16. Sign in through the operator session panel before approving plans, managing connectors, or replaying failed inbound orders so protected actions run through the signed-in control path
17. Use the preset account chips in the session panel when you want a faster switch into integration-admin or executive-approval demos
18. Choose the correct acting role in the planner before review actions: Review Owner for owner review, Final Approver for final approval, and Escalation Owner for claiming rerouted escalations
19. Use the integration operations panels to confirm which connectors are enabled and to inspect the most recent webhook or CSV import outcomes in the same control center
20. When inbound orders fail for a fixable operational reason, use the replay queue to send the stored failed request back through the real order flow after the missing condition is corrected
21. Import a CSV batch through `POST /api/integrations/orders/csv-import` when you want multiple external orders to enter SynapseCore through a real integration path
22. Acknowledge a rerouted escalation when someone has claimed it so it leaves the live inbox while remaining visible in planning history and notifications
23. Use the system incident inbox to monitor replay backlog, disabled connectors, audit failures, and action-required control notices without scanning multiple panels
24. Then watch inventory drop, alerts appear, recommendations update, recent events/audit activity roll in, and summary numbers change without refreshing

## Docs

- [Architecture](docs/architecture.md)
- [System Flow](docs/system-flow.md)
- [API Spec](docs/api-spec.md)
- [Verification Status](docs/verification-status.md)
- [Deployment Guide](docs/deployment.md)
- [Render Deployment](docs/render-deployment.md)
- [Live Deployment Runbook](docs/live-deployment-runbook.md)
- [Free Test Deployment Guide](docs/free-test-deployment.md)
- [Go-Live Checklist](docs/go-live-checklist.md)
