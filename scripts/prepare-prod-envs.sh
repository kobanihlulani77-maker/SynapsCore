#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_DIR="$ROOT_DIR/infrastructure/env"
BACKEND_SOURCE="$ENV_DIR/backend.prod.example.env"
FRONTEND_SOURCE="$ENV_DIR/frontend.prod.example.env"
EDGE_SOURCE="$ENV_DIR/edge.prod.example.env"
BACKEND_TARGET="$ENV_DIR/backend.prod.env"
FRONTEND_TARGET="$ENV_DIR/frontend.prod.env"
EDGE_TARGET="$ENV_DIR/edge.prod.env"
FORCE="false"

usage() {
  cat <<'EOF'
Usage:
  bash scripts/prepare-prod-envs.sh
  bash scripts/prepare-prod-envs.sh --force

What it does:
  - copies backend.prod.example.env to backend.prod.env
  - copies frontend.prod.example.env to frontend.prod.env
  - copies edge.prod.example.env to edge.prod.env
  - preserves existing target files unless --force is provided
EOF
}

copy_template() {
  local source_file="$1"
  local target_file="$2"
  local label="$3"

  if [[ -f "$target_file" && "$FORCE" != "true" ]]; then
    echo "$label already exists: $target_file"
    return 0
  fi

  cp "$source_file" "$target_file"
  echo "Prepared $label: $target_file"
}

while (($#)); do
  case "$1" in
    --force)
      FORCE="true"
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

copy_template "$BACKEND_SOURCE" "$BACKEND_TARGET" "backend prod env"
copy_template "$FRONTEND_SOURCE" "$FRONTEND_TARGET" "frontend prod env"
copy_template "$EDGE_SOURCE" "$EDGE_TARGET" "edge prod env"

echo
echo "Next steps:"
echo "1. Edit $BACKEND_TARGET"
echo "2. Edit $FRONTEND_TARGET"
echo "3. Edit $EDGE_TARGET"
echo "4. Run: BACKEND_ENV_FILE=./env/backend.prod.env FRONTEND_ENV_FILE=./env/frontend.prod.env bash scripts/release-readiness.sh"
echo "5. Self-hosted deploy: BACKEND_ENV_FILE=./env/backend.prod.env FRONTEND_ENV_FILE=./env/frontend.prod.env bash scripts/start-prod.sh"
echo "6. Public deploy with Caddy: BACKEND_ENV_FILE=./env/backend.prod.env FRONTEND_ENV_FILE=./env/frontend.prod.env EDGE_ENV_FILE=./env/edge.prod.env bash scripts/start-public-prod.sh"
