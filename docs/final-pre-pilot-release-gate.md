# Final Pre-Pilot Release Gate

Last checked: **2026-08-13**

This is the final engineering gate before handing SynapseCore to Company 1 for a controlled pilot.

The purpose of this record is not to create new scope. It answers one question:

**Can this exact build be frozen and handed to Company 1?**

## Verdict

`READY FOR CONTROLLED COMPANY 1 PILOT - WITH DOCUMENTED OPERATING CONDITIONS`

Company 1 status:

`READY FOR CONTROLLED PILOT`

This is not a general-availability or enterprise-scale claim. It means the current SynapseCore build has enough verified evidence to support one bounded Company 1 pilot lane under the operating limits in this report.

## Release Identity

| Item | Value |
| --- | --- |
| Runtime/proof commit | `d096537` |
| Commit message | `Stabilize hosted proof sign-in check` |
| Recommended pilot release name | `v0.9.0-company1-pilot-rc1` |
| Tag status | Not created in this report |
| Product scope | Frozen for Company 1 pilot |
| Runtime behavior changed in final proof fix | No |

The final source change before this report updated only the hosted proof sign-in assertion so the production proof matches the accepted sign-in UI while still requiring `/sign-in`, the real sign-in card, and an accepted sign-in heading.

## Final Gate Summary

| Gate | Result |
| --- | --- |
| Gate 1 - Actuator security lockdown | ACCEPTED |
| Gate 2 - Backup and restore proof | ACCEPTED WITH DOCUMENTED LIMITATION |
| Gate 3 - Performance and scale proof | ACCEPTED |
| Gate 4 - Exhaustive control verification | ACCEPTED WITH DOCUMENTED LIMITATION |
| Final live connection gate | PASS from operator PowerShell |
| Final hosted proof | PASS, `6 / 6` |
| Final pilot classification | READY FOR CONTROLLED PILOT |

## Important Environment Note

Two execution environments produced different network results:

| Environment | Result |
| --- | --- |
| Operator PowerShell on the project machine | Reached Render frontend and backend successfully |
| Codex runtime environment | Could not connect to Render endpoints |

The final release classification uses the operator PowerShell evidence because it successfully reached the deployed Render services and ran the official hosted proof. The Codex runtime failure remains classified as an environment/network-path limitation, not as SynapseCore application evidence.

## Final Live Connection Evidence

Command:

```powershell
cd C:\Users\asus\Downloads\synapsecore_starter\synapsecore
powershell -ExecutionPolicy Bypass -File scripts\check-live-connections.ps1
```

Observed result from operator PowerShell:

```text
FRONTEND_UP=True
BACKEND_UP=True
DB_READY=True
AUTH_READY=True
WS_READY=True
PROOF_ALLOWED=True
```

Endpoint evidence:

| Endpoint | Result |
| --- | --- |
| Frontend `https://synapscore-frontend-3.onrender.com` | `200 OK` |
| Backend `/actuator/health` | `200 OK`, status `UP` |
| Backend `/actuator/health/readiness` | `200 OK`, status `UP` |
| Backend `/actuator/health/liveness` | `200 OK`, status `UP` |
| Backend `/api/auth/session` | `200 OK`, anonymous session response |
| Backend `/ws/info` | `200 OK`, SockJS/WebSocket info response |

Interpretation:

- frontend is live
- backend app is booted
- database readiness is passing through the backend readiness contract
- auth/session endpoint responds
- websocket info endpoint responds
- hosted proof is allowed

## Final Hosted Proof Preparation

Command:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\prepare-hosted-proof.ps1
```

Observed result:

- backend readiness ready
- auth session endpoint ready
- realtime SockJS endpoint ready
- frontend sign-in shell ready
- proof tenant `HOSTED-PROOF-3` reused
- proof operators and users ensured
- real catalog and inventory baseline prepared
- authenticated session ready
- authenticated dashboard summary ready
- authenticated runtime ready
- authenticated dashboard snapshot ready

The proof preparation used supported application APIs and the ignored local proof-state file. No manual database row edits were performed.

## Final Hosted Proof Result

Command:

```powershell
cd frontend
npm.cmd run test:e2e:prod
```

Observed result:

```text
6 passed (2.9m)
```

Passed tests:

| Test | Result |
| --- | --- |
| Auth flow and full authenticated page system render cleanly | PASS |
| Product catalog onboarding through tenant-scoped API and browser surface | PASS |
| Realtime dashboard summary updates without browser refresh | PASS |
| Replay recovery, scenario approval, execution, and browser role gating | PASS |
| Alerts, recommendations, orders, inventory, integrations, users, profile, and settings live-backend surfaces | PASS |
| Backend auth rate limiting surfaces without stuck loading state | PASS |

## Inconsistencies Found During Final Proof

Two issues appeared before the final successful proof pass.

| Issue | Classification | Resolution |
| --- | --- | --- |
| Cloudflare `520` from `https://synapscore-3.onrender.com/api/products` during an earlier hosted proof run | Render/proxy/origin transient availability failure | Not masked. Live connection gate was rerun, proof prep was rerun, and hosted proof was rerun cleanly. |
| Prod proof expected the old sign-in heading after accepted UI polish | Frontend proof selector drift | Fixed in `d096537` by centralizing the sign-in shell check around `/sign-in`, `.public-signin-card`, and the accepted heading pattern. |

No hosted proof standards were weakened. The Cloudflare 520 was not hidden with a retry or removed assertion. The selector fix made the proof match the accepted UI while preserving the proof requirement that sign-in actually renders.

## Gate 4 Control Verification Evidence

Gate 4 remains accepted with documented limitation.

| Item | Result |
| --- | --- |
| Authoritative controls inventory | `201` controls |
| Individually verified controls | `201 / 201` |
| Broken controls | `0` |
| Unverified controls | `0` |
| Controls execution suite | `7 / 7` batches |
| Unexpected 5xx responses in Gate 4 controls | `0` |
| Unexpected network failures in Gate 4 controls | `0` |

The final proof selector fix touched only the hosted production proof file and did not modify frontend runtime behavior, backend behavior, route behavior, or control behavior.

## Build, Test, And Repository Evidence

| Check | Result |
| --- | --- |
| Backend tests | PASS, `133` tests, `0` failures, `0` errors |
| Frontend lint | PASS |
| Frontend build | PASS |
| Frontend verify | PASS |
| Clean-worktree backend tests | PASS |
| Clean-worktree frontend build | PASS |
| Clean-worktree frontend verify | PASS |
| Production config check | PASS |
| Docs link check | PASS |
| Secret scan | PASS, no critical findings |
| Repo health | Local ignored artifacts may be present; do not stage env/proof/report/build artifacts |

Do not commit:

- `.env.local`
- `.hosted-proof`
- Playwright reports
- `frontend/test-results`
- `frontend/dist`
- `backend/target`
- local logs
- backup dumps

## Database And Flyway

| Check | Result |
| --- | --- |
| Replacement Render PostgreSQL database | Validated through readiness and hosted proof |
| Backend readiness | PASS |
| Flyway/migration path | Validated by backend startup/readiness and proof flows |
| Manual production DB mutation | NOT PERFORMED |
| Tenant proof setup | Supported tenant/admin APIs through `prepare-hosted-proof.ps1` |

The final proof used a clean supported bootstrap path. It did not require manual production table edits.

## Managed Database Backup Policy

What is already proven:

- Application-level PostgreSQL backup/restore was proven in Gate 2 with `pg_dump`, isolated restore, deterministic hashes, relational integrity checks, restored backend startup, authenticated restored-data reads, and honest failed-restore handling.

What remains a documented limitation:

- Provider-managed backup state for the current Render database should still be captured from the Render dashboard before operational reliance.
- Provider-level restore for the current managed Render database has not been drilled from this repository session.
- Do not claim enterprise SLA-grade recovery until provider backup/restore evidence is collected.

Policy classification:

`DOCUMENTED LIMITATION - ACCEPTED FOR CONTROLLED PILOT ONLY`

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
| Realtime | Included |

SynapseCore remains positioned as an **Intelligent Operations Platform** for operational visibility, coordination, recovery, governed decision-support, and realtime operational state.

It is not positioned as:

- full ERP replacement
- full WMS replacement
- autonomous business operator
- enterprise-wide replacement platform
- guaranteed cost-saver
- broad general-availability enterprise platform

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
- controlled operational window
- existing systems of record remain authoritative during pilot

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
- realtime delivery remains usable during the pilot window
- alerts help operators identify attention items
- recommendations are understandable and useful as decision support
- operator task completion improves or becomes easier to audit
- backup evidence is collected
- restore drill evidence is captured before operational reliance expands
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
- transient 5xx from Render/proxy/origin

`ROLLBACK`:

- restore or forward repair cannot preserve operational truth
- Company 1 must return fully to existing systems of record
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

## Known Limitations Accepted For Pilot

| Limitation | Pilot Impact | Required Handling |
| --- | --- | --- |
| Provider-level Render restore drill not yet captured | Recovery confidence is application-level plus provider expectation, not full provider proof | Capture provider backup page and perform restore drill before expanding reliance |
| Live Render saturation not proven | Do not exceed controlled pilot envelope | Keep operators and connector lane bounded |
| Single deployment posture | No HA claim | Use pilot stop conditions and fallback to existing systems |
| Codex runtime cannot reach Render | Codex-hosted verification cannot be the final live-run source | Operator PowerShell or CI with known network path remains authoritative |
| First final proof attempt saw Cloudflare 520 | Render/proxy/origin transient risk exists | Require live connection gate immediately before proof and pilot windows |

## Final Acceptance Checklist

| Requirement | Result |
| --- | --- |
| Release tree understood | PASS |
| Clean clone builds | PASS |
| Backend tests pass | PASS |
| Frontend lint/build/verify pass | PASS |
| Gate 4 inventory complete | PASS |
| Controls suite accepted | PASS |
| Hosted proof 6/6 passes | PASS |
| Live health/readiness works | PASS |
| Live auth works | PASS |
| Live WebSocket works | PASS |
| DB/Flyway state healthy through readiness/proof | PASS |
| Secret scan passes | PASS |
| Docs links pass | PASS |
| Managed backup policy documented | PASS WITH LIMITATION |
| Provider recovery limitation accepted or closed | ACCEPTED AS CONTROLLED-PILOT LIMITATION |
| Pilot scope defined | PASS |
| Pilot success metrics defined | PASS |
| Pilot stop conditions defined | PASS |
| Ownership defined | PASS |
| No unresolved Critical blocker | PASS |
| No unresolved High blocker | PASS |
| Release/tag created if appropriate | NOT CREATED |

## Final Classification

| Item | Result |
| --- | --- |
| COMPANY 1 | READY FOR CONTROLLED PILOT |
| RUNTIME/PROOF COMMIT | `d096537` |
| RELEASE | `v0.9.0-company1-pilot-rc1` recommended |
| OPERATORS | 3 to 5 recommended |
| WORKSPACES | 1 |
| CONNECTOR LANES | 1 initially |
| CONTROL PROOF | Gate 4 evidence: `201 / 201` |
| BACKEND TESTS | `133 / 133` |
| HOSTED PROOF | `6 / 6` |
| PROVEN LOCAL CONCURRENCY | 25 |
| PROVEN LOCAL RPS | about 41 |
| PROVEN REALTIME CONNECTIONS | 50 |
| CRITICAL BLOCKERS | None open for controlled pilot |
| HIGH BLOCKERS | None open for controlled pilot |
| DOCUMENTED LIMITATIONS | Provider-managed restore not drilled for current Render DB; live Render load ceiling not proven |
| NEXT MOVE | Prepare Company 1 pilot workspace, capture backup evidence, freeze pilot build, then start controlled onboarding |

Final verdict:

`READY FOR CONTROLLED COMPANY 1 PILOT - WITH DOCUMENTED OPERATING CONDITIONS`
