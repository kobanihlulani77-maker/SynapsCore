# Known Operational Risks

This document is a living operational risk register for SynapseCore.

It is not a list of failures. It is a practical record of conditions that can affect platform trust, pilot readiness, proof execution, or enterprise expansion.

## Risk Register Summary

| Risk | Current Impact | Likelihood | Severity | Current Mitigation | Future Hardening |
|---|---|---:|---:|---|---|
| DB unavailable | Backend readiness/proof blocked | Medium | High | health checks, recovery docs | HA DB, backup drills, alerting |
| Redis unavailable | sessions/realtime may degrade | Medium | High | readiness checks, runbooks | managed Redis posture, failover |
| Render free/trial DB suspended | live backend unavailable | Medium | High | pause proof, classify honestly | paid/stable dependency tier |
| Frontend live while backend down | false confidence risk | Medium | High | live connection script | stronger status banner/ops process |
| Replay queue stale or growing | recovery backlog | Medium | Medium/High | replay UI, incidents | queue architecture and alerting |
| Scheduler/replay transaction noise | log/error pressure | Low/Medium | Medium | automated preflight and tests | deeper worker isolation |
| Connector breadth limited | adoption scope bounded | High | Medium | honest pilot scoping | connector roadmap |
| Websocket instability | realtime trust degraded | Medium | Medium/High | `/ws/info`, reconnect UX | horizontal realtime scale |
| Local Docker/host port conflicts | local bring-up confusion | High | Medium | local runbooks | stronger local doctor script |
| Proof run during unhealthy readiness | misleading proof results | Medium | High | `PROOF_ALLOWED` gate | CI/environment gating |
| Backup/restore untested | recovery uncertainty | Medium | High | scripts and docs | scheduled restore drills |
| Enterprise identity gaps | enterprise adoption friction | Medium | Medium/High | current auth/session model | SSO/SAML/OIDC, advanced RBAC |

## DB Dependency Risk

SynapseCore depends on PostgreSQL for operational truth.

Impact if unavailable:

- backend may fail startup or readiness
- auth/session-dependent proof can fail
- dashboard snapshot cannot be trusted
- replay and approvals cannot be safely processed

Current mitigation:

- readiness endpoints
- live/local connection scripts
- database and migration docs
- backup/restore scripts

Future hardening:

- managed HA database
- routine restore drills
- backup retention policy
- clearer RPO/RTO targets

## Redis And Session Risk

Redis supports production session and realtime posture.

Impact if unavailable:

- auth/session behavior may degrade
- realtime coordination may degrade
- proof may be blocked

Current mitigation:

- readiness/auth/ws checks
- Redis documented as dependency
- local/runbook guidance

Future hardening:

- managed Redis failover
- session resilience strategy
- observability around session failures

## Frontend-Only Availability Risk

The frontend can be reachable while backend/DB are unavailable.

This is dangerous because:

- users may see a live shell
- reviewers may assume the platform is healthy
- proof may be attempted too early

Current mitigation:

- docs repeatedly distinguish frontend availability from full system readiness
- `scripts/check-live-connections.ps1`
- proof is gated by readiness/auth/ws

Future hardening:

- clearer live status presentation
- operational status page
- environment-aware frontend degradation messaging

## Replay Risk

Replay is a strength, but it is also an operational responsibility.

Risks:

- backlog ignored
- dead-lettered records left unreviewed
- stale records causing repeated automated attempts
- replay accepted without audit review

Current mitigation:

- replay queue
- incidents
- runtime telemetry
- manual replay controls
- proof coverage for supported replay flow

Future hardening:

- dedicated worker architecture
- queue dashboards
- richer retry policies
- operator assignment and SLA ownership

## Connector Maturity Risk

Current connector support is useful but bounded.

Current scope:

- webhook order ingestion
- CSV order import
- scheduled pull style support
- connector health visibility
- replay/recovery records

Risk:

- buyer expectations may exceed current connector breadth
- enterprise systems may require custom mapping, auth, transformation, or SLAs

Mitigation:

- pilot scoping
- platform differentiation docs
- current limitations docs

Future hardening:

- connector SDK/pattern
- more connector types
- transformation versioning
- connector-specific observability

## Realtime Risk

Realtime gives command-center value, but it depends on backend and websocket health.

Risks:

- websocket disconnects
- delayed updates
- browser reconnect loops
- multi-node scaling not fully enterprise-hardened yet

Current mitigation:

- `/ws/info`
- realtime indicators
- dashboard snapshot fallback
- proof checks

Future hardening:

- horizontal realtime architecture
- broker posture maturity
- stronger connection telemetry

## Local Environment Risk

Local development can be confused by:

- Docker Postgres vs Windows Postgres
- `localhost` vs `127.0.0.1`
- backend container already using `8080`
- stale volumes
- local `.env.local` drift

Current mitigation:

- local runbook
- infrastructure handbook
- local recovery playbook
- local debug plan

Future hardening:

- stronger local doctor script
- clearer automated port conflict classification

## Proof Risk

Proof can become misleading if run at the wrong time.

Risk cases:

- DB unavailable
- backend readiness failing
- auth/session failing
- websocket failing
- proof tenant data not prepared
- frontend selectors drift after UI changes

Current mitigation:

- `PROOF_ALLOWED` classification
- hosted proof docs
- frontend proof selector discipline
- proof pause rules

Future hardening:

- CI-enforced proof gates
- better proof environment reset
- richer proof reports

## Enterprise Hardening Risks

Before broad enterprise claims, SynapseCore still needs stronger:

- HA deployment posture
- backup/restore maturity
- SSO/SAML/OIDC
- advanced RBAC
- secrets rotation
- audit retention policy
- tracing and metrics stack
- background worker architecture
- connector scalability
- horizontal realtime scale

These are not hidden failures. They are known maturity steps.

## Risk Review Cadence

Recommended cadence:

- review before pilot kickoff
- review after every major release
- review after any backend/DB outage
- review after proof failure
- review before expanding tenant or connector scope

## Related Docs

- [current-limitations.md](current-limitations.md)
- [enterprise-hardening-roadmap.md](enterprise-hardening-roadmap.md)
- [deployment-recovery-guide.md](deployment-recovery-guide.md)
- [failure-classification-matrix.md](failure-classification-matrix.md)
- [pilot-acceptance-criteria.md](pilot-acceptance-criteria.md)

## Bottom Line

SynapseCore becomes more trustworthy when known risks are named clearly, watched deliberately, and hardened in the right order.

