#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
INFRA_DIR="$ROOT_DIR/infrastructure"
COMPOSE_FILE="${COMPOSE_FILE:-$INFRA_DIR/docker-compose.prod.yml}"
SERVICE_NAME="${SERVICE_NAME:-postgres}"
BACKUP_FILE=""
CONFIRMED="false"

usage() {
  cat <<'EOF'
Usage:
  bash scripts/restore-postgres.sh --file backups/synapsecore-postgres-20260402-220000.sql --yes

What it does:
  - drops and recreates the public schema inside the target Postgres database
  - restores the supplied plain SQL backup into that database

Environment overrides:
  COMPOSE_FILE   Compose file to target (defaults to infrastructure/docker-compose.prod.yml)
  SERVICE_NAME   Postgres service name inside the compose file (defaults to postgres)
EOF
}

while (($#)); do
  case "$1" in
    --file)
      BACKUP_FILE="${2:?Missing value for --file}"
      shift 2
      ;;
    --yes)
      CONFIRMED="true"
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

if [[ -z "$BACKUP_FILE" ]]; then
  echo "Missing required --file argument." >&2
  usage
  exit 1
fi

if [[ ! -f "$BACKUP_FILE" ]]; then
  echo "Backup file not found: $BACKUP_FILE" >&2
  exit 1
fi

if [[ "$CONFIRMED" != "true" ]]; then
  echo "Refusing to restore without --yes because this resets the target database schema." >&2
  exit 1
fi

echo "========================================"
echo "SYNAPSECORE POSTGRES RESTORE"
echo "========================================"
echo "Compose file : $COMPOSE_FILE"
echo "Service      : $SERVICE_NAME"
echo "Backup file  : $BACKUP_FILE"
echo
echo "Resetting target schema before restore..."

docker compose -f "$COMPOSE_FILE" exec -T "$SERVICE_NAME" sh -lc \
  'PGPASSWORD="$POSTGRES_PASSWORD" psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"'

echo "Restoring backup..."
docker compose -f "$COMPOSE_FILE" exec -T "$SERVICE_NAME" sh -lc \
  'PGPASSWORD="$POSTGRES_PASSWORD" psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"' \
  < "$BACKUP_FILE"

echo "Restore completed successfully."
