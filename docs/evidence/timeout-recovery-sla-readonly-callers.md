# Timeout Recovery - SLA Read-Only Caller Boundary

## Trigger

Starting HEAD: `62eeb41914e0b936e525432962ab02714d6adbd5` (2026-09-06).
The [early tracing correction](timeout-recovery-early-request-tracing.md)
passed 338 backend tests locally. Its
[CI run 34032181665](https://github.com/kobanihlulani77-maker/SynapsCore/actions/runs/34032181665)
ran 338 tests with one failure: the existing Phase 7 SLA assertion at line 2736
expected one escalation event but found two. All 12 tracing tests and all four
controlled SLA race tests passed in that CI run. Frontend/Compose steps were
skipped after the backend failure.

The earlier [three-reader race correction](timeout-recovery-sla-escalation-race.md)
was therefore insufficient to close every caller path. We stopped forward
movement rather than calling the new CI failure a harmless timing flake.

A single local run of the exact failing integration method with Hibernate
SQL/bind tracing passed (1 test, 0 failures/errors). That healthy result did not
erase the CI failure. Log: `backend/target/sla-ci-recurrence-sql.log`.

## Proven Caller Chain

```text
Runtime / snapshot / realtime
  -> SystemIncidentService.getActiveIncidents() [readOnly transaction]
  -> ScenarioHistoryService.getScenarioNotifications()
  -> applyPendingSlaEscalations()
  -> ScenarioSlaEscalationService.escalateIfEligible() [REQUIRED]
  -> joins the readOnly caller instead of owning a write transaction
```

Workspace support adds another outer read-only wrapper:
`TenantWorkspaceAdministrationService.getWorkspace()` -> incident reader ->
the same SLA path. Removing only the inner wrapper would leave that route
affected.

The actual production service classes and their Spring annotation transaction
proxies were exercised against Hibernate/H2 and real repositories. Unrelated
connector/replay/audit feeds were empty stubs; no production fixture or database
was changed. The direct test established:

1. Incident composition stores one SLA event but leaves the persisted marker null.
2. A subsequent ordinary notification read treats the plan as un-escalated and stores a second event.
3. The same result occurs through workspace support.

This reproduces without racing threads. The read-only Hibernate persistence
context does not dirty-check the loaded Scenario marker, even though the new
identity-generated event is inserted. The prior race test had no ambient
read-only caller, so it did not cover this mismatch.

Corrected red fixture result: **8 tests, 4 failures, 0 errors, 0 skipped**.
Two assertions found a null marker after the event; two found event count 2
instead of 1. Log: `backend/target/sla-readonly-callers-red-corrected.log`.
An earlier expanded fixture had two null-collaborator errors; those are test
harness errors, not production evidence, and are excluded from this result.

## Minimal Correction

Remove the read-only transaction wrappers from `getActiveIncidents()` and
`getWorkspace()`. These methods compose independently read data and invoke an
existing write-capable SLA path. The existing SLA service now owns its short
write transaction for these entrypoints; its marker and event can commit or
roll back together.

The first wider focused run caught a regression introduced by removing the
workspace wrapper: `integrationReadVisibilityFollowsAssignedResponsibility`
returned 500 because connector telemetry accessed a detached lazy tenant.
All eight SLA tests passed in that run; the total was 66 tests, one failure,
zero errors/skips. Log: `backend/target/sla-readonly-callers-focused.log`.
The tenant-scoped connector list query now fetches its tenant explicitly with
an entity graph, preserving connector DTO composition without restoring an
outer read-only transaction. The existing access-boundary assertion is unchanged.

No `REQUIRES_NEW`, pool size, timeout, retry, scheduler, infrastructure, schema,
frontend, role, or Scenario operational-execution behavior changed. Workspace
write operations retain their original transactional annotations. The previous
row lock and eligibility rechecks are unchanged.

The new tests cover both real composition services through their actual Spring
transaction metadata, persisted marker/event coherence, a later normal read,
and unchanged operational table counts. Existing concurrency, rollback, and
authority assertions are retained.

## Verification

Final focused run: **66 tests, 0 failures, 0 errors, 0 skipped** (2026-09-06).
This includes eight SLA tests, 34 original platform/tenant authority tests,
six workspace tests, 12 tracing tests, and six identity-write tests.
Log: `backend/target/sla-readonly-callers-focused-final.log`.

The interrupted verification resumed on 2026-09-08 with the same intended
worktree changes and starting HEAD.

- Full backend: **342 tests, 0 failures, 0 errors, 0 skipped**; BUILD SUCCESS.
  Duration 10m50s, completed `2026-09-08T12:44:37Z`.
  Log: `backend/target/sla-readonly-callers-full-suite.log`.
- Backend package: exit 0; `backend-0.0.1-SNAPSHOT.jar`, 76,186,937 bytes,
  built `2026-09-08T12:45:31Z`.
  Log: `backend/target/sla-readonly-callers-package.log`.
- Docs link check: CLEAN, 792 local links, none missing.
- `git diff --check`: exit 0. Unrelated local changes were preserved.
- Correction committed and pushed as `0d9711bd253fbc8d85300b117754b3d7cf7d07d9`.
- [CI run 34228050405](https://github.com/kobanihlulani77-maker/SynapsCore/actions/runs/34228050405),
  job `102066944190`: SUCCESS. All **342 backend tests passed**, zero
  failures/errors/skips; backend completed `2026-09-08T12:51:08Z` in 3m51s.
  Frontend installation/build and development/production Compose validation
  also passed. This verifies the correction commit, not a served Render revision.
- No hosted verification is claimed.

## Limits and Next Gate

This is a proven caller transaction defect and a reproduction of the CI
duplicate-event signature. It is not a synchronized reconstruction of the
exact CI thread interleaving, a hosted PostgreSQL write proof, or attribution of
the historical ten Hikari holders. No hosted E2E or repeated database sampling
was used to discover it.

After focused/full tests and CI pass, confirm the new served revision before
any hosted proof. The previous 5836 ms login and unavailable Chrome debugger
remain the hosted evidence boundary. Do not substitute a fresh CI pass for
repeatable warm hosted behavior.

On resumption (2026-09-08), browser discovery listed only the in-app browser,
not a connected Chrome session. No new hosted requests were made. Render's
served revision cannot be claimed from the local build or CI result alone.

The prepared next local holder family remains product/import outer-transaction
connection demand. Start it only after this regression gate is verified.
Overall timeout recovery and Consistency Phase 5.1 remain OPEN.
