# SynapseCore Company Fit Report

Generated at: `2026-05-06T19:22:49.711Z`

## Platform Truth

SynapseCore should be positioned as an operations control platform for the lanes it actually implements today.

### Current Supported Scope
- tenant-explicit auth, session control, and operator lane scoping
- catalog onboarding and warehouse-aware inventory control
- order ingestion through webhook, CSV, and scheduled pull
- alerts, recommendations, scenario approvals, and execution governance
- integration replay and recovery with manual-versus-automated eligibility rules
- tenant-scoped realtime visibility backed by Redis pub/sub on Render
- runtime, incident, audit, and event-tracing trust surfaces
- rate-limited auth and integration mutation paths

### Fit Signals
- multiple operational systems feeding the same fulfillment or control process
- warehouse, fleet, fulfillment, field, or approval teams that need a shared operating picture
- teams that lose time reconciling spreadsheets, inboxes, exports, and delayed integration failures
- companies that need incident recovery and audit traceability without rebuilding their whole stack at once

### Not Claimed
- broad out-of-the-box ERP or carrier connector catalogs
- full transport management suite behavior outside the implemented ingestion and visibility lanes
- industry-specific billing, HR, payroll, or finance workflows

### Proof Highlights
- full hosted proof passed end to end on Render twice in a row
- disabled connector CSV failures return structured CONNECTOR_DISABLED rows and create replay records
- manual replay records are not stolen by automated replay while the connector remains disabled
- dashboard, replay, scenario approval, role gating, and auth rate limiting are all browser-proven live

## Included Company Types
- `logistics-companies` - Logistics Companies
- `distributors` - Distributors

## Logistics Companies

Best fit when dispatch, warehouse, fulfillment, and exception handling are split across tools and teams.

### Operational Pain
Orders, shipment promises, dispatch changes, and exceptions move faster than operators can reconcile them manually.

### Fragmented Systems Usually Look Like
TMS, spreadsheet lane boards, carrier portals, email escalations, and a separate stock or warehouse tool.

### Where Delays And Failures Happen
Dispatch changes arrive late, exception ownership is unclear, and failed imports are noticed after downstream teams have already reacted.

### Why Visibility Breaks
Control teams see status snapshots after the fact instead of live pressure building by warehouse, connector, or order lane.

### Where Approvals Become Bottlenecks
Risky reallocations and promise changes wait for managers who have incomplete context or no shared review lane.

### Where Integrations Fail
Carrier or partner feeds fail silently, CSV handoffs break, and support teams discover the gap only after customers escalate.

### Where Inventory Or Order Mismatch Appears
What sales promised, what the warehouse can actually ship, and what the dispatch board shows stop matching.

### How Replay And Recovery Help
Replay records let operations recover failed inbound orders after connector issues are fixed instead of rekeying them from scratch.

### How Realtime Visibility Changes Operations
Live connector, alert, and inventory pressure changes help operations re-route before SLA breaches spread.

### How Audit And Event Tracing Help
Runtime, incidents, audit, and business events show which lane failed, who replayed it, and what changed after recovery.

### Why Tenant Isolation Matters
Useful when one platform team supports multiple logistics clients or brands without mixing their operators or inbound records.

### Metrics That Matter Most
- replay queue depth
- connector failure rate
- fulfillment backlog count
- delayed fulfillment count
- dispatch queue backlog
- orders ingested per hour

### Dashboard Views That Matter Most
- live operations dashboard
- connector diagnostics and replay queue
- runtime and incident inbox
- warehouse inventory posture

### Alerts And Recommendations That Matter Most
- connector disabled or degraded
- backlog growth by lane
- inventory risk on committed orders
- delayed fulfillment escalation

### ROI And Operational Value
- less manual exception chasing
- faster recovery after ingestion failure
- fewer avoidable SLA misses
- clearer operational ownership during incidents

### Realistic Scenarios
#### Carrier CSV lane pauses before shift change

- Before: Teams discover the missing loads only after the warehouse asks why work dried up.
- After: Connector health, replay queue, and dashboard incidents surface the break quickly, then manual replay restores the lane after the connector is re-enabled.

#### Urgent allocation change needs approval

- Before: Managers review partial screenshots and messages with no shared audit trail.
- After: Scenario approval and execution keep the review, decision, and result in one governed path.

## Distributors

Best fit when multi-customer stock movement and inbound partner feeds create constant reconciliation work.

### Operational Pain
Distribution teams need clean views across customer demand, warehouse availability, and inbound supplier or sales feeds.

### Fragmented Systems Usually Look Like
ERP extracts, partner CSVs, warehouse tools, and coordinator spreadsheets.

### Where Delays And Failures Happen
Inbound orders or transfers fail quietly, then downstream teams overreact or duplicate work.

### Why Visibility Breaks
Distribution managers cannot see which warehouse or connector issue is actually driving backlog.

### Where Approvals Become Bottlenecks
High-impact moves such as reallocations and priority changes depend on ad hoc approval threads.

### Where Integrations Fail
Partner source systems send inconsistent files that need clear failure codes and recovery paths.

### Where Inventory Or Order Mismatch Appears
Warehouse-on-hand, committed stock, and replenishment plans drift apart.

### How Replay And Recovery Help
Operators can keep failed partner orders in view until the feed or connector issue is corrected.

### How Realtime Visibility Changes Operations
Live operational summaries help distribution teams rebalance work faster.

### How Audit And Event Tracing Help
Needed when customer operations asks why a lane fell behind and who changed the recovery plan.

### Why Tenant Isolation Matters
Useful when one shared-service team manages multiple customer workspaces or business units.

### Metrics That Matter Most
- warehouse-specific backlog
- connector health by source system
- inventory risk level
- replay queue depth

### Dashboard Views That Matter Most
- distribution dashboard
- connector health rows
- inventory posture by warehouse

### Alerts And Recommendations That Matter Most
- degraded connector
- rising replay queue
- stockout risk on priority SKUs

### ROI And Operational Value
- less time spent reconciling partner feeds
- better warehouse balancing decisions
- fewer missed commitments

### Realistic Scenarios
#### Partner import fails on disabled connector

- Before: Teams retype the order or wait for support to investigate.
- After: The import comes back with CONNECTOR_DISABLED, creates a replay record, and stays ready for manual recovery.

#### Distribution center backlog spikes

- Before: Managers piece together exports from different systems.
- After: Runtime, alerts, and dashboard surfaces show which lane is driving the pressure.