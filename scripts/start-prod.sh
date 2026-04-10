#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
INFRA_DIR="$ROOT_DIR/infrastructure"
BACKEND_ENV_FILE="${BACKEND_ENV_FILE:-./env/backend.prod.selfhost.env}"
FRONTEND_ENV_FILE="${FRONTEND_ENV_FILE:-./env/frontend.prod.selfhost.env}"
SKIP_VERIFY=false

usage() {
  cat <<'EOF'
Usage: bash scripts/start-prod.sh [--skip-verify] [--allow-placeholder-env]

Environment overrides:
  BACKEND_ENV_FILE   Compose env file for backend service
  FRONTEND_ENV_FILE  Compose env file for frontend service
  FRONTEND_URL       Verification target for frontend health/runtime checks
  BACKEND_URL        Verification target for backend health/runtime checks
EOF
}

while (($#)); do
  case "$1" in
    --skip-verify)
      SKIP_VERIFY=true
      ;;
    --allow-placeholder-env)
      ALLOW_PLACEHOLDER_ENV=true
      ;;
    --help|-h)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      usage
      exit 1
      ;;
  esac
  shift
done

CHECK_ARGS=()
if [[ "${ALLOW_PLACEHOLDER_ENV:-false}" == "true" ]]; then
  CHECK_ARGS+=(--allow-placeholders)
fi

BACKEND_ENV_FILE="$BACKEND_ENV_FILE" FRONTEND_ENV_FILE="$FRONTEND_ENV_FILE" \
  bash "$ROOT_DIR/scripts/check-prod-config.sh" "${CHECK_ARGS[@]}"

echo "========================================"
echo "SYNAPSECORE PROD START"
echo "========================================"
echo "Backend env file : $BACKEND_ENV_FILE"
echo "Frontend env file: $FRONTEND_ENV_FILE"
echo

cd "$INFRA_DIR"
export BACKEND_ENV_FILE FRONTEND_ENV_FILE
docker compose -f docker-compose.prod.yml up --build -d

if [[ "$SKIP_VERIFY" == "true" ]]; then
  echo
  echo "Production-shaped stack started without smoke verification."
  exit 0
fi

echo
echo "Running deployment smoke verification..."
bash "$ROOT_DIR/scripts/verify-deployment.sh"
