# SynapseCore Company Fit Report

Generated at: `2026-05-06T19:22:49.715Z`

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
- `ecommerce-fulfillment` - Ecommerce Fulfillment

## Ecommerce Fulfillment

Best fit when order velocity and SLA pressure make integration failures and stale dashboard data expensive.

### Operational Pain
High-volume fulfillment teams cannot afford silent import failures or lag between stock posture and order commitments.

### Fragmented Systems Usually Look Like
Commerce platform exports, marketplace feeds, shipping dashboards, warehouse notes, and support escalations.

### Where Delays And Failures Happen
Failed order ingestion or delayed updates ripple into customer-facing promise misses quickly.

### Why Visibility Breaks
Operators cannot distinguish a real demand spike from a broken feed fast enough.

### Where Approvals Become Bottlenecks
Rush substitutions or inventory overrides move through chat rather than governed review.

### Where Integrations Fail
Marketplace CSV and webhook lanes fail at the worst possible times and need visible recovery.

### Where Inventory Or Order Mismatch Appears
Oversell risk rises when commitments outrun visible stock posture.

### How Replay And Recovery Help
Replay keeps failed inbound orders recoverable with traceability instead of forcing ad hoc reentry.

### How Realtime Visibility Changes Operations
Live dashboard updates change triage speed when order pressure or stock risk shifts.

### How Audit And Event Tracing Help
Critical for proving whether an order failed to ingest, was replayed, and when the promise-relevant state changed.

### Why Tenant Isolation Matters
Useful for agencies or operators running multiple commerce brands in one platform.

### Metrics That Matter Most
- orders ingested total
- fulfillment backlog
- delayed fulfillment count
- replay attempts
- recent import issues

### Dashboard Views That Matter Most
- dashboard summary
- orders and fulfillment surfaces
- integrations and replay

### Alerts And Recommendations That Matter Most
- connector disabled
- inventory shortage against active demand
- fulfillment delay escalation

### ROI And Operational Value
- fewer missed customer promises
- faster operator triage
- less time reconciling order-import exceptions

### Realistic Scenarios
#### Marketplace CSV lane fails mid-campaign

- Before: Support sees angry customers before operations sees the broken feed.
- After: The failed rows are classified, queued, and recovered through a visible replay lane.

#### Warehouse stock drops during flash demand

- Before: Teams refresh different tools and act on lagging numbers.
- After: Realtime dashboard risk and recommendations update immediately.