# Change Management

This document defines the engineering lifecycle for future SynapseCore changes.

The purpose is to preserve proof truth, operational safety, and maintainability.

## Change Lifecycle

Every meaningful change should follow this sequence:

1. identify issue or objective
2. classify affected area
3. define scope boundary
4. implement smallest safe change
5. run targeted verification
6. run required quality gates
7. capture evidence
8. deploy or release only when gates pass
9. monitor after change
10. update docs if truth changed

## Issue Identification

Classify issues as:

- frontend behavior
- backend/API contract
- database/migration
- Redis/session
- realtime/websocket
- replay/recovery
- scenario/approval
- integration connector
- deployment/env
- proof tooling
- documentation/status

Do not start with implementation before classification.

## Scope Control

Before coding, decide:

- what is in scope
- what is out of scope
- what proof or test covers the change
- whether runtime behavior changes
- whether user-visible behavior changes
- whether docs must change

If the change is an engineering hardening task, do not add product features.

## Implementation

Implementation rules:

- keep controllers thin
- keep backend business logic in services
- preserve tenant context enforcement
- avoid manual DB edits
- preserve proof-critical labels and selectors
- keep scripts informational unless explicitly mutating
- do not commit local env files or proof state

## Review

Review should ask:

- does this change alter a contract?
- does it preserve tenant boundaries?
- does it preserve replay determinism?
- does it preserve runtime truth?
- does it increase or decrease proof reliability?
- does it introduce hidden dependency on local state?

## Verification

Baseline verification:

- docs-only: `scripts\docs-link-check.ps1`
- frontend: `cd frontend; npm.cmd run verify`
- backend: `cd backend; cmd /c mvnw.cmd test`
- live deployment: `scripts\check-live-connections.ps1`
- pilot RC: `scripts\pilot-rc-check.ps1`

Hosted proof:

- run only when live trust gates are healthy
- run when proof-covered behavior changes
- run after replacement DB or meaningful deployment change

## Deployment

Deployment should occur only after:

- build passes
- verification passes
- environment variables are known
- readiness expectations are understood
- rollback path exists
- evidence destination is known

Do not deploy to hide local uncertainty.

## Rollback

Rollback actions:

- stop new operational use
- preserve evidence
- restore previous deployment if needed
- disable affected connector lane if applicable
- return operators to system of record
- classify and fix before resuming

Rollback is part of disciplined engineering.

## Evidence Capture

Capture:

- commit hash
- commands run
- pass/fail output
- proof result if applicable
- affected docs
- known residual risk

Never capture:

- passwords
- bootstrap tokens
- platform admin tokens
- `.env.local`
- raw proof state

## Documentation Updates

Update docs when:

- proof status changes
- deployment status changes
- supported scope changes
- known limitation changes
- runbook steps change
- operational classification changes

Docs must remain truthful, even when truth is inconvenient.
