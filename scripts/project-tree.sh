#!/usr/bin/env bash

# SynapseCore project tree helper
# -------------------------------
# This script prints a curated structure view instead of dumping every file.
# The goal is to help a new engineer quickly understand which sections of the
# repo matter and why they exist.
#
# Run with:
#   bash scripts/project-tree.sh

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

section() {
  local title="$1"
  echo
  echo "--------------------------------------------------"
  echo "$title"
  echo "--------------------------------------------------"
}

section "SYNAPSECORE STRUCTURE"
echo "$ROOT_DIR"
echo "|-- backend/              Spring Boot operational brain"
echo "|   |-- pom.xml           Backend build and dependency definition"
echo "|   |-- Dockerfile        Backend container image definition"
echo '|   `-- src/'
echo "|       |-- main/java/com/synapsecore/"
echo "|       |   |-- config/       Application, WebSocket, Redis, CORS, scheduling config"
echo "|       |   |-- integration/  External ingestion and future connector entry points"
echo "|       |   |-- event/        Internal business event recording and querying"
echo "|       |   |-- domain/"
echo "|       |   |   |-- entity/      Core persisted business model"
echo "|       |   |   |-- repository/  Persistence interfaces"
echo "|       |   |   |-- service/     Core business services and operational flows"
echo '|       |   |   `-- dto/         Request and response contracts'
echo "|       |   |-- intelligence/ Low-stock and operational condition detection"
echo "|       |   |-- prediction/   Explainable future-outcome estimation"
echo "|       |   |-- decision/     Recommendation generation"
echo "|       |   |-- alert/        Alert creation and lifecycle management"
echo "|       |   |-- realtime/     WebSocket broadcasting"
echo '|       |   `-- api/controller/ REST endpoints for UI and external callers'
echo '|       `-- main/resources/application.yml  Backend runtime configuration'
echo "|-- frontend/             React + Vite live operational dashboard"
echo "|   |-- package.json      Frontend scripts and dependencies"
echo "|   |-- Dockerfile        Frontend container image definition"
echo '|   `-- src/'
echo "|       |-- App.jsx       Root shell that mounts the workspace application model"
echo "|       |-- components/   Route switch, workspace shell, and page composition"
echo "|       |-- hooks/        Session/bootstrap/realtime/page-context orchestration"
echo "|       |-- main.jsx      Frontend bootstrap"
echo '|       `-- styles.css    Dashboard styling'
echo "|-- infrastructure/      Docker Compose and environment setup"
echo "|   |-- docker-compose.yml  Local stack definition"
echo '|   `-- env/                Backend and frontend environment files'
echo "|-- docs/                Architecture, system flow, and API guides"
echo "|-- scripts/             Onboarding and helper scripts"
echo "|   |-- explain-project.sh  Guided explanation of the full project"
echo "|   |-- project-tree.sh     Curated repo map"
echo "|   |-- setup.sh            Setup helper"
echo "|   |-- start.sh            Startup helper"
echo "|   |-- seed.sh             Seeding helper"
echo '|   `-- codex-brief.sh      Quick agent/project briefing'
echo "|-- README.md             Main repo entry point"
echo '`-- AGENTS.md            Guardrails for Codex and future agents'

section "HOW TO USE THIS VIEW"
echo "Start with README.md for the run flow."
echo "Then read docs/architecture.md and docs/system-flow.md."
echo "Use scripts/explain-project.sh when you want the repo explained in full words."
echo "Use this script when you want a fast map of where responsibilities live."

section "OPTIONAL FOLDER SNAPSHOT"
if command -v find >/dev/null 2>&1; then
  echo "Important directories detected under the project root:"
  find "$ROOT_DIR" -maxdepth 2 -type d | sort
else
  echo "The 'find' command is not available in this shell, so only the curated tree is shown."
fi
