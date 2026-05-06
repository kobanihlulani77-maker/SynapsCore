Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$rootDir = Split-Path -Parent $PSScriptRoot

Write-Host "=================================================="
Write-Host "SYNAPSECORE DEPLOYMENT AND OPERATIONS EXPLAINER"
Write-Host "=================================================="
Write-Host "Repo root: $rootDir"
Write-Host ""
@'
FINAL REAL DEPLOYMENT SHAPE

SynapseCore now runs as a real hosted operational platform for its supported scope.

Current live truths:
- backend profile: prod
- schema posture: Flyway-backed startup with Hibernate validate
- realtime posture on Render: REDIS_PUBSUB
- browser sessions: Redis-backed in production
- hosted proof path: deterministic and browser-proven twice end to end

OFFICIAL HOSTED PROOF ORDER

1. powershell -ExecutionPolicy Bypass -File scripts\prepare-hosted-proof.ps1
2. cd frontend
3. npm.cmd run test:e2e:prod

WHAT THAT PROOF NOW COVERS

- auth and session behavior
- catalog onboarding
- realtime dashboard updates
- replay recovery and scenario approval
- runtime, integrations, users, settings, inventory, orders, alerts, and recommendations
- frontend-visible auth rate limiting

REPLAY AND RECOVERY TRUTH

- disabled connector CSV imports return structured CONNECTOR_DISABLED failures
- those failures create replay records immediately
- automated replay does not steal manual-only disabled-connector records
- operators can enable the connector and recover the order intentionally through manual replay

OPERATIONAL NOISE CLASSIFICATION

Broken pipe and ClientAbortException lines during browser navigation are treated as client disconnect noise.
They should not be read as new business-path breakage unless they line up with a failing proof step or a real requestId-backed server error.

MOST IMPORTANT DOCS

- docs\deployment.md
- docs\render-deployment.md
- docs\live-deployment-runbook.md
- docs\hosted-proof.md
- docs\runtime-observability.md
- docs\replay-recovery.md
'@ | Write-Host
