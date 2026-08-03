# Repository Maturity

This document assigns evidence-based maturity levels to the SynapseCore repository.

It intentionally avoids inflated enterprise claims.

## Maturity Scale

| Level | Meaning |
| --- | --- |
| `1 - Emerging` | Exists but incomplete or inconsistent |
| `2 - Developing` | Useful but still uneven |
| `3 - Pilot Ready` | Strong enough for controlled pilot scope |
| `4 - Operationally Mature` | Reliable operating discipline with repeatable support |
| `5 - Enterprise Mature` | Proven at enterprise scale with HA, compliance, automation, and support depth |

## Architecture

Level: `3 - Pilot Ready`

Evidence:

- frontend/backend separation
- Spring Boot modular backend
- React command-center frontend
- PostgreSQL and Redis roles documented
- realtime and proof flows documented

Not level 4 or 5 because:

- no full HA architecture yet
- background workers and queue architecture are not separated
- enterprise-scale performance evidence is not yet established

## Testing

Level: `3 - Pilot Ready`

Evidence:

- backend tests exist
- frontend verify exists
- hosted proof passed against live replacement DB
- proof state and selectors were hardened

Not level 4 or 5 because:

- broader performance/load coverage is not established
- proof trend reporting is manual
- more API contract tests would improve confidence

## Documentation

Level: `4 - Operationally Mature`

Evidence:

- architecture docs
- runbooks
- recovery docs
- proof docs
- pilot RC docs
- support and operations docs
- docs link check

Risk:

- high doc volume requires ownership discipline

## Deployment

Level: `3 - Pilot Ready`

Evidence:

- Render deployment proven
- Docker Compose paths exist
- live connection checks exist
- release evidence exists

Not level 4 or 5 because:

- rollback drills are not yet routine evidence
- environment drift detection is manual
- no mature staged deployment pipeline yet

## Recovery

Level: `3 - Pilot Ready`

Evidence:

- DB replacement was recovered and revalidated
- recovery runbooks exist
- replay/recovery proof exists
- backup/restore docs and scripts exist

Not level 4 or 5 because:

- scheduled restore drill evidence is not yet routine
- incident automation is limited

## Operations

Level: `3 - Pilot Ready`

Evidence:

- operations handbook
- support playbook
- pilot operator checklist
- runtime trust surfaces
- live connection check

Not level 4 or 5 because:

- support coverage and incident rotation are not formalized
- operational metrics are not yet automated

## Proof

Level: `4 - Operationally Mature`

Evidence:

- full hosted proof passed against live replacement DB
- proof prep validates readiness/auth/ws/frontend/authenticated warm-up
- proof catches selector drift
- proof evidence frozen

Not level 5 because:

- proof history and artifacts are not centrally retained by policy
- broader scale and chaos proof are future work

## Maintainability

Level: `3 - Pilot Ready`

Evidence:

- module boundaries exist
- docs explain structure
- scripts aid onboarding
- quality gates are documented

Risks:

- script overlap
- documentation volume
- local artifact noise
- proof-critical selector drift risk

## Security

Level: `3 - Pilot Ready`

Evidence:

- auth/session proof
- rate limiting proof
- tenant model
- CORS/session posture
- secret scanning script
- security docs

Not level 4 or 5 because:

- SSO/SAML/OIDC is not implemented
- advanced RBAC is not mature
- formal security review cadence is not yet established

## Overall Repository Maturity

Overall level: `3 - Pilot Ready`

Rationale:

SynapseCore is strong enough for a controlled pilot within its current supported scope. It has real proof, operational docs, support guidance, and release candidate discipline.

It is not yet full enterprise mature because HA, large-scale operations, advanced observability, enterprise auth, formal support process, and long-term operational automation still need hardening.
