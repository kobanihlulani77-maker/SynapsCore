#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo "========================================"
echo "SYNAPSECORE SETUP"
echo "========================================"
echo

echo "Checking local dependencies..."
for cmd in docker java; do
  if command -v "$cmd" >/dev/null 2>&1; then
    echo "  [ok] $cmd"
  else
    echo "  [missing] $cmd"
  fi
done

if command -v npm >/dev/null 2>&1; then
  echo "  [ok] npm"
else
  echo "  [info] npm not found in current shell. On Windows PowerShell use npm.cmd if needed."
fi

echo
echo "Next steps:"
echo "1. Start infra:      cd \"$ROOT_DIR/infrastructure\" && docker compose up -d postgres redis"
echo "2. Run backend:      cd \"$ROOT_DIR/backend\" && ./mvnw spring-boot:run"
echo "3. Run frontend:     cd \"$ROOT_DIR/frontend\" && npm install && npm run dev"
echo "4. Open dashboard:   http://localhost:5173"
echo "5. Reseed demo data: bash \"$ROOT_DIR/scripts/seed.sh\""
echo
echo "Or run the full stack with Docker:"
echo "cd \"$ROOT_DIR/infrastructure\" && docker compose up --build"
