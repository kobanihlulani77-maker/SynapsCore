# Security And Trust Model

This document explains how SynapseCore currently approaches security, trust, operational safety, and the boundaries of its current maturity.

It is intentionally honest about what is strong, what is verified, and what still needs hardening.

## Security And Trust Philosophy

SynapseCore is not only trying to protect data. It is trying to protect operational trust.

That means security and trust include:

- who can access what
- whether tenants are isolated
- whether replay and approvals are safe
- whether auth and session state are predictable
- whether runtime truth is visible
- whether proof and validation actually reflect deployed reality

## Auth / Session Model

Primary auth model:

- session-first
- workspace code + username + password
- signed-in tenant-scoped identity

Production expectations:

- browser sessions are Redis-backed
- secure cookie posture is environment-sensitive
- header fallback is disabled in production

Why this matters:

- the platform is built around real operator identity
- protected actions should resolve from the authenticated session, not from loose UI-only assumptions

## Tenant Enforcement

Tenant enforcement is central to trust.

The system is designed so one workspace should not read or mutate another workspace’s:

- products
- inventory
- orders
- alerts
- recommendations
- replay queue rows
- runtime diagnostics
- operator lanes

This is both a security concern and a product integrity concern.

## Replay Safety

Replay is a high-trust action because it can reintroduce failed work into the live order flow.

Replay safety means:

- failed work must be tenant-scoped
- replay records must remain visible
- operator roles must matter
- manual-only recovery should not be stolen by automation where product rules forbid it
- replay should follow deterministic eligibility and locking rules

Replay is intentionally operator-visible because hidden recovery would lower trust.

## Approval Governance

Scenario approval is one of the control-safety layers in the product.

Governance principles:

- risky actions should be reviewable
- roles should matter
- approval states should be visible
- escalations and ownership should be traceable

The platform is not trying to hide operational decisions behind backend-only automation. It is trying to make them governable.

## Rate Limiting

Rate limiting exists because operational systems must protect:

- auth endpoints
- sensitive mutations
- tenant onboarding lanes
- integration administration lanes

This matters for both:

- abuse resistance
- user-visible operational discipline

The hosted proof also validates that rate limiting is real in the browser experience.

## CORS Posture

CORS matters because the frontend and backend are deployed as separate origins on Render.

Safe posture means:

- only intended origins should receive cross-origin browser access
- wildcard production origins are not acceptable
- cookie and credential behavior must align with the deployed frontend/backend pair

This is part of trust because cross-origin behavior affects whether the platform actually works safely in the browser.

## Runtime Trust Posture

Runtime trust is exposed intentionally because the platform should not pretend the system is healthy if it is not.

Important trust surfaces:

- liveness
- readiness
- runtime snapshot
- incident inbox
- websocket info

The goal is:

- frontend truth and backend truth should agree
- degraded states should be visible
- proof should stop when trust preconditions are not met

## Hosted Proof Discipline

Hosted proof is part of the trust model because it verifies the deployed system honestly.

Why it matters:

- prevents drift between code and claims
- prevents fake confidence from frontend-only success
- proves replay, realtime, auth, and scenario flows against the live deployment

Hosted proof should not run when:

- readiness is not healthy
- auth session is not reachable
- websocket info is not reachable
- the backend is timing out

## Leakage Scanning

The project already includes leakage and secret scanning discipline.

That includes:

- secret scanning scripts
- leakage audit docs
- explicit tracking of fixture credential debt
- checks that proof passwords are not leaking through outward-facing docs and scripts

This is an important trust signal: the project knows the difference between dev fixtures and real secrets.

## Secret Scanning

Secret scanning is important because:

- proof credentials are real when used
- platform-admin/bootstrap tokens are sensitive when real
- docs and scripts should never normalize leaking secrets

The current repo already treats this as a real concern rather than a cleanup task for later.

## Proof-Safe Labels / Selectors

Proof-safe selectors are part of trust because they protect end-to-end validation while the UI evolves.

Stable labels are not just test trivia. They are part of keeping proof connected to real UX outcomes.

## Why Some Failures Are Intentionally Operator-Visible

SynapseCore intentionally surfaces some failures because hiding them would create a false sense of control.

Examples:

- disabled connector state
- replay queue backlog
- runtime degradation
- reconnecting realtime posture

The product philosophy is:

- calm UX
- honest status
- visible recovery path

not false reassurance.

## Current Limitations

The trust model is meaningful, but not complete in every enterprise dimension.

Current limitations include:

- fixture credential debt still exists in dev/test contexts
- hosted cookie/CORS posture still depends on continued live validation
- broader production secrets-management maturity can still improve
- full enterprise identity integrations such as SSO/SAML/OIDC are not the current default platform story

## Current Non-Enterprise-Ready Areas

The platform is not yet pretending that every large-enterprise control need is already solved.

Areas still needing hardening:

- deeper secrets-management posture
- broader identity federation
- more advanced RBAC and delegated administration
- stronger infrastructure recovery confidence under repeated dependency outages
- broader operational proof under larger-scale traffic assumptions

## Bottom Line

The SynapseCore security and trust model is strongest where it is most honest:

- tenant safety
- replay safety
- approval governance
- rate limiting
- runtime truth
- hosted proof discipline

It does not claim perfect enterprise completeness yet, but it does show the right engineering instincts and verification discipline.
