# SynapseCore Integrations / Ingestion Phase 1
## Ingress Trust, Authority, Duplicate Safety, and Evidence Truth

Status: implementation and repository verification complete; hosted proof is intentionally not part of this Phase 1 run.

Starting HEAD: `944e357c439b64d0b8d75076aa0bafe56dee33cd`

Scope: `WEBHOOK_ORDER`, `CSV_ORDER_IMPORT`, and `SCHEDULED_PULL` using the
WEBHOOK_ORDER-compatible payload contract. Catalog, Inventory, Orders, and
Fulfillment remain closed domains. Integrations Phase 2 has not started.

## Executive Result

The Phase 1 trust boundary is implemented and locally verified. The important
invariants are:

- machine connector identity is deterministic and inbound token hashes are unique;
- human-session ingestion is checked against the actor's warehouse scope;
- connector configuration and connector reads remain tenant-scoped;
- stable tenant-wide `externalOrderId` remains the business deduplication key;
- duplicate deliveries are terminal evidence, not replay work;
- source-invalid failures are not automatically replayed;
- webhook and scheduled-pull resource use is bounded;
- scheduled pull rejects embedded credentials and unsafe network targets;
- scoped operators do not receive unassigned connector configuration or import-run telemetry;
- persisted inbound payloads contain the allowlisted order DTO, not request headers or connector secrets;
- connector `LIVE` wording describes recorded local state, not a claim that a remote source is reachable.

Classification A remaining: **0**.

## Runtime Boundary

The current request paths are:

```text
External webhook + connector token
  -> connector token hash lookup
  -> tenant-owned connector
  -> connector policy and warehouse resolution
  -> inbound RECEIVED evidence
  -> OrderService business transaction
  -> ACCEPTED or REJECTED evidence
  -> import-run telemetry and realtime state update
```

```text
Human webhook/CSV session
  -> tenant session and INTEGRATION_ADMIN/INTEGRATION_OPERATOR authority
  -> connector policy and warehouse resolution
  -> actor warehouse-scope check
  -> same inbound/order/evidence path
```

```text
Scheduled pull
  -> IntegrationScheduledPullWorkerService
  -> absolute HTTP(S), no userinfo, safe resolved target
  -> no redirects, bounded response stream
  -> compatible order payload
  -> same order/inbound/evidence path
```

Tenant and warehouse decisions are backend decisions. Frontend navigation is
not treated as an authorization boundary.

## Authority Contract

| Actor or identity | Supported authority | Boundary evidence |
| --- | --- | --- |
| `INTEGRATION_ADMIN` | Read and configure connectors, enable/disable, configure inbound token, ingest, replay | Controller/service authorization tests and existing role-boundary suite |
| `INTEGRATION_OPERATOR` | Read connectors, ingest, replay; no connector configuration | Existing role-boundary tests plus scoped-ingress regression |
| Other tenant roles | No integration mutation unless a separately authorized action exists | Existing role-clash and access-boundary tests |
| Machine connector token | Exactly one connector identity after SHA-256 hash lookup | Token uniqueness validation and PostgreSQL partial unique index in V8 |
| Platform Owner | Platform control-plane visibility, not tenant connector mutation or raw inbound payload authority | Platform/tenant access boundary tests |

Connector upsert still requires the current operator to be active and to have
access to an explicitly supplied default warehouse. Connector reads use the
current authenticated operator as follows:

- tenant-wide operator, represented by an empty warehouse scope, may see all
  connectors in the current tenant;
- warehouse-scoped operator may see only connectors with a nonblank default
  warehouse in that operator's scope;
- an unassigned/no-default connector is therefore not exposed to a
  warehouse-only operator.

This prevents tenant-wide connector configuration from being mistaken for a
warehouse-scoped operating lane.

## Phase 1 Classification Matrix

`A` means required operational capability and is complete in this phase.
`B` means an intentional current boundary that matches the supported pilot
contract. `C` means additional evidence or fault-injection coverage that is
useful but is not an identified production defect. `D` means future extension.

| # | Area | Classification | Result |
| ---: | --- | :---: | --- |
| 1 | Connector authority | B | Role checks distinguish connector configuration from read/ingest/replay authority. |
| 2 | Connector tenant ownership | B | Repository selection is tenant-qualified; cross-tenant IDs and source/type access are denied or empty under existing access contracts. |
| 3 | Machine-token tenant identity | A, fixed | Hash lookup is globally unique; duplicate token assignment is rejected and V8 adds a PostgreSQL partial unique index. |
| 4 | Token rotation | B | Connector updates replace the stored hash; read responses expose only a boolean and masked hint, never the raw token. |
| 5 | Disabled connector | B | Disabled webhook/CSV paths reject business demand; scheduled pull selection and replay remain blocked until repair/enabling. |
| 6 | Webhook tenant and warehouse resolution | B | Explicit payload warehouse is preferred; blank payload warehouse may use fallback only when connector policy enables it; invalid and strict mismatches reject. |
| 7 | Human warehouse authority | A, fixed | Human webhook and CSV ingestion now enforce the authenticated actor's warehouse scope after connector policy resolution. |
| 8 | Machine connector warehouse authority | B | Machine identity follows connector policy: STANDARD permits an explicit own-tenant warehouse; STRICT binds to the connector default; the contract is bounded and documented. |
| 9 | Stable external order ID | B | Webhook, CSV, scheduled pull, and replay use the supplied stable `externalOrderId`; no retry replacement ID is generated. |
| 10 | Duplicate webhook delivery | A, fixed | Duplicate business demand maps to `DUPLICATE_EXTERNAL_ORDER_ID` and is not queued for replay. |
| 11 | Duplicate across ingestion paths | C | Focused webhook-to-CSV proof confirms one order and no replay; broader source combinations remain follow-up evidence, not a known defect. |
| 12 | Duplicate across connectors | C | Tenant-wide Order identity is enforced by `externalOrderId`; a broader two-connector rehearsal is a follow-up evidence item for pilot onboarding. |
| 13 | CSV warehouse authority | B | Each grouped CSV order is checked against human actor scope; unauthorized groups fail without creating orders. |
| 14 | CSV group atomicity | B | A grouped order is passed to the existing OrderService atomic business path; a failed group cannot leave a partial reservation or order. |
| 15 | CSV partial-file truth | B | Valid groups may import while invalid groups fail; import counts and `PARTIAL_SUCCESS` describe persisted results. |
| 16 | Source-invalid versus transient failures | A, fixed | Replay eligibility is explicit; malformed/source-invalid, auth, duplicate, unsafe-target, and unknown failures are not automatically replayed. |
| 17 | Duplicate order as replay work | A, fixed | Duplicate terminal failures do not create recovery work that could create a second order. |
| 18 | Malformed webhook visibility | C | Invalid JSON can fail at request binding before an inbound record exists; authenticated business failures are recorded through the bounded DTO path. Raw malformed request capture is intentionally not added. |
| 19 | Unexpected runtime failure normalization | A, fixed | Before order creation, inbound evidence is best-effort rejected as `UNKNOWN`; after order creation, business truth wins and accepted evidence is finalized best-effort without reporting false rejection. |
| 20 | Webhook payload size | A, fixed | The request filter rejects payloads above the configured 256 KiB pilot envelope before business processing. |
| 21 | Scheduled response size | A, fixed | Scheduled pull uses no redirects and bounded streaming with a 1 MiB default response limit. |
| 22 | CSV resource bound | B | Existing 256 KiB file limit bounds the supported pilot input; no arbitrary row-count rule was added. |
| 23 | Scheduled pull URL safety | A, fixed | Localhost, loopback, link-local, site-local/private, multicast, unresolved, and IPv6 unique-local targets are rejected; embedded credentials are rejected; redirects are not followed. |
| 24 | Scheduled pull authentication | B | Current scheduled pull supports controlled HTTPS/HTTP order feeds using SynapseCore headers, not arbitrary outbound customer credentials or private API auth. |
| 25 | Import-run warehouse visibility | A, fixed | Because an import run has no authoritative warehouse field, warehouse-scoped operators receive a safe empty list rather than tenant-wide telemetry. |
| 26 | Import realtime warehouse scope | A, fixed | Raw scoped websocket subscriptions are rejected by the existing websocket boundary; import-run REST data is redacted for scoped operators. |
| 27 | Connector visibility | A, fixed | Scoped operators see only explicitly assigned connector lanes; tenant-wide/no-default connector configuration is not leaked to them. |
| 28 | Raw payload storage | B | The persisted payload is serialized from the known `ExternalOrderWebhookRequest` DTO only: source, external ID, warehouse, customer reference, occurred-at, and items. Headers, tokens, arbitrary fields, and pull credentials are not persisted. |
| 29 | Pull URL secret safety | A, fixed | URL userinfo is rejected, so `user:password@host` cannot be configured as a scheduled pull endpoint. |
| 30 | Transaction/evidence finalization | C | Best-effort finalization now preserves business-order truth and emits an explicit reconciliation log on evidence-finalization failure; fault injection for every repository failure seam remains follow-up coverage. |
| 31 | Accepted-evidence retry idempotency | C | Stable order uniqueness prevents a second business mutation; targeted finalization-failure injection is not part of this focused phase. |
| 32 | Connector health truth | B | `LIVE` means enabled and without recent recorded integration failures; it does not claim that the remote source is currently reachable. |
| 33 | Failure information | B | Failure code/message, source, external ID, warehouse where known, replay eligibility, and timestamps are available through existing integration/replay surfaces. |
| 34 | No false success | A, fixed | Rejected auth, scope, disabled, source, duplicate, oversized, unsafe URL, and processing paths do not create false Orders or reservations; accepted business truth is not overwritten by later evidence noise. |
| 35 | Generic connector framework | D | Arbitrary SDKs, generic schemas, unrestricted egress, and outbound ERP/WMS execution are not claimed in this phase. |

## Implemented Controls

### Token identity and rotation

Inbound tokens are trimmed, SHA-256 hashed, and stored only as a hash plus a
masked hint. Configuration checks for an existing hash before save. The V8
PostgreSQL migration adds a partial unique index for non-null hashes, making the
database enforce the same invariant across tenants and concurrent writers.

The inbound resolver therefore cannot select a connector by ambiguous
`sourceSystem`/type/token combinations. It resolves one authenticated machine
connector and derives the tenant from that connector. Existing token rotation
behavior replaces the hash, so the old token is no longer valid after update.

### Human versus machine warehouse authority

Machine connector policy is not treated as a human operator grant. A machine
connector follows its configured policy, while an authenticated human session
also passes through `SynapseActorContext` warehouse enforcement. A North-scoped
human actor cannot submit a Coast order even if a connector policy would accept
that payload. The same rule is applied per grouped CSV order.

### Replay eligibility

`IntegrationFailureCodes.isReplayable` is the single explicit gate for automatic
replay creation. Current replayable failures are:

- disabled connector;
- product not found;
- inventory not found;
- insufficient inventory.

Duplicate IDs, invalid tokens, invalid source data, invalid warehouses,
connector mismatch, unsafe targets, and unknown failures are terminal or
operator-reconciliation states, not automatic replay work.

### Evidence and business truth

Inbound evidence is written as `RECEIVED` before order processing. A successful
Order is marked `ACCEPTED`; a handled validation/business failure is marked
`REJECTED` and may create replay only when explicitly eligible. Unexpected
runtime failures are normalized best-effort without exposing stack traces to
the caller. If an order already exists, the stable Order identity remains the
source of truth and the duplicate is not turned into a replay item.

This is not a distributed transaction across Order, inbound evidence, and
import-run telemetry. The implemented rule is narrower and deliberate:
business Order truth wins; evidence finalization is idempotent/best-effort and
logs reconciliation when a finalization step itself fails.

### Resource and egress boundaries

| Input | Default bound/control |
| --- | --- |
| Webhook body | 262,144 bytes; rejected with HTTP 413 before controller processing |
| CSV file | 262,144 bytes in `ExternalOrderCsvImportService` |
| Scheduled response | 1,048,576 bytes, bounded streaming read |
| Scheduled redirects | Disabled; redirect response is not followed |
| Scheduled target | HTTP(S) only, no userinfo, no unsafe resolved address |

These are controlled-pilot bounds, not a claim of unlimited ingestion scale.

## Tests and Verification

### Focused verification

The focused access-boundary suite passed:

```text
PlatformTenantAccessBoundaryIntegrationTest
32 tests, 0 failures, 0 errors, 0 skipped
```

The targeted Phase 1 regression run passed:

```text
PlatformTenantAccessBoundaryIntegrationTest
MvpFlowIntegrationTest#duplicateIntegrationDeliveryAcrossWebhookAndCsvDoesNotCreateReplayWork
MvpFlowIntegrationTest#oversizedWebhookPayloadIsRejectedBeforeBusinessProcessing
0 failures, 0 errors
```

The targeted assertions cover:

- tenant-wide versus warehouse-scoped connector visibility;
- human webhook and CSV warehouse denial;
- no unauthorized Order or replay mutation;
- duplicate webhook-to-CSV delivery;
- exactly one Order for a duplicate cross-path delivery;
- no replay entry for a duplicate delivery;
- 413 rejection of an oversized webhook before business processing.

### Full backend verification

The final full backend suite passed after all Phase 1 implementation and
regression-test additions:

```text
Tests run: 228, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

The final run includes the connector-visibility and cross-path duplicate
assertions. No frontend source changed in this phase.

### Not run

- Hosted proof was not run. This phase was intentionally limited to repository
  and local automated evidence; Integrations Phase 2 remains unopened.
- No frontend verification was needed because no frontend files changed.
- No live customer or Render tenant was changed by this Phase 1 work.

## Evidence Gaps and Pilot Handling

The `C` classifications are explicit evidence follow-ups, not disguised A
defects. Before a production-scale connector program, add deterministic tests
for two connectors delivering the same tenant-wide external ID and for failure
injection at each evidence repository boundary. For the controlled pilot, the
stable Order uniqueness invariant, explicit replay gate, and business-truth
finalization behavior are the active safeguards.

Malformed JSON is intentionally not stored as arbitrary raw input. If a pilot
source repeatedly sends syntactically invalid requests, the operator uses the
request ID/server log and source-side correction process; the system does not
persist unbounded or untrusted content merely to create an evidence record.

Scheduled pull is intentionally bounded to feeds that work with the current
header contract. It is not an authenticated outbound enterprise connector
framework. A pilot requiring OAuth refresh, mTLS, signed requests, or customer
credential storage is outside the current Phase 1 contract and must not be
represented as supported.

## Files Changed in This Phase

Production and verification changes are limited to the integration boundary:

- `backend/src/main/java/com/synapsecore/domain/repository/IntegrationConnectorRepository.java`
- `backend/src/main/java/com/synapsecore/integration/ExternalOrderCsvImportService.java`
- `backend/src/main/java/com/synapsecore/integration/ExternalOrderWebhookController.java`
- `backend/src/main/java/com/synapsecore/integration/ExternalOrderWebhookService.java`
- `backend/src/main/java/com/synapsecore/integration/IntegrationConnectorService.java`
- `backend/src/main/java/com/synapsecore/integration/IntegrationFailureCodes.java`
- `backend/src/main/java/com/synapsecore/integration/IntegrationImportRunService.java`
- `backend/src/main/java/com/synapsecore/integration/IntegrationScheduledPullWorkerService.java`
- `backend/src/main/java/com/synapsecore/integration/IntegrationWebhookPayloadLimitFilter.java`
- `backend/src/main/java/db/migration/V8__integration_connector_token_uniqueness.java`
- `backend/src/main/resources/application.yml`
- `backend/src/test/java/com/synapsecore/MvpFlowIntegrationTest.java`
- `backend/src/test/java/com/synapsecore/PlatformTenantAccessBoundaryIntegrationTest.java`
- `backend/src/test/resources/application-test.yml`
- `docs/evidence/integrations-lifecycle-phase-1-ingress-trust-evidence-truth.md`

No frontend runtime, Catalog, Inventory, Orders, Fulfillment, or deployment
files are intentionally included. Existing unrelated local files remain
untouched and must not be staged.

## Closure

Critical blockers: **0**.

High blockers: **0**.

Classification A remaining: **0**.

The repository is ready to close Integrations / Ingestion Phase 1 after the
passing final full backend run, `git diff --check`, and an explicit commit of
only the files listed above. Integrations Phase 2 must remain the next phase;
do not broaden this evidence into replay concurrency, dead-letter lifecycle,
automated replay workers, or generic connector scalability.
