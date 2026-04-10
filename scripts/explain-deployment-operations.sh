#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo "=================================================="
echo "SYNAPSECORE DEPLOYMENT AND OPERATIONS EXPLAINER"
echo "=================================================="
echo "Repo root: $ROOT_DIR"
echo
cat <<'EOF'
DEPLOYMENT SHAPE

SynapseCore supports:
- local development stack
- production-shaped self-hosted stack
- public domain deployment with built-in Caddy edge routing
- release-readiness and config-gated deployment flow

MAIN OPERATIONS SCRIPTS

- bash scripts/check-prod-config.sh
  Validate production env assumptions before launch.

- bash scripts/release-readiness.sh
  Summarize release fingerprint, envs, and rollout commands.

- bash scripts/prepare-prod-envs.sh
  Generate editable production env targets from the example files.

- bash scripts/prepare-free-test-envs.sh
  Fill public test envs automatically from a free VM public IP plus ACME email.

- bash scripts/start-prod.sh
  Start the production-shaped compose stack.

- powershell -ExecutionPolicy Bypass -File scripts\start-prod.ps1
  Windows-native startup for the production-shaped self-hosted stack.

- bash scripts/start-public-prod.sh
  Start the public HTTPS/domain stack with Caddy.

- powershell -ExecutionPolicy Bypass -File scripts\start-public-prod.ps1
  Windows-native startup for the public HTTPS/domain stack.

- bash scripts/verify-deployment.sh
  Smoke-check frontend and backend after launch.

- bash scripts/backup-postgres.sh
  Create timestamped database backups.

- bash scripts/restore-postgres.sh --file backups/<backup>.sql --yes
  Restore a backup into the target database with guarded reset behavior.

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

WHAT IS STILL REQUIRED FOR FINAL REAL-WORLD PROOF

Even when the repo is ready, final proof requires:
- deployment on a real host
- real URL verification
- HTTPS and cookie behavior validation
- real end-to-end hosted smoke testing

BEST SHORT DESCRIPTION

SynapseCore already includes a strong launch and recovery pack. The safe path is:
prepare envs, gate config, summarize release readiness, deploy the right stack
for self-hosted or public rollout, smoke-check it, then operate with runtime trust,
replay, backup, and restore.
EOF
