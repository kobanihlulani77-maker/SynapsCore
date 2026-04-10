Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$rootDir = Split-Path -Parent $PSScriptRoot

Write-Host "=================================================="
Write-Host "SYNAPSECORE COMPANY FIT EXPLAINER"
Write-Host "=================================================="
Write-Host "Repo root: $rootDir"
Write-Host ""
@'
WHAT KIND OF COMPANIES SYNAPSECORE FITS

SynapseCore fits companies that have:
- operational pressure
- multiple systems or lanes
- stock, order, warehouse, fulfillment, or delivery complexity
- the need to detect risk early and act safely

BEST-FIT COMPANY TYPES

1. RETAIL AND MULTI-SITE COMMERCE
2. E-COMMERCE AND FULFILLMENT
3. LOGISTICS AND COURIER OPERATIONS
4. DISTRIBUTION AND WHOLESALE
5. MANUFACTURING OR MATERIAL-DEPENDENT OPERATIONS

WHAT SMALLER COMPANIES GET

For smaller companies SynapseCore can be:
- a simpler control center
- clearer visibility
- less manual cross-checking
- quicker value from one workspace

WHAT LARGER COMPANIES GET

For larger companies SynapseCore can be:
- a tenant-safe command center
- a stronger control and approval layer
- a cross-system intelligence surface
- a more supportable and traceable platform

WHAT IT WILL DO FOR COMPANIES

It helps companies:
- see operations faster
- understand risk earlier
- reduce stock and fulfillment surprises
- reduce delay blindness
- improve coordination across teams
- recover safely when connectors fail
- govern approvals instead of acting informally
- trust the platform through audit, runtime, and release visibility

WHAT IT WILL NOT DO BY MAGIC

SynapseCore does not magically fix a business without:
- deployment
- onboarding
- tenant and user setup
- product and warehouse baseline
- connector setup
- operational tuning
'@ | Write-Host
