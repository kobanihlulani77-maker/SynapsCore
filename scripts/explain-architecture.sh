#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo "=================================================="
echo "SYNAPSECORE ARCHITECTURE EXPLAINER"
echo "=================================================="
echo "Repo root: $ROOT_DIR"
echo
cat <<'EOF'
STACK

- Backend: Java 21 + Spring Boot
- Frontend: React + Vite
- Database: PostgreSQL
- Cache / fast state: Redis
- Realtime: STOMP + SockJS over Spring WebSockets
- Packaging: Docker Compose
- Metrics: Actuator + Prometheus endpoint

TOP-LEVEL SHAPE

- backend/
  The operational brain, APIs, business model, intelligence, approvals, replay,
  runtime trust, and realtime publishing.

- frontend/
  The premium command-center UI with public, core, control, systems, and admin pages.

- infrastructure/
  Dev and production-shaped compose contracts and env files.

- docs/
  Product, flow, API, and deployment reference.

- scripts/
  Setup, verification, deployment, recovery, and explanation helpers.

BACKEND RESPONSIBILITIES

The backend is split into clear lanes:

- domain/service
  Core order, inventory, dashboard, seed, and runtime services

- fulfillment
  Backlog, dispatch, delayed shipment, and lane pressure logic

- integration
  Webhook ingestion, CSV import, connector policy, replay queue

- scenario
  Planning, comparison, save, approval, escalation, execution

- access and auth
  Tenant-aware sign-in, operator mapping, admin control, warehouse scopes

- realtime
  Tenant-scoped topic publishing

- event
  Internal dispatch queue and state-change publication

- observability
  Counters, gauges, Prometheus-ready metrics

- audit
  Request tracing and operational traceability

OPERATIONAL LOOP

The architecture is built around one product loop:

1. receive activity
2. mutate operational truth
3. evaluate risk
4. estimate next pressure
5. generate action guidance
6. publish the result live

REALTIME MODEL

SynapseCore does not use a generic push of one giant payload for every case.
It uses tenant-scoped topics so the UI can receive focused live changes for:
- summary
- alerts
- recommendations
- inventory
- fulfillment
- recent orders
- business events
- audit traces
- integration telemetry
- replay queue
- scenario notifications
- escalations
- simulation state

TRUST MODEL

The platform is designed so support and operators can answer:
- is the runtime healthy?
- is the queue backing up?
- are incidents active?
- what version is running?
- what changed recently?
- what failed and can it be recovered?

That trust layer exists through:
- runtime APIs
- incident APIs
- Prometheus metrics
- audit trail
- business events
- deployment/build fingerprinting

SCALING MODEL

SynapseCore already has a queue-backed seam inside the architecture:
- write-side changes create dispatch work items
- a queue worker drains fan-out work in batches
- failed fan-out work is visible

That keeps the current system simple while creating a clean path for future
broker-backed or horizontally scaled event expansion.

FRONTEND MODEL

The frontend is not one overloaded screen.
It is a page system with:
- public pages
- core operations pages
- control/governance pages
- systems/trust pages
- admin/platform pages

The shell includes:
- sidebar
- topbar
- page-aware command bar
- content workspace
- utility rail

So the UI mirrors the product structure instead of flattening it.
EOF
