# Pilot Release Candidate

This document defines the controlled pilot release candidate for SynapseCore after the replacement Render PostgreSQL database was bootstrapped and revalidated through hosted proof.

This is a release-candidate packaging document. It does not expand product scope, promise enterprise scale, or replace the proof evidence.

## 1. Release Identity

Proposed release candidate:

- `v0.9.0-pilot-rc1`

Release discipline:

- do not create the tag until repository status, live connection status, frontend verification, docs links, and pilot RC checks are green
- do not include local proof state, env files, Playwright reports, screenshots, or archives in the tag
- do not treat this RC as unrestricted production rollout

## 2. Current Commit And Evidence Baseline

Current proof evidence baseline:

- commit: `44552db55c987f06aed82e8c326a8bc786a23801`
- evidence note: [release-evidence-2026-08-03.md](release-evidence-2026-08-03.md)
- hosted proof result: `6 passed (4.1m)`
- proof tenant: `HOSTED-PROOF-2`

The final tag should point to the committed RC-packaging state after this document pack is merged and verification passes.

## 3. Deployment URLs

Current live deployment:

- frontend: `https://synapscore-frontend-3.onrender.com`
- backend: `https://synapscore-3.onrender.com`

Live trust gate:

- `FRONTEND_UP=True`
- `BACKEND_UP=True`
- `DB_READY=True`
- `AUTH_READY=True`
- `WS_READY=True`
- `PROOF_ALLOWED=True`

## 4. Proof Result

The replacement database proof passed:

- `6 passed (4.1m)`

The proof covered:

1. auth flow and authenticated page system
2. tenant-scoped product catalog onboarding
3. realtime dashboard updates without refresh
4. replay recovery, scenario approval, execution, and role gating
5. alerts, recommendations, orders, inventory, integrations, users, profile, and settings connected to the live backend
6. frontend-visible auth rate limiting without stuck loading

## 5. Supported Current Scope

The pilot release candidate supports a controlled operations command-center pilot with:

- workspace and tenant isolation
- operator authentication
- Redis-backed session posture where configured by the live backend
- command-center dashboard
- product/catalog onboarding
- warehouse-aware inventory visibility and updates
- recent order visibility
- alerts
- recommendations
- integration connector visibility
- failed inbound replay/recovery
- scenario approval and execution
- role gating for tenant/admin/planner/integration pilot roles
- users, profile, and company settings surfaces
- realtime dashboard updates
- frontend-visible auth rate limiting
- runtime trust surfaces

## 6. Roles Supported

The pilot supports these current role lanes:

- tenant admin for workspace and access administration
- operations planner/operator for non-admin operational review
- integration admin/operator for connector and replay work
- review/escalation owner for scenario approval and operational control

The pilot does not claim mature enterprise RBAC policy depth beyond these supported lanes.

## 7. Pages And Flows Included

Included pages and flows:

- public homepage and sign-in
- authenticated workspace shell
- dashboard and realtime trust state
- catalog
- inventory
- orders
- alerts
- recommendations
- integrations
- replay queue
- scenarios
- scenario history
- approvals
- users
- profile
- company settings
- runtime/observability

Included operational flows:

- workspace sign-in/session validation
- catalog/product creation
- inventory update and low-stock pressure
- recent order visibility
- failed inbound recovery and replay into live flow
- scenario approval and execution into order flow
- role-gated access checks
- runtime readiness and websocket checks

## 8. Known Limitations

This pilot release candidate does not yet include:

- broad connector marketplace
- full ERP replacement
- global high availability guarantees
- multi-region deployment
- enterprise SSO/SAML/OIDC
- advanced RBAC beyond current supported roles
- large-enterprise scale guarantees
- autonomous decision execution
- unrestricted production rollout
- complete HA database and backup/restore automation maturity
- advanced metrics/tracing stack
- horizontally scaled websocket topology beyond the current deployment posture

## 9. Infrastructure Dependencies

The current live RC depends on:

- Render frontend service
- Render backend service
- PostgreSQL
- Redis/session/realtime posture where configured by the backend
- backend readiness/liveness endpoints
- `/api/auth/session`
- `/ws/info`
- hosted proof state stored locally and ignored by Git

Pilot continuation depends on these trust checks remaining healthy.

## 10. Pilot Safety Boundaries

Safety boundaries:

- use a bounded pilot workspace
- use an approved operator list
- use a controlled catalog/inventory slice
- start with one connector lane
- do not manually edit production database rows
- do not run hosted proof when readiness/auth/ws checks are unhealthy
- preserve proof artifacts locally when diagnosing failures
- keep existing systems of record available during the pilot

## 11. Rollback Posture

Rollback means returning operational reliance to the existing company workflow while preserving SynapseCore evidence for diagnosis.

Rollback actions:

- stop new pilot activity
- disable or pause affected connector lanes
- preserve logs, screenshots, Playwright reports, and incident context
- classify the failure before changing the system
- fix the real seam
- rerun readiness checks and proof before resuming

## 12. Support And Escalation Process

Pilot support should follow this sequence:

1. classify the issue as frontend, backend, DB, Redis/session, realtime, integration, replay, proof assumption, or operator workflow
2. preserve evidence before changing state
3. check live readiness and auth/session
4. check `/ws/info` and runtime trust
5. inspect affected operator page and backend response
6. pause pilot activity if tenant isolation, replay correctness, or data trust is in question
7. fix and re-prove before resuming

## 13. Week-One Verification Checklist

Week-one checks:

- company sponsor confirms pilot scope
- operations owner confirms workspace goal
- technical contact confirms live endpoints and support path
- tenant admin signs in
- planner/operator signs in
- integration admin signs in
- dashboard loads and shows current runtime posture
- realtime status is visible
- catalog baseline is present
- inventory baseline is present
- recent order visibility is confirmed
- alert and recommendation behavior is reviewed
- connector state is reviewed
- controlled replay exercise is completed
- scenario approval/execution exercise is completed
- users/profile/company settings are reviewed
- runtime page is reviewed
- proof evidence and known limitations are reviewed

## 14. Conditions That Block Pilot Continuation

Pilot continuation is blocked by:

- readiness failure
- auth/session failure
- persistent websocket failure
- tenant isolation concern
- replay inconsistency
- corrupted or unexplained operational state
- severe data mismatch
- unresolved security concern
- inability to classify a live operational failure
- pressure to broaden scope before the bounded pilot lane is stable

## RC Verdict

`v0.9.0-pilot-rc1` is a valid release-candidate proposal for a controlled company pilot, not a declaration of unrestricted enterprise production readiness.
