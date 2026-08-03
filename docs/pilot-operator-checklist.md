# Pilot Operator Checklist

This checklist is for daily pilot operators using SynapseCore during a controlled company evaluation.

It keeps operators focused on operational truth, not just page availability.

## Daily Start

Verify:

- sign in succeeds
- correct workspace name is visible
- correct operator identity is visible
- session does not unexpectedly require repeated sign-in
- dashboard loads without stuck loading state
- runtime state is visible

## Dashboard Freshness

Check:

- dashboard summary is current enough for the pilot lane
- realtime status is `Live` or clearly explains degraded/waiting state
- alerts/actions/replay counts make sense
- recent operational activity is visible

Escalate if:

- dashboard remains stale
- realtime stays disconnected
- dashboard contradicts known operational state

## Orders

Verify:

- recent orders are visible
- important order identifiers match expected pilot data
- order status is understandable
- order visibility aligns with inventory or fulfillment activity

Escalate if:

- orders disappear unexpectedly
- recent order data is materially wrong
- order visibility conflicts with the system of record

## Inventory

Verify:

- product names and SKUs are recognizable
- warehouse/site context is correct
- quantities and thresholds are understandable
- low-stock or risk state is visible when expected

Escalate if:

- inventory values are unexplained
- warehouse scope is wrong
- stock risk is missing or false

## Alerts

Verify:

- active alerts are visible
- severity is understandable
- impact summary explains why the alert matters
- recommended action is visible

Escalate if:

- critical alert has no clear action
- alert contradicts known operating reality
- alert remains stale after recovery

## Recommendations

Verify:

- urgent recommendations are readable
- recommendation priority makes sense
- action guidance is useful
- selected recommendation detail is visible

Escalate if:

- recommendation suggests unsafe or confusing action
- recommendation is stale after underlying state changes

## Replay Queue

Verify:

- failed inbound records are visible when expected
- failure reason is understandable
- replay action is only used after the underlying issue is repaired
- replay outcome is visible

Escalate if:

- failed event is missing
- replay result is ambiguous
- replay appears to double-process or mutate the wrong record

## Connector State

Verify:

- connector name and source system are recognizable
- connector enabled/disabled state is intentional
- support owner is known
- connector telemetry matches pilot expectations

Escalate if:

- connector appears degraded unexpectedly
- connector state conflicts with pilot plan
- inbound failures are not represented in replay or alerts

## Scenario And Approval State

Verify:

- scenarios are visible
- approval state is clear
- approval/rejection actions are understandable
- execution only happens after approval when required

Escalate if:

- approval status is unclear
- rejected scenario appears executable
- scenario execution creates unexpected operational state

## Runtime Trust

Verify:

- readiness/liveness posture is visible
- runtime page shows live backend trust
- websocket/realtime state is understandable
- incidents are visible and classified

Escalate if:

- readiness fails
- auth/session fails
- websocket stays degraded
- runtime trust contradicts operator experience

## End Of Day Notes

Record:

- what helped operations
- what was confusing
- which failures were visible
- which failures still required manual reconciliation
- whether the next pilot day should continue, pause, or narrow
