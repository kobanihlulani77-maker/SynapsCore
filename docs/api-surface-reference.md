# API Surface Reference

This document provides a structured map of the current SynapseCore API surface.

It is meant to complement, not replace, the detailed payload and example coverage in [api-spec.md](api-spec.md).

Use this document when you want to understand:

- which API families exist
- which routes support which product surfaces
- which routes are proof-critical
- which routes are operationally sensitive

## How To Read This

- use this doc for domain-level orientation
- use [api-spec.md](api-spec.md) for detailed request and response examples
- use [backend-flow.md](backend-flow.md) and [data-flow-playbook.md](data-flow-playbook.md) for behavior and system context

## Auth And Session

Purpose:

- workspace sign-in
- session visibility
- password rotation
- logout

Main routes:

- `GET /api/auth/session`
- `POST /api/auth/session/login`
- `POST /api/auth/session/password`
- `POST /api/auth/session/logout`

Operational importance:

- critical

Proof-critical:

- yes

## Tenant Access And Workspace Administration

Purpose:

- tenant/workspace creation
- workspace settings
- operator and user administration
- warehouse and connector support ownership

Main route families:

- `/api/access/tenants`
- `/api/access/operators`
- `/api/access/admin/workspace`
- `/api/access/admin/operators`
- `/api/access/admin/users`

Operational importance:

- high

Production-sensitive:

- yes

## Dashboard And Runtime Trust

Purpose:

- dashboard summary and snapshot
- runtime trust and incidents
- service reachability

Main routes:

- `GET /`
- `GET /api/dashboard/summary`
- `GET /api/dashboard/snapshot`
- `GET /api/system/runtime`
- `GET /api/system/incidents`
- `GET /actuator/health`
- `GET /actuator/health/readiness`
- `GET /actuator/health/liveness`
- `GET /actuator/prometheus`

Operational importance:

- critical

Proof-critical:

- yes

## Catalog And Products

Purpose:

- tenant product catalog
- catalog import

Main routes:

- `GET /api/products`
- `POST /api/products`
- `PUT /api/products/{productId}`
- `POST /api/products/import`

Operational importance:

- high

Proof-critical:

- yes

## Warehouses

Purpose:

- warehouse directory and warehouse-aware operations context

Main route:

- `GET /api/warehouses`

Operational importance:

- medium to high

## Inventory

Purpose:

- live stock posture
- threshold and reorder visibility

Main routes:

- `GET /api/inventory`
- `POST /api/inventory/update`

Operational importance:

- critical

Proof-critical:

- yes

## Orders

Purpose:

- order ingestion and live order visibility

Main routes:

- `POST /api/orders`
- `GET /api/orders/recent`

Operational importance:

- critical

Proof-critical:

- yes

## Fulfillment

Purpose:

- fulfillment lane visibility and risk updates

Main routes:

- `GET /api/fulfillment`
- `POST /api/fulfillment/updates`

Operational importance:

- high

## Integrations

Purpose:

- inbound connector configuration
- webhook and CSV ingestion
- import run visibility
- replay queue and replay actions

Main routes:

- `POST /api/integrations/orders/webhook`
- `POST /api/integrations/orders/csv-import`
- `GET /api/integrations/orders/connectors`
- `POST /api/integrations/orders/connectors`
- `GET /api/integrations/orders/imports/recent`
- `GET /api/integrations/orders/replay-queue`
- `POST /api/integrations/orders/replay/{replayRecordId}`

Operational importance:

- critical

Proof-critical:

- yes

## Scenarios, Approvals, And Execution

Purpose:

- what-if planning
- saved scenarios
- approvals
- rejection
- escalation acknowledgment
- execution into live flow

Main routes:

- `POST /api/scenarios/order-impact`
- `POST /api/scenarios/order-impact/compare`
- `POST /api/scenarios/save`
- `GET /api/scenarios/history`
- `GET /api/scenarios/notifications`
- `GET /api/scenarios/{scenarioRunId}/request`
- `POST /api/scenarios/{scenarioRunId}/approve`
- `POST /api/scenarios/{scenarioRunId}/reject`
- `POST /api/scenarios/{scenarioRunId}/acknowledge-escalation`
- `POST /api/scenarios/{scenarioRunId}/execute`

Operational importance:

- critical

Proof-critical:

- yes

## Alerts And Recommendations

Purpose:

- risk visibility
- operator guidance

Main routes:

- `GET /api/alerts`
- `GET /api/recommendations`

Operational importance:

- high

## Events And Audit

Purpose:

- recent business events
- audit traceability

Main routes:

- `GET /api/events/recent`
- `GET /api/audit/recent`

Operational importance:

- high

## Realtime

Purpose:

- tenant-scoped live updates

Endpoints:

- websocket endpoint: `/ws`
- info endpoint: `/ws/info`

Topics include:

- dashboard summary
- alerts
- recommendations
- inventory
- recent orders
- recent events
- audit
- incidents
- integrations connectors/imports/replay
- scenario notifications and escalations

Operational importance:

- critical for command-center freshness

Proof-critical:

- yes

## Dev-Only Support

Purpose:

- reseed local development state

Route:

- `POST /api/dev/reseed`

Operational importance:

- development only

Production-sensitive:

- should not be exposed in production workflows

## Most Operationally Sensitive API Families

Treat these families with extra care:

- auth/session
- dashboard snapshot and runtime trust
- integrations and replay
- scenario approval and execution
- inventory and orders

These routes affect:

- proof truth
- operator trust
- deployment confidence

## Related Docs

- [api-spec.md](api-spec.md)
- [backend-flow.md](backend-flow.md)
- [system-communication-map.md](system-communication-map.md)
- [proof-and-validation.md](proof-and-validation.md)

## Bottom Line

The SynapseCore API surface is not a random set of CRUD routes.

It is a domain-shaped operational API built around:

- workspace trust
- command-center visibility
- replay recovery
- approval governance
- runtime truth
