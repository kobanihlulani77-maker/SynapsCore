# SynapseCore Architecture

## Product Shape

SynapseCore is an intelligence layer that watches operations, updates live state, interprets impact, predicts risk, and recommends action.

The architecture should always preserve this operating loop:

1. receive activity
2. update state
3. evaluate risk
4. estimate likely outcomes
5. generate action guidance
6. push the result live

## MVP Modules

### Backend

- `config/`
  - CORS configuration
  - Redis template setup
  - scheduling
  - WebSocket/STOMP broker
  - profile-driven runtime settings for local versus deployment-oriented behavior
- `api/controller/`
  - REST APIs for orders, inventory, dashboard, alerts, recommendations, and simulation
- `domain/entity/`
  - `Product`
  - `Warehouse`
  - `Inventory`
  - `CustomerOrder`
  - `FulfillmentTask`
  - `OrderItem`
  - `Alert`
  - `Recommendation`
  - `BusinessEvent`
- `domain/repository/`
  - JPA persistence for core entities
- `domain/service/`
  - command services for orders and inventory
  - snapshot/query services for the dashboard
  - summary caching
  - seed initialization and reseed support
- `intelligence/`
  - low-stock evaluation
  - early depletion-risk detection before threshold breach
  - urgency assessment
- `prediction/`
  - stock depletion estimation from recent usage
- `decision/`
  - replenishment recommendation generation
  - cross-warehouse transfer guidance when another location can cover the shortfall
- `fulfillment/`
  - order-to-dispatch lifecycle management
  - backlog and delivery-risk assessment
  - fulfillment overview generation for the dashboard
- `access/`
  - lightweight actor and control-role enforcement for protected operational actions
  - seeded operator directory lookup so powerful actions map to known active operators
- `auth/`
  - lightweight user-account sign-in session mapped onto the operator directory
  - credential-checked seeded demo users for local testing
  - session-first identity resolution with dev/test-only header fallback for non-UI flows
- `alert/`
  - alert lifecycle management
- `event/`
  - business event recording and recent-event retrieval
  - internal operational state-change publication
  - persisted operational dispatch queue for deferred dashboard refresh and realtime fan-out
- `realtime/`
  - dashboard summary broadcasting
  - alerts, recommendations, inventory, fulfillment overview, recent orders, business-event timeline, audit trail, integration operations, and simulation status channels
- `observability/`
  - Micrometer-backed gauges and tenant-scoped counters
  - Prometheus-ready metrics exposure through Actuator
- `simulation/`
  - scheduled fake orders using the real order service
- `scenario/`
  - non-persistent what-if order impact analysis
  - multi-line order mix projection before commit
  - side-by-side comparison between two proposed order plans
  - recent scenario history so planning runs become queryable operational memory, including ownership and approval filtering for control-center review
  - automatic review priority and risk scoring for saved plans based on projected operational impact
  - escalated approval policy for critical saved plans
  - staged owner-review then final-approval flow for escalated saved plans
  - automatic final-approval routing so critical saved plans land with a specific final approver and review queue
  - stage-aware approval due times and overdue filtering so delayed plans surface in a dedicated SLA queue
  - automatic SLA escalation so overdue critical final approvals reroute to an executive fallback approver and remain traceable
  - active SLA escalation inbox for live operational visibility of rerouted critical approvals
  - dedicated scenario notifications so reroutes and ownership acknowledgments become first-class operational notices
  - escalation acknowledgment so rerouted approvals can move from urgent inbox state into owned history state
  - reload of stored preview requests back into the planner
  - saving named scenario plans for later reuse
  - lightweight approval of saved plans before execution
  - execution of stored preview scenarios into the live order flow
  - projected inventory, alerts, and recommendations before a live commit
- `integration/`
  - webhook-style inbound activity adapters
  - CSV batch import adapter that groups incoming rows into real orders
  - connector management so inbound sources can be enabled, disabled, and described operationally
  - recent import-run history for visibility into connector behavior
  - failed inbound replay queue so fixable rejected orders can be re-entered into the real order flow
  - raw external payload mapping into the real SynapseCore order flow
- `audit/`
  - request tracing
  - structured audit logging for state-changing actions and rejected requests
  - recent audit retrieval for operational review
- `api/system`
  - safe runtime trust surface for the control center
  - exposes active profile, health state, secure-cookie posture, and header-fallback mode

### Frontend

- single-page React control center
- runtime-configurable API and WebSocket endpoints through `/runtime-config.js` for container deployments
- bootstraps from the expanded dashboard snapshot plus dedicated control endpoints
- subscribes to dedicated channels for summary, alerts, recommendations, inventory, recent orders, recent events, recent audit activity, integration connector status, recent import activity, failed inbound replay queue state, and simulation status
- subscribes to a dedicated SLA escalation inbox channel for rerouted critical approvals
- subscribes to a dedicated scenario notification channel so reroutes and acknowledgments surface as operational notices
- uses a signed-in user session for protected control actions and an `Acting As` role boundary in the planner so review actions declare whether the mapped operator is acting as review owner, final approver, or escalation owner
- presents summary cards, alerts, recommendations, inventory health, recent orders, operational timeline, audit traceability, integration operations including replayable failures, and simulation controls
- includes a what-if order planner that previews projected inventory posture, compares two proposed order plans, saves named plans with assigned review ownership, automatically assigns review priority from projected risk, escalates critical plans into a staged approval policy, surfaces recent scenario history in the control center, reloads loadable plans for iteration, tracks revision-linked resubmissions after rejection, uses lightweight approval or rejection before executing saved plans, and can promote approved plans into live orders
- includes a what-if order planner that previews projected inventory posture, compares two proposed order plans, saves named plans with assigned review ownership, automatically assigns review priority from projected risk, escalates critical plans into a staged approval policy, auto-routes final approval ownership by operational context, auto-reroutes overdue final approvals to an executive fallback, surfaces recent scenario history in the control center, exposes overdue and SLA-escalated queues from backend policy timing, supports acknowledgment of rerouted escalations as owned work, reloads loadable plans for iteration, tracks revision-linked resubmissions after rejection, uses lightweight approval or rejection before executing saved plans, and can promote approved plans into live orders

## Runtime Path

### Order ingestion path

1. `POST /api/orders` accepts an order
2. backend validates warehouse and product references
3. order and order items are persisted
4. a `FulfillmentTask` is opened for the order with default dispatch and delivery SLAs
5. related inventory is reduced
6. monitoring logic evaluates each affected inventory record and the warehouse fulfillment lane
7. alerts and recommendations are generated if thresholds, backlog pressure, or delay signals are crossed
8. an internal operational state-change event is published and persisted into the dispatch queue
9. the dispatch worker drains queued fan-out work in small batches
10. focused realtime topics are pushed to the frontend

### Fulfillment update path

1. `POST /api/fulfillment/updates` resolves the order’s fulfillment lane
2. SynapseCore validates the tenant and order identity
3. the order moves through queued, picking, packed, dispatched, delayed, delivered, or exception state
4. backlog, dispatch SLA pressure, delivery-delay risk, and logistics anomalies are re-evaluated
5. fulfillment alerts and recommendations are opened, refreshed, or resolved
6. a queued dispatch refresh pushes the fulfillment realtime update to the dashboard

### Inventory update path

1. `POST /api/inventory/update` updates quantity and threshold
2. the inventory record is persisted
3. monitoring logic re-evaluates that stock position
4. active alerts are opened or resolved
5. an internal operational state-change event is published into the dispatch queue
6. the updated operational channels are pushed live when the queue worker drains that item

### Simulation path

1. simulation mode is started
2. a scheduled job selects live inventory
3. a fake order is created through the real order service
4. all downstream intelligence runs exactly as it would for a real order

### External integration path

1. `POST /api/integrations/orders/webhook` receives an external order payload
2. the integration layer validates and maps the raw connector payload
3. the mapped order is handed to the real `OrderService`
4. inventory, intelligence, alerts, recommendations, summary, and realtime updates all run through the normal path

## Persistence Roles

- PostgreSQL stores the operational record of truth
- PostgreSQL also stores audit logs for traceability of critical actions
- Redis caches the dashboard summary and mirrors simulation status
- WebSockets push focused tenant-scoped operational topics to connected clients

## Scale Preparation

- core write services now publish an internal operational state-change event instead of directly owning fan-out work
- each state-change event is also stored as an `OperationalDispatchWorkItem`
- a scheduled queue worker drains dispatch work in batches and records failed fan-out attempts for support visibility
- Micrometer counters and gauges expose alert, fulfillment, replay, and dispatch health through `GET /actuator/prometheus`
- this keeps the current architecture simple while creating a clean seam for future broker-backed streaming later

## Developer Reset Flow

- `SeedService` owns the starter data baseline for SynapseCore
- startup seeding only runs when the catalog is empty
- `POST /api/dev/reseed` clears demo operational data and restores the baseline
- reseed also stops simulation, refreshes the dashboard summary, and pushes live updates

## Test Coverage Shape

The MVP integration tests validate the operational loop rather than isolated CRUD:

- order ingestion reduces inventory
- low stock produces alerts and recommendations
- dashboard summary reflects live changes
- simulation state starts and stops cleanly
