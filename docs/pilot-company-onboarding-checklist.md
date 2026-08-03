# Pilot Company Onboarding Checklist

This checklist helps a company start a SynapseCore pilot safely. It is designed for a bounded operational trial, not a broad cutover.

## Before Onboarding

Confirm these items before inviting operators:

- company sponsor
- operations owner
- technical contact
- pilot workspace name
- pilot workspace code
- approved operator list
- target warehouse or site
- catalog scope
- inventory scope
- integration scope
- success criteria
- rollback owner
- support and escalation contact
- existing system of record that remains authoritative during the pilot

## Day 1 - Workspace Foundation

Day 1 goals:

- confirm the workspace name and code
- confirm tenant admin sign-in
- confirm planner/operator sign-in
- confirm integration admin sign-in if connector work is in scope
- review the authenticated shell
- review dashboard freshness
- review runtime trust surfaces
- confirm users/profile/company settings

Day 1 output:

- the pilot team understands the workspace boundary
- the first operators can sign in
- the team knows where runtime trust and degraded state appear

## Day 1 - Data Baseline

Baseline setup:

- confirm first catalog slice
- confirm first product identifiers
- confirm warehouse/site code
- confirm inventory quantities
- confirm reorder thresholds
- confirm expected low-stock or risk conditions

Baseline checks:

- catalog page shows the pilot products
- inventory page shows the pilot warehouse/site posture
- dashboard reflects operational pressure when inventory or orders create it

## Days 2-3 - Operational Walkthrough

Walk through:

- dashboard
- orders
- inventory
- alerts
- recommendations
- integrations
- replay queue
- scenarios
- scenario history
- approvals
- runtime

The goal is to make the operator path familiar before real operational observation begins.

## Days 2-3 - Controlled Recovery Exercise

Perform one controlled recovery exercise:

- create or simulate a supported failed inbound condition
- confirm the failure is visible
- confirm the replay queue shows the record
- repair or enable the affected lane
- replay into live flow
- confirm audit/history or user-visible outcome

Do not perform manual database cleanup as part of the exercise.

## Days 2-3 - Role And Approval Exercise

Validate:

- tenant admin can perform admin-scoped actions
- planner/operator has expected non-admin boundaries
- integration admin can review connector/replay surfaces
- approval-required scenario can be reviewed
- approved scenario can proceed
- rejected or blocked scenario does not silently execute

## Week 1 - Operational Observation

Observe:

- sign-in/session reliability
- dashboard freshness
- realtime state
- order visibility
- inventory risk surfacing
- alert/recommendation quality
- connector state clarity
- replay queue usefulness
- scenario approval clarity
- runtime trust and incident visibility

## Week 1 - Review Cadence

Review daily:

- what operators trusted
- what operators ignored
- what data felt useful
- what data felt confusing
- what failures were visible
- what failures still required manual reconciliation
- whether the pilot lane should continue, pause, narrow, or expand

## Week 1 - Continuation Decision

Continue only if:

- operators can sign in reliably
- runtime trust is understandable
- pilot data maps to real operations
- failed inbound work is visible and recoverable
- role boundaries are clear enough for pilot use
- support owner can classify incidents
- no tenant isolation or security concern is unresolved

Pause or narrow if:

- readiness/auth/ws health is unstable
- replay behavior is inconsistent
- operators cannot explain what the platform is showing
- pilot scope is expanding faster than trust
- existing systems of record cannot safely absorb rollback
