# Replay And Recovery Guide

This guide describes the real replay and recovery contract that SynapseCore now uses in production.

## Supported Inbound Lanes

Replay and recovery currently apply to:

- webhook order ingestion
- CSV order import
- scheduled pull order ingestion

## Failure Classification Truth

Inbound failures are not all equivalent.

Important distinctions:

- validation or parse failures can be rejected without replay eligibility
- connector-disabled failures must return structured `CONNECTOR_DISABLED`
- connector-disabled failures are operationally recoverable and should create replay records immediately

## Disabled Connector CSV Contract

For a disabled connector CSV import:

1. the request returns a structured import response
2. `ordersFailed = 1` for the failed order
3. the failed row includes `failureCode = CONNECTOR_DISABLED`
4. an inbound record is created
5. a replay record is created immediately
6. the replay queue API can see it
7. the dashboard snapshot can reflect replay pressure

This path must not return a generic `500`.

## Replay Record Lifecycle

Typical lifecycle:

1. inbound failure detected
2. inbound record stored
3. replay record stored as `PENDING`
4. operator or automation becomes eligible to process it based on the failure type and connector state
5. replay succeeds, fails again, or becomes dead-lettered if the record is truly not recoverable

## Manual Versus Automated Replay

Current product rules:

- `CONNECTOR_DISABLED` replay failures are manual-only while the connector remains disabled
- automated replay skips those records
- once the connector is enabled, the record remains available for intentional manual recovery
- automated replay does not steal the record while the operator is meant to own the recovery path

This is important for both real operations and hosted proof determinism.

## Replay Eligibility Rules

Automated replay should only act when:

- the record status is eligible
- `nextEligibleAt <= now`
- the record has tenant context
- the record is not a manual-only failure code
- a matching enabled connector exists

Manual replay should only act when:

- the operator has the right warehouse and role access
- the record still exists
- the record is not already resolved
- the record is not dead-lettered

## Concurrency And Ownership

Replay processing now uses claim and locking behavior so:

- manual and automated replay do not double-process the same record
- a record cannot be replayed twice by concurrent actors
- the UI is not racing the automated worker for the same proof record

## Dead-Letter And Orphan Handling

If a replay record cannot be processed because it lacks required tenant context, it should not crash the scheduled worker. It should be investigated and handled as a broken record, not silently ignored.

Operators should treat dead-lettered records as real operational exceptions that need correction or re-ingest.

## Operational Advice

Use the replay queue when:

- a connector was disabled intentionally and then repaired
- a token or configuration issue was corrected
- an inbound order failed in a recoverable way

Do not use replay as a substitute for fixing a broken connector contract or a malformed integration source.

## Hosted Proof Dependence

The hosted proof now depends on this exact contract:

- the record appears
- the UI sees it
- automation leaves it alone while manual recovery is intended
- manual recovery succeeds after connector repair

If that stops being true, both product operations and hosted proof are degraded.
