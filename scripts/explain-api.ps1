Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$rootDir = Split-Path -Parent $PSScriptRoot

Write-Host "=================================================="
Write-Host "SYNAPSECORE API EXPLAINER"
Write-Host "=================================================="
Write-Host "Repo root: $rootDir"
Write-Host ""
@'
The SynapseCore API is organized around operational domains.

1. ORDERS
- POST /api/orders
- GET /api/orders/recent
What it does:
- accepts an incoming order
- validates and processes it
- updates operational state
- affects inventory, fulfillment, alerts, recommendations, and events

2. INVENTORY
- POST /api/inventory/update
- GET /api/inventory
What it does:
- updates quantity and threshold posture
- re-runs stock pressure logic
- changes alerts and recommended action when needed

3. FULFILLMENT
- GET /api/fulfillment
- POST /api/fulfillment/updates
What it does:
- tracks queued, picking, packed, dispatched, delayed, delivered, and exception flow
- measures backlog, overdue dispatch work, and delivery-delay pressure

4. DASHBOARD AND DECISION OUTPUT
- GET /api/dashboard/summary
- GET /api/dashboard/snapshot
- GET /api/alerts
- GET /api/recommendations
What it does:
- returns the live control-center state
- exposes active alerts and ranked recommendations
- powers the main operational pages in the frontend

5. INTEGRATIONS
- POST /api/integrations/orders/webhook
- POST /api/integrations/orders/csv-import
- POST /api/integrations/orders/connectors
- GET /api/integrations/orders/imports/recent
What it does:
- accepts inbound work from other systems
- applies connector policy for validation, transformation, sync mode, and fallback behavior
- exposes connector health and recent import history

6. REPLAY AND RECOVERY
- POST /api/integrations/orders/replay/{id}
What it does:
- retries failed inbound work after the blocking condition is fixed
- preserves operational recovery instead of dropping business activity

7. SCENARIOS, APPROVALS, AND CONTROL
- POST /api/scenarios/order-impact
- POST /api/scenarios/order-impact/compare
- POST /api/scenarios/save
- GET /api/scenarios/history
- GET /api/scenarios/{id}/request
- POST /api/scenarios/{id}/approve
- POST /api/scenarios/{id}/reject
- POST /api/scenarios/{id}/execute
What it does:
- previews operational impact before live execution
- compares plans
- supports save, review, rejection, escalation, and controlled execution

8. AUTH, TENANTS, AND ACCESS
- POST /api/auth/session/login
- GET /api/auth/session
- POST /api/auth/session/password
- POST /api/auth/session/logout
- GET /api/access/tenants
- POST /api/access/tenants
- tenant admin endpoints under /api/access/admin/*
What it does:
- signs users into tenant-scoped workspaces
- enforces operator identity and warehouse-scoped access
- supports workspace onboarding and tenant administration

9. RUNTIME AND TRUST
- GET /api/system/runtime
- GET /api/system/incidents
- GET /actuator/health
- GET /actuator/health/readiness
- GET /actuator/prometheus
What it does:
- exposes runtime health, incidents, queue posture, and metrics
- gives operators and admins a trust surface, not just a business surface

10. REALTIME
- WebSocket/SockJS endpoint at /ws
- tenant topics under /topic/tenant/{TENANT_CODE}/...
What it does:
- pushes live updates without refresh
- keeps summary, alerts, recommendations, incidents, and control pages current

WHAT THIS MEANS FOR COMPANIES

The API is not only CRUD.
It is an operational engine surface that lets a company:
- feed real business activity into the platform
- inspect live state
- control planning and approval workflows
- recover failed inbound work
- trust system posture and health

BEST WAY TO READ THIS WITH THE REPO

Use this explainer together with:
- docs/api-spec.md
- docs/system-flow.md
- docs/architecture.md
'@ | Write-Host
