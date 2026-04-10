#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo "=================================================="
echo "SYNAPSECORE USAGE EXPLAINER"
echo "=================================================="
echo "Repo root: $ROOT_DIR"
echo
cat <<'EOF'
HOW A COMPANY USES SYNAPSECORE

1. ONBOARD THE WORKSPACE

- create a tenant workspace
- create operators and user accounts
- assign warehouse scopes and roles
- configure workspace policy and connectors

2. SIGN IN

Users sign into their tenant workspace and enter the operational shell.

3. START ON THE DASHBOARD

The dashboard should answer:
- what is happening now?
- what is urgent?
- what changed recently?
- what approvals or escalations are waiting?

4. MOVE INTO FOCUSED PAGES

- Alerts
  review warnings, impact, and recommended response

- Recommendations
  see ranked actions by urgency

- Orders
  follow the live order stream and downstream effects

- Inventory
  inspect stock posture, risk, and depletion timing

- Locations
  inspect pressure by warehouse or site

- Fulfillment
  inspect backlog, dispatch, and delay pressure

5. USE CONTROL PAGES WHEN ACTION IS RISKY

- Scenarios
  preview what-if impact before changing live flow

- Scenario History
  reload, revise, compare, and execute prior plans

- Approvals
  route actions through review and staged release

- Escalations
  handle SLA-breached or urgent control items

6. USE SYSTEMS PAGES WHEN TRUST OR INPUTS MATTER

- Integrations
  inspect connectors and import posture

- Replay Queue
  recover failed inbound work

- Runtime
  inspect incidents, queue, and metrics posture

- Audit & Events
  understand exactly what happened

7. USE ADMIN PAGES TO CONTROL THE WORKSPACE

- Users
  manage operators, users, roles, and scope

- Company Settings
  tune workspace profile, policy, warehouses, and connector ownership

- Profile
  manage personal password and session hygiene

- Tenant Management / Platform / Releases
  used where rollout and platform-level oversight is appropriate

WHAT DAILY OPERATION LOOKS LIKE

Morning:
- open dashboard
- inspect alerts
- inspect incidents
- inspect approvals and escalations

During the day:
- follow orders and fulfillment pressure
- act on recommendations
- replay failed work if needed
- route plans through review

When something goes wrong:
- inspect alerts
- inspect runtime or integration state
- use audit/events for traceability
- recover with replay or corrected action

End of day:
- review what happened
- review what is still risky
- review what actions are still pending

WHAT THE USER EXPERIENCE SHOULD FEEL LIKE

Users should feel:
- the system is calm under pressure
- the urgent item is obvious
- action is clearer than guesswork
- approval paths are safe
- trust is visible

BEST SHORT DESCRIPTION

Companies use SynapseCore by signing into a tenant workspace, watching live
operations, moving from awareness into action, routing risky changes through
approval, recovering failures safely, and staying inside one trusted control center.
EOF
