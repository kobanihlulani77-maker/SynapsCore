# Master Project Tree

This document explains the SynapseCore repository as a complete software program rather than as a random collection of folders.

The goal is to help a new engineer, reviewer, operator, or future contributor understand:

- what each major folder is for
- which parts are production-sensitive
- which parts are optional or generated
- which parts exist mainly for proof, testing, or operational support

## Top-Level Repository Structure

```text
synapsecore/
|-- .github/
|-- .vscode/
|-- backend/
|-- backups/
|-- docs/
|-- frontend/
|-- infrastructure/
|-- scripts/
|-- README.md
|-- render.yaml
|-- AGENTS.md
```

## Top-Level Folder Map

### `backend/`

Purpose:

- Spring Boot application
- business logic
- auth and tenant enforcement
- inventory, orders, replay, scenarios, runtime, and integrations

Communicates with:

- PostgreSQL
- Redis
- frontend API clients
- frontend realtime websocket clients

Production-sensitive:

- yes

Critical:

- yes

Proof/testing only:

- no, but it includes test code and local startup helpers

### `frontend/`

Purpose:

- public homepage
- controlled provisioning handoff and login entry
- sign-in experience
- authenticated operations command center
- proof-facing UI surfaces

Communicates with:

- backend REST API through `VITE_API_URL`
- websocket endpoint through `VITE_WS_URL`

Production-sensitive:

- yes

Critical:

- yes

Proof/testing only:

- no, but it includes proof specs and frontend verification tooling

### `infrastructure/`

Purpose:

- Docker Compose environments
- environment templates
- edge and deployment support files

Communicates with:

- backend container
- frontend container
- PostgreSQL and Redis services

Production-sensitive:

- yes, especially compose files and env templates

Critical:

- important for local/full-stack and deployment understanding

Proof/testing only:

- no

### `docs/`

Purpose:

- architecture docs
- product docs
- pilot docs
- proof docs
- resilience and runbooks
- due-diligence material

Communicates with:

- every other layer conceptually

Production-sensitive:

- no runtime effect

Critical:

- critical for understanding, operational discipline, and handoff quality

Proof/testing only:

- partially, where proof docs and QA guides live

### `scripts/`

Purpose:

- local and live checks
- proof preparation
- explanatory scripts
- backup and restore helpers
- deployment support

Communicates with:

- local services
- live URLs
- docs and verification paths

Production-sensitive:

- some scripts are operationally sensitive

Critical:

- important for verification and operations

Proof/testing only:

- many scripts support proof or diagnostics, but not all

### `backups/`

Purpose:

- backup artifacts or backup-related local storage

Production-sensitive:

- potentially, depending on contents

Critical:

- not part of normal app runtime

Proof/testing only:

- no

### `.github/`

Purpose:

- workflows and automation metadata

Production-sensitive:

- yes, for CI and repository governance

Critical:

- important for project automation

Proof/testing only:

- partially, because workflows may enforce verification and release posture

### `.vscode/`

Purpose:

- editor convenience

Production-sensitive:

- no

Critical:

- optional

Proof/testing only:

- no

## Backend Structure

```text
backend/
|-- .mvn/
|-- src/
|   |-- main/
|   |   |-- java/com/synapsecore/
|   |   |-- resources/
|   |-- test/
|       |-- java/com/synapsecore/
|-- pom.xml
|-- mvnw
|-- mvnw.cmd
|-- Dockerfile
|-- start-local-demo.cmd
|-- start-local-prod.cmd
|-- .env.local.example
```

### `backend/src/main/java/com/synapsecore/`

Major domain packages include:

- `api` for controllers and API edges
- `auth` and `security` for session, auth, and protection posture
- `tenant` for workspace scoping
- `domain` for core business entities and repositories
- `integration` for connectors, imports, and replay-adjacent flows
- `scenario` and `decision` for governed action and planning flows
- `event` and `realtime` for live updates and dispatch
- `observability` and `audit` for trust and traceability
- `alert`, `intelligence`, `prediction` for operational guidance layers

Critical:

- this is the production core of SynapseCore

### `backend/src/main/resources/`

Contains:

- `application.yml` and environment-specific Spring configs
- logging config
- `db/support/full-schema-baseline.sql`

Production-sensitive:

- highly

### `backend/src/test/java/com/synapsecore/`

Contains integration and hardening tests such as:

- `MvpFlowIntegrationTest`
- `ProductionHardeningIntegrationTest`
- `SecurityHardeningIntegrationTest`
- `InventoryConcurrencyIntegrationTest`

Purpose:

- protect real backend behavior
- validate security and operational assumptions
- support replay and proof-adjacent stability

Critical:

- yes for engineering confidence

### Generated Or Local-Only Backend Artifacts

Examples:

- `target/`
- local logs
- crash logs

These are useful for debugging, but they are not source-of-truth code and should not be treated as architectural input.

## Frontend Structure

```text
frontend/
|-- public/
|-- scripts/
|-- src/
|   |-- components/
|   |-- config/
|   |-- hooks/
|   |-- layout/
|   |-- pages/
|   |-- services/
|   |-- App.jsx
|   |-- main.jsx
|   |-- design-system.css
|   |-- styles.css
|-- tests/
|-- package.json
|-- vite.config.js
|-- Dockerfile
|-- nginx.conf
|-- playwright.prod.config.mjs
|-- .env.local.example
```

### `frontend/src/pages/`

Purpose:

- route-level product surfaces
- public pages
- dashboard
- operational pages
- admin and runtime pages

Critical:

- yes, this is where the visible product lives

### `frontend/src/layout/`

Purpose:

- authenticated shell
- sidebar
- topbar
- workspace-level structure

Critical:

- yes, because it shapes the command-center experience

### `frontend/src/components/`

Purpose:

- reusable UI building blocks
- tables
- notices
- route helpers

Critical:

- important for consistency and proof-safe behavior

### `frontend/src/hooks/`

Purpose:

- page context
- realtime state
- workspace-aware behavior

Critical:

- important because runtime trust and live UX depend on these hooks

### `frontend/src/services/`

Purpose:

- backend API wiring
- request helpers
- network behavior shared across pages

Production-sensitive:

- yes

### `frontend/tests/`

Contains:

- hosted proof global setup
- proof state helpers
- deployed production proof spec

Critical:

- yes for proof discipline

Proof/testing only:

- yes

### Generated Or Local-Only Frontend Artifacts

Examples:

- `dist/`
- `node_modules/`
- `playwright-report/`
- `test-results/`
- screenshots

These are not product source files. They are outputs or local aids.

## Infrastructure Structure

```text
infrastructure/
|-- env/
|   |-- backend.env
|   |-- frontend.env
|   |-- backend.prod.env
|   |-- frontend.prod.env
|   |-- *.example.env
|-- docker-compose.yml
|-- docker-compose.prod.yml
|-- docker-compose.public.yml
|-- Caddyfile
|-- cookies.txt
```

### `infrastructure/env/`

Purpose:

- environment templates and deployment wiring

Production-sensitive:

- yes

Critical:

- important for real deployments and local full-stack bring-up

### Compose Files

Purpose:

- define service topology
- local/full-stack orchestration
- alternate deployment modes

Production-sensitive:

- yes

## Scripts Structure

The `scripts/` folder is a mixed operational toolkit.

Main categories:

- connection checks
- proof preparation
- verification
- explanatory scripts
- backup and restore
- env preparation
- deployment support

Examples:

- `check-live-connections.ps1`
- `check-local-connections.ps1`
- `prepare-hosted-proof.ps1`
- `verify-deployment.ps1`
- `verify-realtime.ps1`
- `explain-infrastructure.ps1`
- `explain-proof-system.ps1`
- `recovery-checklist.ps1`

Critical:

- yes for operator and engineer workflow

Optional:

- some explainer scripts are convenience-oriented

Production-sensitive:

- backup, restore, env, and deployment scripts should be treated carefully

## Docs Structure

The `docs/` folder now acts as the knowledge layer of the platform.

Major categories include:

- product understanding
- architecture and infrastructure
- frontend and backend flow
- proof and validation
- resilience and recovery
- technical review
- buyer and pilot evaluation
- roadmap and engineering guidance

Critical:

- yes for handoff, review, and operational discipline

## Render And Deployment Files

### `render.yaml`

Purpose:

- deployment metadata for Render

Production-sensitive:

- yes

### Dockerfiles

Located in:

- `backend/Dockerfile`
- `frontend/Dockerfile`

Purpose:

- image build instructions

Production-sensitive:

- yes

## What Is Critical Vs Optional

Critical source-of-truth layers:

- `backend/src/main`
- `frontend/src`
- `infrastructure/`
- `render.yaml`
- proof specs under `frontend/tests`
- operational scripts under `scripts/`
- architecture and runbook docs under `docs/`

Optional or generated layers:

- `.vscode/`
- `dist/`
- `target/`
- `node_modules/`
- screenshots and logs
- local crash artifacts

## What Is Production-Sensitive

Treat these areas carefully:

- backend configs and domain logic
- frontend API and realtime wiring
- infrastructure env templates
- Dockerfiles and compose files
- Render deployment config
- auth, session, replay, scenario, and runtime trust paths
- proof selectors and proof specs

## What Supports Proof Or Testing Only

Main proof/testing-only layers:

- `frontend/tests/`
- verification-oriented scripts
- Playwright artifacts
- certain backend integration tests

These are not optional in practice. They are optional for runtime, but essential for trust.

## Bottom Line

SynapseCore is not organized as a single app folder with some extras around it.

It is a coordinated program with:

- a productized frontend
- a domain-heavy backend
- explicit infrastructure definitions
- proof and verification tooling
- operational runbooks
- long-form product and technical documentation

Understanding those layers together is what makes the repository understandable.
