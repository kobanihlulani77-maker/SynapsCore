# Pilot Adoption Roadmap

This roadmap explains how a company can adopt SynapseCore safely and realistically for an operational pilot.

The goal is not to replace everything at once. The goal is to prove operational value in a controlled lane.

## Adoption Principles

A safe pilot should:

- start with one bounded operational scope
- use real company workspace structure
- include real operators or realistic pilot owners
- validate replay and recovery intentionally
- validate approvals and runtime trust deliberately
- expand only after stability is understood

## Before Week 1

Preparation should confirm:

- pilot sponsor and technical owner are identified
- first workspace is defined
- first operator roles are known
- first catalog and inventory slice are chosen
- first connector lane is chosen
- runtime and proof expectations are understood

The company should also agree on what success looks like before rollout starts.

## Week 1: Foundation And Workspace Setup

Focus:

- create the company workspace
- establish workspace code conventions
- provision the first admin and first operators
- confirm sign-in flow and workspace identity
- align on the first warehouse or site context

Expected outcomes:

- the team understands the workspace model
- the first operators can sign in confidently
- the workspace feels like a real operating environment, not a demo shell

Warning signs:

- confusion about workspace ownership
- unclear role boundaries
- pressure to broaden scope before basic trust is established

## Week 2: Catalog, Inventory, And First Operational Baseline

Focus:

- add the first meaningful products
- define warehouse-aware inventory context
- establish an initial inventory baseline
- confirm the dashboard and core operational pages reflect real pilot data

Expected outcomes:

- operators can see catalog and inventory clearly
- the command center reflects a real slice of operations
- the business can identify low-stock or inventory-risk posture from the product

Warning signs:

- pilot data is too synthetic to create real operational behavior
- product definitions do not map cleanly to the pilot lane
- inventory baseline is so incomplete that the product cannot tell a useful story

## Week 3: First Connector And Recovery Validation

Focus:

- establish one real connector lane
- confirm connector ownership
- validate failed inbound visibility
- validate replay and recovery intentionally

Expected outcomes:

- the business can see connector posture
- failed inbound work becomes visible instead of disappearing
- operators can understand and explain replay outcomes

Warning signs:

- connector failures require undocumented manual cleanup
- the business cannot tell whether a failed inbound record is recoverable
- runtime truth is unclear during replay activity

## Stabilization Phase

After the first three weeks, the pilot should enter a stabilization phase rather than broadening immediately.

Focus:

- refine workflows
- validate runtime trust under normal and degraded conditions
- confirm scenario approval and execution behavior
- improve role confidence
- reduce confusion in day-to-day operator use

Success signals:

- operators understand what healthy and degraded look like
- scenario approval feels controlled rather than improvised
- replay and recovery are trusted
- the company has fewer manual reconciliation blind spots

Warning signs:

- the team still treats failures as hidden or external to operations
- operators do not trust runtime posture
- too much effort is spent explaining what the platform is instead of using it

## Expansion Phase

Only after stabilization should the company consider expansion.

Possible expansion areas:

- additional connectors
- broader catalog and inventory scope
- more operators and roles
- more warehouses or sites
- more scenario governance
- deeper runtime and incident workflows

Expansion should follow evidence, not enthusiasm alone.

## Success Signals

Strong pilot success signals include:

- one operational lane is clearly better coordinated than before
- failed inbound work is understood and recoverable
- approvals feel safer and more traceable
- runtime trust is meaningful to both technical and operational users
- the workspace model makes sense to the company

## Warning Signs

Signals that the pilot should pause or narrow include:

- infrastructure instability is being mistaken for product maturity
- the pilot scope is expanding faster than trust is forming
- operators still rely on manual reconciliation as the primary recovery path
- the business cannot explain who owns connector and replay actions

## Rollback Posture

Every pilot should define rollback expectations before broader operational reliance.

Rollback posture should include:

- how to pause connector activity
- how to stop risky rollout expansion
- how to isolate pilot issues from company-wide process issues
- how to return to the previous workflow if needed

Rollback readiness is a sign of operational discipline, not a sign of product weakness.

## Bottom Line

A good SynapseCore pilot should prove one thing clearly:

the company now has a more trustworthy operational coordination layer than it had before.
