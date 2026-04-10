#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MODE="${1:-full}"

echo "========================================"
echo "SYNAPSECORE START"
echo "========================================"
echo

cd "$ROOT_DIR/infrastructure"

if [[ "$MODE" == "infra" ]]; then
  echo "Starting PostgreSQL and Redis only..."
  docker compose up -d postgres redis
else
  echo "Starting the full SynapseCore stack..."
  docker compose up --build
fi

echo
echo "For a clean demo baseline after startup, run:"
echo "  bash \"$ROOT_DIR/scripts/seed.sh\""
