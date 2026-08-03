# Operations Manager Guide

This guide explains SynapseCore from the perspective of the person responsible for keeping daily operations moving.

## Your Job In SynapseCore

As an operations manager, SynapseCore helps you answer:

- what is happening right now?
- what is delayed, degraded, or risky?
- what failed and needs recovery?
- which recommendations deserve action?
- which scenarios need approval?
- which integrations are healthy?
- can the platform be trusted right now?

## Daily Command-Center Routine

Start with this loop:

```text
Sign in
-> confirm workspace
-> check runtime trust
-> review dashboard
-> inspect alerts and recommendations
-> review orders and inventory
-> check integrations
-> clear or escalate replay items
-> approve/reject scenarios
-> review audit/history
```

## What To Watch First

### Runtime

Runtime tells you whether the system is healthy enough to trust. If readiness, auth, websocket, DB, Redis, or connector status is degraded, treat the UI as operationally limited until the issue is classified.

### Dashboard

The dashboard is your command-center overview. It should show current operational state, not just historical reports.

### Alerts And Recommendations

Alerts indicate something needs attention. Recommendations help frame possible action.

Do not treat recommendations as automatic execution. They may lead to scenarios and approvals.

### Replay Queue

Replay is where failed inbound work becomes visible. This is one of the most important operator surfaces because it prevents failures from becoming silent manual cleanup.

### Approvals

Approvals are where governed actions wait for a human decision. Pending approvals should have ownership and urgency.

## How To Respond To Common States

| State | Meaning | Operations response |
| --- | --- | --- |
| Healthy runtime | System dependencies are responding | Continue normal operations |
| Reconnecting | Realtime is trying to recover | Avoid assuming live data is current until restored |
| Readiness failing | Backend or dependency may not be ready | Pause proof and escalate technical check |
| Replay pending | Failed inbound work is waiting | Review, classify, replay if safe |
| Approval blocked | Scenario needs decision or ownership | Assign reviewer or escalate |
| Connector degraded | External data path may be unreliable | Check integration status and replay impact |

## Manager Responsibilities

Operations managers should:

- keep replay queues from becoming invisible backlog
- ensure approvals have owners
- use runtime trust before making platform-wide assumptions
- distinguish true operational issues from infrastructure issues
- capture evidence during incidents
- avoid manual side channels when SynapseCore should own the operational record

## What SynapseCore Does Not Replace

SynapseCore does not replace every existing operational system.

It does not claim to be:

- a full ERP
- a full WMS
- a full transport system
- a complete BI warehouse
- a universal integration marketplace

It coordinates live operational truth across the supported surfaces.

## Successful Daily Use Looks Like

Success looks like:

- fewer hidden failures
- faster operational classification
- clearer approval ownership
- less manual reconciliation
- better visibility into inventory/order pressure
- more confidence in whether data is current and trustworthy
