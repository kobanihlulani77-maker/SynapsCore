#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo "=================================================="
echo "SYNAPSECORE PAGES EXPLAINER"
echo "=================================================="
echo "Repo root: $ROOT_DIR"
echo
cat <<'EOF'
SynapseCore is organized as a real product page system, not one overloaded dashboard.

PUBLIC PAGES

1. Landing (/)
- first impression
- explains the product promise
- shows SynapseCore as a premium operational command center

2. Product (/product)
- deeper product story
- explains visibility, prediction, recommendations, control, trust, and integrations

3. Sign In (/sign-in)
- tenant-aware sign-in surface
- enters the protected company workspace

4. Contact (/contact)
- business-fit and rollout capture page
- used to describe operational challenges and deployment intent

CORE PAGES

5. Dashboard (/dashboard)
- live command center
- shows summary, urgent actions, risks, alerts, recommendations, activity, approvals, and trust signals

6. Alerts (/alerts)
- warning center
- shows what is wrong, where it is happening, why it matters, and what should happen next

7. Recommendations (/recommendations)
- action queue
- shows ranked next actions based on live operational state

8. Orders (/orders)
- live order operations page
- tracks order flow, warehouse assignment, and order-linked pressure

9. Inventory (/inventory)
- inventory intelligence page
- shows stock posture, thresholds, velocity, risk, and depletion forecast

10. Locations (/locations)
- warehouse/site health page
- shows pressure, health, and issues by location

11. Fulfillment (/fulfillment)
- fulfillment and logistics page
- shows backlog, overdue dispatch, delayed shipments, and lane-level delivery pressure

CONTROL PAGES

12. Scenarios (/scenarios)
- decision lab
- preview changes before live execution

13. Scenario History (/scenario-history)
- history and comparison page
- reload, revise, compare, and revisit plans

14. Approvals (/approvals)
- approval queue
- tracks pending, approved, rejected, and overdue decisions

15. Escalations (/escalations)
- urgent operational inbox
- surfaces rerouted approvals and high-priority unresolved operational issues

SYSTEMS PAGES

16. Integrations (/integrations)
- connector management page
- shows health, policy, support ownership, and recent import posture

17. Replay Queue (/replay-queue)
- failed inbound recovery page
- lets operators inspect broken work and replay it safely

18. Runtime (/runtime)
- trust and observability page
- shows health, incidents, queue pressure, and system posture

19. Audit & Events (/audit-events)
- traceability page
- shows business events, audit records, and operational history

ADMIN PAGES

20. Users (/users)
- access and operator management
- create, edit, deactivate, and scope people correctly

21. Company Settings (/company-settings)
- tenant/workspace control page
- configures workspace profile, warehouse metadata, policies, and connector settings

22. Profile (/profile)
- personal session and security page
- password, rotation posture, and current-user controls

23. Platform Admin (/platform-admin)
- cross-workspace/admin posture page
- shows platform-level visibility and release posture

24. Tenant Management (/tenant-management)
- rollout and tenant portfolio page
- used for workspace onboarding and tenant readiness

25. System Config (/system-config)
- system behavior and operational defaults page
- explains global settings posture and environment controls

26. Releases (/releases)
- release and deployment trust page
- shows build fingerprints, readiness, and release status

HOW TO THINK ABOUT THE PAGE MAP

The page groups are intentional:
- Core pages increase awareness
- Control pages support safer decisions
- Systems pages build trust and recovery
- Admin pages control workspace structure and governance

BEST WAY TO READ THIS WITH THE REPO

Use this explainer with:
- frontend/src/App.jsx
- frontend/src/styles.css
EOF
