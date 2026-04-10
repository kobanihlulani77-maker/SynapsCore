#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo "==> Backend tests"
(
  cd "$ROOT_DIR/backend"
  ./mvnw test
)

echo
echo "==> Frontend build"
(
  cd "$ROOT_DIR/frontend"
  npm ci
  npm run build
)

echo
echo "==> Docker Compose validation"
(
  cd "$ROOT_DIR/infrastructure"
  docker compose config >/dev/null
  docker compose -f docker-compose.prod.yml config >/dev/null
)

echo
echo "==> Production config safety"
(
  cd "$ROOT_DIR"
  bash scripts/check-prod-config.sh
  BACKEND_ENV_FILE=./env/backend.prod.example.env FRONTEND_ENV_FILE=./env/frontend.prod.example.env \
    bash scripts/check-prod-config.sh --allow-placeholders
)

echo
echo "==> Release readiness summary"
(
  cd "$ROOT_DIR"
  bash scripts/release-readiness.sh >/dev/null
)

echo
echo "SynapseCore CI verification checks passed."
