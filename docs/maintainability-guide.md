# Maintainability Guide

This guide helps future engineers safely understand and modify SynapseCore.

The goal is to preserve the proven pilot release candidate while allowing careful evolution.

## Repository Structure

Top-level structure:

- `backend`: Spring Boot application, API, domain logic, persistence, realtime, auth, integrations, replay, scenarios, runtime
- `frontend`: React/Vite command-center UI and Playwright hosted proof
- `infrastructure`: Docker Compose and local/self-host infrastructure
- `scripts`: verification, proof, deployment, explanation, backup, recovery, and readiness helpers
- `docs`: architecture, operations, proof, release, support, pilot, and engineering documentation
- `render.yaml`: Render deployment definition

## Backend Responsibilities

The backend owns:

- tenant/workspace truth
- auth/session behavior
- domain state
- product/catalog APIs
- inventory APIs
- order ingestion
- integration connector state
- replay/recovery
- scenario approval and execution
- alerts and recommendations
- runtime/observability
- realtime event publication
- Flyway migrations

High-value packages:

- `com.synapsecore.api.controller`: HTTP controllers
- `com.synapsecore.domain.service`: core business services
- `com.synapsecore.domain.repository`: persistence access
- `com.synapsecore.domain.entity`: database-backed domain model
- `com.synapsecore.integration`: connector, inbound, replay, import logic
- `com.synapsecore.scenario`: scenario planning, approval, execution
- `com.synapsecore.realtime`: websocket and Redis pub/sub realtime posture
- `com.synapsecore.access`: tenant workspace and access administration
- `com.synapsecore.auth`: session and sign-in behavior
- `com.synapsecore.security`: rate limiting and security filters
- `com.synapsecore.config`: environment, CORS, Redis, realtime, and deployment configuration

## Frontend Responsibilities

The frontend owns:

- public experience
- workspace creation/sign-in screens
- authenticated shell
- navigation and page chrome
- dashboard and operational pages
- realtime connection state display
- loading/error/empty-state behavior
- proof-critical selectors and labels

Important folders:

- `frontend/src/pages`: page-level views
- `frontend/src/layout`: shell, sidebar, topbar, utility rail
- `frontend/src/hooks`: application state and orchestration
- `frontend/src/services`: API and auth helpers
- `frontend/src/config`: page registry and workspace data model
- `frontend/src/components`: reusable visual primitives
- `frontend/tests`: hosted proof tests and proof state helpers
- `frontend/scripts`: frontend launch-readiness checks

## Dependency Flow

Preferred flow:

- frontend pages call hooks and services
- hooks call API helpers
- backend controllers call services
- services use repositories and domain helpers
- domain events update runtime/realtime surfaces
- proof tooling validates deployed frontend/backend behavior

Avoid:

- controllers owning business logic
- frontend pages duplicating API error handling
- scripts becoming hidden product behavior
- docs claiming behavior that proof does not validate

## Safe Modification Areas

Usually safe with normal verification:

- docs and runbooks
- script output wording
- frontend layout copy that is not proof-critical
- non-contract UI polish
- additional docs index links
- local-only helper improvements

Still run:

- docs link check
- frontend verify if frontend files changed
- relevant readiness or proof checks when operational behavior is touched

## High-Risk Areas

Treat these as high risk:

- auth/session APIs
- tenant context enforcement
- CORS/session cookie configuration
- Flyway migrations
- replay/recovery logic
- scenario approval/execution logic
- inventory concurrency
- realtime websocket/Redis behavior
- hosted proof selectors
- proof state/password handling
- Render env configuration

Changes here should trigger targeted tests and possibly hosted proof.

## Extension Points

Safe extension points:

- new docs under `docs`
- new informational scripts under `scripts` when they do not overlap existing helpers
- new frontend pages only when added to `pageRegistry` and authenticated shell intentionally
- new backend APIs through controller/service/DTO/repository boundaries
- new integration behaviors through connector services and replay model

Do not add new architecture such as queues, workers, or HA layers without an engineering strategy and migration plan.

## Coding Conventions Already Present

Backend conventions:

- Java 21
- Spring Boot
- DTO-based API responses
- JPA repositories
- Flyway migrations
- services for business logic
- tenant context guards
- explicit runtime/security configuration

Frontend conventions:

- React functional components
- Vite
- page registry for routing metadata
- hooks for orchestration
- services for API/auth primitives
- proof-critical labels preserved by checks
- CSS/design-system split after frontend hardening

Script conventions:

- PowerShell for Windows operational checks
- shell scripts for Unix/self-host support
- scripts should print classifications
- scripts should not expose secrets
- scripts should not mutate runtime unless explicitly named as setup, backup, restore, or preparation

## Verification Expectations

Minimum verification:

- docs-only change: `scripts\docs-link-check.ps1`
- frontend change: `cd frontend; npm.cmd run verify`
- backend change: `cd backend; cmd /c mvnw.cmd test`
- deployment/proof change: `scripts\check-live-connections.ps1`
- proof-covered behavior change: `scripts\prepare-hosted-proof.ps1` then `cd frontend; npm.cmd run test:e2e:prod`

Do not run hosted proof when `PROOF_ALLOWED=False`.

## Maintenance Rule

Every future change should answer:

- what contract does this touch?
- what proof protects it?
- what doc needs updating?
- what rollback path exists?
- what operator impact can occur if it fails?
