Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$rootDir = Split-Path -Parent $PSScriptRoot

Write-Host "=================================================="
Write-Host "SYNAPSECORE DEPLOYMENT AND OPERATIONS EXPLAINER"
Write-Host "=================================================="
Write-Host "Repo root: $rootDir"
Write-Host ""
@'
DEPLOYMENT SHAPE

SynapseCore supports:
- local development stack
- production-shaped self-hosted stack
- public domain deployment with built-in Caddy edge routing
- release-readiness and config-gated deployment flow

MAIN OPERATIONS SCRIPTS

- powershell -ExecutionPolicy Bypass -File scripts\check-prod-config.ps1
- powershell -ExecutionPolicy Bypass -File scripts\release-readiness.ps1
- powershell -ExecutionPolicy Bypass -File scripts\prepare-prod-envs.ps1
- bash scripts/prepare-free-test-envs.sh
- powershell -ExecutionPolicy Bypass -File scripts\prepare-free-test-envs.ps1
- bash scripts/start-prod.sh
- powershell -ExecutionPolicy Bypass -File scripts\start-prod.ps1
- bash scripts/start-public-prod.sh
- powershell -ExecutionPolicy Bypass -File scripts\start-public-prod.ps1
- powershell -ExecutionPolicy Bypass -File scripts\verify-deployment.ps1
- bash scripts/backup-postgres.sh
- bash scripts/restore-postgres.sh --file backups/<backup>.sql --yes

HOW TO PREPARE FOR GO-LIVE

1. Generate real env targets
2. Fill in backend, frontend, and edge production env values
3. Run the production config gate
4. Run release readiness
5. Start either the self-hosted stack or the public domain stack
6. Run deployment smoke verification
7. Confirm auth, dashboard, integrations, planning, and trust surfaces

WHAT MUST BE VERIFIED AFTER DEPLOYMENT

- frontend loads
- backend readiness is UP
- runtime and incidents load
- websocket/realtime path works
- tenant sign-in works
- dashboard snapshot works
- integrations can ingest
- replay queue behaves correctly
- scenarios and approvals behave correctly
- release fingerprint matches the deployment

WHAT SAFE OPERATIONS LOOK LIKE

SynapseCore is designed to be operated with:
- explicit env files
- deployment gates
- smoke verification
- runtime trust surfaces
- metrics exposure
- backup and restore helpers

WHAT FAILURE OPERATIONS LOOK LIKE

When something breaks, the intended operator path is:
- inspect runtime and incidents
- inspect integrations or replay queue
- inspect audit and events
- recover through replay, corrected config, or restore if needed

FREE PUBLIC TEST DEPLOYMENT

SynapseCore also now supports a zero-domain-cost public test lane:
- use a free Ubuntu VM like Oracle Cloud Always Free
- run prepare-free-test-envs with the VM public IP and ACME email
- get app/api hostnames generated from sslip.io or nip.io
- launch the same public Caddy stack for real browser testing

BEST SHORT DESCRIPTION

SynapseCore already includes a strong launch and recovery pack. The safe path is:
prepare envs, gate config, summarize release readiness, deploy the right stack
for self-hosted or public rollout, smoke-check it, then operate with runtime trust,
replay, backup, and restore.
'@ | Write-Host
