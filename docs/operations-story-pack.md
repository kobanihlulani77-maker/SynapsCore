# Operations Story Pack

This pack translates SynapseCore into operational stories that executives, operators, and pilot stakeholders can understand quickly.

Each story shows the same pattern:

- what the company experiences before SynapseCore
- what the platform changes
- what the operator can do with replay, realtime, approvals, and audit visibility

## 1. Delayed Warehouse Feed

### Before SynapseCore

- receiving or inbound order data stops landing
- warehouse teams continue working from stale priorities
- supervisors do not know whether the issue is floor execution or upstream data loss
- support escalations start before the operating team has a shared explanation

### What SynapseCore Changes

- connector diagnostics make the lane visible
- incidents show the degraded or disabled connector posture
- replay queue keeps failed inbound work visible instead of forcing manual re-entry
- warehouse-aware surfaces help operators see which lane is actually affected

### Operational Result

The company moves from "Why is nothing arriving?" to "The warehouse feed failed, the replay queue is building, the connector owner is known, and recovery is controlled."

## 2. Connector Outage

### Before SynapseCore

- a connector fails
- order intake becomes partial or silent
- teams argue over whether the business problem is real demand, system lag, or user error

### What SynapseCore Changes

- runtime and incident surfaces expose the connector problem as a first-class operational issue
- failed CSV or inbound work stays visible through replay
- operators can repair the connector and recover intentionally instead of retyping orders

### Operational Result

The outage becomes a governed recovery flow instead of a hidden data-loss event.

## 3. Replay Recovery

### Before SynapseCore

- failed inbound records are re-entered manually
- teams lose the connection between the original failure and the recovered work
- operators cannot prove what was replayed or who decided to recover it

### What SynapseCore Changes

- disabled connector CSV failures return `CONNECTOR_DISABLED`
- replay records are created immediately
- manual recovery remains available after the connector is repaired
- audit and event tracing keep the narrative intact

### Operational Result

The recovery path becomes deterministic and visible.

## 4. Inventory Mismatch

### Before SynapseCore

- one team trusts the warehouse number
- another trusts a spreadsheet
- another trusts a storefront or export
- the business reacts late because there is no shared live view

### What SynapseCore Changes

- inventory posture is warehouse-aware
- low-stock and risk pressure are surfaced centrally
- realtime dashboard changes reduce the lag between floor change and control-center response

### Operational Result

The company spends less time debating what is true and more time acting on it.

## 5. Approval Escalation

### Before SynapseCore

- risky changes move through chat or email
- reviewers lack a shared incident or inventory context
- nobody can reconstruct why the decision was made later

### What SynapseCore Changes

- scenario review, approval, and execution are role-gated
- the decision path is traceable
- approvals can be tied to the live operational signals that triggered them

### Operational Result

Approval becomes operational governance instead of informal signoff.

## 6. Low-Stock Risk

### Before SynapseCore

- shortages appear in one tool long before decision-makers see them in another
- replenishment or transfer actions happen late
- operations teams act from stale views

### What SynapseCore Changes

- alerts and recommendations surface live stock pressure
- runtime and dashboard views give a common operating picture
- approval paths can govern riskier interventions

### Operational Result

Teams catch risk earlier and act with more context.

## 7. Failed External Order Ingestion

### Before SynapseCore

- an inbound order never lands
- downstream teams only notice when fulfillment or customer-facing work looks wrong
- someone manually recreates the work and traceability is lost

### What SynapseCore Changes

- the import failure is classified explicitly
- the replay queue keeps the failed inbound work visible
- manual or automated recovery follows the actual eligibility rules
- audit and runtime surfaces preserve the incident story

### Operational Result

The company can explain not only that the order failed, but how it was recovered and what changed afterward.

## Executive Summary

The common pattern across all of these stories is simple:

Before SynapseCore, the company has systems but weak operational control over the gaps between them.

With SynapseCore, the company gains one control layer that makes:

- failures visible
- recovery deliberate
- approvals governed
- realtime state actionable
- audit and runtime truth usable
