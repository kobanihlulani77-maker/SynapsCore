# Replay / Recovery Bounded Domain Closure

## Scope

This document closes the bounded Replay / Recovery lifecycle review. It does not
start a second Replay phase, claim generic platform recovery, or replace the
broader Integrations evidence. The reviewed boundary is tenant-scoped inbound
order recovery for the supported webhook and CSV paths.

The existing Integrations Phase 1 and Phase 2 evidence already covered inbound
trust, per-row CSV handling, duplicate identity protection, connector repair,
bounded retry, warehouse authorization, and replay visibility. This closure
adds only the missing seams needed to state the recovery contract precisely:

- business outcome versus evidence-finalization behavior;
- historical ImportRun truth after a later replay;
- retained dead-letter recovery after a prerequisite repair;
- disabled-webhook evidence boundaries; and
- the final A/B/C/D completeness classification.

## Current Contract

Replay is a recovery path for failed inbound orders. It is not an external
system executor and it is not a general-purpose job scheduler. A replay record
retains the tenant, source, connector type, external order identity, immutable
warehouse identity, normalized request payload, failure code/message, attempt
history, eligibility, and terminal timestamps.

The active status model is:

```text
PENDING -> REPLAY_FAILED -> DEAD_LETTERED
                         \-> REPLAYED
PENDING -----------------/
```

Inbound evidence is separately represented as `RECEIVED`, `ACCEPTED`,
`REJECTED`, `REPLAY_QUEUED`, or `REPLAYED`. An ImportRun records the historical
receipt/import outcome; it is not rewritten when an individual failed row is
later recovered.

## Bounded Closure Results

### 1. Business success and evidence finalization

The normal replay business transaction covers order creation or existing-order
reconciliation, inventory reservation, fulfillment setup, replay state, inbound
state, audit, and business-event work according to the existing service
transaction boundaries. A failed business attempt does not leave a false
completed order in the supported transaction path.

The webhook boundary also has an explicit defensive path for an unexpected
evidence-finalization failure after an order has already been created: it logs
that the accepted webhook evidence could not be finalized and preserves the
created-order identity for reconciliation. This is intentionally classified as
a **C evidence gap**, not an A product defect, because the current supported
business path does not silently claim that the evidence was finalized. A future
outbox or durable evidence-finalization boundary would make this stronger.

The closure therefore distinguishes:

- business success: an order exists and is reconciled exactly once;
- evidence finalization: inbound/replay/audit evidence records the outcome; and
- operator truth: any finalization gap remains visible for reconciliation rather
  than being converted into a fabricated success.

### 2. Failure before business creation

For a supported recoverable failure such as a missing product or missing
inventory, the failed inbound row creates replay evidence but does not create
the customer order or fulfillment task. Repeated attempts remain bounded by
the configured maximum and eventually become `DEAD_LETTERED`.

### 3. Existing-order reconciliation

Before creating a new order, replay checks the tenant-scoped original external
order identity. If the authoritative internal order already exists, replay
marks the retained recovery record `REPLAYED` and reconciles the existing order
instead of creating a second order, reservation, or fulfillment task.

### 4. Historical ImportRun truth

An import with one accepted row and one failed row remains a historical
`PARTIAL_SUCCESS` run with the original records-received, orders-imported, and
orders-failed counts after the failed row is replayed. The individual inbound
record changes to `REPLAYED`; the original ImportRun does not become a false
`SUCCESS`.

### 5. DEAD_LETTERED correction contract

A dead-lettered record is terminal until an explicit recovery decision is made.
The retained record may be explicitly requeued after a prerequisite repair only
when its failure code is one of:

- `CONNECTOR_DISABLED`;
- `PRODUCT_NOT_FOUND`;
- `INVENTORY_NOT_FOUND`; or
- `INSUFFICIENT_INVENTORY`.

The requeue retains the attempt history and dead-letter timestamp, sets the
record to `PENDING`, makes it immediately eligible, and records a requeue
business event and audit entry. A later successful replay clears the failure
code, marks the record `REPLAYED`, and preserves one business identity.

Dead-lettered records caused by source-invalid, duplicate, missing-connector,
invalid-token, unknown, or other non-replayable failures cannot be reopened by
the generic replay action. The operator must correct the source and submit a
new legitimate inbound operation. This protects the original evidence and
avoids turning a source correction into an untraceable mutation.

### 6. Duplicate and concurrency safety

The tenant plus external order identity remains protected by the V9 partial
unique index for active `PENDING`, `REPLAY_FAILED`, and `DEAD_LETTERED` records.
The replay row is locked for manual processing. The closure test confirms that
dead-letter repair produces one replay record, one order, one fulfillment task,
and one inventory decrement, while retaining the original dead-letter time.

The repository does not claim a multi-process PostgreSQL race rehearsal or a
distributed worker lease. Those are deployment-scale concerns, not reasons to
weaken the current identity boundary.

### 7. Disabled-webhook evidence

The supported disabled-connector contract is preserved for webhook and CSV
ingress where connector resolution identifies the tenant: the failure code,
source, external order identity, warehouse, attempt state, and replay linkage
remain available for operator diagnosis. A disabled webhook does not create a
business order, and its replay record is not silently discarded.

The repository has focused service/integration evidence for disabled connector
handling, including detached inbound behavior. A fresh rendered browser
walkthrough specifically for disabled-webhook replay visibility was not run in
this bounded local closure. That is a **C evidence limitation**, not an A or B
contract failure. No raw connector token or request payload is added to the
operational signal.

### 8. UNKNOWN and source-invalid failures

`UNKNOWN`, malformed source data, invalid source system, duplicate external
identity, missing connector configuration, invalid connector token, and missing
warehouse failures are not automatically replayable. They require operator
investigation, corrected source input, or connector/configuration repair as
appropriate. This prevents an automatic retry from repeating a failure that
cannot become valid merely by waiting.

## Evidence Matrix

| Required capability | Evidence | Result | Classification |
| --- | --- | --- | --- |
| Failure before business creation | Existing integration tests plus dead-letter repair test | No false order, reservation, or fulfillment result | A/B: proven |
| Existing-order reconciliation | Existing integration evidence and focused integration coverage | One existing order is reconciled; no duplicate business objects | A/B: proven |
| ImportRun historical truth | `replayDoesNotRewriteHistoricalImportRunOutcome` | Original `PARTIAL_SUCCESS` counts/status remain unchanged | A/B: proven |
| Recoverable dead-letter repair | `deadLetteredRecoverableOrderCanBeRequeuedAfterPrerequisiteRepair` | Requeue, replay, audit/event, and exactly-once business result | A/B: proven |
| Non-replayable dead-letter boundary | Service code and frontend state handling | Source correction/new inbound required | A/B: proven |
| Disabled connector/webhook linkage | Existing integration coverage and retained record model | Evidence remains operator-visible; fresh browser rendering not repeated | C evidence gap |
| Post-business evidence failure | Webhook defensive finalization path | Logged reconciliation path; no fabricated success | C hardening gap |
| Multi-process replay race | No distributed rehearsal in this closure | Not claimed | C |
| Distributed worker coordination | Single backend plus scheduled worker topology | No lease/claim guarantee claimed | D |
| Retention and payload lifecycle | No explicit retention policy | Future operational hardening | B/D |

## Focused Tests

Added to `MvpFlowIntegrationTest`:

- `deadLetteredRecoverableOrderCanBeRequeuedAfterPrerequisiteRepair`;
- `replayDoesNotRewriteHistoricalImportRunOutcome`.

The focused `MvpFlowIntegrationTest` run completed with **90 tests, 0
failures, and 0 errors**. The tests cover persisted dead-letter state,
prerequisite repair, retained attempt history, successful recovery, inventory
decrement, fulfillment creation, replay event presence, duplicate safety, and
ImportRun invariance.

## Frontend Boundary

The Replay page now distinguishes a recoverable dead letter from a terminal
source-correction record. Recoverable records present an explicit
`Requeue and Replay` action and explain that prior attempts are retained.
Non-replayable records disable the action and direct the operator to source
correction and legitimate re-ingestion. The backend remains authoritative for
tenant, warehouse, role, eligibility, connector, and status enforcement; the
UI is not treated as a security boundary.

## Completeness and Remaining Scope

### Classification A: required pilot capability

**0 remaining.** Supported replay eligibility, retained identity,
tenant/warehouse authority, bounded retry/dead-letter behavior, recoverable
dead-letter requeue, ImportRun history, duplicate protection, CSV row recovery,
and operator visibility are covered by repository evidence.

### Classification B: supported pilot boundary

The current pilot supports webhook and CSV inbound recovery, connector repair,
manual replay authority, scheduled replay in the single-backend topology,
tenant/warehouse filtering, audit/business events, realtime refresh signals,
and no outbound execution claim.

### Classification C: evidence or hardening limits

The remaining C items are a fresh browser walkthrough of disabled-webhook
visibility, a fault-injection rehearsal around evidence finalization, a
multi-process PostgreSQL race rehearsal, cross-connector collision testing,
scheduled telemetry overlap, and in-flight connector/token change testing.

### Classification D: future evolution

Future work includes durable queue/worker separation, distributed leases or
claims, source cursors/checkpoints, versioned mapping snapshots, richer replay
history, explicit retention controls, stronger connector authentication,
generic secret management, and horizontally scaled realtime coordination.

## Verification Boundary

This closure requires local repository evidence only. Hosted proof was not run
because the requested owner/browser walkthrough is deferred and no deployment
change is required to establish the bounded recovery contract. No unrelated
worktree files are part of this closure.

## Final Closure

- Classification A remaining: **0**.
- Critical blockers: **0**.
- High blockers: **0**.
- Controlled-pilot replay contract: **verified**.
- Remaining limitations: documented C/D evidence and evolution items only.

The final conclusion is:

**REPLAY / RECOVERY LIFECYCLE VERIFIED AND OPERATIONALLY COMPLETE FOR CONTROLLED PILOT — OWNER LIVE WALKTHROUGH DEFERRED**
