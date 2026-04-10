#!/usr/bin/env bash

set -euo pipefail

FRONTEND_URL="${FRONTEND_URL:-http://127.0.0.1}"
BACKEND_URL="${BACKEND_URL:-http://127.0.0.1:8080}"
SEED_TENANT_CODE="${SEED_TENANT_CODE:-SYNAPSE-DEMO}"
SEED_ADMIN_USERNAME="${SEED_ADMIN_USERNAME:-operations.lead}"
SEED_ADMIN_PASSWORD="${SEED_ADMIN_PASSWORD:-lead-2026}"
MAX_ATTEMPTS="${MAX_ATTEMPTS:-20}"
SLEEP_SECONDS="${SLEEP_SECONDS:-5}"
COOKIE_JAR="${COOKIE_JAR:-$(mktemp)}"

check_endpoint() {
  local label="$1"
  local url="$2"
  local attempt=1
  echo "Checking $label -> $url"
  while (( attempt <= MAX_ATTEMPTS )); do
    if curl --fail --silent --show-error "$url" >/dev/null; then
      return 0
    fi
    if (( attempt == MAX_ATTEMPTS )); then
      echo "Failed to verify $label after $MAX_ATTEMPTS attempts." >&2
      return 1
    fi
    echo "  attempt $attempt/$MAX_ATTEMPTS did not pass yet; retrying in ${SLEEP_SECONDS}s..."
    sleep "$SLEEP_SECONDS"
    attempt=$((attempt + 1))
  done
}

check_endpoint "backend health" "$BACKEND_URL/actuator/health"
check_endpoint "backend readiness" "$BACKEND_URL/actuator/health/readiness"
check_endpoint "backend prometheus" "$BACKEND_URL/actuator/prometheus"
check_endpoint "frontend health" "$FRONTEND_URL/healthz"
check_endpoint "frontend runtime config" "$FRONTEND_URL/runtime-config.js"

echo "Checking backend sign-in -> $BACKEND_URL/api/auth/session/login"
curl --fail --silent --show-error \
  --cookie-jar "$COOKIE_JAR" \
  --header "Content-Type: application/json" \
  --data "{\"tenantCode\":\"$SEED_TENANT_CODE\",\"username\":\"$SEED_ADMIN_USERNAME\",\"password\":\"$SEED_ADMIN_PASSWORD\"}" \
  "$BACKEND_URL/api/auth/session/login" >/dev/null

echo "Checking dashboard summary -> $BACKEND_URL/api/dashboard/summary"
curl --fail --silent --show-error --cookie "$COOKIE_JAR" "$BACKEND_URL/api/dashboard/summary" >/dev/null
echo "Checking system runtime -> $BACKEND_URL/api/system/runtime"
curl --fail --silent --show-error --cookie "$COOKIE_JAR" "$BACKEND_URL/api/system/runtime" >/dev/null
curl --fail --silent --show-error --cookie "$COOKIE_JAR" "$BACKEND_URL/api/system/incidents" >/dev/null
echo "Checking system incidents -> $BACKEND_URL/api/system/incidents"

echo
echo "SynapseCore deployment checks passed."

rm -f "$COOKIE_JAR"
