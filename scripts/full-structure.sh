#!/usr/bin/env bash

# SynapseCore full structure helper
# ---------------------------------
# This script prints the full project structure from the repository root down
# to the deepest files and folders.
#
# Why it exists:
# - onboarding a new engineer quickly
# - helping agents understand the real repo shape
# - making hidden depth visible during setup and debugging
#
# Usage:
#   bash scripts/full-structure.sh
#   bash scripts/full-structure.sh --full
#   bash scripts/full-structure.sh --help
#
# Default behavior:
# - prints the complete project structure
# - excludes only heavy/generated folders that often make the output unreadable:
#   .git, node_modules, target, dist
#
# Full behavior:
# - if you pass --full, absolutely everything is included
# - this can be very large, especially after installs and builds
#
# Implementation notes:
# - uses a feature-compatible "tree" command when available
# - falls back to a find-based renderer when tree is unavailable
#   or when the local tree command does not support the filtering options we need

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
INCLUDE_ALL=false
EXCLUDE_NAMES=(".git" "node_modules" "target" "dist")

print_header() {
  echo "========================================"
  echo "SYNAPSECORE FULL PROJECT STRUCTURE"
  echo "========================================"
  echo "Root: $ROOT_DIR"
  if [ "$INCLUDE_ALL" = true ]; then
    echo "Mode: --full (includes everything)"
  else
    echo "Mode: default (excludes: ${EXCLUDE_NAMES[*]})"
    echo "Tip: use --full to include every folder, including generated/build folders."
  fi
  echo
}

print_help() {
  cat <<'EOF'
SynapseCore full structure helper

Usage:
  bash scripts/full-structure.sh
  bash scripts/full-structure.sh --full
  bash scripts/full-structure.sh --help

Modes:
  default   Shows the complete project structure while excluding only heavy,
            generated folders: .git, node_modules, target, dist
  --full    Shows absolutely everything with no exclusions

Why it is useful:
  - lets a developer inspect the whole repo shape quickly
  - helps verify setup output after installs or builds
  - helps agents understand real project depth before making changes
EOF
}

for arg in "$@"; do
  case "$arg" in
    --full)
      INCLUDE_ALL=true
      ;;
    -h|--help)
      print_help
      exit 0
      ;;
    *)
      echo "Unknown option: $arg" >&2
      echo >&2
      print_help >&2
      exit 1
      ;;
  esac
done

exclude_pattern() {
  local joined=""
  local name
  for name in "${EXCLUDE_NAMES[@]}"; do
    if [ -z "$joined" ]; then
      joined="$name"
    else
      joined="$joined|$name"
    fi
  done
  printf '%s' "$joined"
}

supports_gnu_tree_filters() {
  if ! command -v tree >/dev/null 2>&1; then
    return 1
  fi

  tree --help 2>&1 | grep -q -- '-I'
}

print_with_tree() {
  if [ "$INCLUDE_ALL" = true ]; then
    tree -a "$ROOT_DIR"
  else
    tree -a -I "$(exclude_pattern)" "$ROOT_DIR"
  fi
}

print_with_find() {
  local rel_path depth indent name path
  local -a find_cmd=()

  echo "$ROOT_DIR"

  if [ "$INCLUDE_ALL" = true ]; then
    find_cmd=(find "$ROOT_DIR" -mindepth 1)
  else
    find_cmd=(find "$ROOT_DIR" \( -name ".git" -o -name "node_modules" -o -name "target" -o -name "dist" \) -prune -o -mindepth 1 -print)
  fi

  "${find_cmd[@]}" | LC_ALL=C sort | while IFS= read -r path; do
    rel_path="${path#$ROOT_DIR/}"
    depth="$(awk -F/ '{print NF-1}' <<< "$rel_path")"
    indent=""
    while [ "$depth" -gt 0 ]; do
      indent="${indent}|   "
      depth=$((depth - 1))
    done

    name="$(basename "$path")"
    if [ -d "$path" ] && [ ! -L "$path" ]; then
      printf '%s|-- %s/\n' "$indent" "$name"
    else
      printf '%s|-- %s\n' "$indent" "$name"
    fi
  done
}

print_header

if supports_gnu_tree_filters; then
  echo "Renderer: tree"
  echo
  print_with_tree
else
  echo "Renderer: find fallback"
  echo
  print_with_find
fi
