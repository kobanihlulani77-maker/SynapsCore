#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo "=================================================="
echo "SYNAPSECORE CONNECTIONS EXPLAINER"
echo "=================================================="
echo "Repo root: $ROOT_DIR"
echo
cat <<'EOF'
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
- Used when a system or internal tool sends a real order directly.

2. FULFILLMENT
- POST /api/fulfillment/updates
- Used when warehouse or logistics state changes after order creation.

3. INVENTORY
- POST /api/inventory/update
- Used when stock quantity or thresholds change.

4. WEBHOOK INTEGRATION
- POST /api/integrations/orders/webhook
- Used when another system pushes inbound order events into SynapseCore.

5. CSV IMPORT
- POST /api/integrations/orders/csv-import
- Used when batch files are the practical integration path.

6. REPLAY
- POST /api/integrations/orders/replay/{replayRecordId}
- Used when failed normalized inbound work should be retried safely.

HOW CONNECTOR POLICY WORKS

Connector policy controls whether and how inbound work is accepted.
Policies can shape:
- sync mode
- sync interval
- validation strength
- transformation rules
- default warehouse fallback
- support ownership

This means teams can tune integration behavior without changing core business code.

WHAT HAPPENS WHEN A COMPANY CONNECTS SYSTEMS

1. tenant workspace exists
2. tenant admins create or tune connectors
3. external source sends activity
4. SynapseCore validates and normalizes it
5. the normal order/inventory/fulfillment path runs
6. alerts, recommendations, and realtime updates follow naturally

WHAT HAPPENS WHEN INPUT IS BAD

SynapseCore is not supposed to silently swallow broken inbound work.

Instead it should:
- reject safely
- record the failure
- expose it in integration visibility
- keep replayable normalized work where possible
- let operators recover the lane later

WHAT A COMPANY NEEDS TO CONNECT

At minimum:
- tenant workspace
- users/operators
- warehouses
- products and inventory baseline
- connector definitions
- env/config values for the deployment target

BEST FIT CONNECTED SYSTEMS

SynapseCore fits above:
- ERP systems
- order systems
- warehouse systems
- inventory tools
- courier or logistics feeds
- spreadsheets used as operational import sources
- internal control workflows

BEST SHORT DESCRIPTION

SynapseCore connects to company systems through APIs, webhooks, CSV, and control
actions, validates and normalizes the activity, then feeds it into one live
operational model so the company sees one truth instead of disconnected tools.
EOF
