#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo "=================================================="
echo "SYNAPSECORE EXPLAINER SUITE"
echo "=================================================="
echo "Repo root: $ROOT_DIR"
echo
echo "Use these scripts when you want the product explained in separated lanes."
echo
echo "1. Product"
echo "   bash scripts/explain-product.sh"
echo "   What SynapseCore is, what it does, and the full operating promise."
echo
echo "2. Architecture"
echo "   bash scripts/explain-architecture.sh"
echo "   How the platform is built across backend, frontend, realtime, and trust."
echo
echo "3. Connections"
echo "   bash scripts/explain-connections.sh"
echo "   How company systems connect into SynapseCore and what each inbound lane does."
echo
echo "4. Usage"
echo "   bash scripts/explain-usage.sh"
echo "   How a company team signs in, works, approves, recovers, and runs the platform daily."
echo
echo "5. API"
echo "   bash scripts/explain-api.sh"
echo "   Full backend surface by domain: orders, fulfillment, integrations, control, auth, and trust."
echo
echo "6. Pages"
echo "   bash scripts/explain-pages.sh"
echo "   Full product page map, route by route, and what each page is for."
echo
echo "7. Onboarding"
echo "   bash scripts/explain-onboarding.sh"
echo "   How a company is onboarded, configured, and brought into live use."
echo
echo "8. Testing"
echo "   bash scripts/explain-testing.sh"
echo "   What to verify, which scripts prove what, and what ready really means."
echo
echo "9. Deployment And Operations"
echo "   bash scripts/explain-deployment-operations.sh"
echo "   How to prepare envs, deploy, verify, back up, restore, and operate safely."
echo
echo "10. Company Fit"
echo "   bash scripts/explain-company-fit.sh"
echo "   Which companies SynapseCore fits, what value it gives them, and what changes operationally."
echo
echo "11. Existing Guided Repo Tour"
echo "   bash scripts/explain-project.sh"
echo "   Repo-first walkthrough of folders, packages, and the original MVP identity."
