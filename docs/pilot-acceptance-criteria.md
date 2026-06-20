# Pilot Acceptance Criteria

This document defines what should be true before a company pilot is considered accepted, stable, or ready for expansion.

It is meant to prevent vague pilot success. A pilot should not be accepted just because the UI opens or a demo path looks polished.

## Pilot Acceptance Philosophy

A SynapseCore pilot should prove operational usefulness and runtime trust inside a controlled scope.

Acceptance should require:

- real workspace setup
- real operator sign-in
- real catalog/inventory/order flows
- real integration and replay visibility
- approval/scenario behavior
- runtime trust visibility
- recovery understanding
- proof evidence
- honest limitation acknowledgement

## Pilot Scope Definition

Before pilot start, define:

- tenant/workspace name
- number of operators
- roles and responsibilities
- warehouses or sites in scope
- product/catalog sample size
- order sources in scope
- connectors in scope
- replay scenarios in scope
- approval scenarios in scope
- proof and verification cadence
- rollback expectations

Anything outside that scope should be treated as future expansion, not implicit pilot failure.

## Minimum Technical Acceptance

| Area | Acceptance Criteria |
|---|---|
| Frontend | Public shell and authenticated shell load reliably |
| Backend | Health, liveness, and readiness are understood |
| DB | PostgreSQL is connected and migrations are stable |
| Redis/session | Session behavior works for pilot users |
| Auth | Operators can sign in and sign out |
| Workspace | Tenant workspace is identifiable and scoped |
| API | Core API families respond as expected |
| Websocket | Realtime endpoint works or degraded state is clear |
| Proof | Proof gates are respected |

## Minimum Operational Acceptance

| Area | Acceptance Criteria |
|---|---|
| Catalog | Pilot products can be created or imported |
| Warehouses | Pilot warehouse context is visible |
| Inventory | Stock posture is visible and updateable |
| Orders | Pilot orders can enter the system |
| Alerts | Risk signals appear when expected |
| Recommendations | Operator guidance appears from real state |
| Integrations | Connector visibility is understandable |
| Replay | Failed inbound work enters visible recovery |
| Approvals | Approval/rejection paths are testable |
| Scenarios | Scenario history and execution path are visible |
| Runtime | Operators can see degraded/trust state |

## Proof Acceptance

Hosted proof should only run when:

- live frontend is reachable
- live backend is reachable
- readiness is up
- auth/session endpoint responds
- websocket info responds
- `PROOF_ALLOWED=True`

Acceptance evidence can include:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\check-live-connections.ps1
```

If allowed:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\prepare-hosted-proof.ps1

cd frontend
npm.cmd run test:e2e:prod
```

If backend/DB are unavailable, hosted proof must remain paused.

## Local Acceptance

Local acceptance is useful for development but is not the same as hosted pilot acceptance.

Local checks:

```powershell
cd C:\Users\asus\Downloads\synapsecore_starter\synapsecore
powershell -ExecutionPolicy Bypass -File scripts\check-local-connections.ps1

cd frontend
npm.cmd run build

cd ..\backend
cmd /c mvnw.cmd test
```

Local acceptance should prove:

- repo builds
- backend tests pass
- local stack can connect
- frontend can reach backend

## Success Signals

A pilot is showing value when:

- operators use one shared surface instead of scattered screenshots
- failed inbound data is visible and recoverable
- replay outcomes are auditable
- approval ownership is clearer
- inventory/order pressure is visible earlier
- runtime degradation is understood instead of hidden
- technical reviewers can classify failures without guessing

## Warning Signs

Warning signs include:

- operators cannot explain what degraded state means
- replay queue grows without review
- connector failures are ignored
- proof is run while readiness/auth/ws are unhealthy
- DB backup/restore expectations are unclear
- pilot scope keeps expanding without new acceptance criteria
- the frontend is mistaken for full system readiness

## Expansion Criteria

Before expanding pilot scope, confirm:

- core pilot criteria passed
- operators understand incident states
- proof has passed in the intended environment
- backup/restore posture is understood
- limitations are accepted
- support ownership is defined
- connector maturity is sufficient for the next data source

## Non-Acceptance Conditions

Do not classify a pilot as accepted if:

- backend readiness is not stable
- auth/session is unreliable
- replay recovery cannot be demonstrated
- operators cannot see failure states
- proof is blocked
- DB dependency is unavailable
- current limitations are being ignored

## Related Docs

- [pilot-program-guide.md](pilot-program-guide.md)
- [pilot-adoption-roadmap.md](pilot-adoption-roadmap.md)
- [pilot-faq.md](pilot-faq.md)
- [proof-and-validation.md](proof-and-validation.md)
- [current-limitations.md](current-limitations.md)

## Bottom Line

A SynapseCore pilot succeeds when the company can trust the operational loop, not when a demo screen looks complete.

