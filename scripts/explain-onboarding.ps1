Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$rootDir = Split-Path -Parent $PSScriptRoot

Write-Host "=================================================="
Write-Host "SYNAPSECORE ONBOARDING EXPLAINER"
Write-Host "=================================================="
Write-Host "Repo root: $rootDir"
Write-Host ""
@'
REAL TENANT ONBOARDING FLOW

1. Create the workspace through the correct production lane:
   - bootstrap token for the very first tenant on an empty production database
   - platform-admin token for later tenant provisioning

2. Define the operating footprint:
   - warehouses
   - operators
   - warehouse scopes
   - approval and support ownership

3. Create real users:
   - tenant admin
   - planner / reviewer
   - integration admin or operator
   - map each user to the correct operator lane

4. Configure real inputs:
   - connector source system
   - validation policy
   - transformation policy
   - default warehouse fallback only when the business truly needs it

5. Verify trust surfaces:
   - dashboard
   - runtime
   - incidents
   - integrations
   - replay queue
   - users and settings

6. Run hosted proof:
   - prepare-hosted-proof.ps1
   - frontend hosted Playwright proof

WHAT MUST BE TRUE BEFORE FIRST LIVE DAY

- the tenant can sign in cleanly
- warehouse scopes match real responsibilities
- connector support ownership is defined
- disabled connector failures create replay records instead of silent gaps
- manual recovery can be performed after connector repair
- dashboard, runtime, alerts, and audit views load for the real tenant

MOST IMPORTANT DOCS

- docs\onboarding-playbook.md
- docs\hosted-proof.md
- docs\replay-recovery.md
- docs\company-fit-playbook.md
'@ | Write-Host
