# Dashboard Lifecycle Phase 2: Currentness, Degraded State, and Completeness

Status: source and local verification complete; hosted owner walkthrough deferred.

This evidence closes the Dashboard Phase 2 implementation work without reopening
the Dashboard Phase 1 authority contract. It records what the repository proves,
what is intentionally bounded, and which runtime fault-injection scenarios still
need a controlled hosted exercise.

## 1. Baseline and scope

- Starting HEAD: `186287e1a8b1df16202da674d4d219e009e1bbaf`
- Phase 1 baseline: Dashboard authority, tenant isolation, scoped counts, websocket authority, cache bypass for scoped actors, Scenario separation, Runtime separation, and Platform Owner separation were already closed.
- Phase 2 scope: freshness semantics, bounded caching, partial-source truth, retained snapshots, reconnect convergence, and false-all-clear prevention.
- Phase 2 does not add Dashboard mutation authority, change business APIs, or replace the domain services that own operational state.
- The Phase 1 deployment was reported complete by the prior gate. The Phase 2 working-tree changes were not deployed or live-checked in this turn.

## 2. Currentness contract

The Dashboard is a projection of authoritative domain reads and realtime updates.

- `current`: the latest requested source set completed successfully and the UI may present evaluated zero values as zero.
- `degraded`: at least one decision-surface source failed during a partial refresh; successful values remain visible only with an explicit unavailable warning.
- `stale`: a previously successful snapshot is retained after a later snapshot failure; the UI must not describe it as current.
- `unknown`: no successful snapshot exists for the affected surface.
- `lastUpdatedAt` on `DashboardSummaryResponse` is the time the summary was calculated, not a global transaction commit time, frontend refresh time, or proof that every Dashboard source is current.
- The frontend now separately tracks `freshness`, `lastSuccessfulAt`, and `degradedSources`. A failed refresh does not advance the last-success timestamp.

## 3. Source-by-source model

| Source | Current contract |
| --- | --- |
| Orders | Snapshot/recent-order read with tenant and warehouse authority applied before presentation. |
| Inventory | Snapshot/read of scoped inventory rows; low-stock pressure is derived from evaluated rows. |
| Fulfillment | Snapshot/read of the current fulfillment overview and workload posture. |
| Alerts | Current active-alert response and summary count; zero means the query completed and found none. |
| Recommendations | Current recommendation response and summary count; retired items do not represent current pressure. |
| Replay | Current replay queue projection from integration/recovery state. |
| Integrations | Connector and import/replay status projection; it is not a claim of remote-system reachability beyond the backend contract. |
| Runtime | Separate runtime/readiness/dispatch trust context; it does not rewrite business counts. |
| Recent activity | Recent event/audit projection, not proof that no older activity exists. |
| Incidents | Runtime/system incident projection, scoped before presentation where applicable. |

## 4. Redis summary cache

- Key shape remains tenant-qualified: `synapsecore:dashboard:summary:<tenantCode>`.
- Tenant-wide actors may use the cache; warehouse-scoped actors bypass the tenant-wide summary cache and receive scoped/live calculation.
- Cache read and write failures fall back to live calculation rather than returning zero or failing a valid Dashboard solely because Redis is unavailable.
- The cache now has a bounded configurable TTL: `DASHBOARD_SUMMARY_CACHE_TTL_SECONDS`, default `30` seconds, with a minimum effective value of one second.
- Operational dispatch still refreshes the summary after committed domain changes where that dispatch path exists. TTL protects against paths such as scheduled reconciliation or missed dispatch from serving tenant-wide data indefinitely.
- Serialization and cache exceptions remain non-authoritative infrastructure concerns; the database/domain calculation remains the source of truth.

Classification: the unbounded-staleness defect was Classification A and is fixed in this change. Redis outage injection and a live TTL observation remain Classification C evidence gaps.

## 5. Failure and degraded-state behavior

### Source failure and partial failure

`useWorkspaceBootstrap` and `useWorkspaceRealtime` now preserve successful values while recording the failed source names. The page displays a warning such as `Alerts unavailable. Retained values may be stale.` rather than silently converting the failed source to an empty result. If no source succeeds, the page remains unavailable/unknown rather than presenting false zeroes.

### Retained snapshots

A previous snapshot may remain visible after a failed refresh, but the Dashboard marks the state stale/degraded, shows the last successful snapshot time, identifies degraded sources, and prevents the utility rail from showing numeric zeroes or `Stable` as if evaluated. This is intentionally conservative for operator trust.

### Whole snapshot recovery

When a later authoritative refresh succeeds, freshness returns to `current`, the error/degraded-source markers clear, and new values replace retained values without requiring logout or a browser reload.

### Realtime disconnect and reconnect

Transport failures move the connection to `reconnecting` or `degraded` and start the existing REST fallback loop. A successful reconnect now requests a fresh snapshot after subscriptions are established, so transport recovery actively re-establishes authoritative freshness instead of waiting for a future event.

### Out-of-order summary protection

Partial summary merges compare incoming `summary.lastUpdatedAt` with the currently visible summary timestamp and ignore an older incoming summary. The frontend no longer fabricates a new root snapshot timestamp for partial realtime merges.

## 6. Zero versus unknown

- Evaluated empty sources remain truthful zero/empty values and can show normal workspace guidance.
- Failed or unavailable sources are represented as unavailable, degraded, stale, or unknown. The Dashboard does not treat a failed Alert, Recommendation, replay, incident, or source query as an evaluated empty collection.
- The Dashboard health/attention calculation treats non-current data as a trust blocker/degraded condition; it cannot report a fully healthy operating state while currentness is unknown.
- Attention cards and the shared utility rail avoid `No alerts`, `No recommendations`, `Stable`, or numeric zero claims when the source set is not current.

## 7. Operational boundaries

- Dashboard remains read-only: it exposes summary, attention, navigation, and trust state; it does not become a business mutation authority.
- Domain mutations remain authoritative in their own services and repositories. Post-commit dispatch failure may delay a projection update, but it must not roll back the committed business mutation.
- Realtime is an acceleration and visibility path. REST fallback and bounded cache expiry provide eventual convergence when an event is missed.
- Nearby domain values may represent different committed moments. That is an intentional eventual-consistency boundary, not permission to fabricate values.

## 8. Representative domain currentness

The existing lifecycle suites cover the underlying current-state transitions and
Phase 1 Dashboard scope semantics. The Phase 2 source changes preserve those
contracts and add freshness handling around them:

- Alerts: active/resolved currentness remains owned by the Alert lifecycle and the summary projection.
- Recommendations: current/retired/recurrence behavior remains owned by Recommendation lifecycle logic.
- Orders: recent totals and top-N authority filtering remain unchanged.
- Inventory: low-stock pressure remains derived from current scoped inventory.
- Fulfillment: backlog and delayed workload remain separate from Runtime trust.
- Replay: pending/failed/dead-lettered recovery attention remains distinct from replayed/completed state.
- Integrations: connector and import/replay posture remains a separate operational projection.
- Runtime: readiness, dispatch, and incident trust remain visible without manipulating business counts.

Classification: the source contracts are B (intentional boundary) where already
closed by Phase 1/domain lifecycle evidence. A focused hosted mutation-to-refresh
matrix is C and is not claimed here as live proof.

## 9. Performance and failure visibility

- The change does not introduce a global serializable Dashboard transaction or a generic cache subsystem.
- The existing source fan-out remains bounded to the established Dashboard surfaces; no N+1 or pathological query failure was observed in the local suite.
- Backend failures remain logged with request/tenant context according to the existing logging policy; tenant-facing UI receives safe operational wording rather than stack traces.
- Expected test warnings include deliberate disabled-connector/import/replay failures and security test failure endpoints. They are fixture behavior, not a failed build.

## 10. A/B/C/D classification

| Classification | Finding | Status |
| --- | --- | --- |
| A | Tenant-wide summary could remain stale indefinitely without expiry. | Fixed with bounded 30-second default TTL and focused configuration test. |
| A | Partial/failed Dashboard data could be presented as fresh or all-clear. | Fixed with freshness, degraded-source, retained-snapshot, and trust-state handling. |
| A | Realtime partial merges fabricated a fresh root timestamp. | Fixed by preserving authoritative timestamps and rejecting older summary merges. |
| A | Reconnect could restore transport without immediately restoring a fresh snapshot. | Fixed by requesting a snapshot after reconnect subscriptions. |
| B | Dashboard remains a read-only projection and accepts bounded eventual consistency. | Intentional current boundary. |
| B | Scoped actors bypass tenant-wide summary caching. | Existing Phase 1 boundary preserved. |
| C | Redis read/write outage injection and live TTL expiry observation. | Not run in this turn; requires controlled environment access. |
| C | Forced browser websocket disconnect, missed-event, and reconnect walkthrough. | Existing fallback/reconnect code is covered by source review; rendered fault injection remains deferred. |
| C | Hosted Phase 2 owner/browser walkthrough and live mutation convergence matrix. | Phase 2 revision is not deployed in this turn; deferred until deployment. |
| D | Per-card freshness timestamps, configurable refresh intervals, advanced caching, and custom Dashboard layouts. | Future extension; not required for controlled pilot closure. |

Classification A remaining: 0.

## 11. Verification evidence

### Focused backend

Command:

```powershell
cd C:\Users\asus\Downloads\synapsecore_starter\synapsecore\backend
cmd /c mvnw.cmd "-Dtest=DeploymentHardeningConfigurationTest" test
```

Result: 5 tests, 0 failures, 0 errors; `BUILD SUCCESS`.

### Full backend

Command:

```powershell
cd C:\Users\asus\Downloads\synapsecore_starter\synapsecore\backend
cmd /c mvnw.cmd test
```

Result: 244 tests, 0 failures, 0 errors, 0 skipped.

The suite completed Flyway validation/migration, Hikari startup, JPA initialization,
Spring test servlet initialization, and broker lifecycle per isolated test context.

### Frontend

Command:

```powershell
cd C:\Users\asus\Downloads\synapsecore_starter\synapsecore\frontend
npm.cmd run verify
```

Result: frontend launch-readiness check passed for 72 source files; Vite build
passed with 140 modules transformed.

### Repository checks

- `git diff --check`: passed; only normal line-ending conversion warnings were emitted by Git.
- Hosted proof: not rerun; no Phase 2 deployment or runtime behavior validation was performed in this turn.
- Owner/browser walkthrough: deferred as requested by the Phase 2 gate.

## 12. Requested closure matrix

1. Starting HEAD: `186287e1a8b1df16202da674d4d219e009e1bbaf`.
2. Deployed/live status: Phase 1 deployment was reported complete; Phase 2 changes are local and not live-checked here.
3. Dashboard currentness contract: `current`, `degraded`, `stale`, and `unknown` are explicit frontend states over authoritative source reads.
4. `lastUpdatedAt`: summary calculation timestamp, not a global cross-domain commit watermark.
5. Redis model: tenant-qualified tenant-wide summary cache; scoped actors bypass it.
6. TTL result: bounded, configurable 30-second default; minimum effective one second.
7. Redis failure: code falls back to live calculation; live fault injection is deferred C evidence.
8. Tenant cache isolation: tenant-qualified key and existing Phase 1 scope evidence preserved.
9. Scoped cache bypass: preserved for warehouse-scoped actors.
10. Source-query failure: failed sources are named and marked unavailable; no fabricated zero.
11. Partial-source failure: known values remain visible with degraded-source warning.
12. Zero-versus-unknown: evaluated empty is zero; failed evaluation is unknown/degraded/stale.
13. False all-clear: health/attention and utility-rail output do not claim current stability when freshness is not current.
14. Retained snapshot: retained values remain visible only with explicit stale/degraded context.
15. Freshness age: last successful snapshot/refresh state is tracked without an SLA timer.
16. Websocket disconnect: reconnecting/degraded state and REST fallback activate.
17. REST fallback: existing 15-second convergence path remains active; representative hosted fault injection is C.
18. Websocket reconnect: transport returns live and requests an authoritative snapshot.
19. Missed-message convergence: REST fallback and reconnect snapshot provide bounded convergence.
20. Out-of-order update: older summary timestamps are ignored.
21. Dispatch failure: domain commit remains authoritative; projection can recover through refresh paths.
22. Alert currentness: lifecycle semantics preserved; failure state cannot become false zero.
23. Recommendation currentness: lifecycle semantics preserved; failure state cannot become false zero.
24. Order currentness: Phase 1 current count/top-N semantics preserved.
25. Inventory currentness: scoped current inventory semantics preserved.
26. Fulfillment currentness: current workload projection remains distinct from runtime trust.
27. Replay currentness: recovery pressure remains tied to queue state, not stale empty values.
28. Integration currentness: connector/import/replay posture remains separately projected.
29. Runtime currentness: runtime unknown/degraded cannot collapse to healthy.
30. Empty tenant: evaluated empty remains truthful zero plus workspace guidance.
31. Full snapshot failure: retained state is marked stale, otherwise the page is unknown/unavailable.
32. Snapshot recovery: successful refresh clears degraded markers without manual reload.
33. Cache/realtime race: bounded TTL, dispatch refresh, and fresh snapshot path converge; advanced race injection is C.
34. Last-successful refresh: failed refresh does not advance the timestamp.
35. Health formula: non-current Dashboard truth contributes a blocked/degraded trust state.
36. Unknown attention panel: unavailable wording replaces false no-alert/no-recommendation claims.
37. Current versus historical wording: Dashboard labels use current/last-successful snapshot language and retain domain-specific recent semantics.
38. Recent activity failure: source failure is surfaced through the page error/degraded source state rather than empty activity claims.
39. Top-N currentness: Phase 1 authority-before-limit behavior remains unchanged; live mutation proof is C.
40. Stale navigation: navigation remains available; Dashboard itself has no destructive mutation control.
41. Mutation boundary: Dashboard remains read/attention/navigation only.
42. Eventual consistency: nearby source reads may differ in commit moment, but values are not fabricated and refresh converges.
43. Performance/query: no pilot-relevant N+1 or pathological expansion observed in the full suite; deeper production profiling is future work.
44. Failure visibility: backend logs carry safe context; frontend exposes safe degraded state; no stack traces are shown to operators.
45. Hosted/live evidence: Phase 2 hosted proof was not run because the Phase 2 revision is not deployed in this turn.
46. Consistency matrix: underlying lifecycle/Phase 1 contracts remain green; live cross-page mutation matrix remains C.
47. Production defects: four source-level trust defects were fixed; no known remaining A defect.
48. Frontend defects: fabricated partial freshness and silent stale utility values were fixed.
49. Fixes: bounded cache TTL, freshness model, degraded-source tracking, timestamp ordering, reconnect snapshot, and utility-rail truth.
50. A/B/C/D table: recorded above.
51. Focused tests: 5 passed, 0 failures, 0 errors.
52. Full backend: 244 passed, 0 failures, 0 errors, 0 skipped.
53. Frontend checks: verify/build passed; 72 source files checked, 140 modules built.
54. Docs/diff checks: `git diff --check` passed; docs link check is run after this evidence file is added.
55. Files changed: listed in the final commit report; unrelated worktree files remain unstaged.
56. Commits: recorded after the intended Phase 2 files are committed.
57. Critical blockers: 0.
58. High blockers: 0.
59. Classification A remaining: 0.
60. Owner/browser evidence: deliberately deferred; hosted fault-injection items remain C.
61. Dashboard final readiness: source-level and local verification ready for controlled-pilot review; deployment of this revision is required before live acceptance.
62. Phase 2 verdict: `DASHBOARD LIFECYCLE VERIFIED AND OPERATIONALLY COMPLETE FOR CONTROLLED PILOT - OWNER LIVE WALKTHROUGH DEFERRED`.

## Final note

This closure is honest about evidence boundaries: the operational trust defects
are fixed and locally verified, while live Redis failure injection, forced
realtime fault testing, and owner/browser walkthrough remain explicit C items
until this revision is deployed. No Public Entry or Onboarding work is started.
