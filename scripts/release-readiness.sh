#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
INFRA_DIR="$ROOT_DIR/infrastructure"
COMPOSE_FILE="${COMPOSE_FILE:-$INFRA_DIR/docker-compose.prod.yml}"
BACKEND_ENV_FILE="${BACKEND_ENV_FILE:-./env/backend.prod.selfhost.env}"
FRONTEND_ENV_FILE="${FRONTEND_ENV_FILE:-./env/frontend.prod.selfhost.env}"
ALLOW_PLACEHOLDER_ENV="${ALLOW_PLACEHOLDER_ENV:-false}"

usage() {
  cat <<'EOF'
Usage:
  bash scripts/release-readiness.sh
  BACKEND_ENV_FILE=./env/backend.prod.env FRONTEND_ENV_FILE=./env/frontend.prod.env bash scripts/release-readiness.sh
  bash scripts/release-readiness.sh --allow-placeholder-env

What it does:
  - runs the SynapseCore prod-config safety gate
  - validates the selected production compose file
  - prints a release summary with build fingerprints, runtime endpoints, and rollout commands
EOF
}

resolve_env_path() {
  local raw_path="$1"
  case "$raw_path" in
    /*) printf '%s\n' "$raw_path" ;;
    *) printf '%s\n' "$INFRA_DIR/${raw_path#./}" ;;
  esac
}

get_env_value() {
  local file_path="$1"
  local key="$2"
  local line
  line="$(grep -E "^${key}=" "$file_path" | tail -n 1 || true)"
  printf '%s\n' "${line#*=}"
}

while (($#)); do
  case "$1" in
    --allow-placeholder-env)
      ALLOW_PLACEHOLDER_ENV="true"
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
if [[ "$ALLOW_PLACEHOLDER_ENV" == "true" ]]; then
  CHECK_ARGS+=(--allow-placeholders)
fi

BACKEND_ENV_FILE="$BACKEND_ENV_FILE" FRONTEND_ENV_FILE="$FRONTEND_ENV_FILE" \
  bash "$ROOT_DIR/scripts/check-prod-config.sh" "${CHECK_ARGS[@]}" >/dev/null

docker compose -f "$COMPOSE_FILE" config >/dev/null

BACKEND_FILE="$(resolve_env_path "$BACKEND_ENV_FILE")"
FRONTEND_FILE="$(resolve_env_path "$FRONTEND_ENV_FILE")"

backend_profile="$(get_env_value "$BACKEND_FILE" "SPRING_PROFILES_ACTIVE")"
backend_origin="$(get_env_value "$BACKEND_FILE" "CORS_ALLOWED_ORIGINS")"
backend_cookie_secure="$(get_env_value "$BACKEND_FILE" "SESSION_COOKIE_SECURE")"
backend_version="$(get_env_value "$BACKEND_FILE" "SYNAPSECORE_BUILD_VERSION")"
backend_commit="$(get_env_value "$BACKEND_FILE" "SYNAPSECORE_BUILD_COMMIT")"
backend_build_time="$(get_env_value "$BACKEND_FILE" "SYNAPSECORE_BUILD_TIME")"
frontend_api_url="$(get_env_value "$FRONTEND_FILE" "VITE_API_URL")"
frontend_ws_url="$(get_env_value "$FRONTEND_FILE" "VITE_WS_URL")"
frontend_version="$(get_env_value "$FRONTEND_FILE" "VITE_APP_BUILD_VERSION")"
frontend_commit="$(get_env_value "$FRONTEND_FILE" "VITE_APP_BUILD_COMMIT")"
frontend_build_time="$(get_env_value "$FRONTEND_FILE" "VITE_APP_BUILD_TIME")"

echo "========================================"
echo "SYNAPSECORE RELEASE READINESS"
echo "========================================"
echo "Compose file       : $COMPOSE_FILE"
echo "Backend env file   : $BACKEND_ENV_FILE"
echo "Frontend env file  : $FRONTEND_ENV_FILE"
echo
echo "Backend fingerprint"
echo "  Profile          : $backend_profile"
echo "  Version          : $backend_version"
echo "  Commit           : $backend_commit"
echo "  Build time       : $backend_build_time"
echo "  Allowed origins  : $backend_origin"
echo "  Secure cookies   : $backend_cookie_secure"
echo
echo "Frontend fingerprint"
echo "  Version          : $frontend_version"
echo "  Commit           : $frontend_commit"
echo "  Build time       : $frontend_build_time"
echo "  API URL          : $frontend_api_url"
echo "  WS URL           : $frontend_ws_url"
echo
echo "Release commands"
echo "  Start            : BACKEND_ENV_FILE=$BACKEND_ENV_FILE FRONTEND_ENV_FILE=$FRONTEND_ENV_FILE bash scripts/start-prod.sh"
echo "  Smoke verify     : FRONTEND_URL=http://localhost BACKEND_URL=http://localhost:8080 bash scripts/verify-deployment.sh"
echo "  Backup           : bash scripts/backup-postgres.sh"
echo "  Restore          : bash scripts/restore-postgres.sh --file backups/<backup>.sql --yes"
echo
echo "Release readiness checks passed."
