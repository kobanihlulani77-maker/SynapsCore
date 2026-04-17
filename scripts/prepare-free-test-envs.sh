#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_DIR="$ROOT_DIR/infrastructure/env"
BACKEND_TARGET="$ENV_DIR/backend.prod.env"
FRONTEND_TARGET="$ENV_DIR/frontend.prod.env"
EDGE_TARGET="$ENV_DIR/edge.prod.env"
PUBLIC_IP=""
ACME_EMAIL=""
DNS_SUFFIX="sslip.io"
DB_PASSWORD=""
FORCE="false"

usage() {
  cat <<'EOF'
Usage:
  bash scripts/prepare-free-test-envs.sh --public-ip 203.0.113.10 --email you@example.com [options]

Options:
  --public-ip IP         Public IPv4 address of the free test VM
  --email EMAIL          Email for Caddy / ACME certificate registration
  --dns-suffix SUFFIX    sslip.io or nip.io (default: sslip.io)
  --db-password VALUE    Optional explicit Postgres password
  --force                Recopy env templates before filling values
  --help                 Show this help
EOF
}

while (($#)); do
  case "$1" in
    --public-ip)
      PUBLIC_IP="$2"
      shift 2
      ;;
    --email)
      ACME_EMAIL="$2"
      shift 2
      ;;
    --dns-suffix)
      DNS_SUFFIX="$2"
      shift 2
      ;;
    --db-password)
      DB_PASSWORD="$2"
      shift 2
      ;;
    --force)
      FORCE="true"
      shift
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
done

if [[ -z "$PUBLIC_IP" || -z "$ACME_EMAIL" ]]; then
  usage
  exit 1
fi

if [[ ! "$PUBLIC_IP" =~ ^([0-9]{1,3}\.){3}[0-9]{1,3}$ ]]; then
  echo "Public IP must be a dotted IPv4 address." >&2
  exit 1
fi

if [[ "$DNS_SUFFIX" != "sslip.io" && "$DNS_SUFFIX" != "nip.io" ]]; then
  echo "DNS suffix must be sslip.io or nip.io." >&2
  exit 1
fi

set_env_value() {
  local file_path="$1"
  local key="$2"
  local value="$3"
  local temp_file
  temp_file="$(mktemp)"
  awk -v key="$key" -v value="$value" '
    BEGIN { updated = 0 }
    $0 ~ ("^" key "=") {
      print key "=" value
      updated = 1
      next
    }
    { print }
    END {
      if (!updated) {
        print key "=" value
      }
    }
  ' "$file_path" >"$temp_file"
  mv "$temp_file" "$file_path"
}

read_env_value() {
  local file_path="$1"
  local key="$2"
  grep -E "^${key}=" "$file_path" | head -n 1 | cut -d'=' -f2- || true
}

generate_password() {
  if command -v openssl >/dev/null 2>&1; then
    openssl rand -hex 16
    return
  fi
  if command -v uuidgen >/dev/null 2>&1; then
    uuidgen | tr -d '-' | cut -c1-32
    return
  fi
  printf 'synapsecore%s' "$(date +%s)"
}

PREPARE_ARGS=()
if [[ "$FORCE" == "true" ]]; then
  PREPARE_ARGS+=(--force)
fi
bash "$ROOT_DIR/scripts/prepare-prod-envs.sh" "${PREPARE_ARGS[@]}"

existing_db_password="$(read_env_value "$BACKEND_TARGET" "DB_PASSWORD")"
if [[ -z "$DB_PASSWORD" ]]; then
  if [[ -z "$existing_db_password" || "$existing_db_password" =~ change-me|example|set-at-release ]]; then
    DB_PASSWORD="$(generate_password)"
  else
    DB_PASSWORD="$existing_db_password"
  fi
fi

build_time="$(date -u +"%Y-%m-%dT%H:%M:%SZ")"
build_version="free-test-$(date -u +%Y%m%d-%H%M%S)"
build_commit="$(git -C "$ROOT_DIR" rev-parse --short HEAD 2>/dev/null || true)"
if [[ -z "$build_commit" ]]; then
  build_commit="free-test"
fi

app_domain="app.$PUBLIC_IP.$DNS_SUFFIX"
api_domain="api.$PUBLIC_IP.$DNS_SUFFIX"

set_env_value "$BACKEND_TARGET" "SPRING_PROFILES_ACTIVE" "prod"
set_env_value "$BACKEND_TARGET" "DB_HOST" "postgres"
set_env_value "$BACKEND_TARGET" "DB_PORT" "5432"
set_env_value "$BACKEND_TARGET" "DB_NAME" "synapsecore"
set_env_value "$BACKEND_TARGET" "DB_USER" "synapsecore"
set_env_value "$BACKEND_TARGET" "DB_PASSWORD" "$DB_PASSWORD"
set_env_value "$BACKEND_TARGET" "SPRING_DATA_REDIS_URL" "redis://redis:6379"
set_env_value "$BACKEND_TARGET" "CORS_ALLOWED_ORIGINS" "https://$app_domain"
set_env_value "$BACKEND_TARGET" "SESSION_COOKIE_SECURE" "true"
set_env_value "$BACKEND_TARGET" "SESSION_COOKIE_SAME_SITE" "Lax"
set_env_value "$BACKEND_TARGET" "ALLOW_HEADER_FALLBACK" "false"
set_env_value "$BACKEND_TARGET" "SPRING_JPA_HIBERNATE_DDL_AUTO" "update"
set_env_value "$BACKEND_TARGET" "SYNAPSECORE_BUILD_VERSION" "$build_version"
set_env_value "$BACKEND_TARGET" "SYNAPSECORE_BUILD_COMMIT" "$build_commit"
set_env_value "$BACKEND_TARGET" "SYNAPSECORE_BUILD_TIME" "$build_time"

set_env_value "$FRONTEND_TARGET" "VITE_API_URL" "https://$api_domain"
set_env_value "$FRONTEND_TARGET" "VITE_WS_URL" "https://$api_domain/ws"
set_env_value "$FRONTEND_TARGET" "VITE_APP_BUILD_VERSION" "$build_version"
set_env_value "$FRONTEND_TARGET" "VITE_APP_BUILD_COMMIT" "$build_commit"
set_env_value "$FRONTEND_TARGET" "VITE_APP_BUILD_TIME" "$build_time"

set_env_value "$EDGE_TARGET" "SYNAPSECORE_APP_DOMAIN" "$app_domain"
set_env_value "$EDGE_TARGET" "SYNAPSECORE_API_DOMAIN" "$api_domain"
set_env_value "$EDGE_TARGET" "SYNAPSECORE_ACME_EMAIL" "$ACME_EMAIL"

echo
echo "Prepared free-test deployment env files:"
echo "  $BACKEND_TARGET"
echo "  $FRONTEND_TARGET"
echo "  $EDGE_TARGET"
echo
echo "Generated public test URLs:"
echo "  Frontend: https://$app_domain"
echo "  Backend : https://$api_domain"
echo
echo "Database password in backend.prod.env:"
echo "  $DB_PASSWORD"
echo
echo "Next steps:"
echo "1. BACKEND_ENV_FILE=./env/backend.prod.env FRONTEND_ENV_FILE=./env/frontend.prod.env bash scripts/release-readiness.sh"
echo "2. BACKEND_ENV_FILE=./env/backend.prod.env FRONTEND_ENV_FILE=./env/frontend.prod.env EDGE_ENV_FILE=./env/edge.prod.env bash scripts/start-public-prod.sh"
echo "3. FRONTEND_URL=https://$app_domain BACKEND_URL=https://$api_domain bash scripts/verify-deployment.sh"
