#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
GENERATOR="$ROOT_DIR/scripts/generate-company-fit-report.mjs"

echo "=================================================="
echo "SYNAPSECORE COMPANY FIT ANALYZER"
echo "=================================================="
echo "Repo root: $ROOT_DIR"
echo
echo "Using the real company-fit generator grounded in the current supported platform scope."
echo

if [ "$#" -gt 0 ]; then
  node "$GENERATOR" "$@"
else
  node "$GENERATOR" --all --format markdown
fi
