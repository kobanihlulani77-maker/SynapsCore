#!/usr/bin/env bash

# SynapseCore reseed helper
# -------------------------
# This script is for developer/demo convenience, not production automation.
# Its main job is to restore the platform to a meaningful operational baseline
# so the dashboard, simulation, and order flow start from a known state.
#
# By default it:
# 1. verifies the backend is reachable
# 2. calls the development reseed endpoint
# 3. prints the returned seed summary
#
# Optional flags:
#   --with-simulation   reseed first, then start simulation mode
#   --api-url <url>     use a different backend base URL
#   --help              show usage

set -euo pipefail

API_URL="http://localhost:8080"
WITH_SIMULATION="false"
HEALTH_ATTEMPTS=30
HEALTH_DELAY_SECONDS=2

while [[ $# -gt 0 ]]; do
  case "$1" in
    --with-simulation)
      WITH_SIMULATION="true"
      shift
      ;;
    --api-url)
      API_URL="${2:?Missing value for --api-url}"
      shift 2
      ;;
    --help|-h)
      cat <<'EOF'
Usage:
  bash scripts/seed.sh
  bash scripts/seed.sh --with-simulation
  bash scripts/seed.sh --api-url http://localhost:8080

What it does:
  - calls POST /api/dev/reseed
  - restores SynapseCore starter products, warehouses, and inventory
  - clears prior orders, alerts, recommendations, and business events
  - optionally starts simulation after the reset
EOF
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      exit 1
      ;;
  esac
done

echo "========================================"
echo "SYNAPSECORE RESEED"
echo "========================================"
echo
echo "Target API: $API_URL"
echo "Checking backend health..."
backend_ready="false"
for ((attempt = 1; attempt <= HEALTH_ATTEMPTS; attempt++)); do
  if curl -fsS "$API_URL/actuator/health" >/dev/null 2>&1; then
    backend_ready="true"
    break
  fi
  sleep "$HEALTH_DELAY_SECONDS"
done

if [[ "$backend_ready" != "true" ]]; then
  echo "Backend did not become ready after $((HEALTH_ATTEMPTS * HEALTH_DELAY_SECONDS)) seconds." >&2
  exit 1
fi

echo "Backend reachable."
echo

echo "Resetting starter data..."
curl -fsS -X POST "$API_URL/api/dev/reseed"
echo

if [[ "$WITH_SIMULATION" == "true" ]]; then
  echo
  echo "Starting simulation after reseed..."
  curl -fsS -X POST "$API_URL/api/simulation/start"
  echo
fi
