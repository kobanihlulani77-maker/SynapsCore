#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo "=================================================="
echo "SYNAPSECORE TESTING EXPLAINER"
echo "=================================================="
echo "Repo root: $ROOT_DIR"
echo
cat <<'EOF'
This explainer covers what should be verified, what each verification step proves,
and what "ready" means for SynapseCore.

1. CODE AND BUILD PROOF
- backend tests
- frontend production build
- compose config validation
What this proves:
- core backend behavior is holding
- frontend ships cleanly
- deployment contracts resolve

2. RELEASE GATE
- scripts/check-prod-config.sh
- scripts/release-readiness.sh
- scripts/prepare-prod-envs.sh
What this proves:
- env files are complete
- build fingerprints are present
- rollout inputs are shaped correctly

3. DEPLOYMENT SMOKE
- scripts/verify-deployment.sh
- scripts/verify-deployment.ps1
What this proves:
- frontend routes are reachable
- backend health is up
- dashboard and runtime surfaces respond
- auth and control-center basics are alive

4. COMPANY-READINESS FLOW
- scripts/verify-company-readiness.ps1
What this proves:
- workspace onboarding works
- tenant admin flow works
- connectors work
- webhook ingestion works
- replay works
- planning and approvals work
- fulfillment flow works
- trust surfaces work

5. MANUAL PAGE-BY-PAGE QA
What should be checked in the browser:
- landing/product/sign-in/contact
- dashboard
- alerts and recommendations
- orders, inventory, locations, fulfillment
- scenarios, approvals, escalations
- integrations and replay queue
- runtime and audit
- users, settings, profile, tenant management, releases

6. REALTIME QA
What should be checked:
- summary updates without refresh
- alert/recommendation changes appear live
- incident and integration posture update live
- the correct tenant receives the correct changes

7. WHAT "READY FOR COMPANIES" MEANS

Repo-ready means:
- code is implemented
- tests pass
- build passes
- deployment scripts and env gates exist

Runtime-ready means:
- a healthy environment is running
- deployment smoke passes
- company-readiness flow passes
- real inbound paths are tested

Company-ready means:
- the workspace can be onboarded
- the team can operate safely by role
- failures are visible and recoverable
- planning and approval control works
- runtime trust is visible

8. HONEST FINAL RULE

No serious system should claim "nothing can fail."
SynapseCore should be considered proven when it is:
- tested
- deployable
- observable
- recoverable
- and smoke-verified on the target environment

BEST WAY TO READ THIS WITH THE REPO

Use this explainer with:
- scripts/ci-verify.sh
- scripts/check-prod-config.sh
- scripts/release-readiness.sh
- scripts/verify-deployment.sh
- scripts/verify-deployment.ps1
- scripts/verify-company-readiness.ps1
- scripts/generate-verification-report.sh
- scripts/generate-verification-report.ps1

On Windows/WSL, the bash generator automatically hands off to the PowerShell generator so localhost route and company-readiness checks still verify the live stack correctly.
EOF
