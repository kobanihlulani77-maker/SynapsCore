# Engineering Priorities

This document ranks the engineering priorities for SynapseCore based on the current platform state, proof posture, and operational goals.

## Immediate Priorities

### 1. Proof Reliability

Why it matters:

- proof is the discipline that keeps deployment truth aligned with product claims
- replay, auth, readiness, and websocket flows must remain proof-safe

Key focus:

- hosted proof stability
- selector safety
- readiness gating discipline
- stable replay verification paths

### 2. Deployment Reliability

Why it matters:

- a live frontend without a trustworthy backend creates misleading partial availability

Key focus:

- startup clarity
- readiness behavior
- Render recovery behavior
- operator-visible degraded-state truth

### 3. DB Resilience

Why it matters:

- PostgreSQL is the operational record of truth
- DB failure directly impacts readiness and trust

Key focus:

- better dependency recovery understanding
- safer startup and restore posture
- clearer backup and restore confidence

### 4. Realtime Resilience

Why it matters:

- the command-center value depends on trustworthy live updates

Key focus:

- websocket reliability
- Redis posture
- reconnect and stale-state handling

## Medium-Term Priorities

### 5. Connector Maturity

Why it matters:

- current connector scope is real but still intentionally narrow

Key focus:

- connector breadth only where operationally justified
- stronger connector diagnostics
- safer degraded-state and replay flows

### 6. Queue Architecture And Background Processing

Why it matters:

- some operational work should evolve beyond synchronous request paths

Key focus:

- worker separation
- clearer queue ownership
- safer replay and dispatch processing under pressure

### 7. Audit And Event Systems

Why it matters:

- the platform promise includes recovery traceability and runtime trust

Key focus:

- clearer event capture
- stronger audit retention strategy
- operational memory quality

### 8. Observability

Why it matters:

- runtime trust improves only if engineering can explain what degraded means

Key focus:

- metrics
- traceability
- clearer incident correlation

## Long-Term Priorities

### 9. Enterprise Auth

Why it matters:

- larger buyers will expect stronger identity posture

Key focus:

- SSO
- SAML or OIDC maturity
- enterprise onboarding compatibility

### 10. Advanced RBAC

Why it matters:

- broader adoption requires more nuanced authorization than early pilot roles

Key focus:

- role expansion
- safer approval and execution governance
- finer workspace controls

### 11. Metrics And Tracing Platform

Why it matters:

- larger-scale trust requires richer runtime understanding

Key focus:

- telemetry maturity
- correlation across backend, realtime, and operational incidents

### 12. HA Deployment And Scaling Posture

Why it matters:

- true enterprise confidence requires more than a single deployment path

Key focus:

- high-availability planning
- state separation
- websocket scale strategy
- background worker topology

## Bottom Line

The most important engineering theme is not adding random features faster.

It is strengthening the truth chain:

- proof
- deployment
- database
- realtime
- replay
- observability

That is what makes SynapseCore more trustworthy as an operational platform.
