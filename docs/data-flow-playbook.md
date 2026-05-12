# Data Flow Playbook

This playbook describes the main end-to-end SynapseCore flows using the actual product and backend vocabulary.

Each flow includes:

- user action
- frontend page or component
- backend controller and service lane
- DB touchpoints
- realtime or event output
- user-visible result
- failure modes

## 1. Sign-In / Session Flow

### User action

- operator opens sign-in
- enters workspace code, username, and password

### Frontend page / component

- `SignIn.jsx`
- authenticated route switch and shell bootstrap

### Backend controller / service

- `AuthController`
- auth/session service lane
- operator and tenant identity resolution

### DB / repository touchpoints

- user account lookup
- operator mapping lookup
- tenant/workspace lookup
- session persistence through Redis in production

### Realtime / event output

- no major business realtime event is required
- runtime/session posture becomes available after sign-in

### User-visible result

- session established
- user enters the authenticated shell
- dashboard and protected pages become accessible

### Failure modes

- wrong credentials
- wrong workspace code
- rate limit
- backend unavailable
- Redis/session failure in production-like mode

## 2. Product / Catalog Flow

### User action

- operator creates or imports products

### Frontend page / component

- `Catalog.jsx`

### Backend controller / service

- `ProductController`
- product and catalog service lane

### DB / repository touchpoints

- product persistence
- tenant-owned product queries
- import history or validation persistence where applicable

### Realtime / event output

- dashboard and related operational surfaces can reflect catalog readiness

### User-visible result

- product appears in catalog
- order and inventory flows can reference the SKU

### Failure modes

- duplicate SKU
- malformed import
- tenant mismatch
- backend unavailable

## 3. Inventory Update Flow

### User action

- operator updates stock quantity or threshold

### Frontend page / component

- `Inventory.jsx`
- dashboard inventory guidance panels

### Backend controller / service

- `InventoryController`
- inventory service
- intelligence and recommendation services

### DB / repository touchpoints

- inventory record update
- threshold persistence
- alert and recommendation persistence
- business event and audit persistence

### Realtime / event output

- inventory topic update
- dashboard summary refresh
- alerts and recommendations refresh

### User-visible result

- inventory state updates
- low-stock or recovery posture changes
- dashboard reacts without full refresh

### Failure modes

- missing SKU or warehouse
- contention/retry failure
- backend unavailable
- stale or delayed realtime when backend/runtime is degraded

## 4. External CSV Import Flow

### User action

- operator uploads or triggers CSV import through the product or integration surface

### Frontend page / component

- `Catalog.jsx` for import guidance
- `Integrations.jsx` for connector posture and recent import visibility

### Backend controller / service

- `ExternalOrderWebhookController`
- `ExternalOrderCsvImportService`
- connector and import-run service lane

### DB / repository touchpoints

- connector validation
- import-run persistence
- normalized order or replay-failure persistence
- downstream order persistence if successful

### Realtime / event output

- integration telemetry update
- replay queue update if failed
- orders/inventory/alerts if successful

### User-visible result

- import success, partial success, or explicit failure
- recent import history updates
- replay queue receives recoverable failures

### Failure modes

- connector disabled
- malformed CSV
- unknown SKU or warehouse
- backend unavailable
- DB/runtime degradation during import

## 5. Failed Inbound Replay Flow

### User action

- operator opens replay queue
- inspects a failed inbound record
- chooses `Replay Into Live Flow`

### Frontend page / component

- `Replay.jsx`

### Backend controller / service

- `ExternalOrderWebhookController`
- `IntegrationReplayService`
- connector policy and replay eligibility services

### DB / repository touchpoints

- replay record read
- replay record mutation
- connector read
- downstream order persistence if replay succeeds

### Realtime / event output

- replay queue update
- integration telemetry update
- dashboard summary update
- order/inventory/alert/recommendation updates if replay succeeds

### User-visible result

- failed record resolves or leaves the queue
- live order flow reflects the recovered inbound work

### Failure modes

- connector still disabled
- replay record not eligible
- backend unavailable
- DB/runtime timeout or upstream degradation

## 6. Scenario Approval / Execution Flow

### User action

- planner creates a scenario
- saves it
- reviewer approves it
- operator executes it

### Frontend page / component

- `ScenarioHistory.jsx`
- `Approvals.jsx`
- scenario planner surfaces

### Backend controller / service

- `ScenarioController`
- `ScenarioAnalysisService`
- `ScenarioHistoryService`
- `ScenarioExecutionService`

### DB / repository touchpoints

- scenario history persistence
- approval state persistence
- execution lineage persistence
- downstream order persistence on execution

### Realtime / event output

- scenario notifications
- escalation events
- dashboard/order/inventory/alert changes after execution

### User-visible result

- saved plan becomes visible in history
- approvals change status visibly
- execution produces real operational outcomes

### Failure modes

- invalid scenario payload
- unauthorized role action
- approval policy mismatch
- execution blocked by backend/runtime unavailability

## 7. Dashboard Snapshot / Realtime Flow

### User action

- operator lands on dashboard or stays inside authenticated shell

### Frontend page / component

- `Dashboard.jsx`
- shell realtime hooks

### Backend controller / service

- `DashboardController`
- operational summary and snapshot services
- realtime publisher service

### DB / repository touchpoints

- summary reads
- alert/recommendation/order/inventory/integration/runtime reads
- cached summary support where enabled

### Realtime / event output

- summary topic
- alerts topic
- recommendations topic
- inventory topic
- recent orders/events/audit topics

### User-visible result

- command center loads
- signal cards and lanes stay current
- users see live posture instead of stale report snapshots

### Failure modes

- snapshot load issue
- realtime connection degraded
- backend unavailable
- readiness not passing

## 8. Alert / Recommendation Flow

### User action

- user opens alerts or recommendations
- or indirectly triggers them through inventory/order changes

### Frontend page / component

- `Alerts.jsx`
- `Recommendations.jsx`
- dashboard guidance rail

### Backend controller / service

- `AlertController`
- `RecommendationController`
- intelligence, prediction, and decision services

### DB / repository touchpoints

- alert persistence
- recommendation persistence
- supporting inventory/order reads

### Realtime / event output

- alert and recommendation topics
- dashboard summary refresh

### User-visible result

- warning center and action queue reflect the live operational situation

### Failure modes

- no underlying risk data because upstream state is missing
- backend unavailable
- runtime lag delaying event propagation

## 9. Runtime Health Flow

### User action

- operator opens runtime page
- engineer or script calls health endpoints

### Frontend page / component

- `Runtime.jsx`
- shell trust indicators

### Backend controller / service

- `SystemController`
- runtime and incident services
- Actuator health endpoints

### DB / repository touchpoints

- readiness depends on DB
- Redis-backed session and realtime posture influence readiness/runtime truth
- incident views may depend on persisted operational backlog or failure records

### Realtime / event output

- runtime or incident-related operational notices where exposed

### User-visible result

- runtime trust center shows live, degraded, or unavailable posture
- scripts can classify whether proof should run

### Failure modes

- DB unavailable
- Redis unavailable
- backend hung or timing out
- frontend reachable but backend not responsive

## Flow Summary

The major SynapseCore flows all share the same architectural promise:

1. a real user action or external event enters the system
2. the backend routes it through tenant-aware business logic
3. PostgreSQL and Redis support real persistence and runtime posture
4. realtime or snapshot layers surface the result visibly
5. the user sees not just data, but operational consequence and next action

That is what makes SynapseCore an operations control platform rather than a set of disconnected CRUD pages.
