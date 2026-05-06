# Pilot Onboarding Pack

This pack is the real operator and rollout guide for bringing a new customer or business unit onto SynapseCore.

It is written for teams preparing a controlled pilot, not for demo traffic.

## Pilot Goal

A pilot is ready when the company can:

- sign in with real tenant accounts
- see real catalog, inventory, orders, runtime, incidents, and integrations
- recover failed inbound work through replay
- route approvals and escalations with the right role boundaries
- operate daily without relying on hidden seed data or manual database edits

## 1. Tenant Onboarding Flow

### Initial Workspace Creation

Use the correct production lane:

- bootstrap token only for the first tenant on an empty production database
- platform-admin token for later tenant provisioning

Required outputs:

- tenant code
- tenant name
- initial tenant admin account

### Workspace Definition

Before live proof or pilot traffic begins, define:

- warehouses
- operators
- warehouse scopes
- scenario review owners
- connector support owners

## 2. First Operator Setup

Minimum pilot roles:

- tenant admin
- planner or reviewer
- integration admin or operator

Each user should map to a real operator with the correct warehouse scope.

Operator setup should answer:

- who can approve or reject scenarios
- who can administer connectors
- who can replay failed inbound work
- who can see all warehouses versus a scoped warehouse lane

## 3. Catalog And Inventory Onboarding

Before the first live day:

- load the real or pilot catalog
- create warehouse inventory baselines
- confirm reorder thresholds reflect real operating posture
- verify at least one key proof SKU is present and visible in the tenant

Operational check:

- low-stock behavior should mean something real for the pilot, not just test data noise

## 4. Connector Onboarding

Current supported connector scope:

- webhook order ingestion
- CSV order import
- scheduled pull order ingestion

For each connector, define:

- `sourceSystem`
- connector type
- enabled or disabled state
- validation policy
- transformation policy
- default warehouse fallback only when the business truly needs it
- support owner

## 5. Hosted Proof Before Pilot Use

Do not call the pilot ready until the hosted proof passes using the real tenant.

Official sequence:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\prepare-hosted-proof.ps1
cd frontend
npm.cmd run test:e2e:prod
```

The proof verifies:

- auth and session behavior
- catalog onboarding
- realtime dashboard changes
- replay recovery
- scenario approval and role gating
- integrations, runtime, settings, and auth rate limiting

## 6. Replay And Recovery Operations

Operators should understand the live replay rules:

- a disabled connector CSV import returns `CONNECTOR_DISABLED`
- that failure creates a replay record immediately
- the record remains visible for manual recovery
- automated replay does not steal the record while the connector is still disabled
- once the connector is enabled, the operator can recover the order intentionally

Daily replay checks:

- replay queue depth
- oldest pending replay age
- disabled connectors that still have backlog
- dead-lettered or orphaned records

## 7. Runtime Monitoring

Operators should use these surfaces daily:

- `/api/system/runtime`
- `/api/system/incidents`
- integrations page
- replay queue
- dashboard

Things to watch:

- connector health
- replay backlog
- active incident count
- delayed fulfillment count
- low-stock or risk pressure
- broker mode and realtime trust posture

## 8. Operational Checklist

Before pilot go-live:

- tenant created through the correct production lane
- warehouses defined
- operators and users mapped
- catalog baseline loaded
- inventory baseline loaded
- connector ownership defined
- runtime and incident surfaces reachable
- replay queue reachable
- hosted proof green

## 9. Escalation Workflow

When something fails:

1. identify whether the failure is auth, connector, inventory, approval, or runtime driven
2. inspect runtime and incidents
3. inspect integrations and replay queue
4. decide whether the right fix is connector repair, manual replay, approval action, or deployment rollback
5. record the resolution path through audit and business-event surfaces

## 10. Daily Operations Checklist

- sign in and confirm dashboard loads
- check incidents and replay queue first
- check disabled connectors are intentional
- review active alerts and recommendations
- confirm low-stock or delay signals reflect real work, not stale data
- verify new inbound orders are landing cleanly
- verify auth remains healthy and wrong-password rejection is still fast

## 11. Incident Handling Examples

### Connector Disabled Before Inbound CSV Window

- What happens: CSV import returns `CONNECTOR_DISABLED`
- What SynapseCore changes: the failed order becomes a visible replay record
- What the operator does: repair connector, enable connector, replay manually

### Dashboard Shows Reconnecting

- What happens: realtime path is degraded
- What SynapseCore changes: runtime and frontend state make the degradation visible instead of hiding it
- What the operator does: check broker mode, Redis availability, and current incident posture

### Inventory Risk Spikes

- What happens: warehouse pressure changes faster than spreadsheets can explain
- What SynapseCore changes: dashboard, alerts, and recommendations move live
- What the operator does: review scenario options and route approval if a risky move is required

## 12. Final Pilot Readiness Rule

A pilot is not ready just because the app loads.

A pilot is ready when:

- the tenant is real
- the users are real
- the connectors are real
- the replay path is real
- the runtime path is real
- the hosted proof is green
