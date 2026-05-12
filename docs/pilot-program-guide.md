# Pilot Program Guide

This guide explains what a safe SynapseCore pilot should look like, what it requires, and how to judge success without pretending the product is already beyond its current supported scope.

## Purpose Of The Pilot

A pilot should prove that SynapseCore changes how a company manages:

- operational visibility
- failed inbound work
- recovery and replay
- scenario approval and execution
- runtime trust

The pilot should not try to replace every existing system at once.

## What A Pilot Deployment Looks Like

A serious pilot typically includes:

- one real tenant workspace
- a real company workspace code
- a limited set of real or pilot operators
- a selected catalog and inventory baseline
- a narrow but meaningful connector scope
- one or more replay/recovery scenarios
- one or more scenario approval/execution exercises

The goal is to prove control, recovery, and trust in a real operating lane.

## What Is Required Before A Pilot Starts

Minimum prerequisites:

- deployment path understood
- backend and DB available
- auth/session working
- runtime and readiness surfaces working
- known pilot users and roles
- workspace and warehouse definitions
- starter catalog and inventory baseline
- connector ownership defined

## Safe Rollout Stages

### Stage 1 — Workspace foundation

Set up:

- company workspace
- workspace code
- initial admin
- initial operator roles

### Stage 2 — Catalog and inventory baseline

Load:

- first product set
- first warehouse or site definitions
- initial inventory posture

### Stage 3 — Connector visibility

Configure:

- webhook, CSV, or scheduled pull lane
- source system identity
- ownership
- support expectations

### Stage 4 — Replay and recovery validation

Intentionally validate:

- failed inbound visibility
- replay queue presence
- operator recovery path
- audit and user-visible outcome

### Stage 5 — Scenario and approval validation

Validate:

- scenario creation
- scenario save
- approval path
- execution into live order flow

### Stage 6 — Runtime trust validation

Validate:

- runtime page
- incident visibility
- readiness/liveness understanding
- operational response to degraded state

## First Workspace Setup

The first workspace should be prepared as a real operational environment, even if the scope is narrow.

That means:

- real workspace identity
- real user roles
- real warehouse codes
- real proof or pilot SKUs
- no hidden dependence on undocumented manual DB edits

## First Operators

Minimum roles for a meaningful pilot:

- tenant admin
- planner or reviewer
- integration admin or integration operator

These roles should map to real people or realistic pilot personas with clear ownership.

## First Inventory / Catalog

Pilot data should be meaningful enough to create real operational behavior:

- products that matter to the chosen pilot lane
- stock levels that can create real low-stock or replay-relevant behavior
- thresholds that are intentionally chosen

The goal is not large volume. The goal is realistic operational pressure.

## First Integrations

The pilot should stay inside the current supported connector scope:

- webhook order ingestion
- CSV order import
- scheduled pull order ingestion

The point is not breadth. The point is proving:

- visibility
- failure classification
- recovery

## Replay / Recovery Testing

A good pilot must include a replay test because replay is one of the strongest reasons the product exists.

A replay test should verify:

- failed inbound work stays visible
- the failure reason is understandable
- connector or data repair can happen first
- replay intentionally moves the work into the live order flow
- the business can trace what happened

## Approval / Scenario Testing

A good pilot should include at least one governed decision flow:

- create scenario
- save scenario
- review or approve
- execute into live operational state

This proves that SynapseCore is not only a monitoring product. It is a control product.

## Runtime Observation

The pilot should actively observe:

- health and readiness behavior
- auth/session behavior
- runtime trust page behavior
- incident surface usefulness
- what degraded state looks like to users

This matters because operational trust is part of the product promise.

## Success Metrics

Reasonable pilot success indicators include:

- operators understand the workspace model
- core users can sign in and navigate confidently
- replay and recovery are understandable
- scenario approval and execution are trusted
- runtime trust surfaces are meaningful
- the team spends less time guessing what failed
- the team has one better operating story than before

## Rollback Expectations

Every pilot should define rollback expectations before go-live.

That includes:

- how to pause live use
- how to stop connector activity if needed
- how to back out risky rollout steps
- how to separate platform issues from company data issues

Rollback readiness is part of pilot safety, not a sign of weakness.

## Operational Safety Rules

Pilot safety rules should include:

- do not broaden scope too fast
- do not hide degraded state
- do not call runtime-unsafe behavior acceptable
- do not confuse demo mode with real validation
- do not skip replay and scenario verification if those are part of the business value story

## Honest Pilot Positioning

SynapseCore pilots should be positioned honestly:

- this is a serious operational platform with a real supported scope
- it is not yet pretending to be every system for every company
- the pilot proves control, recovery, trust, and decision flow in real operations

## Bottom Line

A good pilot should leave the company with one clear conclusion:

SynapseCore gave them a better live operational coordination layer than they had before.
