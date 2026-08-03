# Business Process Library

This library describes how companies use SynapseCore in operational workflows.

The exact integrations and operating procedures can vary by company, but the product pattern remains consistent: operational work enters, gets validated, becomes visible, triggers guidance when needed, routes approvals when required, and remains recoverable through replay if it fails.

## Process 1: Inbound Order

```mermaid
flowchart TD
    A["Inbound order"] --> B["Integration or operator surface"]
    B --> C["Tenant workspace context"]
    C --> D{"Valid product and operational data?"}
    D -->|Yes| E["Persist order"]
    D -->|No| F["Failed inbound state"]
    F --> G["Replay queue"]
    G --> H["Operator review"]
    H --> I["Replay into live flow"]
    I --> D
    E --> J["Inventory impact visibility"]
    J --> K{"Operational risk?"}
    K -->|No| L["Dashboard update"]
    K -->|Yes| M["Alert or recommendation"]
    M --> N["Scenario if action is needed"]
    N --> O{"Approval required?"}
    O -->|Yes| P["Approval queue"]
    O -->|No| Q["Execution path"]
    P --> R{"Approved?"}
    R -->|Yes| Q
    R -->|No| S["Stop or revise"]
    Q --> T["Audit/history"]
    L --> T
    S --> T
```

Business result:

- orders do not disappear when inbound data is unhealthy
- inventory and order pressure become visible
- risky actions can require approval
- recovery is auditable

## Process 2: Inventory Pressure

Trigger examples:

- order demand increases
- warehouse stock becomes constrained
- product availability differs from operational expectation
- planner notices a stock issue

Flow:

```text
Inventory signal
-> dashboard or inventory page
-> alert/recommendation
-> scenario planning if action is needed
-> approval if action is governed
-> execution or rejection
-> audit and realtime visibility
```

What SynapseCore contributes:

- one workspace view of stock pressure
- recommendation and scenario framing
- controlled approval for risky response
- visible history after action

## Process 3: Failed Inbound Recovery

Trigger examples:

- connector disabled
- payload fails validation
- external data cannot be processed safely
- runtime dependency is degraded

Flow:

```mermaid
flowchart TD
    A["Inbound failure"] --> B["Replay queue"]
    B --> C["Operator sees failed item"]
    C --> D{"Safe to replay?"}
    D -->|No| E["Manual review or hold"]
    D -->|Yes| F["Replay into live flow"]
    F --> G["Validation"]
    G --> H{"Valid now?"}
    H -->|Yes| I["Persist and process"]
    H -->|No| B
    I --> J["Audit confirmation"]
    E --> J
```

What SynapseCore contributes:

- failed work remains visible
- replay is intentional, not hidden
- operators can distinguish waiting, failed, and recovered states
- proof can validate recovery behavior

## Process 4: Recommendation To Approval

Trigger examples:

- inventory pressure suggests action
- operational risk needs review
- scenario should not execute without approval

Flow:

```text
Signal
-> recommendation
-> scenario
-> approval queue
-> approve or reject
-> execute, terminate, or revise
-> audit
```

What SynapseCore contributes:

- guidance without unsafe auto-action
- role-aware operational governance
- decision traceability

## Process 5: Runtime Trust Check

Trigger examples:

- backend timeout
- readiness failure
- websocket reconnecting
- DB/Redis unavailable
- connector telemetry stale

Flow:

```mermaid
flowchart TD
    A["Runtime signal"] --> B{"Trust healthy?"}
    B -->|Yes| C["Operators continue"]
    B -->|No| D["Degraded state visible"]
    D --> E["Pause proof if affected"]
    D --> F["Classify dependency"]
    F --> G{"Recoverable by wait/redeploy/config?"}
    G -->|Wait| H["Warm-up or reconnect"]
    G -->|Redeploy| I["Controlled redeploy"]
    G -->|Config/dependency| J["Restore DB/Redis/backend dependency"]
    H --> K["Recheck readiness/auth/ws"]
    I --> K
    J --> K
    K --> B
```

What SynapseCore contributes:

- live product truth
- safer proof discipline
- fewer false assumptions from frontend-only availability

## Process 6: Operator Daily Control Loop

Daily operators use SynapseCore like this:

```text
Open command center
-> confirm runtime trust
-> inspect dashboard
-> review alerts and recommendations
-> inspect orders/inventory
-> review replay queue
-> act on approvals/scenarios
-> monitor integrations
-> confirm audit/history
```

What stays consistent:

- workspace context
- tenant-scoped visibility
- realtime state
- replay and approval discipline

What changes by company:

- connector sources
- operational thresholds
- approval ownership
- warehouse/site model
- escalation rules
