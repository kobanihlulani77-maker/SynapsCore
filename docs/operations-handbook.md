# Operations Handbook

This handbook defines recurring operational routines for SynapseCore.

It is written for operators, deployment owners, support engineers, and maintainers.

## Daily Operations

Daily checks:

- confirm frontend reachable
- confirm backend health
- confirm readiness
- confirm liveness
- confirm auth/session response
- confirm `/ws/info`
- review runtime page
- review active incidents
- review replay queue
- review connector state
- review dashboard freshness

Command:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\check-live-connections.ps1
```

Expected:

- `FRONTEND_UP=True`
- `BACKEND_UP=True`
- `DB_READY=True`
- `AUTH_READY=True`
- `WS_READY=True`
- `PROOF_ALLOWED=True`

## Weekly Checks

Weekly checks:

- run frontend verify
- run docs link check
- run repo health
- review open local artifacts
- review proof evidence status
- inspect runtime incidents
- review replay backlog
- review connector degradation
- confirm support owners

Commands:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\docs-link-check.ps1
powershell -ExecutionPolicy Bypass -File scripts\repo-health.ps1

cd frontend
npm.cmd run verify
```

## Monthly Maintenance

Monthly checks:

- review dependency posture
- review backup/restore runbooks
- perform a restore drill if scheduled
- review environment variables
- review Render logs and deployment posture
- review documentation map for stale status
- review security/leakage docs
- review local artifact policy

Do not rotate secrets casually without updating deployment and proof procedures.

## Release Preparation

Before release:

- confirm `main` equals `origin/main`
- confirm working tree has no production changes
- confirm local env files are unstaged
- confirm Playwright artifacts are unstaged
- run frontend verify
- run backend tests if backend changed
- run docs link check
- run live connection gate
- run hosted proof when runtime behavior or proof-covered flows changed
- update release evidence docs
- run `scripts\pilot-rc-check.ps1` for pilot RCs

## Evidence Collection

Collect:

- commit hash
- live URLs
- proof tenant code
- proof result
- frontend verify result
- docs link result
- live connection classification
- relevant screenshots or reports when intentionally archived

Do not collect:

- proof passwords
- `.env.local`
- Render secrets
- raw local proof state

## Health Verification

Health endpoints:

- `/actuator/health`
- `/actuator/health/readiness`
- `/actuator/health/liveness`
- `/api/auth/session`
- `/ws/info`

Readiness matters more than liveness for proof and pilot continuation.

## Backup Verification

Use backup/restore docs and scripts:

- [backup-restore-runbook.md](backup-restore-runbook.md)
- `scripts\backup-postgres.ps1`
- `scripts\restore-postgres.ps1`
- `scripts\verify-restore-drill.ps1`

Backup verification should prove that data can be restored, not merely that a backup file exists.

## Recovery Drills

Drill these scenarios:

- DB unavailable
- Redis unavailable
- backend restart
- frontend live but backend unavailable
- websocket degraded
- replay queue contains failed inbound records
- connector disabled

Use:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\recovery-checklist.ps1
```

## Hosted Proof Cadence

Run hosted proof when:

- release candidate is being frozen
- backend/API contracts changed
- frontend proof-critical selectors changed
- replay/scenario/auth/runtime behavior changed
- deployment environment changed materially
- recovery from infrastructure replacement is complete

Do not run hosted proof when:

- readiness is down
- auth/session is unhealthy
- websocket info is unhealthy
- backend/DB availability is uncertain

Hosted proof is validation, not a wake-up tool.
