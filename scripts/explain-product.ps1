Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$rootDir = Split-Path -Parent $PSScriptRoot

Write-Host "=================================================="
Write-Host "SYNAPSECORE PRODUCT EXPLAINER"
Write-Host "=================================================="
Write-Host "Repo root: $rootDir"
Write-Host ""
@'
SynapseCore is a real-time operational intelligence and control platform.

It is not:
- a static dashboard
- a simple inventory app
- a generic admin panel
- a BI report viewer

It is:
- a live operational command center
- a tenant-safe business workspace
- a decision-support system
- a control and governance surface
- a trust and recovery surface

FULL PRODUCT PROMISE

SynapseCore receives business activity, updates live operational state, interprets
risk, estimates likely near-term outcomes, generates alerts and recommendations,
pushes live updates to the right people, and gives teams one place to plan,
approve, execute, recover, and trust what happened.

WHAT IT DOES ALL THE TIME

SynapseCore is designed to run continuously:

1. receive activity
2. validate it
3. normalize it
4. update live state
5. evaluate risk
6. estimate what may happen next
7. generate guidance
8. push the change live
9. record traceability and trust signals

WHAT IT WORKS ON

The platform can operate across:
- orders
- inventory
- warehouses and locations
- fulfillment and logistics lanes
- alerts
- recommendations
- scenarios and approvals
- escalations
- integrations
- replay and recovery
- incidents and runtime trust
- tenant administration and user access

WHAT USERS SHOULD FEEL

A company should be able to log in and feel:
- we see operations in one place
- we know what matters now
- we know what is going wrong
- we know what is likely to go wrong next
- we know what action should happen
- we can act safely and traceably

WHAT MAKES THE PRODUCT DIFFERENT

The value is not only that SynapseCore records state.
The value is that it combines:
- live visibility
- prediction
- recommendation
- approvals and control
- integration awareness
- recovery tooling
- audit and runtime trust

That combination is what makes it an operational operating system instead of a
normal line-of-business UI.

WHAT IT DOES NOT PRETEND

SynapseCore does not honestly promise:
- that predictions are certainty
- that operations will never fail
- that integrations never break

Instead, it is built to:
- make risk visible early
- fail safely
- surface incidents
- support replay and recovery
- preserve trust and traceability

BEST SHORT DESCRIPTION

SynapseCore is a premium multi-tenant operational control system that sits above
company systems, turns activity into live understanding, highlights near-term
risk, recommends action, supports approvals and execution, and gives companies
one trusted place to run operations.
'@ | Write-Host
