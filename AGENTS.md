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

## Investigation Efficiency And Evidence Discipline

For reliability, performance, hosted-runtime, and timeout work, use one
evidence-led path instead of repeatedly generating broad traffic.

1. State the single active question and the exact failure classification before
   running a test or opening a dashboard.
2. Establish and record a warm baseline before a hosted proof. Do not label a
   cold-start delay as an active-runtime regression.
3. Use focused tests while diagnosing. Run the broad hosted E2E only after a
   focused change is ready for verification, or when the active phase explicitly
   requires repeatability proof.
4. Stop at the first failure. Preserve a bounded window: at least 60 seconds
   before it, the failure interval, and 60 seconds after recovery. Do not keep
   creating traffic after evidence has been captured.
5. Use Chrome only when browser timing, response content, request ID, or UI
   convergence is the missing link. Capture the relevant request rather than
   repeatedly collecting whole-page console and network state.
6. Treat a healthy capture as a control, not a reason to keep sampling. Compare
   it with the next unhealthy boundary; do not rerun identical healthy windows
   without a new question.
7. Keep raw logs local and report compact evidence: UTC window, endpoint or
   scheduler, request ID, Hikari state, PostgreSQL state, observed duration,
   and outcome. Pull larger logs only when they can answer the active question.
8. Separate `CHROME_HTTP_SLOW` from `HTTP_FAST_BUT_UI_STALE`. Do not change
   frontend convergence behavior while the authoritative HTTP response is slow.
9. Use high-effort reasoning for root-cause mapping, production changes, and
   final review. Use lightweight retrieval and compact summaries for routine
   status checks, log filtering, and already-proven evidence.
10. After a production change, commit and push the bounded change, wait for the
    exact revision to deploy and become ready, then verify that served revision
    before treating any hosted result as evidence.
