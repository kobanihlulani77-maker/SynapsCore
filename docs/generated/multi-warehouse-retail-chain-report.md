# SynapseCore Company Fit Report

Generated at: `2026-05-06T19:23:05.211Z`

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
- `retail-chains` - Retail Chains
- `multi-warehouse-businesses` - Multi-Warehouse Businesses

## Retail Chains

Best fit when store, warehouse, and central operations need one tenant-safe control center instead of scattered spreadsheets and delayed reports.

### Operational Pain
Store-level demand changes faster than central teams can rebalance stock and respond to fulfillment risk.

### Fragmented Systems Usually Look Like
POS reports, store spreadsheets, email approvals, warehouse inventory views, and separate support inboxes.

### Where Delays And Failures Happen
Store replenishment issues surface after shelves are already empty or store teams escalate manually.

### Why Visibility Breaks
Central ops see reports, not live operational pressure across stores and warehouse lanes.

### Where Approvals Become Bottlenecks
Transfers, substitutions, and urgent procurement waits for informal approval chains.

### Where Integrations Fail
Store or partner order feeds fail without visible ownership.

### Where Inventory Or Order Mismatch Appears
Store-facing availability and warehouse reality drift apart.

### How Replay And Recovery Help
Failed store or partner imports can be recovered deliberately rather than reentered from scratch.

### How Realtime Visibility Changes Operations
Central dashboards show fresh risk posture while inventory and inbound data change.

### How Audit And Event Tracing Help
Useful for tracing who approved reallocation, who replayed inbound data, and when the state changed.

### Why Tenant Isolation Matters
Supports brand or franchise separation without shared-user leakage.

### Metrics That Matter Most
- stockout risk by warehouse
- replay backlog by source system
- active recommendation count
- store-impacting alert count

### Dashboard Views That Matter Most
- central dashboard summary
- catalog and inventory management
- alerts and recommendations

### Alerts And Recommendations That Matter Most
- inventory below threshold
- connector degraded for a retail feed
- delayed fulfillment with store impact

### ROI And Operational Value
- fewer lost sales from invisible stock pressure
- faster store-operations response
- better cross-store transfer governance

### Realistic Scenarios
#### Promoted SKU falls below safe threshold

- Before: Store teams find out after service complaints and manual phone calls.
- After: Central operations sees the risk live, reviews scenario options, and executes an approved response path.

#### Store-import lane fails overnight

- Before: Morning teams manually compare exports and assume missing orders are already handled.
- After: Replay queue and incidents make the gap visible before store teams promise against bad data.

## Multi-Warehouse Businesses

Best fit when visibility and replay must stay warehouse-aware instead of blending every lane together.

### Operational Pain
Multiple warehouses create different risk profiles, but many systems flatten the view until problems are already expensive.

### Fragmented Systems Usually Look Like
Warehouse-specific tools, central reporting, manual transfer sheets, and connector feeds with uneven quality.

### Where Delays And Failures Happen
One location's connector or stock issue spreads confusion across the whole network.

### Why Visibility Breaks
Teams cannot tell which warehouse is actually constrained or which lane still has capacity.

### Where Approvals Become Bottlenecks
Transfers and priority changes need warehouse-aware approvals, not generic signoff.

### Where Integrations Fail
A single warehouse-bound feed can fail and disappear into noise.

### Where Inventory Or Order Mismatch Appears
On-hand versus committed posture differs sharply by warehouse, but teams act on blended totals.

### How Replay And Recovery Help
Replay records remain visible with warehouse context so the right lane can recover them.

### How Realtime Visibility Changes Operations
Warehouse-specific dashboard changes drive faster balancing decisions.

### How Audit And Event Tracing Help
Useful for reviewing why inventory or transfer actions changed by location.

### Why Tenant Isolation Matters
Tenant safety matters when the same platform supports multiple warehouse networks.

### Metrics That Matter Most
- risk by warehouse
- replay backlog by warehouse
- transfer-related approval count
- connector failures by source

### Dashboard Views That Matter Most
- warehouse inventory view
- replay queue with warehouse context
- alerts and recommendations

### Alerts And Recommendations That Matter Most
- warehouse-specific low stock
- connector disabled for one lane
- fulfillment delay tied to one location

### ROI And Operational Value
- better rebalancing decisions
- less confusion caused by blended reporting
- faster warehouse-specific recovery

### Realistic Scenarios
#### WH-NORTH import fails while WH-SOUTH stays healthy

- Before: Central teams overreact across both sites.
- After: Replay queue and incidents isolate the failing lane and keep the healthy one moving.

#### Transfer approval affects a live shortage

- Before: Decision history is scattered across messages.
- After: Scenario approval and audit keep the warehouse-specific rationale visible.