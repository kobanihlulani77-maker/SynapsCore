#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo "=================================================="
echo "SYNAPSECORE ONBOARDING EXPLAINER"
echo "=================================================="
echo "Repo root: $ROOT_DIR"
echo
cat <<'EOF'
This explainer covers how a real company gets started in SynapseCore.

1. CREATE THE WORKSPACE
- create a tenant
- define tenant code and tenant name
- create the initial tenant admin

2. DEFINE THE OPERATING SHAPE
- create warehouses or locations
- define operator lanes
- assign roles
- assign warehouse scopes

3. CREATE PEOPLE
- create operators
- create user accounts
- map users to operators
- confirm password and session policy

4. CONNECT REAL INPUTS
- configure connectors
- choose sync mode
- choose validation policy
- choose transformation policy
- define default warehouse fallback if the business needs it

5. PREPARE TRUST AND SUPPORT
- review workspace settings
- review support ownership for connectors
- review runtime and incident visibility
- review who can replay, approve, and administer

6. START THE FIRST LIVE DAY
- sign in as tenant admin
- inspect dashboard, runtime, and integrations
- create or receive the first order/event
- verify alerts, recommendations, and fulfillment updates appear

7. TRAIN THE TEAM BY ROLE
- operators: orders, inventory, fulfillment, alerts, replay
- reviewers/approvers: scenarios, approvals, escalations
- admins: users, settings, connectors, trust surfaces

8. DAILY OPERATING MOTION
- open dashboard in the morning
- work the alerts and recommendations
- follow fulfillment and location pressure
- use scenarios for risky changes
- use replay and runtime pages when systems fail

9. WHAT A COMPANY SHOULD HAVE BEFORE GO-LIVE
- real tenant settings
- real users and scoped operators
- real connector policy
- known warehouses/locations
- tested inbound lanes
- approval ownership defined
- incident and replay handling understood

10. WHAT SUCCESS LOOKS LIKE

A company is onboarded correctly when:
- the right people can sign in
- the right pages are visible
- live business activity changes the control center
- alerts and recommendations show up meaningfully
- failures can be seen and replayed safely
- approvals and escalations route correctly

BEST WAY TO READ THIS WITH THE REPO

Use this explainer with:
- docs/deployment.md
- docs/system-flow.md
- scripts/verify-company-readiness.ps1
EOF
