# SynapseCore Activity and Audit Lifecycle Closure

## Scope

This is one bounded Domain 15 closure cycle. It does not reopen the closed
Catalog, Inventory, Orders, Fulfillment, Integrations, Replay, Scenarios,
Alerts, Recommendations, Dashboard, Public Entry, Auth, Tenant Admin, or
Warehouse Context domains.

Starting HEAD before this cycle:

`4f0189be6c0d4bfa4ca675f60f7171a2f3d88e81`

## Contract

Activity is the human-readable operational timeline derived from persisted
`BusinessEvent` records. Audit is durable accountability evidence for
consequential actions through `AuditLog`. Realtime is notification and refresh
transport only. Logs are technical diagnostics, metrics are aggregate
measurements, and runtime incidents are a separate health evidence channel.

For a consequential action, the intended evidence answers who or what acted,
which tenant was involved, what action and target were involved, when it
happened, what result occurred, and which request ID is available.

## Direct Endpoint Boundary

The previous implementation applied warehouse-scoped suppression to dashboard
snapshots but not consistently to the direct reads:

- `GET /api/events/recent`
- `GET /api/audit/recent`

The smallest correction was applied in the query services. When the current
authenticated operator has one or more warehouse scopes, both direct endpoints
return a safe empty collection, matching the existing snapshot and realtime
policy. Tenant-wide operators continue to receive the bounded latest-20 tenant
records. No warehouse is inferred from free-form details, actor names, or
target references.

This is policy suppression, not proof that no activity exists.

## Visibility Evidence

The focused access-boundary integration suite passed with 33 tests, 0 failures,
and 0 errors. It covers tenant-wide workspace reads, all six role sessions,
tenant isolation, platform separation, and now asserts that warehouse-scoped
direct Activity and Audit reads are successful but empty.

The platform control plane remains metadata-first. Platform Activity does not
return raw tenant orders, products, inventory, inbound payloads, replay
payloads, credentials, or secrets.

Cross-tenant direct reads remain tenant-context constrained. A tenant session
cannot use a tenant header or query value to read another tenant's evidence.

## Transaction Truth

Core mutation services normally persist domain state, BusinessEvent, and
AuditLog inside the same transaction through
`CoreIdentityWriteIsolationService`. The normal consequence is:

- business failure: no committed success evidence;
- business commit: required evidence commits with it;
- rejected request: it is not represented as a successful mutation;
- realtime failure after commit: committed business and evidence remain;
- audit failure: behavior is path-specific; required same-transaction evidence
  can roll back the mutation, while after-commit notification failure cannot
  reverse committed truth.

Rejection auditing is attempted safely by the exception handler and does not
recurse indefinitely if the rejection audit itself fails.

## Realtime and Recovery

After-commit dispatch publishes `events.recent` and `audit.recent` through the
tenant realtime topic. Realtime is not persistence. If a notification is
missed, disconnected, duplicated, or cannot be delivered, a later REST
snapshot remains the recovery source for persisted evidence. Scoped operators
receive the same raw-evidence suppression through the operational snapshot and
realtime projection.

## Secret Safety

Inspected producers use whitelisted summaries rather than serializing request
bodies. Passwords, password hashes, session cookies, authorization headers,
bootstrap tokens, platform-admin tokens, connector tokens, and machine
credentials are not written to Activity or Audit. Password reset evidence
identifies the actor, target, action, status, time, and request ID only.

There is no general redaction framework. That remains a hardening boundary for
future arbitrary exception text, but no actual credential leak was found in the
representative producers inspected for this cycle.

## History and Bounds

Product APIs expose Activity and Audit as append-only reads; no normal tenant
endpoint updates or deletes either channel. Historical records remain when a
user is deactivated or a warehouse is retired. Audit and BusinessEvent records
are server-timestamped with UTC-oriented `Instant` values and bounded to the
latest 20 records.

Current intentional boundaries are the absence of a warehouse field, stable
actor ID, deterministic secondary ordering, pagination, search, automated
retention, database immutability triggers, and a universal BusinessEvent
request ID.

## Scenario Wording

The current Scenario contract remains planning and governance only. The
compatibility endpoint `POST /api/scenarios/{id}/execute` returns `410 Gone`
with an external-action handoff message. No current Scenario lifecycle producer
was found to emit `SCENARIO_EXECUTED`; the enum/runtime compatibility reference
is historical or defensive and must not be interpreted as actual customer
operation. Approval does not create orders, change inventory, or promote
projected intelligence into live operational truth.

## A/B/C/D Classification

| Class | Result |
| --- | --- |
| A - required accountability/security capability | 0 remaining after direct endpoint suppression and truthful scoped UI state |
| B - intentional current boundary | No warehouse field, historical actor strings, latest-20 reads, no retention/search/pagination, no universal rejection audit, and no universal BusinessEvent request ID |
| C - evidence gap | Dedicated hosted scoped read, destructive rollback/fault injection, provider-level failure, high-volume concurrency, and owner browser walkthrough |
| D - future extension | Structured actor/warehouse identity, immutable audit chain, SIEM/archive export, configurable retention, forensic search, and high-volume evidence pipeline |

## Verification

Focused backend command:

`cmd /c mvnw.cmd -Dtest=PlatformTenantAccessBoundaryIntegrationTest test`

Result: `BUILD SUCCESS`; 33 tests, 0 failures, 0 errors, 0 skipped.

Frontend command:

`npm.cmd run verify`

Result: lint/check passed and Vite production build passed.

Hosted Domain 15 proof was not run in this cycle. The existing six-test hosted
proof remains prior evidence, not dedicated Activity/Audit acceptance evidence.
The hosted scoped read, destructive fault-injection, and owner walkthrough
remain Classification C and must not be described as live-proven.

## Files Changed

Production boundary:

- `backend/src/main/java/com/synapsecore/audit/AuditLogService.java`
- `backend/src/main/java/com/synapsecore/event/BusinessEventQueryService.java`

Frontend truthfulness:

- `frontend/src/hooks/useWorkspacePageContexts.js`
- `frontend/src/pages/Audit.jsx`

Focused regression coverage:

- `backend/src/test/java/com/synapsecore/PlatformTenantAccessBoundaryIntegrationTest.java`

Evidence:

- `docs/evidence/activity-audit-lifecycle-closure.md`

Unrelated worktree files were preserved and are not part of this closure:

- `frontend/Dockerfile`
- `.gitattributes`
- `docs/evidence/scenario-lifecycle-phase-0-authority-census.md`
- `docs/evidence/scenario-lifecycle-phase-10-source-observation.md`

## Final Status

Classification A remaining: `0`

Critical blockers: `0`

High blockers: `0`

Activity and Audit are verified for the controlled pilot contract. Remaining
items are evidence-only follow-up, not unresolved accountability defects.
