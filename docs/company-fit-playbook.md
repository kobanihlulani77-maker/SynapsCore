# Company Fit Playbook

This playbook explains where SynapseCore fits, what pains it solves, and how to discuss it in real operational language.

## Positioning Truth

SynapseCore should be positioned as an operations control platform for the lanes it actually implements today:

- tenant-safe access and role-gated operator lanes
- catalog onboarding and warehouse-aware inventory control
- order ingestion through webhook, CSV, and scheduled pull
- alerts, recommendations, approvals, and execution governance
- deterministic replay and recovery
- tenant-scoped realtime visibility
- runtime, incidents, audit, and event tracing

It should not be pitched as:

- a generic all-in-one ERP replacement
- a huge connector marketplace
- a magical industry-specific suite with features the repo does not implement

## What Good Fit Looks Like

SynapseCore is a strong fit when a company has:

- multiple operational systems feeding one fulfillment or control process
- warehouse, dispatch, procurement, or field teams making decisions from delayed or conflicting views
- recurring integration failures that currently disappear into spreadsheets, inboxes, or support queues
- approval-heavy changes that need traceability instead of informal chat decisions
- live operational pressure that benefits from tenant-safe realtime visibility

## Operational Pains SynapseCore Addresses

SynapseCore is strongest when the company is struggling with:

- failed inbound data that needs recovery, not just error logging
- inventory or order mismatch between business systems and warehouse reality
- weak ownership during approvals, escalations, and exception handling
- delayed visibility into connector failures, backlog growth, or low-stock risk
- fragmented runtime truth across dashboards, spreadsheets, and support channels

## Current Supported Company Types

The company-fit analyzer covers:

- logistics companies
- warehouses
- retail chains
- ecommerce fulfillment
- distributors
- manufacturers
- procurement-heavy businesses
- operations centers
- supply chain coordinators
- multi-warehouse businesses
- transport and fleet operations
- field operations
- enterprise admin teams

## Before Versus After SynapseCore

Before SynapseCore:

- operators reconcile spreadsheets, inboxes, portals, and exports
- integration failures are discovered late
- replay is manual re-entry instead of governed recovery
- approvals move through incomplete screenshots and chat threads
- inventory and order truth drifts across teams

After SynapseCore in its current supported scope:

- failed inbound lanes create replay records with visible ownership
- disabled connectors stay visible as recoverable operational issues
- manual replay and automated replay no longer fight for the same record
- dashboard, incidents, runtime, and audit surfaces create one trusted operating narrative
- realtime risk and alert changes reach the control center without manual refresh

## How To Use The Analyzer

Use the generator to produce grounded company-fit reports:

```powershell
node scripts\generate-company-fit-report.mjs --list
node scripts\generate-company-fit-report.mjs --company-type logistics-companies --format markdown
node scripts\generate-company-fit-report.mjs --company-type retail-chains,ecommerce-fulfillment --format html --output docs\generated\commerce-fit.html
node scripts\generate-company-fit-report.mjs --all --format markdown --output docs\generated\company-fit-report.md
```

The analyzer maps each company type to:

- real operational pain
- fragmented-system pattern
- delay and visibility failure points
- approval bottlenecks
- integration failure patterns
- inventory or order mismatch points
- replay and recovery value
- realtime value
- audit and tenant-isolation value
- metrics, dashboards, alerts, and ROI language

A generated HTML showcase is available at:

- [generated/company-fit-showcase.html](generated/company-fit-showcase.html)

## Most Important Rule

Keep the narrative grounded in the current supported product surface.

Strong positioning comes from honesty:

- narrow connector scope stated clearly
- replay and recovery explained precisely
- realtime benefits explained with current Render-backed proof
- approval, runtime, and audit value tied to actual implemented screens and APIs
