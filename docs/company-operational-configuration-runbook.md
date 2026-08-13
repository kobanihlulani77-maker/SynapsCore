# Company 1 Operational Configuration Runbook

This is the internal SynapseCore Phase 7 runbook for configuring the operational behavior of a provisioned Company 1 pilot tenant after tenant provisioning, user provisioning, connector setup, and bounded data onboarding.

Phase 7 does not create customers, hand over credentials, run pre-handover verification, change backend behavior, change frontend behavior, or invent new configuration surfaces. It documents the operating model that Company 1 is allowed to use with the current product.

## Phase Boundary

Phase 7 starts only after:

- tenant/workspace boundary exists as a `Tenant`
- users and operators exist as `AccessUser` and `AccessOperator`
- roles and warehouse scopes have been assigned
- one approved connector lane exists
- approved catalog, inventory, and order data have been onboarded through supported APIs

Phase 7 ends when:

- alert behavior is mapped
- recommendation behavior is mapped
- replay operating rules are defined
- approval governance is mapped
- scenario scope is explicitly in or out of Company 1 pilot
- integration policies are recorded
- tenant settings and operator scopes are recorded
- realtime and runtime expectations are documented
- unsupported settings are visible
- a baseline configuration record is ready
- no critical operational-configuration blocker is open

Stop after this phase. Phase 8 owns final pre-handover verification.

## Configuration Classification

Use these labels consistently.

| Label | Meaning |
| --- | --- |
| `CONFIGURABLE` | A supported API/UI path persists this setting. |
| `SYSTEM-GENERATED` | The backend derives the state from data, policy, events, or runtime. |
| `OPERATOR-CONTROLLED` | A signed-in operator can trigger an action through a supported control. |
| `ROLE-CONTROLLED` | Access depends on assigned `SynapseAccessRole` and, where relevant, warehouse scope. |
| `FIXED IN CURRENT PRODUCT` | The behavior exists but is not customer-configurable today. |
| `NOT SUPPORTED` | The requested behavior does not exist in the current product. |

## Operational Configuration Inventory

| Capability | Current surface | Classification | Phase 7 action |
| --- | --- | --- | --- |
| Alert generation | `AlertService`, `/api/alerts`, Alerts page | `SYSTEM-GENERATED`, partly `CONFIGURABLE` through tenant operational policy | Map conditions to current generated alerts and policy fields. |
| Recommendation generation | `RecommendationService`, `/api/recommendations`, Recommendations page | `SYSTEM-GENERATED`, partly `CONFIGURABLE` through tenant operational policy | Map advice to current evidence-based recommendation types. |
| Tenant operational policy | `/api/system/policy` | `CONFIGURABLE`, `ROLE-CONTROLLED` by `TENANT_ADMIN` | Record default values and any approved Company 1 deviations. |
| Replay queue | `IntegrationReplayRecord`, `/api/integrations/orders/replay-queue` | `SYSTEM-GENERATED`, `OPERATOR-CONTROLLED` for manual replay | Define inspect, replay, stop, and evidence rules. |
| Manual replay | `POST /api/integrations/orders/replay/{id}` | `OPERATOR-CONTROLLED`, `ROLE-CONTROLLED` by `INTEGRATION_OPERATOR` or `INTEGRATION_ADMIN` | Restrict to trained replay operator lanes. |
| Automated replay | `IntegrationReplayAutomationService` | `FIXED IN CURRENT PRODUCT` via backend properties | Do not create Company 1-specific retry rules unless supported later. |
| Connector policy | `IntegrationConnector`, Integration page, Settings page | `CONFIGURABLE`, `ROLE-CONTROLLED` by `INTEGRATION_ADMIN` or `TENANT_ADMIN` depending route | Record one approved connector policy baseline. |
| Approvals | `ScenarioRun`, Scenario decision console | `OPERATOR-CONTROLLED`, `ROLE-CONTROLLED` | Map governance only for saved scenario plans. |
| Scenario planning | `/api/scenarios/*`, Scenarios page | `OPERATOR-CONTROLLED`, `ROLE-CONTROLLED` by workspace and warehouse access | Mark in pilot only if Phase 2 explicitly approved it. |
| Workspace profile | `/api/access/admin/workspace`, Settings page | `CONFIGURABLE`, `ROLE-CONTROLLED` by `TENANT_ADMIN` | Record tenant name and description. |
| Security settings | `/api/access/admin/workspace/security`, Settings page | `CONFIGURABLE`, `ROLE-CONTROLLED` by `TENANT_ADMIN` | Record rotation, timeout, and session-invalidation decision. |
| Warehouse metadata | `/api/access/admin/workspace/warehouses/{id}`, Settings page | `CONFIGURABLE`, `ROLE-CONTROLLED` by `TENANT_ADMIN` | Record final warehouse name/location labels. |
| Realtime | `/ws`, STOMP/SockJS subscriptions | `SYSTEM-GENERATED`, infrastructure-driven | Record expectations and polling/degraded fallback. |
| Runtime/trust | `/api/system/runtime`, Runtime page | `SYSTEM-GENERATED`, read-only | Record interpretation rules; do not configure runtime truth. |

## Alert Model

Alert implementation:

- entity/class: `Alert`
- table: `alerts`
- tenant relationship: many-to-one `Tenant`
- controller: `AlertController`
- API: `GET /api/alerts`
- frontend: Alerts page, route `/alerts`
- status values: `ACTIVE`, `RESOLVED`
- severity values: `MEDIUM`, `HIGH`, `CRITICAL`
- type values: `LOW_STOCK`, `DEPLETION_RISK`, `FULFILLMENT_BACKLOG`, `DELIVERY_DELAY_RISK`, `FULFILLMENT_ANOMALY`
- fields include title, description, impact summary, recommended action, policy explanation, timestamps

Alerts are created by backend services when operational data crosses known conditions:

- inventory changes can produce low-stock and depletion-risk alerts
- fulfillment/order pressure can produce backlog, delivery-delay-risk, and fulfillment-anomaly alerts
- alert records are tenant-scoped
- active alerts may be refreshed when the same condition remains active
- relevant alerts resolve when the condition no longer applies

Alerts are not freely created by operators in the current product. The alert page is an inspection surface, not a manual alert authoring tool.

## Alert Configuration Capability

| Alert concern | Current support | Notes |
| --- | --- | --- |
| Thresholds | `SUPPORTED THROUGH EXISTING CONFIG` | `TenantOperationalPolicy` exposes low-stock critical ratio, depletion-risk hours, rapid-consumption settings, backlog counts, delay tolerance, and risk score thresholds. |
| Alert types | `FIXED` | Alert type enum is fixed in backend code. |
| Severity | `SUPPORTED THROUGH EXISTING CONFIG` | Operational policy exposes severity choices for known alert families. |
| Recipients | `NOT SUPPORTED` | No alert recipient, subscription, email, SMS, or Slack routing model exists today. |
| Warehouse scope | `SYSTEM-GENERATED` | Alerts arise from tenant data and warehouse-related data; operator visibility is affected by broader page/data access and some domain filters, not an alert-specific scope editor. |
| Product scope | `SYSTEM-GENERATED` | Product conditions come from catalog/inventory records. There is no per-product alert rule UI. |
| Order conditions | `FIXED` | Fulfillment/order alert logic is backend-defined. |
| Connector conditions | `SYSTEM-GENERATED` | Connector/replay failures surface through integration/runtime telemetry and system incidents; alert hook emissions exist for severe replay states, but there is no customer rule builder. |
| Manual create/resolve | `NOT SUPPORTED` | No public operator create/resolve alert API is implemented. |

## Company 1 Alert Mapping Approach

Use this mapping method rather than building custom rules.

| Company condition | Current SynapseCore capability | Supported? | Configuration action | Owner | Pilot decision |
| --- | --- | --- | --- | --- | --- |
| Stock below reorder expectation | Inventory low-stock detection and `LOW_STOCK` alert | `SUPPORTED AS-IS` | Confirm product reorder thresholds and optional operational policy thresholds. | SynapseCore platform owner with Company 1 operations approver | In pilot if inventory lane is in scope. |
| Inventory likely to deplete quickly | `DEPLETION_RISK` alert | `SUPPORTED THROUGH EXISTING CONFIG` | Confirm depletion-risk hours and urgent threshold. | SynapseCore platform owner | In pilot if inventory consumption/order lane is in scope. |
| Fulfillment backlog | `FULFILLMENT_BACKLOG` alert | `SUPPORTED THROUGH EXISTING CONFIG` | Confirm backlog count/critical count defaults; change only with approved reason. | SynapseCore platform owner | In pilot if order/fulfillment lane is in scope. |
| Delivery delay or overdue dispatch | `DELIVERY_DELAY_RISK` alert | `SUPPORTED THROUGH EXISTING CONFIG` | Confirm delay tolerance and delayed/overdue count defaults. | SynapseCore platform owner | In pilot if fulfillment statuses are used. |
| Unexpected fulfillment anomaly | `FULFILLMENT_ANOMALY` alert | `FIXED PRODUCT BEHAVIOR` | No algorithm customization in Phase 7. | SynapseCore engineering/product | Out of custom configuration; visible if generated. |
| Email/SMS recipient routing | No recipient model | `NOT SUPPORTED` | Do not promise. Use in-app visibility and manual support communication. | SynapseCore pilot owner | Out of pilot. |
| Custom customer-specific alert formulas | No rule builder | `NOT SUPPORTED` | Record as post-pilot product input if evidence justifies it. | Product review | Post-pilot only. |

## Recommendation Model

Recommendation implementation:

- entity/class: `Recommendation`
- table: `recommendations`
- tenant relationship: many-to-one `Tenant`
- controller: `RecommendationController`
- API: `GET /api/recommendations`
- frontend: Recommendations page, route `/recommendations`
- type values: `REORDER_STOCK`, `REORDER_URGENTLY`, `TRANSFER_STOCK`, `PRIORITIZE_FULFILLMENT`, `ESCALATE_LOGISTICS`, `INVESTIGATE_LOGISTICS_ANOMALY`
- priority values: `MEDIUM`, `HIGH`, `CRITICAL`
- fields include title, description, policy explanation, priority, created timestamp

Recommendations are generated by backend services from inventory and fulfillment evidence. They are decision support. They do not automatically mutate inventory, create orders, approve scenarios, replay inbound records, or execute business actions by themselves.

## Recommendation Configuration Capability

| Recommendation concern | Current support | Notes |
| --- | --- | --- |
| Thresholds | `CONFIGURABLE` through `TenantOperationalPolicy` | Policy affects inventory and fulfillment pressure that can generate recommendations. |
| Priority | `CONFIGURABLE` through `TenantOperationalPolicy` | Known recommendation families have configurable priorities. |
| Applicability | `FIXED` | Recommendation applicability is backend-generated from current supported flows. |
| Warehouse-specific rule | `NOT SUPPORTED` as a dedicated rule | Recommendations can be tied to warehouse-related data, but there is no per-warehouse rule builder. |
| Product-specific rule | `NOT SUPPORTED` as a dedicated rule | Product thresholds influence outcomes, but no per-product recommendation policy UI exists. |
| Action suggestion text | `FIXED` | Recommendation titles/descriptions are generated by backend service logic. |
| Suppression/dismissal | `NOT SUPPORTED` | No operator dismiss/suppress API was found. |
| Automatic execution | `NOT SUPPORTED` | Recommendation is advisory only. |

## Intelligence Boundary

The official Company 1 operating model is:

```text
Recommendation -> Evidence -> Human Review -> Governed Action
```

Recommendations are not autonomous operations. Operators use them to decide whether to inspect inventory, adjust fulfillment priority, create or save a scenario plan, request approval, or raise an operational discussion. Any live mutation must occur through a supported order, inventory, connector, replay, or scenario execution path with the appropriate role.

## Replay Policy Model

Replay implementation:

- entity/class: `IntegrationReplayRecord`
- table: `integration_replay_records`
- controller path: `/api/integrations/orders/replay-queue` and `/api/integrations/orders/replay/{replayRecordId}`
- statuses: `PENDING`, `REPLAY_FAILED`, `DEAD_LETTERED`, `REPLAYED`
- replay role: `INTEGRATION_OPERATOR` or `INTEGRATION_ADMIN`
- queue view: authenticated workspace access
- tenant scoping: replay records are queried by current tenant code
- warehouse scoping: replay visibility/action respects operator warehouse access where warehouse code is present
- persistence: replay record stores source system, connector type, external order id, warehouse code, payload, failure code/message, attempt count, next eligibility, resolution/dead-letter timestamps, replayed order id
- evidence: business events, audit logs, operational metrics, integration telemetry, and realtime integration updates

Replay has both automated and manual behavior:

- automated replay is controlled by backend properties such as max attempts, backoff seconds, automation enabled, batch size, and interval
- manual replay is performed through the replay API/UI by an authorized integration operator
- connector-disabled records are manual-only for automated replay
- replay refuses already replayed records
- dead-lettered records must be re-ingested manually
- records are row-locked during replay to reduce concurrent replay collisions

Company 1 does not receive a programmable replay policy model in the current product.

## Company 1 Replay Operating Rules

| Question | Company 1 rule |
| --- | --- |
| Who may inspect failure? | Signed-in workspace operators may inspect replay visibility according to their tenant and warehouse visibility. |
| Who may replay? | Only operators with `INTEGRATION_OPERATOR` or `INTEGRATION_ADMIN`, and warehouse access to the replay record warehouse. |
| What must be checked first? | Confirm tenant, connector, source system, external order id, warehouse, failure code/message, duplicate risk, and current source-system truth. |
| When is replay prohibited? | Stop if tenant is uncertain, payload is malformed, source state changed, duplicate risk is unresolved, connector is wrong/disabled unexpectedly, record is dead-lettered, eligibility is blocked, or authorization is missing. |
| When is approval required? | The current replay feature does not require a scenario approval workflow. Company 1 may impose a manual operating approval before replaying high-risk records, but that is procedural, not enforced by replay code. |
| What counts as duplicate risk? | Same tenant and external order id may already have produced a live order; replay must not be used to create hidden duplicate operational state. |
| What evidence must be recorded? | Replay record id, failure code/message, operator, timestamp, replay result, replayed order id if successful, and any customer/source-system confirmation. |

## Replay Stop Conditions

Stop replay when any of these are true:

- source record already successfully processed
- duplicate risk is unresolved
- tenant is uncertain
- mapping is uncertain
- source system state changed after the failed inbound event
- malformed payload has not been corrected upstream or re-ingested
- replay eligibility is blocked by `nextEligibleAt`
- replay record is `DEAD_LETTERED`
- data integrity is uncertain
- connector or source system is wrong
- connector is disabled and the operational reason is unknown
- current user lacks replay role or warehouse access

## Approval Model

Approvals currently apply to saved scenario plans.

Approval implementation:

- entity/class: `ScenarioRun`
- table: `scenario_runs`
- controller path: `/api/scenarios`
- saved-plan approval endpoints:
  - `POST /api/scenarios/{scenarioRunId}/approve`
  - `POST /api/scenarios/{scenarioRunId}/reject`
  - `POST /api/scenarios/{scenarioRunId}/acknowledge-escalation`
- statuses: `NOT_REQUIRED`, `PENDING_APPROVAL`, `APPROVED`, `REJECTED`
- stages: `NOT_REQUIRED`, `PENDING_REVIEW`, `PENDING_FINAL_APPROVAL`, `APPROVED`, `REJECTED`
- policies: `STANDARD`, `ESCALATED`
- priorities: `MEDIUM`, `HIGH`, `CRITICAL`
- actor roles: `REQUESTER`, `REVIEW_OWNER`, `FINAL_APPROVER`, `ESCALATION_OWNER`

Only `SAVED_PLAN` scenario runs go through approval. Preview and comparison runs are history/evidence. Execution records are generated after execution.

## Actual Approval Roles

| Role | Actual governance authority |
| --- | --- |
| `TENANT_ADMIN` | Can manage tenant users/operators, settings, warehouse metadata, connector support/settings, products, and operational policy. It is not automatically a scenario approver unless the operator also has scenario approval roles. |
| `REVIEW_OWNER` | Can perform protected review actions when declared actor name matches the signed-in operator and the scenario requires review-owner action. |
| `FINAL_APPROVER` | Can perform final approval/rejection on escalated plans when the scenario is in final-approval stage and the actor is the assigned final approver. |
| `ESCALATION_OWNER` | Can acknowledge escalated overdue scenario approvals when assigned and authorized. |
| `INTEGRATION_ADMIN` | Can manage integration connectors and can replay because integration admin satisfies integration operator requirements. |
| `INTEGRATION_OPERATOR` | Can replay failed inbound orders but cannot manage connector policy unless also assigned an admin role. |

## Self-Approval and Separation of Duty

Current behavior:

- standard saved-plan approval uses assigned review owner and role checks
- escalated approval requires an approval note
- escalated approval prevents requester approving their own escalated plan
- escalated final approval prevents the same operator who performed owner review from also performing final approval
- final approval owner selection tries to exclude the review owner when assigning the final approver
- the backend does not provide a universal separation-of-duty engine for every possible role combination
- the same operator can be assigned multiple access roles unless Phase 4/Company 1 operating procedure prevents that

Company 1 operating condition:

Separation of duty must be enforced through approved role assignment and the configuration record. Do not assign all governance roles to one customer operator unless the pilot owner explicitly accepts the risk and records it.

## Approval Failure Conditions

| Failure condition | Detect | Stop | Recover | Verify |
| --- | --- | --- | --- | --- |
| Already decided | API returns current state or rejects invalid transition | Do not force another decision | Reload scenario history | Confirm final status and audit/event record |
| Unauthorized role | API `403` or `400` role mismatch | Stop action | Assign correct operator/role or request approved change | Retry with correct signed-in operator |
| Stale state | UI/API shows changed approval status/stage | Stop before acting | Refresh scenario history | Confirm stage before approve/reject |
| Wrong tenant | Scenario not found or forbidden | Stop immediately | Confirm signed-in tenant/session | Verify no cross-tenant record is visible |
| Invalid scenario | API rejects non-saved-plan approval | Stop | Use saved-plan workflow | Confirm scenario type is `SAVED_PLAN` |
| Missing evidence/note | API rejects required escalated approval note | Stop | Add evidence note | Confirm approval note persisted |
| Backend failure | 5xx/timeout/readiness issue | Pause high-impact actions | Restore readiness first | Re-run live connection checks before continuing |

## Scenario Model

Scenario implementation:

- entity/class: `ScenarioRun`
- table: `scenario_runs`
- types: `PREVIEW`, `COMPARISON`, `SAVED_PLAN`, `EXECUTION`
- tenant relationship: many-to-one `Tenant`
- inputs: `OrderCreateRequest` stored as JSON request payload for preview/saved-plan execution paths
- configurable saved-plan fields: title, requestedBy, reviewOwner, revision source id, and order request containing warehouse code, item SKUs, quantities, and unit prices
- derived fields: summary, recommended option, risk score, review priority, approval policy, approval due dates, escalation owner, and history records
- controller endpoints include order-impact preview, comparison, save, approve, reject, acknowledge escalation, execute, request reload, history, and notifications

A scenario represents a proposed order-impact plan or comparison inside the current tenant. It lets an operator preview consequences before creating a live order and, for saved plans, route governed approval before execution.

## Scenario Configuration

Company 1 operators can configure only what the scenario request supports:

- plan title
- requester name when supplied
- review owner when supplied and valid
- revision source when resubmitting a rejected plan
- warehouse code through the order request
- product SKU lines
- quantities
- unit prices

Company 1 cannot configure arbitrary formulas, custom scenario variable engines, external optimizers, or custom workflow chains in the current product.

## Scenario Relevance Gate

If Company 1 Phase 2 marked scenarios as in scope:

1. Select one approved scenario use case.
2. Use safe deterministic catalog/inventory data.
3. Preview impact.
4. Compare if relevant.
5. Save only when the plan is intended to exercise governance.
6. Approve/reject according to the governance matrix.
7. Execute only after pre-execution checks confirm live mutation is understood.

If Company 1 Phase 2 marked scenarios as out of scope:

Record `NOT CONFIGURED FOR COMPANY 1 PILOT` in the configuration record. The feature may remain in the product but should not be part of customer training or success criteria.

## Scenario Execution Safety

Execution trace:

```text
Scenarios UI
-> POST /api/scenarios/{scenarioRunId}/execute
-> ScenarioExecutionService.execute
-> ScenarioHistoryService.getExecutableOrderRequest
-> OrderService.createOrder
-> live order, inventory reservation/fulfillment side effects
-> ScenarioRun EXECUTION history
-> business event and realtime updates
```

Pre-execution verification:

- scenario belongs to Company 1 tenant
- scenario is `PREVIEW` or approved `SAVED_PLAN`
- warehouse scope is correct
- request payload is known and safe
- product SKUs and quantities are approved for the pilot
- duplicate/external order risk is understood
- the approver has completed required governance
- runtime readiness is healthy

Do not demonstrate scenario execution against real Company 1 operational data unless Company 1 has explicitly authorized the action.

## Integration Policy Model

Integration connector implementation:

- entity/class: `IntegrationConnector`
- table: `integration_connectors`
- connector types: `WEBHOOK_ORDER`, `CSV_ORDER_IMPORT`
- sync modes: `REALTIME_PUSH`, `BATCH_FILE_DROP`, `SCHEDULED_PULL`
- validation policies: `STANDARD`, `STRICT`, `RELAXED`
- transformation policies: `NONE`, `NORMALIZE_CODES`
- mapping version: only version `1` is supported
- optional default warehouse fallback
- optional default warehouse code
- support owner actor name
- notes
- inbound connector token hash/hint
- telemetry fields for last pull status and connector health

Supported configuration paths:

- `POST /api/integrations/orders/connectors` by `INTEGRATION_ADMIN`
- `PUT /api/access/admin/workspace/connectors/{connectorId}` by `TENANT_ADMIN`
- Integrations page `Manage Policies` button routes to Settings
- Settings connector focus panel edits support owner, sync mode, validation, transformation, cadence for scheduled pull, and pull endpoint when relevant

The Settings path does not expose every low-level connector field. Full connector creation and inbound token handling belongs to the integration setup runbook and the connector API, not routine customer self-service.

## Manage Policies Actual Behavior

The `Manage Policies` control on the Integrations page navigates to the Settings page. The Settings connector panel:

- lists tenant connectors
- selects one connector
- allows an authorized tenant admin to edit sync mode from supported modes
- allows validation policy selection
- allows transformation policy selection
- allows scheduled-pull cadence when scheduled pull is selected
- allows scheduled-pull endpoint URL when scheduled pull is selected
- allows support owner selection
- saves through `saveWorkspaceConnectorSupport`
- persists through `PUT /api/access/admin/workspace/connectors/{connectorId}`
- returns an updated connector response

Operational effect:

- validation/transformation policy affects inbound handling behavior
- sync mode/cadence/pull endpoint affects supported connector intake behavior
- support owner affects operational accountability and connector diagnostics
- unsupported sync mode combinations are rejected by backend validation

## Default vs Company-Specific Policy Strategy

Default to platform-supported defaults unless Company 1 Phase 2 requirements justify a change.

| Field | Default strategy | Company 1 strategy |
| --- | --- | --- |
| Webhook sync mode | `REALTIME_PUSH` | Keep unless scheduled pull was explicitly approved. |
| CSV sync mode | `BATCH_FILE_DROP` | Keep. CSV realtime and scheduled pull are not supported. |
| Validation policy | Webhook `STANDARD`, CSV `RELAXED` | Use `STRICT` only after payload completeness is proven. |
| Transformation policy | `NORMALIZE_CODES` by default | Keep if Company 1 identifiers require normalization; otherwise record the chosen value. |
| Mapping version | `1` | Must remain `1`. Other values are rejected. |
| Default warehouse fallback | Webhook default can be on, CSV default is off | Use only when Company 1 approved fallback behavior; otherwise keep off. |
| Support owner | None until assigned | Assign one trained operator/internal owner per connector lane. |
| Inbound access token | Set during integration provisioning when required | Do not store token in docs. Record only token hint and custody. |

## Tenant Settings

Actual tenant/workspace settings available:

| Setting | Type | API/UI | Notes |
| --- | --- | --- | --- |
| tenant name | `PROFILE SETTING` | Settings profile, `/api/access/admin/workspace` | Descriptive company identity. |
| description | `PROFILE SETTING` | Settings profile, `/api/access/admin/workspace` | Operational summary; no secrets. |
| active flag | `SYSTEM/READ-ONLY` in Settings | Returned in workspace response | No routine customer-facing toggle in Settings. |
| password rotation days | `SECURITY SETTING` | Settings security, `/api/access/admin/workspace/security` | Min 7, max 365. |
| session timeout minutes | `SECURITY SETTING` | Settings security, `/api/access/admin/workspace/security` | Min 15, max 1440. |
| invalidate other sessions | `SECURITY SETTING` | Settings security | Increments tenant security policy version. |
| warehouse name/location | `OPERATIONAL SETTING` | Settings warehouse focus | Code is not edited here. |
| connector support/policy | `OPERATIONAL SETTING` | Settings connector focus | Tenant-scoped connector support and supported policy fields. |
| operational thresholds/priorities | `OPERATIONAL SETTING` | `/api/system/policy` | API-backed; no broad customer rule builder. |

Unsupported tenant/workspace settings:

- separate workspace entity
- tenant self-delete/deactivate endpoint for customer use
- arbitrary feature flags
- customer-managed workflow builder
- notification recipient configuration
- MFA/SSO configuration
- connector-specific RBAC scopes
- inventory CSV settings
- per-import rollback settings
- automated retention cleanup controls

## Warehouse and Location Configuration

Warehouses are tenant-scoped operational locations. Phase 6 handles data mapping and onboarding. Phase 7 may adjust only final display metadata after data reconciliation:

- warehouse code: operational identifier, not edited in Settings
- warehouse name: editable by tenant admin
- warehouse location: editable by tenant admin
- tenant relationship: enforced by repository queries and current tenant context
- active status: no routine active/inactive warehouse setting was found in the Settings flow

Do not create duplicate warehouse mappings in Phase 7. If a location mapping is wrong, pause and return to the Phase 6 data onboarding evidence.

## Operator Scope Model

`AccessOperator` supports:

- tenant relationship
- actor name
- display name
- description
- active flag
- roles
- warehouse scopes

`AccessOperator` does not support:

- connector-specific scopes
- approval-lane-specific scopes beyond roles and assigned actor names in scenario records
- formal read-only role
- arbitrary permission bundles

Warehouse-scope behavior:

- empty warehouse scope means broad warehouse access inside the tenant
- non-empty warehouse scope limits warehouse-aware actions and filtered scenario/replay visibility where implemented
- warehouse scopes are normalized uppercase

Company 1 strategy:

- assign the smallest role set needed for each pilot operator
- use warehouse scopes for operators who should only operate a specific site/lane
- avoid empty warehouse scope unless the operator truly needs all Company 1 warehouses
- do not assign all roles to customer users for convenience

## Role and Scope Strategy

| Operator lane | Roles | Warehouse scope | Connector scope | Governance scope |
| --- | --- | --- | --- | --- |
| Company admin | `TENANT_ADMIN` only unless additional duties approved | Approved tenant-wide or specific warehouse scopes | No connector-specific scope model | Can manage access/settings, not automatically scenario approver |
| Integration admin | `INTEGRATION_ADMIN` | Approved lane warehouses | No connector-specific scope model; connector ownership is metadata | Can manage connectors and replay if warehouse access allows |
| Replay operator | `INTEGRATION_OPERATOR` | Approved lane warehouses | No connector-specific scope model | Can replay failed inbound orders; no scenario approval authority |
| Review owner | `REVIEW_OWNER` | Warehouses they may review | None | Can review assigned scenario plans |
| Final approver | `FINAL_APPROVER` | Warehouses they may approve | None | Can final-approve escalated plans when assigned |
| Escalation owner | `ESCALATION_OWNER` | Warehouses they may own | None | Can acknowledge escalations when assigned |

## Realtime Configuration

Realtime behavior is infrastructure/application-generated, not Company 1-configurable.

Current behavior:

- websocket/SockJS endpoint: `/ws`
- frontend uses STOMP subscriptions
- topic prefix: `/topic/tenant/{TENANT_CODE}`
- topics include dashboard summary, alerts, recommendations, inventory, fulfillment overview, recent orders, events, audit logs, system incidents, integration connectors/imports/replay, scenario notifications, and SLA escalations
- frontend starts in `connecting`, enters `live`, and moves to `reconnecting` or `degraded` when transport fails
- degraded mode performs periodic snapshot refresh every 15 seconds
- decision surfaces refresh via API after key realtime events
- runtime reports broker mode as simple in-memory, Redis pub/sub, or STOMP relay depending configuration

Company 1 cannot configure realtime topics, reconnect cadence, broker mode, or heartbeat from the tenant UI.

## Company 1 Realtime Expectations

| Business event | SynapseCore event/surface | Realtime supported? | Expected screen | Fallback if disconnected |
| --- | --- | --- | --- | --- |
| Inventory update | Inventory snapshot, dashboard summary, alerts/recommendations if triggered | Yes, when websocket is live | Dashboard, Inventory, Alerts, Recommendations | Degraded polling/snapshot refresh |
| New order | Recent orders, fulfillment overview, inventory impact, events | Yes, when websocket is live | Dashboard, Orders, Inventory | Manual refresh or degraded polling |
| Connector update | Integration connector telemetry and events | Yes, integration update topics | Integrations, Runtime | Manual refresh |
| Replay queued/completed/failed | Integration replay queue and events | Yes, integration replay topic | Replay, Integrations, Runtime | Manual refresh |
| Scenario saved/approved/rejected/escalated | Scenario notifications/history and events | Partial through scenario topics and snapshot refresh | Scenarios, Approvals, Runtime | Manual refresh |
| Runtime incident | System incidents topic and runtime API | Yes for incident lists when published | Runtime, dashboard trust surfaces | Manual runtime refresh |

## Runtime and Trust Behavior

Runtime implementation:

- controller: `SystemController`
- API: `GET /api/system/runtime`
- frontend: Runtime page, route `/runtime`
- requires workspace access
- generated from Spring availability, repositories, metrics, connector diagnostics, dispatch queue, realtime broker mode, CORS/session posture, and build/runtime environment

Runtime is read-only evidence. Company 1 operators should interpret it, not configure it.

Runtime classifications:

- `SAFE`: normal operation can continue inside pilot scope
- `WATCH`: operation may continue with observation
- `STOP`: pause sensitive operations and inspect readiness/incidents/dependencies

## Failure Honesty

Company 1 operational configuration must preserve truth over fake success. Do not hide:

- disabled connectors
- stale data
- failed inbound records
- degraded realtime
- blocked replay
- rejected approvals
- backend/readiness failures
- dead-lettered replay records
- unresolved runtime incidents

Operators should see degraded, waiting, reconnecting, pending, blocked, rejected, or failed states when those are true.

## Empty and Low-Activity States

The Company 1 pilot may have low volume. Empty states are valid and should not be artificially populated.

Expected empty states:

- zero alerts: means no active generated alerts
- zero recommendations: means no active recommendation evidence
- zero failed inbound records: replay queue clear
- zero scenarios: no scenario plans have been created
- no pending approvals: approval queue clear
- no runtime incidents: trust lane clear

Do not create fake activity for demonstration. Use approved synthetic records only during verification, clearly labeled and recorded.

## Phase 7 Test Procedures

Phase 7 defines procedures. Phase 8 executes the final pre-handover checklist.

### Alert Test

Procedure:

1. Use approved test inventory/product data only.
2. Trigger a low-stock condition through supported inventory update or order flow.
3. Verify `GET /api/alerts` returns the expected tenant-scoped alert.
4. Confirm severity/status/policy evidence.
5. Confirm authorized operator visibility.
6. Restore/record final data state as approved.

Phase 7 result: procedure defined, not executed in this docs-only phase.

### Recommendation Test

Procedure:

1. Use approved inventory or fulfillment condition.
2. Trigger condition through supported API/UI path.
3. Verify `GET /api/recommendations`.
4. Confirm recommendation evidence and priority.
5. Confirm no business mutation occurred merely because the recommendation was generated.

Phase 7 result: procedure defined, not executed in this docs-only phase.

### Approval Test

Procedure:

1. Create a safe saved scenario plan.
2. Verify pending review stage.
3. Approve with authorized review owner.
4. Attempt unauthorized approval and confirm denial.
5. For escalated path, verify note requirement and final approver separation.
6. Confirm persistence/history/audit.

Phase 7 result: procedure defined, not executed in this docs-only phase.

### Scenario Test

If scenarios are in Company 1 scope:

1. Preview.
2. Compare if relevant.
3. Save.
4. Approve/reject as applicable.
5. Execute only against approved safe deterministic data.
6. Verify resulting order/inventory/history.

If scenarios are out of scope:

Record `NOT CONFIGURED FOR COMPANY 1 PILOT`.

Phase 7 result: procedure defined, execution deferred to Phase 8 only if in scope.

### Replay Test

Procedure:

1. Create or use an approved deterministic failed inbound record.
2. Confirm failure evidence and replay eligibility.
3. Replay with authorized integration operator.
4. Verify result state: `REPLAYED`, `REPLAY_FAILED`, or `DEAD_LETTERED`.
5. Confirm resulting order only if replay succeeds.
6. Confirm tenant/warehouse boundaries.

Phase 7 result: procedure defined, not executed in this docs-only phase.

### Connector Policy Test

Procedure:

1. Change only an approved non-secret connector setting.
2. Verify persistence through API/UI readback.
3. Verify role restriction.
4. Verify no wrong-tenant effect.
5. Return policy to approved final Company 1 baseline.

Phase 7 result: procedure defined, not executed in this docs-only phase.

### Operator Access Test

Procedure:

1. For every Company 1 pilot role, sign in as that user.
2. Verify expected access.
3. Verify expected denial.
4. Verify warehouse scope effects.
5. Do not repeat the full 201-control Gate 4 unless Phase 8 requires it.

Phase 7 result: procedure defined, not executed in this docs-only phase.

### Cross-Tenant Configuration Negative Test

Procedure:

1. Use a safe proof/test tenant or existing non-customer test tenant.
2. Confirm Company 1 admin/operator cannot view or mutate another tenant's policies, alerts, scenarios, approvals, connectors, settings, or operator scopes.
3. Confirm another tenant cannot view or mutate Company 1 operational configuration.

Any cross-tenant mutation is a critical pilot blocker.

Phase 7 result: procedure defined, not executed in this docs-only phase.

## Configuration Change Authority

Customer requests do not automatically become production configuration changes.

| Change type | May request | May implement | Approval needed |
| --- | --- | --- | --- |
| Alert threshold/severity change | Company operations sponsor or approved Company admin | SynapseCore platform owner / `TENANT_ADMIN` | Pilot owner approval and evidence update |
| Recommendation priority change | Company operations sponsor | SynapseCore platform owner / `TENANT_ADMIN` | Pilot owner approval |
| Integration policy change | Company technical contact or operations sponsor | `INTEGRATION_ADMIN` for connector API or `TENANT_ADMIN` for Settings connector support path | Technical approval and rollback plan |
| User scope change | Company sponsor or approved manager | `TENANT_ADMIN` | Access approval and record update |
| Governance role change | Company sponsor plus SynapseCore pilot owner | `TENANT_ADMIN` | Separation-of-duty review |
| Scenario scope change | Company sponsor | SynapseCore pilot/product owner | Phase 2 scope update or pilot change record |

## Baseline Configuration Snapshot

Before customer handover, create a baseline configuration record from `docs/templates/company-operational-configuration-record.md`.

Record safely:

- company name/reference
- tenant code and tenant name
- configuration date
- configuration owner
- alert thresholds/severities
- recommendation priorities
- replay rules and assigned operators
- approval roles and named governance owners
- scenario scope decision
- connector policy values
- operator role/warehouse scopes
- tenant profile/security settings
- warehouse display metadata
- realtime expectations
- runtime/trust interpretation
- known limitations
- Phase 8 verification references

Do not record secrets, passwords, raw connector tokens, private payloads, or customer-sensitive row data.

## Configuration Drift

Current product does not provide formal automated drift detection for every configuration area.

Phase 7 drift handling:

1. Treat the completed configuration record as the approved baseline.
2. During pilot support, compare current Settings/API readback to the baseline.
3. Record all approved changes in the same record or a linked change record.
4. Treat unapproved connector/security/access/governance changes as support incidents.
5. Restore approved values only through supported APIs/UI paths.

## Change Freeze Before Handover

At the end of Phase 7, freeze Company 1 operational configuration.

Further changes require:

```text
REQUEST -> APPROVAL -> IMPLEMENT -> VERIFY -> RECORD
```

No undocumented settings changes should be made after Phase 7 without a pilot change record.

## Company 1 Pilot Feature Scope Matrix

Use Phase 2 to set final in/out values. The default recommended Company 1 posture is:

| Feature | Pilot posture | Role | Dependency | Limitation |
| --- | --- | --- | --- | --- |
| Dashboard | In pilot | Workspace access | Backend readiness, data snapshot, realtime | Snapshot/realtime proof must be healthy. |
| Catalog | In pilot if catalog data was onboarded | Workspace access; product mutation requires tenant admin paths | Product import/create/update APIs | No broad product lifecycle suite. |
| Inventory | In pilot | Workspace/warehouse access | Approved inventory data | No inventory CSV import. |
| Orders | In pilot for approved lane | Workspace/warehouse access | Connector/order APIs | Orders are create-only by tenant + external order id. |
| Alerts | In pilot as generated visibility | Workspace access | Inventory/fulfillment conditions and policy | No manual alert rule builder or recipients. |
| Recommendations | In pilot as decision support | Workspace access | Inventory/fulfillment evidence and policy | No automatic execution or dismissal. |
| Integrations | In pilot for one connector lane | `INTEGRATION_ADMIN`, workspace access | Phase 5 connector | Supported types only: webhook order, CSV order import. |
| Replay | In pilot for failed inbound recovery | `INTEGRATION_OPERATOR` or `INTEGRATION_ADMIN` | Failed inbound/replay records | No programmable per-company replay policy. |
| Approvals | In pilot only for scenario governance if relevant | Review/final/escalation roles | Saved scenario plans | Not a generic workflow engine. |
| Scenarios | In pilot only if Phase 2 approved | Workspace/warehouse access plus approval roles | Approved catalog/inventory/order test path | Execution creates live order side effects. |
| Runtime | In pilot as trust/readiness visibility | Workspace access | Runtime API | Read-only evidence, not configuration. |
| Settings | Internal/admin only | `TENANT_ADMIN` | Access admin APIs | High-impact controls; not ordinary operator surface. |
| Platform admin/tenant management | SynapseCore internal only | Platform/admin credentials | Provisioning controls | Not customer self-service. |

## Customer-Visible vs Internal

| Area | Classification |
| --- | --- |
| Dashboard, Orders, Inventory, Catalog, Alerts, Recommendations | Customer operator visible when in pilot scope |
| Integrations, Replay | Customer operator/admin visible only for approved users |
| Approvals, Scenarios | Customer visible only if Phase 2 placed them in scope |
| Runtime | Customer/admin visible as trust/readiness surface if approved for pilot training |
| Settings, Users, Profile | Customer admin visible for approved admins; ordinary operators should not configure access/settings |
| Tenant management, platform provisioning, bootstrap/admin tokens | SynapseCore internal only |
| Infrastructure secrets, database credentials, Render controls | SynapseCore internal only |

## Known Operating Conditions

Relevant Company 1 limitations:

- provider-level Render restore evidence was documented as limited; application-level backup/restore proof exists
- no MFA/SSO/invitation flow
- no customer forgot-password flow
- no webhook HMAC signature verification; connector token support exists
- no arbitrary connector mapping engine
- only connector types are `WEBHOOK_ORDER` and `CSV_ORDER_IMPORT`
- mapping version is fixed to `1`
- no inventory CSV import
- no per-import rollback
- no automatic retention cleanup control
- no tenant delete/deactivate endpoint for routine pilot operations
- no formal read-only role
- no connector-specific operator scope model
- no notification recipient model for alerts
- no recommendation suppression/dismissal
- no generic approval workflow builder
- scenario execution creates live order side effects and must be treated as high-impact
- separation of duty is partly enforced for escalated scenarios and partly procedural through role assignment

## Pilot Blocker Review

| Finding | Classification | Reason |
| --- | --- | --- |
| No alert recipient/routing configuration | `OPERATING CONDITION` | Does not block in-app pilot operation; do not promise external notifications. |
| No recommendation dismissal/suppression | `OPERATING CONDITION` | Recommendations remain visible decision support. |
| No programmable replay policy | `OPERATING CONDITION` | Replay is still safe if manual rules are followed. |
| No universal separation-of-duty engine | `OPERATING CONDITION` | Escalated scenarios enforce key separation; role assignment record must enforce the rest. |
| No connector-specific scopes | `OPERATING CONDITION` | Use warehouse scopes and role separation. |
| Cross-tenant mutation | `BLOCKER` if discovered | Must stop pilot until fixed. |
| Unsafe replay causing duplicate/wrong tenant data | `BLOCKER` if discovered | Must stop pilot until fixed. |
| Hidden runtime/readiness failure | `BLOCKER` if discovered | Must restore failure honesty before handover. |

## Company Readiness Script Applicability

`scripts/verify-company-readiness.ps1` is a local/self-host rehearsal script. It is useful evidence for supported flows, but it is not Company 1 live pre-handover verification by itself.

Ready to verify in Phase 8:

- frontend route availability
- authenticated tenant session
- workspace/security/admin surfaces
- users/operators
- connectors
- webhook/CSV integration behavior
- replay flow
- catalog/inventory/order interactions
- alerts and recommendations
- scenario approval/execution paths
- runtime/trust surfaces
- rate/security checks

Not covered by script:

- real Company 1 business authorization
- actual customer data reconciliation
- secure credential handover
- customer sign-off
- provider-level backup restore
- all manual operating conditions

Manual check required:

- completed configuration record
- Company 1 pilot feature scope
- separation-of-duty role assignment
- connector policy baseline
- replay stop-condition acknowledgement
- scenario in/out decision
- customer-visible/internal classification

## Phase 7 Verification Gate

Before authorizing Phase 8, confirm:

- tenant configuration understood
- user/operator scopes understood
- approved roles aligned
- alerts capability mapped
- recommendations capability mapped
- replay operating rules defined
- approvals/governance mapped
- scenarios explicitly in or out of scope
- integration policies verified and recorded
- settings verified and recorded
- realtime expectations mapped
- Runtime/trust behavior understood
- cross-tenant configuration test defined for Phase 8
- operating limitations documented
- baseline configuration record ready
- pilot feature scope frozen
- no secrets in configuration docs
- no unresolved critical pilot blocker
- no unresolved high pilot blocker

## Phase 8 Handoff

Hand Phase 8:

- tenant provisioning evidence
- user provisioning evidence
- role matrix
- connector evidence
- data onboarding evidence
- reconciliation results
- operational configuration baseline
- alert configuration
- recommendation configuration
- replay rules
- governance matrix
- scenario scope
- integration policies
- operator scopes
- realtime expectations
- known limitations
- Company 1 pilot feature matrix

Do not perform customer handover in Phase 7.

## Phase 7 Verdict Guidance

Use `COMPANY PILOT PHASE 7 ACCEPTED` only when all operational configuration is supported, recorded, and free of material limitations.

Use `COMPANY PILOT PHASE 7 ACCEPTED WITH DOCUMENTED LIMITATION` when the current product can be operated safely for Company 1 but limitations such as no notification recipients, no generic rule engine, no connector scopes, no recommendation dismissal, and partial procedural separation-of-duty remain visible.

Use `COMPANY PILOT PHASE 7 NOT ACCEPTED - SAFE OPERATIONAL CONFIGURATION INCOMPLETE` if any critical blocker remains open, especially tenant leakage, unsafe replay, wrong governance authority, hidden failure, destructive wrong action, or uncontrolled connector mutation.
