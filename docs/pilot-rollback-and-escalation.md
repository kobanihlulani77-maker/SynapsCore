# Pilot Rollback And Escalation

This document explains how to pause, rollback, and escalate during a SynapseCore pilot without hiding evidence or pretending degraded state is healthy.

## Pause Conditions

Pause pilot activity when any of these occur:

- readiness failure
- auth/session failure
- persistent websocket failure
- tenant isolation concern
- replay inconsistency
- corrupted or unexplained operational state
- severe data mismatch
- unresolved security concern
- inability to classify a failure
- operator confusion that creates operational risk

## Immediate Response

When a pause condition occurs:

1. stop new pilot activity in the affected lane
2. preserve screenshots, browser reports, request IDs, logs, and operator notes
3. record the time and affected workspace
4. identify whether the issue affects frontend, backend, DB, Redis/session, realtime, integration, replay, proof assumption, or operator workflow
5. decide whether existing systems of record should fully resume the workflow

Do not clear state just to make the UI look clean.

## Rollback Actions

Rollback actions may include:

- stop new pilot activity
- disable affected integration lane
- return operators to the existing system of record
- preserve replay queue and incident evidence
- stop scenario execution
- pause approval exercise
- classify issue before remediation
- fix and re-prove before resuming

Rollback is not failure theatre. It is how the pilot stays safe.

## Escalation Levels

### Level 1 - Operator Confusion

Examples:

- unclear alert meaning
- unclear recommendation
- operator cannot find replay state
- user does not understand role boundary

Action:

- support walkthrough
- improve pilot notes
- record feedback for product refinement

### Level 2 - Operational Mismatch

Examples:

- inventory/order mismatch
- connector state confusion
- replay outcome unclear
- stale dashboard state

Action:

- pause affected flow
- preserve evidence
- compare with source of record
- classify whether this is data quality, integration, UI, or backend issue

### Level 3 - Trust Failure

Examples:

- readiness down
- auth/session unavailable
- persistent websocket failure
- runtime trust contradicts product behavior
- replay inconsistency

Action:

- stop affected pilot lane
- run live connection checks
- inspect runtime and logs
- fix and re-prove before resuming

### Level 4 - Security Or Isolation Concern

Examples:

- tenant isolation concern
- unauthorized access concern
- secret leakage concern
- unexplained cross-workspace data

Action:

- stop pilot activity immediately
- preserve evidence
- do not broaden testing
- classify and fix before any continuation
- rerun relevant proof and security checks

## Evidence Preservation

Preserve:

- Playwright HTML report
- screenshots
- traces
- request IDs
- backend log window
- operator notes
- timeline of actions

Do not commit local proof passwords or `.env.local` files.

## Resume Criteria

Resume only when:

- failure is classified
- remediation is understood
- affected support owner agrees
- readiness/auth/ws trust is healthy
- affected flow has been revalidated
- rollback owner approves continuation

## Hosted Proof After Incident

Rerun hosted proof when:

- runtime behavior changed
- proof selectors changed
- backend/API contract changed
- replay/recovery behavior changed
- incident affected trust in a proof-covered flow

Do not use hosted proof as a wake-up command for unhealthy infrastructure.
