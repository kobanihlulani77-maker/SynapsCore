# Timeout Recovery CI Gate: Concurrent SLA Escalation

Date: 2026-09-06.
Starting HEAD: `bef469789a498eb686f00e7dbbb4c48695285c72`.

## Scope and Diagnosis

The identity-repair correction passed its six focused tests in CI, but
[run 34029193241](https://github.com/kobanihlulani77-maker/SynapsCore/actions/runs/34029193241)
failed the existing Scenario Phase 7 assertion: one matching SLA escalation
event was expected, three were persisted. The documentation-only follow-up
[run 34029736980](https://github.com/kobanihlulani77-maker/SynapsCore/actions/runs/34029736980)
passed with unchanged production code. That green run does not remove the race.

Source inspection identified three readers sharing the same mutation path:

- Scenario detail -> `getScenarioRun` -> `applySlaEscalationIfNeeded`.
- Scenario history -> `getScenarioRuns` -> `applyPendingSlaEscalations`.
- Notifications/realtime -> `getScenarioNotifications` -> `applyPendingSlaEscalations`.

Before correction, eligibility was checked on a detached row, followed by a
repository save and an independently committed business event. Concurrent
readers could all observe `slaEscalatedAt == null` and each perform the same
transition. The row had no version check or SLA transition lock.

## Controlled Reproduction

[ScenarioSlaEscalationConcurrencyIntegrationTest](../../backend/src/test/java/com/synapsecore/ScenarioSlaEscalationConcurrencyIntegrationTest.java)
uses real Spring Data JPA repositories, Hibernate, H2, and the existing core
identity persistence helper. Fixed tenant/operator/policy collaborators isolate
the boundary; a barrier at policy resolution aligns three readers after each
has read the un-escalated row. SQL and persistence are not stubbed. No hosted
traffic or scheduler configuration changes are used.

Before correction: **1 test, 1 failure, 0 errors**. The event count was **3**,
not **1**, reproducing the CI assertion signature deterministically. This proves
the production race; the original CI log does not identify the exact three
historical reader threads.

## Bounded Correction

[ScenarioSlaEscalationService](../../backend/src/main/java/com/synapsecore/scenario/ScenarioSlaEscalationService.java)
owns one `@Transactional` transition:

```text
resolve candidate owners outside the transition transaction
  -> acquire one tenant-scoped Scenario row lock
  -> recheck deadline, pending final stage, marker, warehouse and prior owner
  -> update the escalation marker and existing owner fields
  -> persist exactly one governance event in the same transaction
  -> commit both, or roll back both
```

The service uses normal REQUIRED propagation, not another REQUIRES_NEW wrapper.
Concurrent readers recheck the current locked row and return without another
event once the first transition commits. An event failure rolls back the marker
and event, allowing a later clean attempt.

No approval/rejection permission, review/final-stage contract, projection logic,
order, inventory, fulfillment, migration, pool, timeout, scheduler or frontend
behavior was intentionally changed. This correction preserves the existing SLA
eligibility policy; it does not reopen or extend the Scenario lifecycle.

## Verification

Initial corrected concurrency reproduction: **1 passed**.

Focused command from `backend`:

```powershell
cmd /c "mvnw.cmd -q -Dtest=ScenarioSlaEscalationConcurrencyIntegrationTest,PlatformTenantAccessBoundaryIntegrationTest,CoreIdentityConnectionDemandTest,CoreIdentityWriteIsolationServiceTest test"
```

Result: **44 tests, 0 failures, 0 errors, 0 skipped**.

The four new tests cover:

- Concurrent detail/history/notification reads plus repeated reads persist one escalation event; operational table counts remain unchanged.
- Failure after event persistence rolls back both event and marker, followed by one successful retry.
- Wrong tenant is rejected; changed warehouse or prior owner prevents mutation.
- A future deadline and already approved/rejected decisions are not escalated.

The original 34-test authority class and six identity connection-demand tests
also pass without changing their assertions.

Full backend: **326 tests, 0 failures, 0 errors, 0 skipped; BUILD SUCCESS**.
Command: `cmd /c mvnw.cmd test`. Duration: 5m25s. Completed at
2026-09-06T11:32:24Z. Local generated log:
`backend/target/sla-closure-full-suite.log`.

Backend package: `cmd /c mvnw.cmd -q -DskipTests package`, exit 0, after the
full suite. Documentation links: 785 checked, none missing. `git diff --check`
passes. Frontend code, configuration, infrastructure and the original CI test
assertion were not changed.
New-commit CI and hosted revision/warm verification: pending.

## Next Work

This closes neither historical Hikari ownership nor warm-runtime latency.
It clears a separately reproduced CI-blocking race only after verification.

1. Finish full local verification, commit/push this bounded correction, and inspect its CI result.
2. Confirm the new Render revision before a warm baseline; stop at the first failed/slow boundary rather than running broad E2E.
3. If ownership still cannot be measured, locally prove the early request-timing/correlation gap before changing instrumentation. Do not ask for another identical database capture.
4. Once measurement is adequate, resume the unresolved product/import outer-transaction connection-demand family from the recovery map.

Recommendation reconciliation remains ruled down for its captured window.
The healthy control capture remains valid. Overall timeout recovery and
Consistency Phase 5.1 remain OPEN; Phase 6 repeatability is not accepted.
