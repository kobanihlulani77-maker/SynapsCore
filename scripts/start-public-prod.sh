#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
INFRA_DIR="$ROOT_DIR/infrastructure"
BACKEND_ENV_FILE="${BACKEND_ENV_FILE:-./env/backend.prod.env}"
FRONTEND_ENV_FILE="${FRONTEND_ENV_FILE:-./env/frontend.prod.env}"
EDGE_ENV_FILE="${EDGE_ENV_FILE:-./env/edge.prod.env}"
SKIP_VERIFY=false

usage() {
  cat <<'EOF'
Usage: bash scripts/start-public-prod.sh [--skip-verify] [--allow-placeholder-env]

Environment overrides:
  BACKEND_ENV_FILE   Compose env file for backend service
  FRONTEND_ENV_FILE  Compose env file for frontend service
  EDGE_ENV_FILE      Edge/Caddy env file with public domains and ACME email
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

read_env_value() {
  local file_path="$1"
  local key="$2"
  local value
  value="$(grep -E "^${key}=" "$file_path" | head -n 1 | cut -d'=' -f2- || true)"
  if [[ -z "${value:-}" ]]; then
    echo "Missing required key $key in $file_path" >&2
    exit 1
  fi
  printf '%s' "$value"
}

assert_edge_placeholders() {
  local value="$1"
  if [[ "$value" =~ example\.com|change-me ]]; then
    echo "Edge env file still contains placeholder values: $EDGE_ENV_FILE" >&2
    exit 1
  fi
}

BACKEND_ENV_FILE="$BACKEND_ENV_FILE" FRONTEND_ENV_FILE="$FRONTEND_ENV_FILE" \
  bash "$ROOT_DIR/scripts/check-prod-config.sh" "${CHECK_ARGS[@]}"

EDGE_FILE="$INFRA_DIR/${EDGE_ENV_FILE#./}"
if [[ ! -f "$EDGE_FILE" ]]; then
  echo "Missing edge env file: $EDGE_FILE" >&2
  exit 1
fi

APP_DOMAIN="$(read_env_value "$EDGE_FILE" "SYNAPSECORE_APP_DOMAIN")"
API_DOMAIN="$(read_env_value "$EDGE_FILE" "SYNAPSECORE_API_DOMAIN")"
ACME_EMAIL="$(read_env_value "$EDGE_FILE" "SYNAPSECORE_ACME_EMAIL")"

if [[ "${ALLOW_PLACEHOLDER_ENV:-false}" != "true" ]]; then
  assert_edge_placeholders "$APP_DOMAIN"
  assert_edge_placeholders "$API_DOMAIN"
  assert_edge_placeholders "$ACME_EMAIL"
fi

echo "========================================"
echo "SYNAPSECORE PUBLIC PROD START"
echo "========================================"
echo "Backend env file : $BACKEND_ENV_FILE"
echo "Frontend env file: $FRONTEND_ENV_FILE"
echo "Edge env file    : $EDGE_ENV_FILE"
echo "App domain       : $APP_DOMAIN"
echo "API domain       : $API_DOMAIN"
echo

cd "$INFRA_DIR"
export BACKEND_ENV_FILE FRONTEND_ENV_FILE EDGE_ENV_FILE
docker compose -f docker-compose.public.yml up --build -d

if [[ "$SKIP_VERIFY" == "true" ]]; then
  echo
  echo "Public production stack started without smoke verification."
  exit 0
fi

echo
echo "Running public deployment smoke verification..."
FRONTEND_URL="https://$APP_DOMAIN" BACKEND_URL="https://$API_DOMAIN" \
  bash "$ROOT_DIR/scripts/verify-deployment.sh"
