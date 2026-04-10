# System Flow

## SynapseCore Operating Loop

The MVP is built around one visible reaction cycle:

1. receive operational activity
2. update business state
3. interpret whether the change matters
4. estimate what happens next
5. recommend action
6. push a live dashboard update

## Main MVP Flow

### 1. Order arrives

- client sends `POST /api/orders`
- payload includes warehouse and line items
- an external system can also send one order through `POST /api/integrations/orders/webhook`
- an external system can also send a CSV batch through `POST /api/integrations/orders/csv-import`, where rows are grouped into real orders before ingestion
- connector configuration determines whether a given source system is enabled before SynapseCore accepts the inbound activity

### 2. State changes

- `CustomerOrder` is persisted
- a `FulfillmentTask` is opened so the order enters the warehouse and delivery lane immediately
- `OrderItem` rows are created
- matching `Inventory` quantities are reduced
- `BusinessEvent` records are written

### 3. Intelligence evaluates impact

- SynapseCore checks whether `quantityAvailable <= reorderThreshold`
- SynapseCore checks recent demand to estimate depletion timing
- SynapseCore checks whether fulfillment backlog is building in the warehouse lane
- SynapseCore checks whether dispatch SLA or delivery SLA timing is slipping
- SynapseCore checks whether repeated exceptions or stacked late shipments look anomalous
- urgency is elevated when stock is critically low or depletion is near

### 4. Decision layer responds

- a structured `Alert` is created or refreshed
- a `Recommendation` is created for replenishment
- a structured fulfillment or delivery alert is created when backlog or delay pressure becomes material
- a logistics recommendation is created when warehouse teams should prioritize backlog or escalate a delayed route

### 5. Dashboard updates live

- an internal dispatch work item is persisted for the tenant update
- the dispatch worker drains queued fan-out work in small batches
- dashboard summary is recalculated and cached
- the backend publishes dedicated live topics for summary, alerts, recommendations, inventory, recent orders, recent events, audit activity, integration connectors, recent import activity, and simulation state
- the backend also publishes a dedicated fulfillment overview topic so backlog and delayed shipments update without refresh
- the React UI updates without refresh
- recent business events and audit activity are also surfaced in the control-center timeline
- connector status, recent inbound webhook or CSV runs, and replayable failed inbound orders are also surfaced in the control-center integration lane

## Inventory Update Flow

1. `POST /api/inventory/update` sets quantity and threshold
2. monitoring logic runs immediately
3. alerts may resolve or appear
4. focused realtime updates are pushed live
5. queue failures surface in the system incident inbox instead of disappearing silently

## Simulation Flow

1. `POST /api/simulation/start` enables simulation mode
2. every few seconds a fake order is generated
3. the fake order goes through the real order ingestion path
4. SynapseCore also advances one active fulfillment lane, so dispatch and delivery pressure evolve with the simulated order stream
5. inventory pressure, fulfillment backlog, and delivery signals build naturally
6. alerts and recommendations appear as operational pressure rises

## Fulfillment Update Flow

1. `POST /api/fulfillment/updates` receives a warehouse or logistics update
2. SynapseCore validates the payload and resolves the current tenant order
3. the fulfillment lane is normalized into a consistent internal status
4. the live fulfillment state is updated
5. backlog, delay-risk, and anomaly signals are re-evaluated for the warehouse
6. related alerts, recommendations, business events, audit logs, and realtime topics are refreshed

## Observability Flow

1. SynapseCore records operational counters and gauges as activity moves through orders, fulfillment, imports, replay, and dispatch
2. `GET /api/system/runtime` exposes a tenant-scoped snapshot of queue posture, fulfillment pressure, and recent diagnostics
3. `GET /api/system/incidents` exposes active trust issues including failed dispatch work
4. `GET /actuator/prometheus` exposes scrape-friendly metrics for external monitoring

## Integration Replay Flow

1. a webhook order or grouped CSV order fails after SynapseCore can normalize it into a real inbound order request
2. the failed normalized request is stored in the integration replay queue with the failure reason
3. operators fix the blocking condition, such as enabling a connector or restoring missing product or inventory data
4. `POST /api/integrations/orders/replay/{replayRecordId}` replays the stored request through the normal order ingestion flow
5. if replay succeeds, the queue item resolves and the live dashboard reflects the new order, inventory, alerts, recommendations, and integration telemetry

## What-If Scenario Flow

1. `POST /api/scenarios/order-impact` receives a proposed order shape or multi-line order mix
2. SynapseCore validates warehouse, SKU, and available inventory
3. projected inventory levels are calculated without persisting a real order
4. prediction, intelligence, alert, and recommendation logic run against the projected state
5. the dashboard planner shows the likely operational impact before a live commit

## Scenario Comparison Flow

1. `POST /api/scenarios/order-impact/compare` receives two proposed order plans
2. SynapseCore evaluates both plans against current live inventory
3. each plan gets its own projected inventory, alerts, and recommendations
4. a risk-score comparison is generated
5. the dashboard recommends the operationally safer option before anything is committed

## Scenario History Flow

1. every preview or comparison is recorded as a scenario run
2. SynapseCore stores a concise planning summary and recommended option
3. `GET /api/scenarios/history` returns the recent planning memory
4. the dashboard snapshot includes recent scenario history so operators can see what-if activity alongside live operations

## Scenario Reload Flow

1. an operator chooses a loadable preview or saved plan from recent scenario history
2. `GET /api/scenarios/{scenarioRunId}/request` returns the stored preview request
3. the planner reloads that request into Scenario A
4. the operator can refine, rerun, compare, or execute the updated plan without rebuilding it from scratch

## Scenario Save Flow

1. an operator names Scenario A and saves it
2. `POST /api/scenarios/save` stores the request as a named executable plan
3. SynapseCore records the saved plan in scenario history and business events
4. the saved plan becomes reloadable from planning memory and waits for approval before execution

## Scenario Approval Flow

1. an operator or lead approves a saved plan
2. `POST /api/scenarios/{scenarioRunId}/approve` records the approver and approval time
3. standard plans move directly to `APPROVED`
4. critical plans enforce staged escalated approval policy, so the assigned review owner must complete owner review first with a review note and cannot be the original requester
5. owner review uses the `REVIEW_OWNER` role boundary, and after that the plan moves to `PENDING_FINAL_APPROVAL` with an assigned final approval owner
6. final release uses the `FINAL_APPROVER` role boundary, and the assigned final approval owner is still distinct from both requester and owner reviewer
7. each pending stage carries a due time so overdue approvals can be surfaced without guessing in the UI
8. if a critical final approval becomes overdue, SynapseCore reroutes it once to an executive fallback approver and records an SLA escalation event
9. the rerouted plan appears in the live SLA escalation inbox so operators can act without digging through history filters
10. the reroute also appears in the dedicated scenario notification feed so it is visible as an operational notice in the control center
11. an operator using the `ESCALATION_OWNER` role boundary can acknowledge the escalation to mark it as owned, which removes it from the live inbox while preserving the handoff in scenario history and notifications
12. SynapseCore marks the plan as approved in scenario history
13. the plan becomes executable through the live order path

## Scenario Rejection Flow

1. an operator or lead rejects a saved plan with a review note
2. `POST /api/scenarios/{scenarioRunId}/reject` records the reviewer, rejection time, and reason
3. SynapseCore marks the plan as rejected in scenario history
4. the plan stays loadable for refinement but is blocked from live execution

## Scenario Revision Flow

1. an operator loads a rejected saved plan back into Scenario A
2. the planner enters revision mode and keeps the rejected plan as the revision source
3. `POST /api/scenarios/save` creates a new pending saved plan with `revisionOfScenarioRunId`
4. SynapseCore records the new revision in scenario history and keeps the lineage visible for the next review cycle

## Scenario Review Queue Flow

1. an operator saves a plan with an assigned review owner
2. SynapseCore assigns a review priority, risk score, approval policy, and approval stage from the projected operational impact
3. planning history can be filtered by `reviewOwner`, `finalApprovalOwner`, `PENDING_APPROVAL`, `approvalPolicy`, `approvalStage`, `minimumReviewPriority`, `overdueOnly`, and `slaEscalatedOnly`
4. the dashboard quick actions let reviewers jump straight to their pending review queue, high-risk queue, escalated queue, final approval queue, their own final-approval queue, the overdue queue, or the SLA-escalated queue
5. review ownership, final approval ownership, triage priority, approval policy, approval stage, due-time status, SLA-escalation state, and escalation acknowledgment state stay visible across approval, rejection, and revision cycles

## Scenario Execution Flow

1. an operator chooses an executable preview or approved saved plan from recent scenario history
2. `POST /api/scenarios/{scenarioRunId}/execute` loads the stored request payload
3. SynapseCore sends that request through the real order ingestion flow
4. live inventory changes, intelligence re-runs, and alerts/recommendations update
5. execution is recorded in scenario history and business events so planning-to-action remains traceable

## Reseed Flow

1. `POST /api/dev/reseed` clears current demo operational data
2. starter products, warehouses, and inventory are restored
3. simulation is forced back to stopped state
4. dashboard summary is recalculated
5. fresh realtime operational updates are pushed to connected clients

## Control Access Flow

1. users sign in through `POST /api/auth/session/login`
2. the backend validates the seeded username and password, resolves the mapped operator through the operator directory, and stores the session identity
3. sensitive review and integration actions resolve the actor from the signed-in session first
4. header fallback with `X-Synapse-Actor` and `X-Synapse-Roles` still exists for test and non-UI flows when no session is present, but it is disabled by default in the `prod` profile
5. backend access control validates that the acting operator and declared role match the requested control action
6. scenario review actions must use the same role in the request body as the mapped signed-in operator or enabled header fallback is allowed to perform
7. connector updates require `INTEGRATION_ADMIN`
8. replaying failed inbound orders requires `INTEGRATION_OPERATOR` or `INTEGRATION_ADMIN`
9. rejected or unauthorized control actions still produce traceable request IDs and audit visibility

## What The User Should Feel

When the simulation is running or a real order is posted, the user should see:

- order count rise
- inventory levels drop
- low-stock risk appear
- recommendations surface
- the business event timeline advance
- the audit trail record the request and system reaction
- the dashboard change in place

## Validation Flow

The recommended MVP verification sequence is:

1. fetch `GET /api/inventory`
2. create an order through `POST /api/orders`
3. confirm `GET /api/alerts` and `GET /api/recommendations` changed
4. confirm `GET /api/dashboard/summary` changed
5. confirm the UI updated through WebSocket subscriptions
