# API Spec

## Orders

### `POST /api/orders`

Create and process a new incoming order.

Example body:

```json
{
  "externalOrderId": "ERP-10021",
  "warehouseCode": "WH-NORTH",
  "items": [
    {
      "productSku": "SKU-FLX-100",
      "quantity": 4,
      "unitPrice": 95.00
    }
  ]
}
```

### `GET /api/orders/recent`

Returns the most recent processed orders for the dashboard feed.

## Fulfillment

### `GET /api/fulfillment`

Returns the tenant-scoped fulfillment overview used by the control center.

The response includes:

- `backlogCount`
- `overdueDispatchCount`
- `delayedShipmentCount`
- `atRiskCount`
- `activeFulfillments`
  - `externalOrderId`
  - `orderStatus`
  - `fulfillmentStatus`
  - `warehouseCode`
  - `warehouseName`
  - `carrier`
  - `trackingReference`
  - `promisedDispatchAt`
  - `expectedDeliveryAt`
  - `backlogGrowthPerHour`
  - `estimatedBacklogClearHours`
  - `hoursUntilDispatchDue`
  - `hoursUntilDeliveryDue`
  - `backlogRisk`
  - `deliveryDelayRisk`
  - `anomalyDetected`
  - `riskLevel`
  - `impactSummary`

### `POST /api/fulfillment/updates`

Moves an order through the fulfillment or delivery lane and re-runs logistics risk monitoring.

Example body:

```json
{
  "externalOrderId": "FULFILL-1001",
  "status": "DELAYED",
  "carrier": "Synapse Courier",
  "trackingReference": "TRK-FULFILL-1001",
  "expectedDeliveryAt": "2026-04-08T10:00:00Z",
  "note": "Carrier lane is running behind the expected handoff."
}
```

Typical statuses:

- `QUEUED`
- `PICKING`
- `PACKED`
- `DISPATCHED`
- `DELAYED`
- `DELIVERED`
- `EXCEPTION`

Each update can change:

- the order’s coarse operational status
- fulfillment backlog pressure
- delivery-delay risk
- fulfillment anomaly detection
- alerts, recommendations, business events, audit logs, and realtime dashboard state

## Integrations

### `POST /api/integrations/orders/webhook`

Receives an external order payload from another system and feeds it into the real SynapseCore order ingestion flow.

Authentication:

- connector-authenticated machine ingress may send `X-Synapse-Connector-Token`
- when the connector token header is present, SynapseCore authenticates the configured connector directly and does not require a signed-in workspace session
- when the connector token header is absent, the existing signed-in workspace or header-fallback access rules still apply

Example body:

```json
{
  "sourceSystem": "erp_north",
  "externalOrderId": "ERP-EXT-1001",
  "warehouseCode": "WH-NORTH",
  "customerReference": "CUST-778",
  "occurredAt": "2026-04-01T09:30:00Z",
  "items": [
    {
      "productSku": "SKU-FLX-100",
      "quantity": 3,
      "unitPrice": 95.00
    }
  ]
}
```

Returns:

- external source system name
- normalized SynapseCore ingestion source
- acceptance timestamp
- created internal order response

Connector policy behavior:

- `transformationPolicy=NORMALIZE_CODES` uppercases warehouse and product codes before order ingestion
- `allowDefaultWarehouseFallback=true` allows blank inbound warehouse codes to fall back to the connector default warehouse
- `validationPolicy=STRICT` enforces tighter rules such as matching the configured default warehouse and rejecting duplicate product lines

## Operator Session And Header Fallback

SynapseCore uses a lightweight signed-in user session for protected control-center actions. The frontend signs in with a real tenant username and password, the backend resolves that user to a mapped operator, and the session cookie is then used on later protected requests.

Header-based access remains available as a fallback for tests, scripts, and non-UI operational tooling. It is enabled by default in the `dev` profile and disabled by default in the `prod` profile.

### `GET /api/auth/session`

Returns the current signed-in user session, if one exists.

The response includes:

- signedIn
- tenantCode
- tenantName
- username
- actorName
- displayName
- roles
- warehouseScopes
- authenticatedAt
- sessionExpiresAt
- passwordExpiresAt
- passwordChangeRequired
- passwordRotationRequired
- sessionTimeoutMinutes
- passwordRotationDays

### `POST /api/auth/session/login`

Signs in as a known active user account that is mapped to an operator in the tenant access directory.

Example body:

```json
{
  "tenantCode": "STARTER-OPS",
  "username": "integration.lead",
  "password": "integration-admin-2026"
}
```

### `POST /api/auth/session/password`

Rotates the password for the currently signed-in user session.

Example body:

```json
{
  "currentPassword": "lead-2026",
  "newPassword": "lead-2026-rotated"
}
```

### `POST /api/auth/session/logout`

Ends the current signed-in user session.

The development environment can include credential-backed local starter accounts for testing. Common local examples are:

- tenant `STARTER-OPS` -> `Starter Operations Workspace`
- `operations.lead` / `lead-2026` -> `Operations Lead`
- `naledi.lead` / `naledi-2026` -> `Naledi Lead`
- `integration.lead` / `integration-admin-2026` -> `Integration Lead`
- `integration.operator` / `integration-ops-2026` -> `Integration Operator`

### Header fallback

Headers:

- `X-Synapse-Actor`: acting operator name
- `X-Synapse-Tenant`: optional acting tenant code for header-fallback tooling
- `X-Synapse-Roles`: comma-separated control roles
- the actor must be a known active operator from `GET /api/access/operators`
- declared header roles must belong to that operator

This fallback is intended for development and operational tooling. Deployed environments should use signed-in sessions for protected actions.

Current control roles:

- `TENANT_ADMIN`
- `REVIEW_OWNER`
- `FINAL_APPROVER`
- `ESCALATION_OWNER`
- `INTEGRATION_ADMIN`
- `INTEGRATION_OPERATOR`

### `GET /api/access/tenants`

Returns the active tenant directory used by the control-center sign-in flow.

Each tenant includes:

- code
- name
- description
- active
- createdAt
- updatedAt

### `POST /api/access/tenants`

Creates a new tenant workspace with bootstrap operators, users, and warehouses.

Production access rules:
- First tenant on an empty production database: send `X-Synapse-Bootstrap-Token`.
- Later production tenant provisioning: send `X-Synapse-Platform-Admin-Token`.
- Signed-in tenant-admin sessions are not allowed to create additional tenant workspaces in production.
- Starter inventory and starter connectors are only created when the environment explicitly enables development onboarding seeding.

Example body:

```json
{
  "tenantCode": "ACME-OPS",
  "tenantName": "Acme Operations",
  "description": "Starter workspace for a new operating team",
  "adminFullName": "Amina Dlamini",
  "adminUsername": "amina.admin",
  "adminPassword": "launchpad-2026",
  "primaryLocation": "Johannesburg",
  "secondaryLocation": "Cape Town"
}
```

Access:

- requires a signed-in user session whose mapped operator has `TENANT_ADMIN`
- or, when header fallback is enabled, fallback headers with `X-Synapse-Roles` containing `TENANT_ADMIN`

Returns:

- tenantId
- tenantCode
- tenantName
- adminUsername
- adminActorName
- executiveUsername
- executiveActorName
- starterWarehouseCodes
- createdAt

### `GET /api/access/operators`

Returns the active operator directory used by the control center for protected review, connector, and replay actions.

Optional query params:

- `tenantCode`

Each operator includes:

- tenantCode
- tenantName
- actorName
- displayName
- roles
- warehouseScopes
- active
- description
- createdAt
- updatedAt

### `GET /api/access/admin/workspace`

Returns the current tenant workspace settings, support summary, support diagnostics, active tenant incidents, recent support activity, warehouses, and connector support ownership.

The response includes:

- tenantCode
- tenantName
- description
- active
- securitySettings
  - passwordRotationDays
  - sessionTimeoutMinutes
  - securityPolicyVersion
- supportSummary
  - warehouseCount
  - activeOperatorCount
  - inactiveOperatorCount
  - activeUserCount
  - inactiveUserCount
  - enabledConnectorCount
  - disabledConnectorCount
  - replayQueueDepth
  - pendingApprovalCount
  - activeIncidentCount
- supportDiagnostics
  - activeUsersRequiringPasswordChange
  - activeUsersPastPasswordRotation
  - activeUsersBlockedByInactiveOperator
  - connectorsWithoutSupportOwner
  - highSeverityIncidentCount
  - latestSupportAuditAt
- supportIncidents
  - incidentKey
  - type
  - severity
  - title
  - detail
  - context
  - actionRequired
  - createdAt
- recentSupportActivity
  - id
  - category
  - action
  - title
  - actor
  - status
  - targetRef
  - details
  - requestId
  - createdAt
- warehouses
- connectors

### `PUT /api/access/admin/workspace`

Updates the current tenant workspace metadata.

Example body:

```json
{
  "tenantName": "Synapse Demo Company Updated",
  "description": "Tenant admin support lane updated through workspace settings."
}
```

### `PUT /api/access/admin/workspace/security`

Updates the current tenant security policy for password rotation and session timeout. Tenant admins can also invalidate other active tenant sessions when they need a forced policy refresh.

Example body:

```json
{
  "passwordRotationDays": 45,
  "sessionTimeoutMinutes": 120,
  "invalidateOtherSessions": true
}
```

### `PUT /api/access/admin/workspace/warehouses/{warehouseId}`

Updates the current tenant warehouse display metadata.

Example body:

```json
{
  "name": "Warehouse North Prime",
  "location": "Johannesburg Prime"
}
```

### `PUT /api/access/admin/workspace/connectors/{connectorId}`

Updates connector support ownership and support notes for the current tenant.

Example body:

```json
{
  "supportOwnerActorName": "North Operations Director",
  "syncMode": "SCHEDULED_PULL",
  "syncIntervalMinutes": 15,
  "pullEndpointUrl": "https://company.example.com/orders-feed",
  "validationPolicy": "STRICT",
  "transformationPolicy": "NORMALIZE_CODES",
  "allowDefaultWarehouseFallback": true,
  "notes": "North operations director now owns webhook support."
}
```

Note: `SCHEDULED_PULL` is supported for `WEBHOOK_ORDER` order API feeds with an absolute HTTP(S) `pullEndpointUrl`. Unsupported connector types remain blocked.

### `GET /api/access/admin/operators`

Returns all operators in the current tenant, including inactive ones, for tenant-admin management.

Access:

- requires a signed-in user session whose mapped operator has `TENANT_ADMIN`
- or, when header fallback is enabled, fallback headers with `X-Synapse-Roles` containing `TENANT_ADMIN`

### `POST /api/access/admin/operators`

Creates a new tenant-scoped operator lane.

Example body:

```json
{
  "actorName": "North Review Manager",
  "displayName": "North Review Manager",
  "description": "Warehouse-scoped review owner for north operations",
  "active": true,
  "roles": ["REVIEW_OWNER"],
  "warehouseScopes": ["WH-NORTH"]
}
```

### `PUT /api/access/admin/operators/{operatorId}`

Updates an existing current-tenant operator. Tenant admins cannot remove the last active `TENANT_ADMIN` from the workspace.

### `GET /api/access/admin/users`

Returns all tenant user accounts in the current tenant.

Each user includes:

- tenantCode
- tenantName
- username
- fullName
- operatorActorName
- operatorDisplayName
- roles
- warehouseScopes
- active
- passwordChangeRequired
- passwordUpdatedAt
- createdAt
- updatedAt

### `POST /api/access/admin/users`

Creates a new current-tenant user account mapped to an existing active operator.

Example body:

```json
{
  "username": "north.review.manager",
  "fullName": "North Review Manager",
  "password": "north-lane-2026",
  "operatorActorName": "North Review Manager"
}
```

### `PUT /api/access/admin/users/{userId}`

Updates an existing current-tenant user account.

Example body:

```json
{
  "fullName": "North Review Manager",
  "active": true,
  "operatorActorName": "North Final Manager"
}
```

### `POST /api/access/admin/users/{userId}/reset-password`

Resets the password for an existing current-tenant user account.

Resetting a tenant user password also marks the account for required password rotation at next sign-in and invalidates that user’s older session version.

Example body:

```json
{
  "password": "north-reset-2026"
}
```

Warehouse scope rules:

- operators may be assigned to one or more warehouse codes
- an empty `warehouseScopes` list means the operator can act across the full tenant
- scoped operators only see and act on the warehouses in their assigned lane for warehouse directory, scenario review lanes, replay queue, and connector rows with a default warehouse
- connector support owners must be active operators in the current tenant and, when a connector is pinned to a default warehouse, the owner must have access to that warehouse lane

## Runtime Trust

### `GET /api/system/runtime`

Returns a safe runtime/trust snapshot for the control center.

The response includes:

- applicationName
- build
  - version
  - commit
  - builtAt
- activeProfiles
- overallStatus
- livenessState
- readinessState
- headerFallbackEnabled
- secureSessionCookies
- allowedOrigins
- telemetry
  - disabledConnectorCount
  - replayQueueDepth
  - recentImportIssues
  - recentAuditFailures
  - activeAlertCount
  - fulfillmentBacklogCount
  - delayedFulfillmentCount
  - dispatchQueueDepth
  - failedDispatchCount
- backbone
  - pendingDispatchCount
  - failedDispatchCount
  - oldestPendingAgeSeconds
  - latestProcessedAt
  - dispatchIntervalMs
  - batchSize
- metrics
  - ordersIngested
  - fulfillmentUpdates
  - integrationImportRuns
  - replayAttempts
  - dispatchQueued
  - dispatchProcessed
  - dispatchFailures
- diagnostics
  - windowHours
  - businessEventsInWindow
  - orderEventsInWindow
  - inventorySignalsInWindow
  - integrationEventsInWindow
  - scenarioEventsInWindow
  - failureAuditsInWindow
  - activeIncidentCount
  - latestBusinessEventAt
  - latestFailureAt
- connectorDiagnostics
  - sourceSystem
  - connectorType
  - displayName
  - healthStatus
  - healthSummary
  - lastFailureCode
  - lastFailureMessage
  - lastFailureAt
  - pendingReplayCount
  - deadLetterCount
  - oldestPendingReplayAgeSeconds
- observedAt

### `GET /api/system/incidents`

Returns the active system incident inbox used by the control center.

The response includes derived incident cards for:

- audit failures
- replay backlog
- disabled connectors
- degraded connectors
- failed operational dispatch work
- action-required control notices such as SLA escalations

Each incident includes:

- incidentKey
- type
- severity
- title
- detail
- context
- actionRequired
- createdAt

Connector-related incidents now surface the most recent connector failure detail when available, plus replay-wait context for degraded lanes.

### `GET /actuator/prometheus`

Returns Prometheus-format metrics for external scraping and deeper observability.

Important metric families include:

- `synapsecore_alerts_active`
- `synapsecore_fulfillment_backlog`
- `synapsecore_fulfillment_delayed`
- `synapsecore_integration_replay_backlog`
- `synapsecore_dispatch_queue_backlog`
- `synapsecore_dispatch_queue_failed`
- `synapsecore_orders_ingested_total`
- `synapsecore_fulfillment_updates_total`
- `synapsecore_integration_import_runs_total`
- `synapsecore_integration_replay_attempts_total`
- `synapsecore_dispatch_queued_total`
- `synapsecore_dispatch_processed_total`
- `synapsecore_dispatch_failed_total`

### `POST /api/integrations/orders/csv-import`

Receives a multipart CSV file and groups rows into real orders before sending each accepted order through the normal SynapseCore order ingestion flow.

Multipart fields:

- `file`
- optional `sourceSystem`

Authentication:

- connector-authenticated machine ingress may send `X-Synapse-Connector-Token`
- connector-authenticated CSV imports must also send the `sourceSystem` request parameter
- when the connector token header is absent, the existing signed-in workspace or header-fallback access rules still apply

Expected CSV header columns:

- `externalOrderId`
- `warehouseCode`
- `productSku`
- `quantity`
- `unitPrice`
- optional `sourceSystem`

Rows are grouped by:

- `sourceSystem`
- `externalOrderId`
- `warehouseCode`

Returns:

- default source system used for rows without `sourceSystem`
- total CSV rows received
- number of imported orders
- number of failed orders
- imported order summaries including the created internal order response
- failed order summaries including row numbers, `failureCode`, and the validation or ingestion failure

Connector policy behavior:

- `transformationPolicy=NORMALIZE_CODES` uppercases CSV warehouse and product codes before grouping and ingestion
- `validationPolicy=RELAXED` consolidates duplicate product lines inside the same grouped external order before the live order is created
- CSV rows may leave `warehouseCode` blank only when the connector allows default-warehouse fallback and has a default warehouse configured

### `GET /api/integrations/orders/connectors`

Returns the currently configured inbound connectors.

Each connector includes:

- sourceSystem
- type
- displayName
- enabled
- syncMode
- syncIntervalMinutes
- validationPolicy
- transformationPolicy
- mappingVersion
- allowDefaultWarehouseFallback
- defaultWarehouseCode
- notes
- supportOwnerActorName
- supportOwnerDisplayName
- inboundAccessConfigured
- inboundAccessTokenHint
- healthStatus (`LIVE`, `DEGRADED`, `OFFLINE`)
- healthSummary
- lastActivityAt
- lastSuccessfulActivityAt
- lastImportStatus
- lastImportAt
- recentInboundFailureCount
- pendingReplayCount
- deadLetterCount
- lastFailureCode
- lastFailureMessage
- lastFailureAt
- oldestPendingReplayAt
- oldestPendingReplayAgeSeconds
- createdAt
- updatedAt

### `POST /api/integrations/orders/connectors`

Creates or updates an inbound connector configuration.

The request contains:

- sourceSystem
- type
- displayName
- enabled
- syncMode
- syncIntervalMinutes
- validationPolicy
- transformationPolicy
- `mappingVersion` (currently only `1` is supported)
- allowDefaultWarehouseFallback
- defaultWarehouseCode
- notes
- optional `inboundAccessToken` to set or rotate connector-authenticated ingress
- optional `clearInboundAccessToken=true` to remove connector-authenticated ingress for that connector

Access:

- requires a signed-in user session whose mapped operator has `INTEGRATION_ADMIN`
- or, when header fallback is enabled, fallback headers with `X-Synapse-Roles` containing `INTEGRATION_ADMIN`

### `GET /api/integrations/orders/imports/recent`

Returns recent integration run history for webhook and CSV ingress.

Each run includes:

- sourceSystem
- connectorType
- fileName
- recordsReceived
- ordersImported
- ordersFailed
- status
- summary
- createdAt

### `GET /api/integrations/orders/replay-queue`

Returns unresolved failed inbound orders that can be replayed after the operational problem is fixed.

Each replay record includes:

- sourceSystem
- connectorType
- externalOrderId
- warehouseCode
- failureCode
- failureMessage
- status
- replayAttemptCount
- lastReplayMessage
- lastAttemptedAt
- createdAt

### `POST /api/integrations/orders/replay/{replayRecordId}`

Replays one stored failed inbound order through the normal SynapseCore order ingestion flow.

Returns:

- replay record with updated replay status
- created internal order response when the replay succeeds
- replay timestamp

Access:

- requires a signed-in user session whose mapped operator has `INTEGRATION_OPERATOR` or `INTEGRATION_ADMIN`
- or, when header fallback is enabled, fallback headers with `X-Synapse-Roles` containing `INTEGRATION_OPERATOR` or `INTEGRATION_ADMIN`

## Inventory

### `POST /api/inventory/update`

Create or update inventory quantity and reorder threshold.

Example body:

```json
{
  "productSku": "SKU-FLX-100",
  "warehouseCode": "WH-NORTH",
  "quantityAvailable": 18,
  "reorderThreshold": 20
}
```

### `GET /api/inventory`

Returns live inventory posture with:

- available quantity
- threshold
- low-stock state
- risk level
- units per hour
- estimated time to stockout

## Products

### `GET /api/products`

Returns tenant-owned product catalog definitions for inventory, orders, scenarios, and ingestion tooling.

### `POST /api/products`

Tenant-admin only. Creates a product in the current tenant catalog.

```json
{
  "sku": "SKU-ACME-100",
  "name": "Acme Sensor Module",
  "category": "Sensors"
}
```

Rules:

- SKU is unique inside the tenant catalog.
- SKU is normalized to uppercase.
- Another tenant may use the same visible SKU without cross-tenant collision.

### `PUT /api/products/{productId}`

Tenant-admin only. Updates a product owned by the current tenant. Cross-tenant product IDs are not visible or mutable.

### `POST /api/products/import`

Tenant-admin only. Imports product catalog data from `multipart/form-data` with a `file` part.

Required CSV headers:

- `sku`
- `name`
- `category`

The import is tenant-scoped, creates new SKUs, updates existing tenant SKUs, rejects duplicate rows inside the same file, and returns row-level success/failure results.

## Warehouses

### `GET /api/warehouses`

Returns the configured warehouse locations available to the platform.

## Events

### `GET /api/events/recent`

Returns the most recent business events so the operational loop is easy to inspect during operations and debugging.

## Dashboard

### `GET /api/dashboard/summary`

Returns the top-level dashboard metrics:

- total orders
- active alerts
- low-stock items
- recommendations count
- total products
- total warehouses
- recent order count
- inventory records count
- last updated at

### `GET /api/dashboard/snapshot`

Returns the full dashboard payload in one REST response for debugging or future consumers:

- summary
- alerts
- recommendations
- inventory view
- recent orders
- recent business events
- recent audit logs
- integration connectors
- recent integration import runs
- integration replay queue
- active SLA escalation inbox
- recent scenario history

## Scenarios

### `POST /api/scenarios/order-impact`

Runs a non-persistent what-if analysis for a proposed order or order mix.

The request shape matches `POST /api/orders`, including multi-line `items`, but SynapseCore does not create an order or deduct real inventory. Instead it returns:

- projected order value
- projected impacted inventory posture
- projected alerts
- projected recommendations
- analysis timestamp

### `POST /api/scenarios/order-impact/compare`

Runs two non-persistent order-impact analyses and returns:

- primary scenario result
- alternative scenario result
- risk-score comparison
- recommended option
- human-readable rationale

### `POST /api/scenarios/save`

Saves Scenario A as a named loadable plan for later reuse. Saved plans begin in `PENDING_APPROVAL` and become executable only after approval. When `revisionOfScenarioRunId` is supplied, the save call creates a new revision of a rejected saved plan.

The request contains:

- title
- requestedBy
- reviewOwner
- revisionOfScenarioRunId
- request

The response contains:

- scenarioRunId
- type
- title
- warehouseCode
- requestedBy
- reviewOwner
- finalApprovalOwner
- reviewPriority
- riskScore
- approvalPolicy
- approvalStage
- approvalDueAt
- slaEscalatedTo
- slaEscalatedAt
- slaEscalated
- overdue
- revisionOfScenarioRunId
- revisionNumber
- executable
- approvalStatus
- savedAt

### `GET /api/scenarios/history`

Returns the most recent planning runs so operators can see what has already been explored in the control center.

Optional query parameters:

- `type`
- `approvalStatus`
- `approvalPolicy`
- `approvalStage`
- `warehouseCode`
- `requestedBy`
- `reviewOwner`
- `finalApprovalOwner`
- `minimumReviewPriority`
- `overdueOnly`
- `slaEscalatedOnly`
- `limit`

Each history item contains:

- type
- title
- summary
- recommendedOption
- warehouseCode
- revisionOfScenarioRunId
- revisionNumber
- loadable
- executable
- approvalStatus
- approvalPolicy
- approvalStage
- approvalDueAt
- slaEscalatedTo
- slaEscalatedAt
- slaEscalated
- overdue
- slaAcknowledgedBy
- slaAcknowledgedAt
- slaAcknowledgementNote
- slaAcknowledged
- requestedBy
- reviewOwner
- finalApprovalOwner
- reviewPriority
- riskScore
- reviewApprovedBy
- reviewApprovedAt
- reviewApprovalNote
- approvedBy
- approvalNote
- approvedAt
- rejectedBy
- rejectedAt
- rejectionReason
- createdAt

Dashboard snapshot also includes:

- `scenarioNotifications`, which returns recent operational notices for SLA-rerouted saved plans and formal ownership acknowledgments
- `slaEscalations`, which returns the active unacknowledged pending saved plans that were auto-rerouted by SLA escalation policy

### `GET /api/scenarios/notifications`

Returns the dedicated notification feed for scenario-review operations.

Each notification includes:

- scenarioRunId
- type
- title
- message
- warehouseCode
- reviewPriority
- approvalStage
- actor
- note
- actionRequired
- dueAt
- createdAt

### `GET /api/scenarios/{scenarioRunId}/request`

Returns the stored preview or saved-plan request so the planner can reload it for refinement or rerun.

This endpoint returns:

- scenarioRunId
- scenarioTitle
- request
- loadedAt

Only preview scenarios and saved plans with stored request payloads can be loaded this way.

### `POST /api/scenarios/{scenarioRunId}/approve`

Approves a saved plan so it can be executed into the live order flow.

The request contains:

- actorRole
- approverName
- approvalNote for staged escalated approvals

Access:

- requires a signed-in user session whose mapped operator includes the same control role declared in `actorRole`
- or, when header fallback is enabled, fallback headers with `X-Synapse-Roles` containing that same control role

The response contains:

- scenarioRunId
- title
- approvalStatus
- approvalPolicy
- approvalStage
- approvalDueAt
- slaEscalatedTo
- slaEscalatedAt
- slaEscalated
- overdue
- reviewApprovedBy
- reviewApprovedAt
- approvedBy
- approvalNote
- approvedAt
- executionReady

Rejected saved plans cannot be approved in place again. Operators should reload them into the planner, adjust the proposal, and save a new revision-linked plan. Critical saved plans use `ESCALATED` approval policy and staged approval:

- owner review must happen first
- owner review requires `actorRole=REVIEW_OWNER`, the assigned review owner, a distinct reviewer from the requester, and an approval note
- final approval is then auto-routed to an assigned final approval owner
- final approval requires `actorRole=FINAL_APPROVER`, and the final approver must match that assigned final approval owner while still being different from both the requester and the owner reviewer
- each pending approval stage carries its own due time, and `overdueOnly=true` can be used to pull an SLA-breached queue
- overdue critical plans in final approval are auto-rerouted once to an executive fallback approver and exposed through `slaEscalatedOnly=true`
- execution is only enabled once the plan reaches `APPROVED`

### `POST /api/scenarios/{scenarioRunId}/reject`

Rejects a saved plan and records why it should not move into live execution yet.

The request contains:

- actorRole
- reviewerName
- reason

Access:

- requires a signed-in user session whose mapped operator includes the same control role declared in `actorRole`
- or, when header fallback is enabled, fallback headers with `X-Synapse-Roles` containing that same control role

The response contains:

- scenarioRunId
- title
- approvalStatus
- rejectedBy
- rejectedAt
- rejectionReason

### `POST /api/scenarios/{scenarioRunId}/acknowledge-escalation`

Acknowledges an SLA-escalated pending saved plan so the issue becomes owned and leaves the live escalation inbox.

The request contains:

- actorRole
- acknowledgedBy
- note

Access:

- requires a signed-in user session whose mapped operator includes the same control role declared in `actorRole`
- or, when header fallback is enabled, fallback headers with `X-Synapse-Roles` containing that same control role

The response returns the scenario history shape, including:

- slaEscalated
- slaAcknowledged
- slaAcknowledgedBy
- slaAcknowledgedAt
- slaAcknowledgementNote

Acknowledgment requires `actorRole=ESCALATION_OWNER`. Acknowledged escalations remain visible through `GET /api/scenarios/notifications` and scenario history, but they no longer appear in the active SLA escalation inbox.

### `POST /api/scenarios/{scenarioRunId}/execute`

Promotes an executable preview scenario or approved saved plan into the live order flow.

This endpoint:

- loads the stored preview request
- hands it to the real order service
- deducts live inventory
- triggers intelligence, alerts, recommendations, audit logging, and realtime updates

Only preview scenarios and approved saved plans with stored request payloads are executable. Rejected saved plans remain loadable for refinement but cannot execute. Comparison history items remain advisory-only.

## Audit

### `GET /api/audit/recent`

Returns the most recent structured audit records for operational traceability.

Audit records contain:

- action
- actor
- source
- targetType
- targetRef
- status
- details
- requestId
- createdAt

## Alerts

### `GET /api/alerts`

Returns:

- active alerts
- recent alerts

Alerts contain:

- type
- severity
- title
- description
- impact summary
- recommended action
- status

The current alert set includes:

- `LOW_STOCK`
- `DEPLETION_RISK` for fast-moving inventory that may stock out soon even before it falls below threshold

## Recommendations

### `GET /api/recommendations`

Returns the most recent recommendations sorted newest first.

Current recommendation types include:

- `REORDER_STOCK`
- `REORDER_URGENTLY`
- `TRANSFER_STOCK` when another warehouse has enough surplus to cover the current shortfall

## Developer Support

### `POST /api/dev/reseed`

Development-only helper endpoint that:

- clears current orders, alerts, recommendations, and business events
- restores the local development baseline products, warehouses, and inventory records
- pushes a fresh live snapshot so the dashboard returns to a clean local baseline

## Realtime

### WebSocket endpoint

- connect to `/ws`

### STOMP topic

- subscribe to `/topic/tenant/{TENANT_CODE}/dashboard.summary`
- subscribe to `/topic/tenant/{TENANT_CODE}/alerts`
- subscribe to `/topic/tenant/{TENANT_CODE}/recommendations`
- subscribe to `/topic/tenant/{TENANT_CODE}/inventory`
- subscribe to `/topic/tenant/{TENANT_CODE}/orders.recent`
- subscribe to `/topic/tenant/{TENANT_CODE}/events.recent`
- subscribe to `/topic/tenant/{TENANT_CODE}/audit.recent`
- subscribe to `/topic/tenant/{TENANT_CODE}/system.incidents`
- subscribe to `/topic/tenant/{TENANT_CODE}/integrations.connectors`
- subscribe to `/topic/tenant/{TENANT_CODE}/integrations.imports`
- subscribe to `/topic/tenant/{TENANT_CODE}/integrations.replay`
- subscribe to `/topic/tenant/{TENANT_CODE}/scenarios.notifications`
- subscribe to `/topic/tenant/{TENANT_CODE}/scenarios.escalated`
SynapseCore uses focused tenant-scoped live topics so the UI receives operational changes by concern instead of one oversized shared stream.

## Traceability

State-changing and rejected API requests include an `X-Request-Id` response header.
Error responses also include `requestId` in the JSON body so backend actions can be traced across audit records.
