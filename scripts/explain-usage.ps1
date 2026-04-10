Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$rootDir = Split-Path -Parent $PSScriptRoot

Write-Host "=================================================="
Write-Host "SYNAPSECORE USAGE EXPLAINER"
Write-Host "=================================================="
Write-Host "Repo root: $rootDir"
Write-Host ""
@'
HOW A COMPANY USES SYNAPSECORE

1. ONBOARD THE WORKSPACE
- create a tenant workspace
- create operators and user accounts
- assign warehouse scopes and roles
- configure workspace policy and connectors

2. SIGN IN
- users sign into their tenant workspace and enter the operational shell

3. START ON THE DASHBOARD
The dashboard should answer:
- what is happening now?
- what is urgent?
- what changed recently?
- what approvals or escalations are waiting?

4. MOVE INTO FOCUSED PAGES
- Alerts
- Recommendations
- Orders
- Inventory
- Locations
- Fulfillment

5. USE CONTROL PAGES WHEN ACTION IS RISKY
- Scenarios
- Scenario History
- Approvals
- Escalations

6. USE SYSTEMS PAGES WHEN TRUST OR INPUTS MATTER
- Integrations
- Replay Queue
- Runtime
- Audit & Events

7. USE ADMIN PAGES TO CONTROL THE WORKSPACE
- Users
- Company Settings
- Profile
- Tenant Management / Platform / Releases

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
'@ | Write-Host
