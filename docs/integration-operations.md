# Integration Operations Guide

This guide describes the currently supported integration lanes and how operators should think about ingestion, recovery, and trust.

## Current Supported Scope

SynapseCore currently supports these inbound order lanes:

- webhook order ingestion
- CSV order import
- scheduled pull order ingestion

This is the real supported connector scope. Do not present broader out-of-the-box connector breadth than that.

## Connector Configuration Truth

Connector administration includes:

- `sourceSystem`
- connector type
- enabled or disabled state
- sync mode
- sync interval
- validation policy
- transformation policy
- optional default warehouse fallback
- support owner and notes

Operators should treat connector enablement as a live operational control, not as documentation-only metadata.

## CSV Import Truth

CSV import accepts:

- a multipart `file`
- optional request-level `sourceSystem`
- optional row-level `sourceSystem` column

Rows are grouped by:

- `sourceSystem`
- `externalOrderId`
- `warehouseCode`

Connector policy behavior can:

- normalize codes
- relax duplicate-line validation for grouped rows
- allow default warehouse fallback when explicitly configured

## Failure Handling Truth

Important rules:

- connector-disabled imports return structured `CONNECTOR_DISABLED` failures
- those failures create replay records immediately
- malformed or non-queueable failures should stay validation-classified rather than pretending to be replayable

## Replay Ownership

Current replay rules:

- disabled-connector replay records are manual-only until the connector is repaired
- automated replay skips those records while disabled
- operators can intentionally enable the connector and recover the record

This behavior now supports both real operations and deterministic hosted proof.

## Connector Health Signals

Operators should watch:

- enabled or disabled state
- health status
- recent inbound failure count
- pending replay count
- dead-letter count
- last failure code and message
- oldest pending replay age

These fields matter more than a simple "connector exists" check.

## Operational Advice

Use connector disablement intentionally when:

- you need to stop broken inbound traffic
- a partner is sending unusable data
- you want failed inbound records to queue for later manual recovery

Do not leave a connector disabled silently. If it is disabled, support ownership and recovery intent should be explicit.
