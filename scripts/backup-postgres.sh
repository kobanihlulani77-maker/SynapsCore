#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
INFRA_DIR="$ROOT_DIR/infrastructure"
COMPOSE_FILE="${COMPOSE_FILE:-$INFRA_DIR/docker-compose.prod.yml}"
SERVICE_NAME="${SERVICE_NAME:-postgres}"
BACKUP_DIR="${BACKUP_DIR:-$ROOT_DIR/backups}"
TIMESTAMP="$(date '+%Y%m%d-%H%M%S')"
OUTPUT_FILE="${OUTPUT_FILE:-$BACKUP_DIR/synapsecore-postgres-$TIMESTAMP.sql}"

usage() {
  cat <<'EOF'
Usage:
  bash scripts/backup-postgres.sh
  BACKUP_DIR=/tmp/synapsecore-backups bash scripts/backup-postgres.sh
  OUTPUT_FILE=/tmp/synapsecore.sql bash scripts/backup-postgres.sh

Environment overrides:
  COMPOSE_FILE   Compose file to target (defaults to infrastructure/docker-compose.prod.yml)
  SERVICE_NAME   Postgres service name inside the compose file (defaults to postgres)
  BACKUP_DIR     Directory for timestamped backups when OUTPUT_FILE is not set
  OUTPUT_FILE    Exact file path for the SQL backup to create
EOF
}

if [[ "${1:-}" == "--help" || "${1:-}" == "-h" ]]; then
  usage
  exit 0
fi

mkdir -p "$(dirname "$OUTPUT_FILE")"

echo "========================================"
echo "SYNAPSECORE POSTGRES BACKUP"
echo "========================================"
echo "Compose file : $COMPOSE_FILE"
echo "Service      : $SERVICE_NAME"
echo "Output file  : $OUTPUT_FILE"
echo

docker compose -f "$COMPOSE_FILE" exec -T "$SERVICE_NAME" sh -lc \
  'PGPASSWORD="$POSTGRES_PASSWORD" pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" --no-owner --no-privileges' \
  > "$OUTPUT_FILE"

echo "Backup written to $OUTPUT_FILE"
