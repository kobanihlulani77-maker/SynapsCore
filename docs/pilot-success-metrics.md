# Pilot Success Metrics

This document defines practical metrics for judging a SynapseCore pilot.

Numbers should be agreed with the pilot company before rollout. Any targets below are proposed pilot targets, not proven universal benchmarks.

## Technical Metrics

Track:

- successful sign-ins
- failed sign-ins
- readiness uptime during the agreed pilot window
- liveness availability
- websocket connection stability
- auth/session endpoint availability
- replay success rate
- API failure rate for pilot flows
- hosted proof rerun status
- runtime incident count
- unresolved runtime warnings

Proposed pilot target examples:

- readiness should remain healthy during planned pilot observation windows
- websocket state should recover or clearly degrade without hiding failure
- replay failures should be classified before any retry
- hosted proof should remain rerunnable when live readiness is healthy

## Operational Metrics

Track:

- failed events detected
- failed events recovered
- time to detect failed inbound work
- time to recover failed inbound work
- approval turnaround time
- inventory/order mismatches surfaced
- manual reconciliation steps reduced
- operator adoption
- operator confidence
- operator-reported confusion points

Proposed pilot target examples:

- operators can identify failed inbound work without searching logs or inboxes
- operators can explain alert/recommendation meaning without engineering translation
- replay and approval outcomes are understandable after the fact
- manual reconciliation should move from primary workflow to exception workflow for the pilot lane

## Product Trust Metrics

Track:

- number of times operators trusted SynapseCore during a real decision
- number of times operators bypassed SynapseCore
- number of unexplained state mismatches
- number of escalations caused by unclear UI wording
- number of incidents where runtime trust helped classify the problem

## Replay Metrics

Track:

- failed inbound records created
- failed inbound records shown in UI
- records replayed successfully
- records blocked for valid reason
- records requiring manual review
- records with ambiguous outcome

Replay is successful when the operator can explain:

- what failed
- why it failed
- what was repaired
- when replay happened
- what live state changed afterward

## Approval Metrics

Track:

- scenarios created
- scenarios requiring approval
- scenarios approved
- scenarios rejected
- scenarios executed
- approval turnaround
- unclear or disputed approvals

Approval success means execution control is visible and governed, not merely possible.

## Adoption Metrics

Track:

- active operators per pilot day
- repeat operator sessions
- pages used without assistance
- support questions by category
- daily operator confidence notes

Operator confidence should be measured directly. A technically green pilot can still fail if operators do not understand the system.

## Continuation Signal

The pilot is moving in the right direction when:

- live readiness remains stable during pilot windows
- operators use the platform for real coordination
- replay/recovery is trusted
- alerts and recommendations reduce confusion
- approvals feel controlled
- runtime trust helps classify problems
- the company can name the next safe expansion lane

The pilot should pause or narrow when:

- trust issues are unexplained
- replay outcomes are ambiguous
- tenant isolation is questioned
- operators cannot map the UI to real operations
- infrastructure instability dominates pilot learning
