#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
INFRA_DIR="$ROOT_DIR/infrastructure"
BACKEND_ENV_FILE="${BACKEND_ENV_FILE:-./env/backend.prod.selfhost.env}"
FRONTEND_ENV_FILE="${FRONTEND_ENV_FILE:-./env/frontend.prod.selfhost.env}"
ALLOW_PLACEHOLDERS="false"

usage() {
  cat <<'EOF'
Usage:
  bash scripts/check-prod-config.sh
  BACKEND_ENV_FILE=./env/backend.prod.example.env FRONTEND_ENV_FILE=./env/frontend.prod.example.env bash scripts/check-prod-config.sh --allow-placeholders

Checks:
  - backend/frontend prod env files exist
  - required values are present
  - production safety flags stay aligned with SynapseCore expectations
  - placeholder/example values are blocked unless explicitly allowed
EOF
}

resolve_env_path() {
  local raw_path="$1"
  case "$raw_path" in
    /*) printf '%s\n' "$raw_path" ;;
    *) printf '%s\n' "$INFRA_DIR/${raw_path#./}" ;;
  esac
}

require_file() {
  local label="$1"
  local path="$2"
  if [[ ! -f "$path" ]]; then
    echo "Missing $label env file: $path" >&2
    exit 1
  fi
}

get_env_value() {
  local file_path="$1"
  local key="$2"
  local line
  line="$(grep -E "^${key}=" "$file_path" | tail -n 1 || true)"
  printf '%s\n' "${line#*=}"
}

get_first_non_empty_env_value() {
  local file_path="$1"
  shift
  local key
  local value
  for key in "$@"; do
    value="$(get_env_value "$file_path" "$key")"
    if [[ -n "$value" ]]; then
      printf '%s\n' "$value"
      return 0
    fi
  done
  printf '\n'
}

require_value() {
  local file_path="$1"
  local key="$2"
  local value
  value="$(get_env_value "$file_path" "$key")"
  if [[ -z "$value" ]]; then
    echo "Missing required key $key in $file_path" >&2
    exit 1
  fi
  printf '%s\n' "$value"
}

require_equals() {
  local file_path="$1"
  local key="$2"
  local expected="$3"
  local actual
  actual="$(require_value "$file_path" "$key")"
  if [[ "$actual" != "$expected" ]]; then
    echo "Expected $key=$expected in $file_path but found $actual" >&2
    exit 1
  fi
}

while (($#)); do
  case "$1" in
    --allow-placeholders)
      ALLOW_PLACEHOLDERS="true"
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

BACKEND_FILE="$(resolve_env_path "$BACKEND_ENV_FILE")"
FRONTEND_FILE="$(resolve_env_path "$FRONTEND_ENV_FILE")"

require_file "backend" "$BACKEND_FILE"
require_file "frontend" "$FRONTEND_FILE"

backend_profile="$(require_value "$BACKEND_FILE" "SPRING_PROFILES_ACTIVE")"
backend_datasource_url="$(get_first_non_empty_env_value "$BACKEND_FILE" "SPRING_DATASOURCE_URL" "DATABASE_URL")"
backend_db_host="$(get_env_value "$BACKEND_FILE" "DB_HOST")"
backend_db_name="$(get_env_value "$BACKEND_FILE" "DB_NAME")"
backend_db_user="$(get_env_value "$BACKEND_FILE" "DB_USER")"
backend_db_password="$(get_env_value "$BACKEND_FILE" "DB_PASSWORD")"
backend_redis_url="$(require_value "$BACKEND_FILE" "SPRING_DATA_REDIS_URL")"
backend_cors_allowed="$(require_value "$BACKEND_FILE" "CORS_ALLOWED_ORIGINS")"
backend_cookie_secure="$(require_value "$BACKEND_FILE" "SESSION_COOKIE_SECURE")"
backend_same_site="$(require_value "$BACKEND_FILE" "SESSION_COOKIE_SAME_SITE")"
backend_header_fallback="$(require_value "$BACKEND_FILE" "ALLOW_HEADER_FALLBACK")"
backend_ddl_auto="$(require_value "$BACKEND_FILE" "SPRING_JPA_HIBERNATE_DDL_AUTO")"
backend_build_version="$(require_value "$BACKEND_FILE" "SYNAPSECORE_BUILD_VERSION")"
backend_build_commit="$(require_value "$BACKEND_FILE" "SYNAPSECORE_BUILD_COMMIT")"
backend_build_time="$(require_value "$BACKEND_FILE" "SYNAPSECORE_BUILD_TIME")"
frontend_api_url="$(require_value "$FRONTEND_FILE" "VITE_API_URL")"
frontend_ws_url="$(require_value "$FRONTEND_FILE" "VITE_WS_URL")"
frontend_build_version="$(require_value "$FRONTEND_FILE" "VITE_APP_BUILD_VERSION")"
frontend_build_commit="$(require_value "$FRONTEND_FILE" "VITE_APP_BUILD_COMMIT")"
frontend_build_time="$(require_value "$FRONTEND_FILE" "VITE_APP_BUILD_TIME")"

require_equals "$BACKEND_FILE" "SPRING_PROFILES_ACTIVE" "prod"
require_equals "$BACKEND_FILE" "ALLOW_HEADER_FALLBACK" "false"
require_equals "$BACKEND_FILE" "SPRING_JPA_HIBERNATE_DDL_AUTO" "update"

if [[ -z "$backend_datasource_url" ]]; then
  if [[ -z "$backend_db_host" || -z "$backend_db_name" || -z "$backend_db_user" || -z "$backend_db_password" ]]; then
    echo "Backend env must define DATABASE_URL or SPRING_DATASOURCE_URL, or the DB_HOST/DB_NAME/DB_USER/DB_PASSWORD fallback set in $BACKEND_FILE" >&2
    exit 1
  fi
fi

if [[ "$backend_same_site" != "Lax" && "$backend_same_site" != "Strict" && "$backend_same_site" != "None" ]]; then
  echo "SESSION_COOKIE_SAME_SITE must be one of Lax, Strict, or None in $BACKEND_FILE" >&2
  exit 1
fi

if [[ "$backend_cors_allowed" == *"https://"* && "$backend_cookie_secure" != "true" ]]; then
  echo "SESSION_COOKIE_SECURE must be true when using HTTPS origins in $BACKEND_FILE" >&2
  exit 1
fi

if [[ "$frontend_api_url" == https://* && "$backend_cookie_secure" != "true" ]]; then
  echo "SESSION_COOKIE_SECURE must be true when VITE_API_URL is HTTPS" >&2
  exit 1
fi

if [[ "$frontend_ws_url" != */ws ]]; then
  echo "VITE_WS_URL should end with /ws in $FRONTEND_FILE" >&2
  exit 1
fi

if [[ "$ALLOW_PLACEHOLDERS" != "true" ]]; then
  if printf '%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n' \
    "$backend_datasource_url" \
    "$backend_db_host" \
    "$backend_db_name" \
    "$backend_db_user" \
    "$backend_db_password" \
    "$backend_redis_url" \
    "$backend_cors_allowed" \
    "$backend_build_commit" \
    "$backend_build_time" \
    | grep -Eq 'change-me|example\.com|example\.internal|set-at-release'; then
    echo "Backend env file still contains placeholder or example values: $BACKEND_FILE" >&2
    exit 1
  fi

  if printf '%s\n%s\n%s\n%s\n%s\n' "$frontend_api_url" "$frontend_ws_url" "$frontend_build_version" "$frontend_build_commit" "$frontend_build_time" | grep -Eq 'example\.com|set-at-release'; then
    echo "Frontend env file still contains placeholder example domains: $FRONTEND_FILE" >&2
    exit 1
  fi
fi

echo "SynapseCore prod config checks passed."
