# Final Pre-Pilot Release Gate

Last checked: **2026-08-13 16:12:25 +02:00**

This is the final engineering gate before handing SynapseCore to Company 1 for a controlled pilot.

The purpose of this record is not to create new scope. It answers one question:

**Can this exact build be frozen and handed to Company 1 today?**

## Verdict

`NOT READY FOR COMPANY 1 - PILOT BLOCKERS REMAIN`

Company 1 status:

`NOT READY`

Reason:

- The deployed frontend is reachable.
- The deployed backend is not reachable on `https://synapscore-3.onrender.com`.
- Live health, liveness, readiness, auth/session, and `/ws/info` cannot be verified.
- `PROOF_ALLOWED=False`.
- Hosted proof and the live controls suite were not run because the backend trust prerequisites are unavailable.

This is an infrastructure/deployment availability blocker, not evidence of a frontend, backend test, or source-code regression.

## Starting Baseline

| Item | Result |
| --- | --- |
| Starting commit | `3459f11c2af5c7760d962b596ec0759773d8a3a4` |
| `origin/main` at start | `3459f11c2af5c7760d962b596ec0759773d8a3a4` |
| HEAD/origin comparison | MATCH |
| Initial `git diff --check` | PASS |
| Initial working tree | `docs/verification-status.md`, `frontend/frontend/`, `frontend/playwright-report-controls/` |

## Local Artifact Disposition

| Path | Classification | Action |
| --- | --- | --- |
| `docs/verification-status.md` | TRACK AND CORRECT | Stale generated local evidence replaced with current final-gate truth. |
| `frontend/frontend/` | DELETE GENERATED ARTIFACT | Removed after inspection. It contained a nested duplicate `tests` folder, not application source. |
| `frontend/playwright-report-controls/` | DELETE GENERATED ARTIFACT / IGNORE | Removed after inspection and added to `.gitignore` as a generated controls HTML report folder. |

Do not commit:

- `.env.local`
- `.hosted-proof`
- Playwright reports
- `frontend/test-results`
- `frontend/dist`
- `backend/target`
- local logs
- backup dumps

## Clean-Clone Reproducibility

Clean worktree:

- `C:\Users\asus\Downloads\synapsecore_starter\synapsecore-release-clean-3459f11`
- commit: `3459f11c2af5c7760d962b596ec0759773d8a3a4`

| Check | Result |
| --- | --- |
| Backend dependency/build/test restore | PASS |
| Backend tests in clean worktree | `133` tests, `0` failures, `0` errors |
| Frontend dependency restore | PASS after using worktree-local npm cache and allowing registry access |
| Frontend clean build | PASS |
| Frontend clean verify | PASS |
| Hidden local source required | NO |

Notes:

- The first clean frontend install attempt failed due to Windows/global npm cache permission and sandboxed registry access, not repository state.
- Re-running with a worktree-local npm cache and registry access succeeded.

## Production Configuration Review

| Area | Result |
| --- | --- |
| Render backend profile | `SPRING_PROFILES_ACTIVE=prod` in `render.yaml` |
| Render PostgreSQL | `DATABASE_URL` sourced from Render database `synapscore-postgres` |
| Render Redis | `SPRING_DATA_REDIS_URL` sourced from Render Redis `synapscore-redis` |
| CORS | `https://synapscore-frontend-3.onrender.com` |
| Session cookies | `SESSION_COOKIE_SECURE=true`, `SESSION_COOKIE_SAME_SITE=None` |
| Header fallback | `ALLOW_HEADER_FALLBACK=false` |
| JPA DDL mode | `SPRING_JPA_HIBERNATE_DDL_AUTO=validate` |
| Realtime broker | `SYNAPSECORE_REALTIME_BROKER_MODE=REDIS_PUBSUB` |
| Frontend API URL | `https://synapscore-3.onrender.com` |
| Frontend WebSocket URL | `https://synapscore-3.onrender.com/ws` |
| Health check path | `/actuator/health/liveness` |
| Secrets | Sourced from environment; no secrets printed or committed |

Config fixes applied during this gate:

- local ignored `infrastructure/env/backend.prod.selfhost.env` changed from `SPRING_JPA_HIBERNATE_DDL_AUTO=update` to `validate` so the local config checker can run safely; this file is not staged or committed.
- `scripts/ProdEnvTools.ps1` now accepts empty optional env values when checking for placeholders, avoiding a strict-mode crash while still flagging real placeholder values.

Production config script result:

- `powershell -ExecutionPolicy Bypass -File scripts\check-prod-config.ps1`
- Result: PASS

## Live Deployment Check

| Endpoint | Result |
| --- | --- |
| Frontend `https://synapscore-frontend-3.onrender.com` | HTTP 200 by `curl.exe` |
| Backend `/actuator/health` | FAIL, could not connect |
| Backend `/actuator/health/liveness` | FAIL, could not connect |
| Backend `/actuator/health/readiness` | FAIL, could not connect |
| Backend `/api/auth/session` | FAIL, could not connect |
| Backend `/ws/info` | FAIL, could not connect |
| Backend `/actuator/metrics` | NOT VERIFIED because backend host could not connect |
| Backend `/actuator/prometheus` | NOT VERIFIED because backend host could not connect |

Live connection classification:

```text
FRONTEND_UP=False from Invoke-WebRequest script path, but curl returned HTTP 200 for the frontend shell.
BACKEND_UP=False
DB_READY=False
AUTH_READY=False
WS_READY=False
PROOF_ALLOWED=False
```

Interpretation:

- `curl.exe` is the stronger frontend evidence in this environment because it returned the deployed shell.
- Backend failure is a true live gate blocker because every direct backend endpoint failed to connect after warm-up retry.
- The database and Flyway state cannot be proven live while the backend is unreachable.

## Database And Flyway

| Check | Result |
| --- | --- |
| Local/backend test Flyway validation | PASS, 7 migrations validated/applied in test contexts |
| Live database reachability through backend readiness | BLOCKED, backend unreachable |
| Live Flyway history cleanliness | BLOCKED, backend unreachable and no manual DB inspection performed |
| Manual production DB mutation | NOT PERFORMED |

No manual table edits were performed.

## Final Proof Results

| Proof | Result |
| --- | --- |
| Backend tests | PASS, `133` tests, `0` failures, `0` errors |
| Frontend lint | PASS |
| Frontend build | PASS |
| Frontend verify | PASS |
| Clean-worktree backend tests | PASS |
| Clean-worktree frontend build | PASS |
| Clean-worktree frontend verify | PASS |
| Control inventory | PASS, `201` controls |
| Individually verified controls | Gate 4 evidence remains `201 / 201` |
| Controls suite | NOT RUN in final gate because live backend is unreachable |
| Hosted proof | NOT RUN in final gate because `PROOF_ALLOWED=False` |
| Docs link check | PASS, `604` links, `0` missing |
| Secret scan | PASS, `0` critical findings, `5` known fixture findings |
| Repo health | NEEDS_ATTENTION before commit because intentional tracked changes existed and ignored artifacts are present locally |
| Release readiness script | PASS, with Docker config warnings caused by local Docker config access |

## Managed Database Backup Policy

What is already proven:

- Application-level PostgreSQL backup/restore was proven in Gate 2 with `pg_dump`, isolated restore, deterministic hashes, relational integrity checks, restored backend startup, authenticated restored-data reads, and honest failed-restore handling.

What is not proven from this repository session:

- Actual Render dashboard backup state for the current database.
- Actual provider restore drill against the current managed Render database.
- Provider-level encryption evidence beyond Render platform expectations.
- Provider-level recovery instance creation evidence.

Current public Render documentation says paid Render Postgres supports point-in-time recovery and logical exports, while free instances do not provide provider recovery capabilities. Recovery windows depend on plan, and PITR restore creates a new database instance for validation before cutover.

References:

- [Render Postgres Recovery and Backups](https://render.com/docs/postgresql-backups)
- [Render PostgreSQL backup and restore article](https://render.com/articles/how-to-backup-and-restore-postgresql-databases)

Policy classification:

`DOCUMENTED LIMITATION`

Company 1 should not start operational reliance until the current Render database's Recovery page has been captured and the selected backup policy is accepted.

## Company 1 Backup Policy

Measured:

- Application-level backup/restore drill is proven.
- Provider-level restore for the current Render database is not proven in this gate.

Target for Company 1:

| Policy Area | Company 1 Target |
| --- | --- |
| Backup frequency | Daily managed backup/export evidence plus backup before release or schema-affecting changes |
| Retention | At least the provider recovery window plus downloaded/off-host export where practical |
| Off-host copy | Required for pilot evidence if provider export is available |
| Restore drill frequency | Before pilot start and at least once during pilot stabilization |
| Responsible owner | Deployment/Infrastructure Owner |
| Recovery escalation | Incident Owner plus Deployment Owner plus Company Technical Contact |
| Expected RPO | Target, not yet contract: <= 24 hours for application-level export; provider PITR window if confirmed |
| Measured RTO | Application-level restored backend startup was proven in Gate 2; live provider RTO not measured |
| Provider restore | Not proven in this gate |

Do not claim enterprise SLA coverage until provider restore evidence is captured.

## Pilot Scope Freeze

Recommended Company 1 starting envelope:

| Scope Area | Limit |
| --- | --- |
| Workspaces | 1 |
| Operators | 3 to 5 |
| Connector lanes | 1 initially |
| Data | Bounded real company slice |
| Catalog | Included |
| Inventory | Included |
| Orders | Included |
| Alerts | Included |
| Recommendations | Included |
| Replay | Included |
| Approvals | Included |
| Scenarios | Included |
| Realtime | Included only after live backend/WebSocket proof is green again |

SynapseCore remains positioned as an **Intelligent Operations Platform** for operational visibility, coordination, recovery, governed decision-support, and realtime operational state.

It is not positioned as:

- full ERP replacement
- full WMS replacement
- autonomous business operator
- enterprise-wide replacement platform

Do not use `AI-powered` as the pilot claim. Use `intelligence inside` only where appropriate.

## Pilot Operating Limits

Proven in Gate 3:

- 25 authenticated local read operators
- about 41 RPS read soak
- p95 under 500 ms in the 25-user soak and focused run
- 50 realtime WebSocket clients

Limitations:

- These are local production-shaped Docker results, not live Render saturation results.
- Single backend/PostgreSQL/Redis posture.
- Synthetic pilot-sized data.
- Multi-tenant and high-write scale are not proven.

Recommended Company 1:

- 3 to 5 operators
- 1 workspace
- 1 connector lane

Safety margin:

- 3 operators are about 12% of the 25-operator proven local read envelope.
- 5 operators are about 20% of the 25-operator proven local read envelope.

## Pilot Success Metrics

Minimum measurable success criteria:

- login/session reliability remains acceptable for active pilot operators
- no cross-tenant exposure
- no authentication or authorization bypass
- no data corruption
- dashboard operational state remains understandable
- integration ingestion is visible and classifiable
- replay/recovery succeeds for agreed failed inbound cases
- approvals complete with correct role boundaries
- scenario execution applies to the intended object only
- realtime delivery works after backend readiness and `/ws/info` are restored
- alert visibility helps operators identify attention items
- recommendations are understandable and useful as decision support
- operator task completion improves or becomes easier to audit
- backup evidence is collected
- restore drill evidence is captured before operational reliance
- latency remains acceptable for the small pilot operator group
- Company feedback is logged and classified before implementation

## Pilot Stop Conditions

Immediate `PAUSE`:

- tenant isolation failure
- authentication/authorization bypass
- secret exposure
- data corruption
- unrecoverable database failure
- governance action applied to the wrong object
- repeated incorrect replay into live flow
- unexpected destructive mutation
- sustained backend unavailability during an active operational window

`INVESTIGATE`:

- intermittent realtime disconnect
- slow connector telemetry
- stale dashboard snapshot
- replay visibility mismatch
- repeated operator confusion
- unexpected 4xx or validation failures

`ROLLBACK`:

- restore or forward repair cannot preserve operational truth
- Company 1 must return to existing systems of record
- the platform cannot maintain the agreed pilot lane safely

## Ownership

| Responsibility | Owner Role |
| --- | --- |
| Deployment | Deployment/Infrastructure Owner |
| Backup | Backup/Restore Owner |
| Restore drill | Backup/Restore Owner plus Deployment Owner |
| Incident handling | Incident Owner |
| Company contact | Pilot Success Owner |
| Rollback | Deployment Owner plus Company Operations Owner |
| Access provisioning | Tenant Admin / Access Owner |
| Connector configuration | Integration Owner |
| Daily health review | Operations Owner |

## Release Identifier

Existing RC convention:

- `v0.9.0-pilot-rc1`

Final gate release recommendation:

- `v0.9.0-company1-pilot-rc1`

Tag status:

- No tag created in this gate.
- Reason: final live backend gate failed and hosted proof could not run.

## Issue Classification

| Severity | Issue | Classification |
| --- | --- | --- |
| Critical | Live backend cannot be reached on `https://synapscore-3.onrender.com` | PILOT BLOCKER |
| High | Hosted proof and controls suite cannot run while backend is unreachable | PILOT BLOCKER consequence |
| Medium | Provider-managed backup state for current Render DB not captured in repository evidence | PRE-PILOT CONDITION |
| Medium | Live Flyway/database state cannot be proven while backend is unreachable | PRE-PILOT CONDITION |
| Medium | Render provider restore capability not drilled for current database | DOCUMENTED LIMITATION |

## Post-Pilot Work

Only after Company 1 begins and evidence justifies it:

- longer live-like load tests
- provider backup automation
- provider restore drills with captured evidence
- metrics/tracing maturity
- connector maturity based on pilot workflows
- multi-tenant load proof
- enterprise SSO/RBAC roadmap work

Do not start new feature work before the live backend gate is restored.

## Final Acceptance Checklist

| Requirement | Result |
| --- | --- |
| HEAD/origin understood | PASS at start |
| Release tree clean/reproducible | PARTIAL, clean worktree reproduced; final commit pending |
| Clean clone builds | PASS |
| Backend tests pass | PASS |
| Frontend lint/build/verify pass | PASS |
| Gate 4 inventory complete | PASS |
| Controls suite passes | BLOCKED, backend unreachable |
| Hosted proof 6/6 passes | BLOCKED, backend unreachable |
| Live health/readiness works | FAIL |
| Live auth works | FAIL |
| Live WebSocket works | FAIL |
| Sensitive actuator endpoints restricted | BLOCKED, backend unreachable |
| DB/Flyway state healthy | BLOCKED, backend unreachable |
| Secret scan passes | PASS |
| Docs links pass | PASS |
| Managed backup policy documented | PASS WITH LIMITATION |
| Provider recovery limitation accepted or closed | NOT CLOSED |
| Pilot scope defined | PASS |
| Pilot success metrics defined | PASS |
| Pilot stop conditions defined | PASS |
| Ownership defined | PASS |
| No unresolved Critical blocker | FAIL |
| No unresolved High blocker | FAIL |
| Final release commit identified | PENDING |
| Release/tag created if appropriate | NOT CREATED |

## Final Summary

| Item | Result |
| --- | --- |
| COMPANY 1 | NOT READY |
| RELEASE COMMIT | Pending final gate evidence commit |
| RELEASE | `v0.9.0-company1-pilot-rc1` recommended, not tagged |
| OPERATORS | 3 to 5 recommended |
| WORKSPACES | 1 |
| CONNECTOR LANES | 1 initially |
| CONTROL PROOF | Gate 4 evidence: `201 / 201`; final live controls rerun blocked |
| BACKEND | `133 / 133` |
| HOSTED PROOF | Blocked in final gate; previous Gate 4 baseline was `6 / 6` |
| PROVEN LOCAL CONCURRENCY | 25 |
| PROVEN LOCAL RPS | about 41 |
| PROVEN REALTIME CONNECTIONS | 50 |
| CRITICAL BLOCKERS | Live backend unreachable |
| HIGH BLOCKERS | Hosted proof and live controls cannot run |
| DOCUMENTED LIMITATIONS | Provider-managed backup/restore not proven for current Render DB; live Render load ceiling not proven |
| NEXT MOVE | Restore backend service availability, rerun live checks, then run hosted proof and controls suite before tagging |

Final verdict:

`NOT READY FOR COMPANY 1 - PILOT BLOCKERS REMAIN`
