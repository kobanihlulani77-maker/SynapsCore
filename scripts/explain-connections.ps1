Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$rootDir = Split-Path -Parent $PSScriptRoot

Write-Host "=================================================="
Write-Host "SYNAPSECORE CONNECTIONS EXPLAINER"
Write-Host "=================================================="
Write-Host "Repo root: $rootDir"
Write-Host ""
@'
HOW COMPANIES CONNECT TO SYNAPSECORE

SynapseCore is meant to sit above company systems, not replace them.

It can receive activity from:
- direct API clients
- webhooks
- CSV imports
- manual operator actions in the UI
- replayed failed inbound work
- local development reseed for controlled verification

PRIMARY INBOUND LANES

1. ORDERS
- POST /api/orders

2. FULFILLMENT
- POST /api/fulfillment/updates

3. INVENTORY
- POST /api/inventory/update

4. WEBHOOK INTEGRATION
- POST /api/integrations/orders/webhook

5. CSV IMPORT
- POST /api/integrations/orders/csv-import

6. REPLAY
- POST /api/integrations/orders/replay/{replayRecordId}

HOW CONNECTOR POLICY WORKS

Connector policy controls:
- sync mode
- sync interval
- validation strength
- transformation rules
- default warehouse fallback
- support ownership

WHAT HAPPENS WHEN A COMPANY CONNECTS SYSTEMS

1. tenant workspace exists
2. tenant admins create or tune connectors
3. external source sends activity
4. SynapseCore validates and normalizes it
5. the normal order/inventory/fulfillment path runs
6. alerts, recommendations, and realtime updates follow naturally

WHAT HAPPENS WHEN INPUT IS BAD

SynapseCore should:
- reject safely
- record the failure
- expose it in integration visibility
- keep replayable normalized work where possible
- let operators recover the lane later

BEST FIT CONNECTED SYSTEMS

SynapseCore fits above:
- ERP systems
- order systems
- warehouse systems
- inventory tools
- courier or logistics feeds
- spreadsheets used as operational import sources
- internal control workflows
'@ | Write-Host
