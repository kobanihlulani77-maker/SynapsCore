# SynapseCore Agent Guide

## Product Identity
SynapseCore is a real-time operational intelligence platform.

It is not a generic dashboard, CRUD admin panel, or reporting template.
Every meaningful change in the system should reinforce this loop:

1. receive business activity
2. update live state
3. evaluate operational impact
4. estimate what may happen next
5. generate alerts and recommendations
6. push the result live

The product promise is simple: SynapseCore turns business activity into real-time decisions.

## MVP Boundary
Build only what is required to prove this core flow:

`order -> inventory deduction -> low-stock detection -> alert -> recommendation -> realtime update`

Keep the MVP focused on:
- order ingestion
- inventory tracking
- low-stock detection
- stock depletion estimation
- live alerts
- rule-based recommendations
- simulation mode
- realtime dashboard updates

Do not add:
- Kafka
- microservices
- machine learning
- authentication complexity
- unrelated domains
- generic template features

## Architecture Rules
- Keep controllers thin.
- Put business logic in services, not controllers.
- Preserve modular package boundaries.
- Use DTOs for API responses.
- Use simulation through the same business services as real activity.
- Prefer explainable rules over vague "AI" language.

## Stack Expectations
- Backend: Java 21 + Spring Boot
- Database: PostgreSQL
- Cache / fast state: Redis
- Realtime: Spring WebSockets
- Frontend: React + Vite
- Infrastructure: Docker Compose

## Commands To Run After Changes
- Backend install/build: `docker compose -f infrastructure/docker-compose.yml build backend`
- Frontend install/build: `npm.cmd install --prefix frontend` then `npm.cmd run build --prefix frontend`
- Backend tests: `./mvnw test` (or `mvnw.cmd test` on Windows)
- Full stack: `docker compose -f infrastructure/docker-compose.yml up --build`
- Guided repo explainer: `bash scripts/explain-project.sh`
- Curated repo map: `bash scripts/project-tree.sh`
- Reset demo baseline: `bash scripts/seed.sh`

## Repo Guidance
- `backend/` is the operational brain.
- `frontend/` is the live control center.
- `infrastructure/` should support one-command local startup.
- `docs/` should explain system flow, architecture, and APIs clearly.
- `scripts/` should help a new developer understand and run the MVP quickly.
