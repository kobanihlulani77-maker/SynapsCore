# Enterprise Hardening Roadmap

This roadmap describes what SynapseCore already does well, what its current deployment posture assumes, and what still needs to be hardened before broad enterprise-scale claims would be responsible.

## Current Strengths

The platform already has credible strengths:

- tenant-based SaaS architecture
- session-aware auth model
- replay recovery as a first-class product surface
- scenario approval and execution governance
- runtime trust and incident surfaces
- hosted proof discipline
- productized frontend command-center UX

These strengths are meaningful because they align product behavior with operational reality.

## Current Supported Scale Assumptions

Current supported posture should be read as:

- serious pilot and controlled operational scope
- modest deployment scale assumptions
- dependency on working DB and Redis for real proof/runtime trust
- connector scope intentionally limited to supported lanes

This is strong enough for serious product work, but not yet the same as broad enterprise-scale signoff.

## Current Deployment Posture

Current deployment posture includes:

- Render frontend + backend separation
- managed Postgres
- managed Redis
- Redis-backed session and realtime in production profile
- readiness/liveness health surfaces
- hosted proof as real deployment validation

That posture is credible, but it is still a controlled deployment story rather than a fully hardened enterprise topology.

## Needs Hardening

### HA Deployment

Current gap:

- no claim of full high-availability multi-node architecture with hard failover discipline

Future hardening:

- clearer HA deployment topologies
- failover playbooks
- dependency recovery expectations

### Background Job Separation

Current gap:

- some platform responsibilities still live close to the main backend app lifecycle

Future hardening:

- clearer separation of long-running or heavy background responsibilities
- stronger operational isolation

### Queue Architecture

Current gap:

- current dispatch/replay/operational queue posture is meaningful, but not yet a broader dedicated job-platform story

Future hardening:

- stronger queue visibility
- clearer retry/dead-letter posture
- more explicit worker scaling

### Metrics Stack

Current gap:

- metrics surfaces exist, but broader production metrics stack maturity is still growing

Future hardening:

- richer dashboards
- long-term retention strategy
- stronger operational thresholding

### Tracing Stack

Current gap:

- request IDs and runtime visibility exist, but full distributed tracing maturity is not the current story

Future hardening:

- deeper request and dependency tracing
- correlation across backend, DB, and queue behavior

### Audit / Event Storage

Current gap:

- audit and business events are real and useful, but larger long-term storage, retention, and archive policy still need more enterprise maturity

Future hardening:

- retention tiers
- export strategy
- long-term operational evidence policy

### Secrets Management

Current gap:

- repo discipline is improving, but broader enterprise-grade secrets posture still needs deeper operational maturity

Future hardening:

- stronger secret lifecycle management
- clearer rotation and runtime policy
- reduced dev/test fixture debt over time

### SSO / SAML / OIDC

Current gap:

- current auth/session model is workspace-code plus username/password

Future hardening:

- SSO/OIDC/SAML integration
- enterprise identity provider alignment

### Advanced RBAC

Current gap:

- role-gated control exists, but broader enterprise authorization models are still limited

Future hardening:

- more granular role hierarchies
- delegated administration
- policy templates

### Connector Scalability

Current gap:

- connector breadth and connector operational depth are intentionally narrow

Future hardening:

- broader connector library
- deeper operational policies
- richer failure classification and automation

### Caching Strategy

Current gap:

- caching exists where useful, but broader cache strategy is not yet an enterprise-scale story

Future hardening:

- clearer cache ownership
- cache invalidation policy
- scale-oriented summary and snapshot posture

### Horizontal Realtime Scale

Current gap:

- current Redis pub/sub posture is meaningful, but broader horizontal realtime scale maturity still needs more work

Future hardening:

- stronger broker and fanout topology options
- more explicit large-scale subscription posture

### DB Backup / Restore Maturity

Current gap:

- backup and restore scripts exist, but long-term operational maturity depends on rehearsal and broader platform process

Future hardening:

- regular restore drills
- retention policy
- rollback discipline

### Advanced Observability

Current gap:

- runtime and incident surfaces are good, but a deeper observability stack is still a growth area

Future hardening:

- richer metrics
- tracing
- dependency correlation
- better incident analytics

## Bottom Line

SynapseCore has a credible operational foundation, but it should not pretend full enterprise-scale hardening already exists.

The right posture is:

- strong current product and pilot credibility
- honest current deployment limits
- a clear roadmap for what enterprise hardening really means
