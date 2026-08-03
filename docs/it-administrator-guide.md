# IT Administrator Guide

This guide explains SynapseCore from the perspective of the team responsible for installation, support, maintenance, monitoring, recovery, and deployment confidence.

## IT Administrator Responsibility

IT administrators help keep SynapseCore reachable, configured, monitored, and recoverable.

They should understand:

- frontend service posture
- backend service posture
- PostgreSQL dependency
- Redis/session dependency
- environment variables
- health endpoints
- readiness and liveness
- websocket endpoint
- hosted proof prerequisites
- backup and restore posture
- local vs Render differences

## Core Services

| Service | Role |
| --- | --- |
| Frontend | Browser application and command-center UI |
| Backend | Spring Boot API, auth, business logic, replay, approvals, runtime |
| PostgreSQL | Operational record of truth |
| Redis | Session support and realtime/distributed posture |
| Websocket/SockJS | Realtime browser updates |
| Scripts | Verification, explanation, readiness, proof preparation |

## Key URLs

Local defaults:

```text
Frontend: http://127.0.0.1:5173
Backend:  http://127.0.0.1:8080
Postgres: 127.0.0.1:5432
Redis:    127.0.0.1:6379
```

Live Render defaults:

```text
Frontend: https://synapscore-frontend-3.onrender.com
Backend:  https://synapscore-3.onrender.com
```

## Health Checks

Use:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\check-live-connections.ps1
```

Key backend endpoints:

```text
/actuator/health
/actuator/health/readiness
/actuator/health/liveness
/api/auth/session
/ws/info
```

Interpretation:

- liveness answers whether the app process is alive
- readiness answers whether the app is ready to serve proof-sensitive traffic
- auth/session confirms session surface reachability
- `/ws/info` confirms SockJS endpoint reachability

## Local Maintenance

Use existing runbooks:

- [local-runbook.md](local-runbook.md)
- [local-recovery-playbook.md](local-recovery-playbook.md)
- [local-debug-plan.md](local-debug-plan.md)

Important local cautions:

- Docker Postgres can conflict with Windows Postgres on port `5432`.
- A backend container can occupy port `8080` while a host backend tries to start.
- `.env.local` files are local-only and must not be staged.
- Local DB volume reset deletes local data and should not be done casually.

## Render Maintenance

Use existing runbooks:

- [render-ops-runbook.md](render-ops-runbook.md)
- [render-recovery-playbook.md](render-recovery-playbook.md)
- [deployment-recovery-guide.md](deployment-recovery-guide.md)

Render-specific cautions:

- frontend can be reachable while backend is unavailable
- backend can time out when DB is unavailable or startup is blocked
- hosted proof must pause when readiness/auth/ws are unhealthy
- replacement databases require supported bootstrap/proof preparation, not manual DB edits

## Monitoring And Evidence

Recommended evidence:

- live connection check output
- frontend verify/build output
- hosted proof output when intentionally run
- Render logs for backend startup and DB connection
- release evidence document
- incident notes and recovery sequence

## Recovery Sequence

Use this order:

```text
Classify
-> restore dependency or service
-> recheck liveness/readiness/auth/ws
-> prepare proof if needed
-> run hosted proof only when allowed
-> record evidence
```

Do not start with hosted proof during an infrastructure incident.

## Useful Scripts

```powershell
powershell -ExecutionPolicy Bypass -File scripts\repo-health.ps1
powershell -ExecutionPolicy Bypass -File scripts\docs-link-check.ps1
powershell -ExecutionPolicy Bypass -File scripts\check-local-connections.ps1
powershell -ExecutionPolicy Bypass -File scripts\check-live-connections.ps1
powershell -ExecutionPolicy Bypass -File scripts\engineering-readiness.ps1
powershell -ExecutionPolicy Bypass -File scripts\recovery-checklist.ps1
```

## IT Bottom Line

SynapseCore is safe to operate when dependencies are explicit, health is checked honestly, proof only runs after readiness, and local artifacts/secrets remain out of Git.
