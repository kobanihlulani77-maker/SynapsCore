# Onboarding Playbook

This playbook describes the real onboarding path for a new company or tenant in SynapseCore.

## 1. Create The Workspace

Production creation lanes are strict:

- use the bootstrap token only for the first tenant on an empty production database
- use the platform-admin token for later tenant provisioning

The output of this step should be:

- tenant code
- tenant name
- initial tenant admin

## 2. Define The Operating Footprint

Before live traffic starts, set up:

- warehouses
- operator lanes
- warehouse scopes
- approval ownership
- connector support ownership

Warehouse scopes matter because they drive what operators can see and act on in:

- replay queue
- scenario review
- connector rows with default warehouses
- warehouse-aware operational surfaces

## 3. Create Real Users

At minimum, a serious tenant should have:

- tenant admin
- planner or reviewer
- integration admin or operator

Each user should map to a real operator lane with the right warehouse scope.

## 4. Configure Real Inputs

For each connector or source system, define:

- source system name
- connector type
- validation policy
- transformation policy
- default warehouse fallback only when the business truly needs it

## 5. Verify The Trust Surfaces

Before first live use, confirm the tenant can load:

- dashboard
- runtime
- incidents
- integrations
- replay queue
- users and settings

## 6. Run The Hosted Proof

Use:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\prepare-hosted-proof.ps1
cd frontend
npm.cmd run test:e2e:prod
```

This proves:

- real auth
- real catalog
- realtime
- replay recovery
- scenario approval and role gating
- runtime, integrations, and rate limiting

## 7. Train The Team By Role

- operators: orders, inventory, fulfillment, alerts, replay
- reviewers and approvers: scenarios, approvals, escalations
- admins: users, settings, connectors, runtime trust surfaces

## 8. First Live Day Checklist

The company is ready for first live use when:

- the right people can sign in
- the right pages are visible by role
- connector ownership is clear
- disabled-connector failures produce replay records instead of silent gaps
- manual recovery works after connector repair
- dashboard, runtime, incidents, alerts, and replay views all load for the real tenant
