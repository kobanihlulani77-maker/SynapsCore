#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo "=================================================="
echo "SYNAPSECORE COMPANY FIT EXPLAINER"
echo "=================================================="
echo "Repo root: $ROOT_DIR"
echo
cat <<'EOF'
WHAT KIND OF COMPANIES SYNAPSECORE FITS

SynapseCore fits companies that have:
- operational pressure
- multiple systems or lanes
- stock, order, warehouse, fulfillment, or delivery complexity
- the need to detect risk early and act safely

BEST-FIT COMPANY TYPES

1. RETAIL AND MULTI-SITE COMMERCE
- stock-sensitive operations
- multiple stores or warehouses
- fast fulfillment promises
- location pressure and replenishment risk

2. E-COMMERCE AND FULFILLMENT
- high order velocity
- fulfillment backlog pressure
- SLA and delivery sensitivity
- stock mismatch risk

3. LOGISTICS AND COURIER OPERATIONS
- dispatch and delivery status pressure
- exception handling
- integration-heavy workflows
- need for a live operations surface

4. DISTRIBUTION AND WHOLESALE
- many stock locations
- transfer/rebalancing value
- high visibility need across warehouses

5. MANUFACTURING OR MATERIAL-DEPENDENT OPERATIONS
- shortage risk
- lane coordination
- downtime sensitivity
- need for alerts and escalation before impact lands

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

Once those are in place, it becomes a real operating advantage.

BEST SHORT DESCRIPTION

SynapseCore is best for operations-heavy companies that need one trusted place to
see what is happening, catch risk early, guide action, recover failures, and run
operations with more control than disconnected tools can provide.
EOF
