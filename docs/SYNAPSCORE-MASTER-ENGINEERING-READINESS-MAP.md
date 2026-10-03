# SynapseCore Master Engineering Readiness Map

## 1. Control record: where we are

This is the canonical engineering progression and pilot-readiness decision map.
It governs the journey; linked evidence records establish what individual checks
proved. The [hosted timeout recovery map](hosted-timeout-recovery-map.md) remains
the detailed runtime investigation playbook within this program.
The [Q4 2026 pilot acquisition blueprint](pilot-acquisition-blueprint-2026-q4.md)
records the parallel commercial path beginning Monday October 5, 2026;
research, discovery and conditional discussions do not bypass M1-M6 technical
acceptance or M7 customer-specific launch authorization.

| Field | Recorded state |
| --- | --- |
| Original source census | 2026-09-27, Africa/Johannesburg; baseline `a5f1982eee5f2366c082060d496352169af5b337`, `Adopt evidence-efficient timeout workflow` |
| Control update | 2026-10-03: bounded dispatch recovery/failure accounting; two naturally slow hosted reads exposed an H1/H2 attribution gap, now recorded in [warm-read evidence](evidence/warm-read-coalescing-and-platform-overview-2026-10-03.md). Neither is an M1 exit decision. |
| Repository verification | The September 27 local `main` and freshly fetched `origin/main` matched the original baseline then. Subsequent revisions are assessed only within their recorded scope; a documentation successor is not a newly verified application release. |
| Current phase | M1: performance, concurrency, correctness, and runtime hardening - OPEN |
| Current sub-workstream | H1/H2/H7: warm hosted latency, connection demand, and measured resource envelope; Replay convergence is a dependent H8 question |
| Immediate objective | Explain the latest warm hosted failure using request timing, pool overlap, transaction ownership, and resource evidence; identify one justified next correction or bounded capacity experiment |
| Next gate | M2: repeatable hosted verification; entry BLOCKED until M1's in-scope hardening gates pass |
| Pilot authorization | NOT GRANTED by this map; current candidate is not accepted for customer operations |
| Last verified live backend | `a123bca595643cb4800b7981ae38d33924ea5e46`, observed Live in Render deploy `dep-db0gogg473hc738514sg` on October 3; six read-only connection flags passed, but observed dispatch scans processed no work ([boundary](evidence/operational-dispatch-claim-recovery-2026-10-03.md)) |
| Latest scoped CI evidence | GitHub Actions run `37128570340` succeeded for exact SHA `a123bca` on October 3, including backend, frontend build, Compose validation and PostgreSQL dispatch-claim tests; no process-crash, hosted browser/load or full M1 proof |
| Local test artifacts | September 23 Surefire aggregate: 390 tests. The latest dispatch correction passed a 70-suite/403-test full backend run with zero failures/errors/skips and production packaging locally; its injected terminal-write failure was exercised in the H2 test profile, not PostgreSQL ([dispatch record](evidence/operational-dispatch-claim-recovery-2026-10-03.md)). |
| Worktree boundary | Unrelated frontend Dockerfile edit, `.gitattributes`, two untracked Scenario evidence files, and raw CSV captures are not part of this assessment's committed implementation. |

The project already implements a substantial operational system. Its immediate
problem is establishing dependable operation under the actual hosted workload,
then completing the operator and adversarial gates on a frozen candidate.
Warning-free logs, successful startup, old pilot verdicts, and individual E2E
passes do not close the present runtime gate.

### What currently stops a company pilot

1. Warm Dashboard/Runtime latency remains open in committed evidence. Historical
   pool starvation is proven; all historical connection owners are not mapped.
2. The September 23 session reported renewed pool pressure and a Replay UI
   failure. The synchronized artifacts and causal boundary need a durable
   evidence record before classifying a frontend defect or infrastructure cause.
3. Repeatable hosted correctness on the final build and approved data/load
   envelope is incomplete. Previous passes have different revisions and scopes.
4. Current candidate operator safety, adversarial security/concurrency review,
   recovery rehearsal, and customer-specific readiness gates remain unaccepted.
5. Provider restore expectations, credential recovery, supported integration
   mappings, support ownership, and real-company operating limits need explicit
   customer-specific records before handover.
6. Final post-remediation connected-engine acceptance has not been run; the
   candidate needs one correlated cross-pipeline proof before M6 sign-off and
   an applicable rerun on the actual M7 customer environment.

These are readiness blockers and evidence requirements. They are not an invented
count of Critical vulnerabilities. Earlier domain reports' zero Critical/High
counts apply to their exercised scope and date, not to this entire program.

## 2. Status and evidence rules

| Status | Meaning |
| --- | --- |
| IMPLEMENTED | Present in inspected source; no runtime claim implied |
| PROVEN LOCALLY | A named local check passed at a recorded revision/environment |
| PROVEN IN CI | An identified CI run passed its actual configured lanes |
| PROVEN HOSTED | A named deployed revision passed a bounded hosted check |
| PROVEN REPEATABLY | Predeclared repetitions passed under recorded equivalent conditions |
| PARTIALLY PROVEN | Some boundaries pass; named gaps remain |
| OPEN | Required investigation, implementation, or verification remains |
| BLOCKED | A stated prerequisite prevents safe progression |
| KNOWN LIMITATION | Understood behavior with scope, consequence, mitigation, and acceptance owner |
| OUT OF PILOT SCOPE | Explicitly excluded and shown unable to undermine the approved lane |
| FUTURE | Product or infrastructure evolution not required for this pilot |
| SUSPECTED | Plausible mechanism; not established as causal |
| RULED DOWN | Evidence lowers a hypothesis for a specified window only |

Evidence strength is separate from severity. A missing live proof is not itself
a product defect, but can still block a gate that depends on it. Historical A/B/C/D
classifications remain attached to their source reports; do not translate every
C evidence gap into new production work, or treat C as automatic pilot acceptance.

Every gate record must contain: revision and deployment identity; environment
and resource allocation; tenant/warehouse/role fixture; dataset size; UTC window;
commands and configuration; result including skipped/failed cases; assertions
and operational before/after ledger; evidence location; remaining uncertainty;
reviewer and owner decision. Credentials and customer payloads stay out of Git.

An existing raw artifact may be inspected without rerunning work. A copied
statement without an inspectable artifact is labeled session-reported. A passing
run after a failing run must preserve the failure and explain what changed.

### Binding M-stage closure rule

M0-M8 are engineering gates, not dates or progress markers. Before work starts
on an exit decision, record the stage's intended claim, all applicable acceptance
criteria, approved workload and numeric budgets, environment, failure cases,
and required evidence. Inspect the implementation and dependencies that exist
at the candidate revision; older documentation or an earlier pass cannot stand
in for that inspection. Include negative, concurrency, authority, isolation,
degraded and recovery paths wherever the stage's claim depends on them.

Close a stage only when **every** in-scope criterion has inspectable,
revision-bound proof at the environment required by that criterion, the
predeclared repeatability standard is met, failures and intermittent outcomes
are explained and re-proven after any material fix, and the stage's explicit
pass gate and exit artifact are accepted. Local, CI, hosted and pilot-environment
results remain distinct. One successful run is not repeatability; local proof
cannot satisfy a hosted or company-environment criterion. Missing evidence,
unexplained failure, a TBD budget, or a serious unknown leaves the stage OPEN
or BLOCKED even when much of the implementation works. A bounded limitation
may be accepted only with demonstrated scope, consequence, mitigation and owner;
it cannot waive an in-scope Critical/High defect or unsafe authority/data state.

The sequence is **diagnose -> root cause -> smallest justified fix -> affected
regression and broader impact proof -> evidence -> gate decision -> next stage**.
Do not hide failure behind retries, relaxed thresholds, disabled work or an
unattributed infrastructure change. Preparation for a later stage may occur,
but it earns no exit credit and does not authorize entering that stage. A
material code, schema, configuration, infrastructure, fixture, workload or
customer-mapping change invalidates the affected proof; record the impact and
rerun it. New contradictory evidence reopens a previously closed gate and
blocks downstream decisions that depended on it until re-proven. Keep the
original failure and the reason for reopening in the evidence ledger.

### Infrastructure and evidence policy

This rule applies to M0-M8 and subsequent production-readiness work, including
backend, PostgreSQL, Redis, schedulers, Replay, realtime, security, browser
convergence, recovery, load and UI verification. Render Free is a useful
engineering environment, not a SynapseCore product constraint. Use the cheapest
environment that still gives valid evidence for the question being tested.
Slower-but-measurable work does not justify an upgrade for convenience.
There is no infrastructure upgrade attached to M1, M2, M3, or any other phase
number. At every gate, ask whether the current environment can still perform
the next required work and produce trustworthy evidence. If yes, continue on
it. If no, classify the blocking boundary below before proposing any change.
Do not remain on Free solely to avoid cost once a genuine engineering limit
has been established.

For each failure or weak proof, record the observed boundary before choosing a
remedy:

| Classification | Required response |
| --- | --- |
| Application defect or inefficiency | Trace and correct the responsible code or transaction path; do not hide waste with a larger plan. |
| Configuration defect | Correct the specific setting and verify the same workload. |
| Test/proof defect | Correct the fixture or observation method without reducing its assertions or acceptance standard. |
| Infrastructure limitation | Identify the measured resource or unavailable capability and show why it, rather than the application, prevents a valid result. |

For a proven infrastructure blocker, preserve the constrained result and name
the missing CPU, memory, database, cold-start, observability, concurrency, or
recovery capability. State the minimum plan/capability change, current and
expected monthly cost, the precise proof it will unlock, workload and stop
thresholds, and residual uncertainty. Obtain owner approval **before** any
paid-plan change. Hold the revision, dataset, workload, instrumentation, and
acceptance criteria constant where practical; measure again and compare
before/after. If the increase removes one ceiling but exposes inefficient SQL,
long-held connections, runaway background work, or memory growth, fix those
application causes rather than treating capacity as permanent compensation.
Do not shrink representative work, disable useful functionality, add retries,
or raise timeouts merely to manufacture a green result.

Environment claims are stage-specific:

| Stage | Infrastructure gate |
| --- | --- |
| Engineering and focused diagnosis | Continue on Free while the evidence remains trustworthy. A paid experiment requires a proven blocking limit and owner approval; it is not an automatic migration. |
| Pre-pilot final verification | Exercise the frozen candidate in an environment representative of the declared operator, tenant, warehouse, data, background-job and realtime envelope. Free-tier passes or failures alone cannot settle company capacity. |
| Controlled company pilot | Use an always-on paid environment, sized with measured CPU/RAM, JVM heap/GC/threads, PostgreSQL and Redis demand, Hikari headroom, request/freshness latency, background overlap, concurrent sessions, recovery and monthly cost. After sizing, deploy the frozen candidate and rerun required hosted verification on that environment before launch. |
| Production reliance or expansion after pilot | Reassess capacity, scaling, monitoring, backup/recovery commitments and cost from actual pilot measurements; do not silently inherit the pilot plan. |

A move from M1 to M2, or from M2 into operator-experience or adversarial
verification, triggers this **question again**, not an automatic plan change.
If Free supports the required hosted repeatability, UI, security, recovery or
concurrency evidence, keep using it. If a particular required test needs
representative resources or an unavailable capability, isolate and justify
that experiment rather than upgrading the entire engineering program by date.

A failure on Free does not prove SynapseCore cannot handle the intended company
workload. A success on Free does not prove that it can. Both require the
appropriate measured envelope and evidence. This policy does not authorize a
plan change or declare a pilot ready.

## 3. The system being hardened

SynapseCore is a tenant-scoped operational intelligence and control layer above
company source systems. It accepts supported operational facts, maintains its
operational state, evaluates stock and fulfillment pressure, records evidence,
and helps authorized operators understand and recover work.

```text
CSV / webhook / scheduled pull / supported direct operational API
 -> connector identity + payload validation + tenant/warehouse authority
 -> order + lines + inventory reservation + queued fulfillment
 -> authorized fulfillment/source updates + inventory ledger convergence
 -> condition-based alerts + advisory recommendations + risk/prediction
 -> persisted events/audit + operational dispatch queue
 -> tenant-scoped REST snapshots + Redis/SockJS/STOMP notification
 -> operator decision / supported recovery / verified source readback

Separate governance branch:
live facts -> hypothetical Scenario projection -> saved governed plan
 -> assigned review -> final approval when required / rejection / revision
 -> external handoff -> later authoritative source observation
```

### Non-negotiable product contracts

- Orders reserve stock; reservation is not physical dispatch. Partial dispatch,
  delivery, cancellation, and return must preserve on-hand/reserved/available
  quantities and legal terminal states. Do not reduce the contract to an old
  `order -> inventory deduction` description.
- Alerts describe authoritative operational conditions. Recommendations remain
  advisory; transfer/replenishment advice is not proof that an action occurred.
- Scenario preview/save/approval do not create Orders, mutate Inventory, perform
  Fulfillment, or turn projected intelligence into live alerts/recommendations.
  `POST /api/scenarios/{id}/execute` checks visibility then returns `410 Gone`.
- Review Owner decides review; Final Approver decides the applicable final stage;
  Escalation Owner handles overdue governance attention, not review authority.
  Session identity, assignment, tenant, and warehouse all matter. No acting-as
  bypass, self-review shortcut, or Tenant Admin superuser inference.
- Source systems remain authoritative for real-company business action unless a
  specific supported direct-operation workflow is explicitly approved.
- REST readback repairs missed realtime updates. Redis Pub/Sub is not a durable
  event log, and a connected socket is not proof of fresh displayed state.
- Platform Owner is a separate control-plane identity with metadata-only support
  surfaces; it does not inherit customer operational access or impersonation.

### Implementation anchors

| Concern | Current implementation and inspection anchor |
| --- | --- |
| Server | Java 21 / Spring Boot modular application, services/repositories/DTOs; [backend source](../backend/src/main/java/com/synapsecore) |
| Persistence | PostgreSQL production, Flyway Java migrations V1-V14, Hibernate validation, open-in-view disabled; [migrations](../backend/src/main/java/db/migration), [production profile](../backend/src/main/resources/application-prod.yml) |
| Operational writes | [OrderService](../backend/src/main/java/com/synapsecore/domain/service/OrderService.java), [FulfillmentService](../backend/src/main/java/com/synapsecore/fulfillment/FulfillmentService.java), [InventoryService](../backend/src/main/java/com/synapsecore/domain/service/InventoryService.java) |
| Read composition | [OperationalViewService](../backend/src/main/java/com/synapsecore/domain/service/OperationalViewService.java) builds a multi-domain snapshot with request coalescing keyed by tenant, actor, roles, and scope; summary/snapshot reuse is already implemented |
| Background work | One default main scheduler, one recommendation scheduler; asynchronous dispatch executor and instance-local drainer guard; [SchedulingConfig](../backend/src/main/java/com/synapsecore/config/SchedulingConfig.java), [dispatch queue](../backend/src/main/java/com/synapsecore/event/OperationalDispatchQueueService.java) |
| Recovery | [IntegrationReplayService](../backend/src/main/java/com/synapsecore/integration/IntegrationReplayService.java) isolates attempts using TransactionTemplate; failed-attempt recording follows rollback; pull fetch/body reads are outside the ingestion transaction |
| Intelligence | Inventory prediction/monitoring, condition identities and reconciliation, fulfillment intelligence; [recommendation service](../backend/src/main/java/com/synapsecore/decision/RecommendationService.java) |
| Governance | [ScenarioHistoryService](../backend/src/main/java/com/synapsecore/scenario/ScenarioHistoryService.java), [ScenarioController](../backend/src/main/java/com/synapsecore/api/controller/ScenarioController.java), V14 successor uniqueness |
| Authority | Six tenant roles: TENANT_ADMIN, REVIEW_OWNER, FINAL_APPROVER, ESCALATION_OWNER, INTEGRATION_ADMIN, INTEGRATION_OPERATOR; [role enum](../backend/src/main/java/com/synapsecore/access/SynapseAccessRole.java). Planner/operator display names are not additional roles. |
| Realtime | [WebSocketConfig](../backend/src/main/java/com/synapsecore/config/WebSocketConfig.java) and [RealtimeService](../backend/src/main/java/com/synapsecore/realtime/RealtimeService.java); Redis sessions and Pub/Sub in production; scoped raw-feed restrictions and authority revalidation |
| Browser | React 18 / Vite SPA; distinct platform/workspace applications, manual history routing, shared workspace model/hooks; [route registry](../frontend/src/config/pageRegistry.js), [workspace model](../frontend/src/hooks/useWorkspaceAppModel.js) |
| Freshness | [useWorkspaceRealtime](../frontend/src/hooks/useWorkspaceRealtime.js): 15-second degraded refresh, 60-second live reconciliation, coalesced authoritative refresh; these are implementation intervals, not accepted end-to-end latency budgets |
| Hosting | [Render blueprint](../render.yaml), [backend Dockerfile](../backend/Dockerfile), [Compose production](../infrastructure/docker-compose.prod.yml); live plans must be verified independently of declared starter plans |

## 4. Evidence ledger and historical progression

| Area | What is supported | Limit carried into this program |
| --- | --- | --- |
| Domain lifecycle program | Catalog, inventory, orders, fulfillment, integrations, replay, intelligence, auth, administration, activity, runtime, realtime, and platform evidence exists in [evidence directory](evidence) | Bounded domain closures often defer owner/browser or production-shaped evidence; not a final candidate-wide acceptance |
| Integrated operational loop | [Layer 2 acceptance](evidence/layer-2-phase-7-full-technical-operational-acceptance.md): source order, reservation, partial dispatch, delivery, replay, governance separation, negative authority checks; 280-test full suite and exact-main CI recorded then | Report explicitly leaves PostgreSQL/Redis/browser and owner walkthrough evidence open |
| Scenario lifecycle | [Negative matrix](evidence/scenario-lifecycle-phase-11-negative-bypass-matrix.md), [approved decision boundary](evidence/scenario-lifecycle-phase-9-approved-decision-boundary.md), [rehearsal](evidence/scenario-lifecycle-phase-12-live-rehearsal.md) | Phase 12 owner rehearsal was deferred for missing legitimate operational data; synthetic proof is not customer acceptance |
| Double-borrow fixes | [Identity repair](evidence/timeout-recovery-identity-repair-connection-demand.md), [product preflight](evidence/timeout-recovery-product-preflight-connection-demand.md), [orders](evidence/timeout-recovery-order-connection-demand.md), [Replay/order](evidence/timeout-recovery-replay-order-connection-demand.md) | Local reproductions close specific seams; cannot identify all historical hosted holders |
| Worker ownership | [Background budget](evidence/timeout-recovery-background-holder-budget.md), [external pull delay](evidence/timeout-recovery-scheduled-pull-external-delay.md), [scheduler telemetry](evidence/timeout-recovery-scheduler-owner-telemetry.md) | Background executors alone ruled down as ten holders on one default instance; overlap, multiple instances and nested demand remain distinct questions |
| Read and event correctness | [Runtime read boundary](evidence/timeout-recovery-runtime-read-dispatch-boundary.md), [trace context](evidence/timeout-recovery-runtime-inline-dispatch-context.md), [SLA read-only callers](evidence/timeout-recovery-sla-readonly-callers.md) | Runtime reads must not trigger unbounded global drain or silently lose transition evidence |
| Composition efficiency | [Timeout map](hosted-timeout-recovery-map.md) records inventory batching, authenticated reuse, fulfillment reuse, and measured hosted improvements | Snapshot and Runtime remained slow despite successful responses; lower duplication does not establish acceptable latency |
| Recommendation locking | `23cb441` direct locking; `7203f91` hosted evidence removes HHH000444 on new instance | Warning removal is not global starvation closure; prior healthy recommendation capture remains ruled down for its window |
| Latest source changes | `1dcd764` recommendation summary reuse; `1244ea7` alert summary reuse; `4162179` explicit scheduled advisory tenant identity; `b8f4217` control proof SPA navigation; `a5f1982` working protocol | Production changes and proof-harness changes have different deployment/evidence implications |
| Current local regression artifacts | September 23 Surefire aggregate 390/390, no failures/errors/skips; baseline CI success freshly inspected | Not rerun today. Earlier session reported one transient inventory concurrency assertion failure before focused repetitions and a passing full rerun; retain that as a repeatability concern until artifact review resolves it. |
| Current browser session reports | September 23 controls: 7/7, 201 controls; full hosted proof: first three tests passed, Replay/Scenario combined test failed, last two not run | Session-reported; needs archived build-bound artifacts. Does not establish the full current system green. |
| Earlier capacity proof | [Performance proof](performance-scale-proof.md): 25 local readers, about 41 requests/sec, five-minute soak, 50 realtime clients | Dev-profile Docker, old V7 schema, synthetic data; not hosted capacity, sustained write throughput, multi-tenant load, or an operating-day soak |
| Recovery | [Backup/restore runbook](backup-restore-runbook.md), [August release gate](final-pre-pilot-release-gate.md) record application-level proof with limitations | Fresh V14 candidate restore and provider policy/RPO/RTO acceptance still required |

### September 23 runtime observation, pending durable correlation

The preceding diagnosis session reported connector 132 enabled at
`17:26:37.630816Z`, while Replay still displayed disabled at `17:26:49.171Z`.
Pool samples reportedly rose from 4 active at `17:26:39.770Z`, to 8 at
`17:26:42.775Z`, to 10 active/0 idle/1 waiter at `17:26:54.778Z`, with waiters
still observed near `17:27:42Z`; recovery to 7 active/3 idle appeared at
`17:27:43.833Z`. These are prior-session observations, not newly queried logs.
The UI symptom preceded the first reported fully saturated sample. Coincidence
does not establish the exact causal sequence or one continuously held transaction.

That session also observed a Free backend (0.1 CPU/512 MB), PostgreSQL
0.1 CPU/256 MB with graph peaks near its resource ceiling, and lightly used Redis
(0.05 CPU/25 MB). On September 30, the Render dashboard still displayed the
backend on Free (0.1 CPU/512 MB, idle spin-down) and PostgreSQL on Basic-256mb
(0.1 CPU/256 MB). Backend CPU/memory charts were unavailable on that plan;
the displayed database charts had no recent sample. The static frontend has no
Java server CPU. These are plan observations, not a measurement of a failure
window. CPU saturation is a candidate contributor; the graph alone does not
prove it caused the Replay failure. SQL sampling cannot measure exact cumulative
SQL time or Java work between calls without additional evidence. The owner
prefers an always-on paid backend for the first controlled company pilot, but
requires a cost/performance comparison before changing the plan.

### Documentation precedence and corrections

The August [final release gate](final-pre-pilot-release-gate.md) and
[Company 1 presentation](company-1-presentation-pack.md) describe historical
acceptance. Their ready-for-pilot wording is not current authorization. The old
[agent guide](../AGENTS.md) MVP bullets and architecture references to execution
must be interpreted against current controllers and the contracts in section 3.
Do not reintroduce Scenario execution or subtract stock on initial reservation.
Broader documentation reconciliation belongs to M3/M6, with history preserved.

## 5. Program and hard gates

| Phase | Status now | Work admitted by its exit |
| --- | --- | --- |
| M0 - Baseline and control map | Source/evidence inspection complete for this document; live state explicitly date-bound | M1 focused work using the objective below |
| M1 - Runtime and operational hardening | OPEN, current | M2 repeatability on a candidate with cleared in-scope hardening risks |
| M2 - Repeatable hosted verification | BLOCKED by M1 | M3 operator experience engineering |
| M3 - Operator experience engineering | BLOCKED by M2; page inventory below is preparation only | M4 adversarial review of the finished candidate |
| M4 - Full adversarial verification | BLOCKED by M2/M3 | M5 remediation with ranked, reproducible findings |
| M5 - Remediation and independent re-verification | BLOCKED by M4 | M6 technical pilot-readiness decision |
| M6 - Final connected-engine acceptance and technical pilot-readiness gate | BLOCKED; mandatory post-remediation whole-system proof not yet run | M7 customer integration and pilot packaging only after integrated acceptance and owner sign-off |
| M7 - Company packaging and launch gate | BLOCKED by M6; existing runbooks are preparation only | M8 controlled real-company pilot, only after frozen-candidate proof in the actual pilot environment and owner/customer sign-off |
| M8 - Controlled pilot and measured value | BLOCKED by M7 | Separate decision on production reliance or expansion |

Phase IDs here are M0-M8. They do not renumber historical domain phases or the
timeout map's phases 0-11. Legacy Consistency Phase 5.1 remains open until its
runtime/convergence conditions are met; its final Phase 6 proof maps into M2.

### M0 - Baseline contract

**Purpose:** Establish one accurate starting point. **Current evidence:** fetched
main, inspected source, CI conclusions, local report totals, linked historical
records. **Open risks:** live revision/plan facts may have changed; some recent
observations exist only in session context. **Required work:** retain this
baseline, label every evidence gap, preserve unrelated changes. **Pass gate:**
no unqualified current claim depends on historical deployment or untracked work.
**Stop condition:** source/remote mismatch or contradictory evidence. **Exit
criteria:** owner can identify phase, active objective, gate and limits from this
map. **Required evidence:** section 1 plus linked inspection anchors. **Next:** M1.

## 6. M1 - Current hardening program

**Purpose:** Keep supported operations correct, responsive, bounded, and
diagnosable under realistic overlap. **Current evidence:** section 4 and the
timeout map. **Open risks:** warm latency, holder overlap, actual resource
envelope, asynchronous convergence, production-shaped concurrency and recovery.
**Required work:** H1-H12 below, one causal work item at a time; reuse prior proofs
where revision and contract still match. **Pass gate:** every in-scope stream has
an evidence-backed pass or an explicitly accepted bounded limitation; no serious
unknown can affect the pilot lane. **Stop condition:** unexplained runtime
collapse, corruption, authority/isolation breach, unsafe recovery, or a test
requiring disabled controls to pass. **Exit criteria:** measured workload and
latency/freshness budgets are accepted; candidate and fixtures are frozen for M2.
**Required evidence:** owner map, negative/race proofs, workload ledger, resource
and request timelines, baseline and failure classification. **Next:** M2.

### Workstream register

Each row states its question, current evidence/risk, required work and explicit
exit. A failed invariant stops the stream and invokes the remediation discipline;
it does not authorize changes to every downstream page.

| ID | Purpose and present evidence/risk | Required work and exit evidence |
| --- | --- | --- |
| H1 - Request/holder ownership | Historical starvation proven; a warm hosted 2026-09-27 sample captured `10/10` Hikari active with one waiter and slow requests, but not the ten holders ([evidence](evidence/warm-runtime-pool-pressure-2026-09-27.md)). On deployed `487b012`, one warm snapshot request took 18,799 ms wall and 559 ms current-thread CPU; composition took 14,803/415 ms, with Hikari 1 active/9 idle at completion ([evidence](evidence/snapshot-cpu-wall-observability-2026-09-30.md)). That rules down CPU execution on this HTTP thread, not DB/network/off-thread wait or historical Hikari ownership. The bounded slow-snapshot stack sampler passed exact-SHA CI and is Live on `50fca48`; its October 3 warm control returned a 4,007 ms snapshot, below the diagnostic trigger, so no sampled wait category was captured ([diagnostic and control](evidence/snapshot-wait-sampling-2026-09-30.md)). Two other October 3 hosted reads took 6.6 and 5.4 seconds with low CPU and no pool waiters at completion; their causal boundary remains open ([warm-read evidence](evidence/warm-read-coalescing-and-platform-overview-2026-10-03.md)). | For a representative slow request/job map UTC, request/thread, controller/service, transaction owner, acquisition wait, JDBC PID, SQL/lock state, non-SQL work, commit/rollback and response. Exit when the failing overlap has a defensible explanation or the missing instrumentation is precisely specified. Low thread CPU or sampled JDBC frames alone do not prove a PostgreSQL wait or CPU throttling. |
| H2 - Query/composition efficiency | Batching and request-local reuse implemented; platform overview's per-tenant count fan-out was replaced by seven grouped counts ([evidence](evidence/platform-tenant-count-query-bounds-2026-09-30.md)). Connector telemetry batching passed 396/396 local backend tests and exact-SHA CI, then showed 58-connector direct-read client latency of 2,022 ms versus an earlier 9,233 ms sample and connector snapshot section time of 499 ms versus 8,532 ms ([evidence](evidence/connector-telemetry-batching-2026-09-30.md)). The same hosted snapshot still took 17,363 ms at the client across many sections; H2 is not closed. | Correlate residual warm snapshot/login time with H1/H7 acquisition/hold and resource measurements before another correction. Inspect PostgreSQL plans only if measured SQL state points there. Also count queries/transactions and payload size for login, summary, snapshot, Runtime, Replay, catalog, platform overview and scoped reads at realistic sizes. Exit with measured budgets and no unexplained query amplification; preserve tenant/role/scope cache keys. |
| H3 - Atomic writes and locks | Product/identity and Order/Replay double-borrow seams have focused proofs; the inventory first-row race test now synchronizes the actual missing-row boundary without changing production behavior ([evidence](evidence/inventory-first-row-race-proof-2026-09-27.md)) | Recheck top-level and ambient transaction callers; lock ordering, sequence repair under concurrent inserts, rollback on constraint failure, PostgreSQL aborted-transaction handling and inventory conservation. Exit with real PostgreSQL races producing documented success/conflict outcomes, exactly one durable result, released pool, and no partial side effects. |
| H4 - Background overlap | One main scheduler, separate recommendation worker, guarded dispatch drain; local budget documented. A source-code gap left claimed `PROCESSING` dispatch items outside the `PENDING`-only drain after a process stop. Lease-based reclaim and attempt-guarded terminal updates passed local and disposable PostgreSQL 17 CI tests. A follow-up red/green test found that a successful broadcast followed by a failed completion write could falsely mark work `FAILED`; the correction preserves lease recovery and reports scheduler failure. Final `a123bca` passed 403 local backend tests, exact-SHA CI and deployment/read-only health, but only idle dispatch scans were observed. PostgreSQL terminal-write failure injection, controlled crash/restart, multi-instance notification, backlog and headroom behavior are not yet proven ([evidence](evidence/operational-dispatch-claim-recovery-2026-10-03.md)). | Map main scheduler jobs, recommendation pass, async drain, HTTP and deploy overlap. Prove dispatch PENDING/PROCESSING/FAILED recovery on PostgreSQL after a controlled crash, starvation/fairness across tenants, backlog age, failure accounting, lease overlap, thread-local cleanup. Exit with bounded headroom and recoverable queue state; at-least-once notification and process-local guards must not be described as exactly-once or multi-node coordination. |
| H5 - Operational ledger and intelligence | Layer 2 plus lifecycle suites demonstrate key contracts locally | Overlap order intake, inventory corrections, partial dispatch, cancel/return and repeated request IDs. Verify available/reserved/on-hand arithmetic, no oversell or double release, terminal immutability, alert condition identity/resolution and recommendation currentness. Include transfer source/destination warehouse visibility and prediction with absent/sparse/stale demand. Exit with reconciled input/state/event/UI ledger. |
| H6 - Ingestion and Replay | CSV/webhook/pull exist; per-attempt Replay transactions implemented | Check repeated external IDs and different bodies, cross-connector collisions, manual/auto replay race, token disable/rotation in flight, payload limits, malformed CSV/JSON, partial import outcomes, remote fetch timeout/body limit and SSRF/redirect paths. Exit with one order/reservation/task per accepted identity and preserved failure history; no manual row insertion to make proof pass. |
| H7 - Capacity and resource behavior | Historical OOM and small-plan pressure reported; September 30 Render dashboard displayed a Free backend with a 0.15 CPU/512 MB metric limit and Basic-256mb PostgreSQL, but backend CPU/memory charts were unavailable. Treat the UI limit as an observed display, not a measured allocation or causal finding. Deployed slow-request CPU timing found 559 ms on-thread CPU across 18,799 ms wall in one warm request ([evidence](evidence/snapshot-cpu-wall-observability-2026-09-30.md)); host CPU, memory, GC and off-thread demand remain unknown. The verified connector batching corrected one genuine application inefficiency on Free. Available evidence does not justify an engineering plan upgrade now. An always-on backend is planned for pilot, but no plan change is authorized yet. | Record actual instance count, CPU/RAM/heap/GC/threads, DB CPU/memory/I/O, Redis, Hikari and browser request fan-out at a declared load/data envelope. Distinguish JVM build MAVEN_OPTS from runtime JVM flags. Inspect unbounded list/event/history/cache/session growth. Exit with a measured sustainable envelope, exhaustion behavior, recovery and cost. Apply the cross-phase infrastructure classification above; prove a Free-tier limit before proposing the smallest paid comparison, and obtain owner approval before changing plans. |
| H8 - Realtime and convergence | REST repair, tenant topics, periodic refresh implemented; Replay poll and route hook-order defects corrected and browser-tested locally. A 2026-09-27 served-code hosted trace found two connector GET aborts at 5 s and snapshot TTFB of 13/26 s; the later enabled state did render, but the exact filtered GET was absent ([evidence](evidence/replay-connector-poll-lifecycle-2026-09-27.md)) | Trace commit -> dispatch -> Pub/Sub -> browser signal -> REST -> render. Challenge missed/duplicate/out-of-order events, reconnects, session/scope changes, multiple tabs and stale responses. First explain the warm HTTP latency with backend/pool/database evidence, then verify the exact connector GET and bounded button convergence. Do not call a diagnostic sampling race permanent UI staleness. |
| H9 - Tenant, warehouse and authority | Six-role model, scoped services, platform separation, V12/V13 safeguards implemented | Exercise object IDs, tenant headers, role/scope revocation, disabled warehouses, last-admin/reviewer coverage, session expiry, CSRF/Origin/CORS and websocket destinations. Check caches, coalesced futures, scheduler context and support metadata. Exit with zero unauthorized read/write/event leakage across two tenants and at least two warehouse scopes. |
| H10 - Governance determinism | Scenario non-execution, assignment, SLA and V14 lineage established in code/tests | Preserve requester identity; independent review; escalated final approval; overdue review/final-stage SLA ownership; rejection/revision races; same-warehouse linear successors; deterministic timestamp ties. Exit with exactly one legal transition/evidence result under conflict and unchanged operational counts for preview/save/approve. |
| H11 - Failure/recovery and deployment | Replay and restore tooling exist; production-shaped recovery evidence incomplete | Rehearse dependency loss/restart and deployment interruption in isolated environment, then queue/session/cache recovery and authoritative reconciliation. Validate migrations V1-V14 on representative upgrade data, duplicate-lineage preflight, constraint/index plans, backup checksum and isolated restore. Exit with no lost/doubled business effects and measured recovery times. |
| H12 - Observability and credential continuity | Request IDs, scheduler pool telemetry and tenant-event correction implemented; bounded slow-HTTP filter timing and pool snapshot deployed on `4f91d89` and observed during a warm proof, but JDBC owner mapping remains open ([evidence](evidence/warm-runtime-pool-pressure-2026-09-27.md)) | Prove rejection/audit paths under DB failure do not amplify starvation; distinguish actorless scheduled work from missing tenant ownership. Preserve early auth timing, event tenant, warehouse where relevant, deploy instance and correlation. Establish safe proof-secret backup and supported lost-admin recovery ownership. Rotate any still-valid credentials previously exposed in chat/logs through approved operations; verify without printing them. Exit when support can reconstruct an incident and regain authorized access without data deletion or bypass. |

### Hardening workload and numeric acceptance contract

Before M1 closes, record actual proposed operators/tabs, tenants, warehouses,
products, inventory rows, order lines/day, peak arrivals, imports, history age,
queue sizes, background schedules and realtime clients. The existing 3-5 operator
Company 1 proposal is a starting scope, not a hosted capacity guarantee.

Record accepted p50/p95/max budgets for login, summary, snapshot, key writes and
replay; freshness delay; maximum safe backlog age; recovery time; error rate;
memory/CPU and pool headroom. Budgets are OPEN until based on operator needs and
measured hosting. The timeout playbook's warm request above five seconds remains
a diagnostic trigger, not a universal final SLA. No numeric gate can pass with
TBD thresholds, inadequate sample size, or post-hoc relaxed expectations.

For each important action measure input -> queries/transactions -> aggregate
calculations -> persisted events -> dispatch -> API fan-out -> visible state.
Preserve whole captures securely; provide compact UTC/request summaries for
review. One-second activity sampling misses subsecond transactions and cannot
establish exact SQL totals. Report sampled estimates and bounds honestly.

### The exact next engineering objective

**H2 connector batching is verified on its exact hosted revision. The later
`487b012` hosted CPU/wall trace found 18,799 ms wall versus 559 ms CPU on one
slow HTTP request; attribute that wait before another behavioral or capacity change.
H1/H7, broader H2 and M1 remain open.**

The [September 30 warm hosted trace](evidence/hosted-connector-telemetry-amplification-2026-09-30.md)
provides the before-state: 58 connector summaries, nine telemetry queries per
connector, and 8.5 seconds spent in the snapshot's connector section. The
[verified correction](evidence/connector-telemetry-batching-2026-09-30.md)
bounds the list path and reduced that section to 499 ms in one exact-revision
hosted sample. The snapshot still took 17.4 seconds at the client. This does
not close H1 or establish that the historical ten Hikari holders were connectors.

**H1/H7 follow-through:**

1. Use the [September 27 UTC/request/instance trace](evidence/warm-runtime-pool-pressure-2026-09-27.md)
   as the warm-pressure reference. The ten holder identities were not captured;
   do not infer them from dispatch telemetry or completion-time pool snapshots.
2. The [bounded snapshot wait sampler](evidence/snapshot-wait-sampling-2026-09-30.md)
   is deployed on exact revision `50fca48`, but the October 3 warm control was
   below its trigger. At the next naturally slow warm request, use its
   request-ID-matched Hikari/JDBC/Redis/Java category to select the next precise
   acquisition/hold, PostgreSQL or service-owner measurement; it cannot supply
   exact SQL duration or a PostgreSQL PID.
3. At the first warm `active=10, idle=0, waiting>0` recurrence, capture the
   request/scheduler threads, JDBC ownership, PostgreSQL session state and
   CPU/GC/resource window together. Stop traffic at the trigger. Classify
   acquisition wait, SQL/lock wait, Java-held connection, routing delay and
   HTTP-fast/UI-stale as separate boundaries.
4. Reproduce and correct only an established application cause. If measured
   normal work is capacity-limited, prepare a one-variable capacity comparison
   for owner decision. Neither conclusion follows from the current point sample.

**Exit artifact:** one incident record with observed versus inferred timeline,
causal classification, transaction-owner evidence or explicit remaining gap,
and exactly one next correction/experiment. The next gate still requires all
in-scope M1 workstreams, not just this incident, to clear.

## 7. M2 - Repeatable hosted verification

**Purpose:** Show the hardened candidate survives the same operational contract
repeatedly on its intended hosting. **Current evidence:** historical six-test
passes and September 23 partial failure; controls proof reported green.
**Open risks:** cold start, fixture drift, accumulated synthetic data, race timing,
backend latency and UI convergence. **Required work:** exact backend/frontend
revision, migration and CI identity; approved proof tenant/warehouse scope;
recorded warm baseline; focused affected proofs twice, then full production proof
twice, with zero hidden retries. **Pass gate:** all required assertions pass,
including controls and realtime, no unexplained starvation/slow response or
authority/data divergence, and measured M1 budgets hold. **Stop condition:**
first unexpected failure or degraded dependency; preserve original attempt.
**Exit criteria:** equivalent recorded conditions pass and final ledgers reconcile.
**Required evidence:** baseline UTC/durations, run IDs, counts, console/network,
pool/resource window, before/after state, known exclusions, and the environment's
representativeness relative to the declared workload. **Next:** M3.

Run the existing six-flag connection gate first; `PROOF_ALLOWED=True` permits
the proof path but does not itself establish authenticated warmth. Include
liveness/readiness, session, login, summary/snapshot, Runtime and SockJS readiness
in the warm record. Respect the proof's login rate-limit cooldown. Do not combine
multiple mutation suites concurrently or use an unknown customer tenant.

Hosted fixture preparation is itself a privileged API workflow. Validate
warehouse reviewer coverage and operator versions before reuse; previous
400/409 fixture errors are not evidence of a Scenario runtime defect. Preserve
credential continuity in a secret store and archive each run before reports are
overwritten. No increase in timeout or retry count to manufacture acceptance.

## 8. M3 - Operator experience engineering

**Purpose:** Make supported operational decisions understandable and safe under
pressure. **Current evidence:** registered pages, shared shell/tokens, controls
inventory and page-guidance evidence already exist. **Open risks:** excessive
guidance, dense competing panels, large lists, stale selection and unsafe action
interpretation; visual severity not yet reviewed on the final candidate.
**Required work:** complete the page contracts below before layout/style changes;
walk actual roles and abnormal states; preserve APIs, selectors and authority.
**Pass gate:** every in-scope page has an accepted information hierarchy, action
and state contract plus browser evidence; current functional controls still pass.
**Stop condition:** authority ambiguity, misleading empty/healthy state, hidden
high-impact action, broken workflow or convergence regression. **Exit criteria:**
operators complete approved journeys without coaching or unsupported actions,
with accessible controls at supported widths. **Required evidence:** page
contracts, before/after captures, abnormal-state fixtures, role/control matrix,
workflow timings and regression results. **Next:** M4.

### Page contract method

Each row below is a source-derived starting contract, not a completed design
review. Before changing a page, attach a page record containing: purpose; user;
reason to open; first fact; attention; evidence; allowed/prohibited actions;
authority; API and realtime dependencies; loading/empty/error/degraded/stale/
high-volume states; current structural problems; desired hierarchy. The shared
state contract applies to every page; the page-specific overrides below must be
tested too. No rendered visual audit was run during this documentation task.

Common hierarchy: scope and freshness -> situation -> priority -> supporting
evidence -> permitted decision/action -> result/readback. Use only applicable
layers. Keep warehouse/tenant identity visible where an action can affect data.

### Operational and governance pages

| Page / component | Purpose, operator and opening trigger | First fact / attention / evidence | Actions and authority | Data / realtime dependencies; desired hierarchy and structural review |
| --- | --- | --- | --- | --- |
| `/dashboard` - Dashboard | Scoped operator starts a shift or investigates pressure | Freshness and operational posture; urgent conditions; source/domain evidence | Inspect and navigate; no implied execution or cross-scope action | Shared snapshot/summary and REST-reconciled notifications. Prioritize act-now, cross-domain pressure, recovery and trust; examine competing KPIs and serial snapshot dependence. |
| `/alerts` - Alerts | Operator investigates an active warning | Active versus resolved; severity, SKU/warehouse, condition timestamp and policy explanation | Inspect condition/evidence and navigate; do not invent acknowledgement/closure mutations | Snapshot alert feed; live signal plus REST. Queue -> selected evidence -> safe next path; remove duplicated explanation only after preserving context. |
| `/recommendations` - Recommendations | Operator evaluates guidance | Current versus invalidated source; urgency and applicable location(s) | Inspect advisory evidence; no transfer/reorder execution claim | Recommendations snapshot and reconciliation. Urgent/soon/watch -> source/destination -> rationale; inspect long helper prose and cross-warehouse transfer visibility. |
| `/orders` - Orders | Operator tracks intake and fulfillment state | External identity, warehouse, status, line quantities and timing | Inspect/navigate; mutations remain supported integration-authorized backend paths | Recent orders + fulfillment snapshot; distinguish recent subset from complete history. Stream -> selected order -> related recovery; verify long/duplicate identifiers. |
| `/inventory` - Inventory | Scoped operator assesses stock risk; authorized maintainer adjusts | Available/reserved/on-hand and freshness; low stock, forecast limits | Controlled adjustment with reason only when role/scope permits; no arbitrary ledger rewrite | Inventory snapshot, prediction and adjustment readback. Exceptions -> complete navigable inventory -> selected ledger. Current six-item preview (`slice(0,6)`) must not hide discoverability of remaining rows. |
| `/catalog` - Catalog | Authorized tenant catalog maintainer onboards/corrects SKU | Tenant identity, duplicate/conflict, CSV row result | Create/update/import under backend authority; never cross-tenant SKU assumptions | Products API + catalog refresh. Search/list -> selected product -> bounded edit/import outcome; prove partial/error/conflict result visibility. |
| `/locations` - Locations | Scoped operator compares site pressure | Visible warehouse and operational pressure, distinct from service health | Inspect site evidence; administration through Settings | Warehouses + scoped domain snapshot. Site comparison -> lane evidence; no zero-pressure inference for unavailable source. |
| `/fulfillment` - Fulfillment | Operator locates delayed or blocked work | Order/lane state, partial quantities, deadline and source update | Inspect and supported role-gated controls; UI must not imply permission absent in API | Fulfillment snapshot/order/inventory links. Exceptions -> task/line history -> permitted path; verify queued/picking/packed/partial/dispatched/delivered distinctions. |
| `/integrations` - Integrations | Integration Admin/Operator handles source health | Connector enabled state, type, last attempt/success, failure | Only capability-permitted configure/import/token/support operations; no secret exposure | Connector/import reads + integration notifications. Connector -> status/evidence -> capability -> result; avoid giant undifferentiated setup form. |
| `/replay-queue` - Replay | Integration Admin/Operator recovers failed inbound | Failure identity, exact connector state, eligibility/backoff and scope | Authorized retry; no duplicate replay or disabled-connector bypass | Queue snapshot + exact connector fetch + refresh. Failure -> cause -> eligibility -> replay -> authoritative result. Effect lifecycle/stale enabled override needs H8 proof before styling. |
| `/scenarios` - ScenarioPlanner/ScenarioControl | Authorized workspace operator explores a proposal | Hypothetical label, actual baseline, warehouse and session requester | Preview/compare/save; valid assigned Review Owner; no acting-as or execute | Projection API, catalog/inventory/operator directory. Inputs -> projected impact -> governed save; separate live and hypothetical warnings throughout. |
| `/scenario-history` - ScenarioHistory | Requester/reviewer inspects decision lineage | Family/revision, warehouse, stage, requester and history order | Read/compare/reload/revise only under lifecycle/authority rules | History/request API + notifications. Family -> revision evidence -> permitted next step; no revision branching or old projection presented as current stock. |
| `/approvals` - Approvals | Assigned Review Owner/Final Approver decides | Exact stage, assignment, source/projection evidence and risk | Approve/reject only own eligible stage; no self-review/admin override | Scenario history/notifications and fresh decision response. My queue -> evidence -> decision -> result; protect concurrent reassignment/decision. |
| `/escalations` - Escalations | Escalation Owner handles overdue governance attention | Deadline, affected stage, decision owner, acknowledgement | Acknowledge SLA attention; never approve/reject by escalation authority | Scenario SLA/history/notifications. Overdue queue -> owner/stage -> attention evidence; don't merge governance overdue with live business incidents. |
| `/runtime` - Runtime | Authorized tenant operator/support checks trust | Reachability, source freshness, incidents, backlog and release identity | Inspect/refresh; no accidental queue-drain side effect | Runtime/incidents plus supporting snapshots. Trust -> active incident -> correlation; healthy status cannot conceal stale supporting reads. |
| `/audit-events` - Audit | Authorized operator reconstructs what happened | Tenant/scope, actor/source, event time, request ID | Filter/inspect supported history; no unsupported immutable-ledger claim | Business event/audit snapshot and REST refresh. Timeline -> record -> related state; separate planning evidence from operational effects. |

### Identity, administration, platform and public pages

| Page / component | Purpose, operator, first fact and evidence | Permitted / prohibited action and authority | Dependencies, desired hierarchy and structural review |
| --- | --- | --- | --- |
| `/users` - Users | Tenant Admin manages users/operators and warehouse responsibilities; inspect active identity, roles and coverage | Supported provisioning/reset/scope changes; no last-admin removal, reviewer-coverage loss or impersonation | Access administration/session readback. Directory -> identity/assignment -> explicit change -> impact; distinguish user account, actor display name and role. |
| `/company-settings` - Settings | Tenant Admin changes workspace, policy and warehouse configuration; know current version/impact | Supported metadata/security/warehouse/connector-owner changes; preserve retirement/coverage safeguards | Workspace admin endpoints. Separate policy, warehouses and connector ownership; verify stale-version conflict and invalidate-other-sessions result. |
| `/profile` - Profile | Signed-in user checks identity, password and expiry | Own supported profile/password/session operations only | Auth session/profile APIs. Identity/scope -> security posture -> action; never show password values in diagnostic output. |
| `/platform-sign-in` - SignIn/platform application | Platform Owner enters separate control plane; verify authority type | Platform credentials only; tenant login cannot substitute | Platform session API. Clear separate identity, errors, expiry/rate limit; no customer payload preview. |
| `/platform-admin` - PlatformAdmin | Platform Owner assesses portfolio support posture | Metadata inspection; no tenant operational mutation or implicit tenant session | Platform overview/health and activity-changed signal. Portfolio risk -> tenant metadata -> support path; unavailable counts are not zero. |
| `/tenant-management` - Tenants | Platform Owner provisions controlled workspace | Supported provisioning and metadata directory; do not invent tenant deletion or tenant-user recovery | Platform/provisioning endpoints and directory refresh. Tenant directory -> bounded provisioning -> credentials handoff result; duplicate/error recovery without leaked bootstrap token. |
| `/system-config` - SystemConfig | Platform Owner inspects safe runtime defaults | Display-safe readback; no implied ability to edit all server settings | Platform runtime/config API. Actual environment -> operational limits -> evidence; mask secrets, distinguish blueprint from served configuration. |
| `/platform-activity` - PlatformActivity | Platform Owner diagnoses platform changes | Metadata-only inspection; no cross-tenant business payloads | Platform activity API + metadata signal. UTC timeline -> tenant/source -> safe detail; inherited explicit unavailable/empty distinction must survive design. |
| `/releases` - Releases | Platform Owner validates release trust | Inspect backend/frontend fingerprints; no claim that commit equals deployment | Build/runtime data. Served revisions -> readiness -> proof status; stale frontend build placeholders require validation. |
| `/sign-in` - SignIn | Tenant user enters correct workspace | Authenticate; remember allowed workspace preference; no platform privilege | Tenant session/login, cookies and redirect. Workspace -> identity -> result; verify logout blank-shell regression, expiry, 429 and password-change-required. |
| `/`, `/product`, `/contact` - PublicExperience | Visitor understands supported product and requests pilot | Public information and controlled handoff only; no open tenant-directory access | Public routes/controlled contact flow. Product truth -> fit -> clear next step; remove execution/scale promises unsupported by current contracts. |

### State matrix required on every page

| State | Required contract and page-specific checks |
| --- | --- |
| Loading | Bounded progress, stable layout and scope; no false empty list or zero health; distinguish initial load from background refresh |
| Empty | Explain no records versus no permitted scope versus filters; Orders recent subset and Catalog onboarding need different messages |
| Error | Identify failed dependency/action, retain safe context, provide supported retry and request ID where useful; no success before authoritative readback |
| Partial/degraded | Mark unavailable subdomains independently; Runtime, Dashboard and platform counts must not convert unavailable into healthy |
| Stale/reconnecting | Show last successful state/time separately from connection status; Replay eligibility and governance decisions revalidate on action |
| Unauthorized/forbidden | Clear protected state on expiry/tenant switch; distinguish missing session from insufficient role; remove stale privileged actions after revocation |
| Action pending/conflict | Prevent accidental duplicate submits; preserve input; explain 409/version/assignment change and fetch current state; no silent retry of non-idempotent writes |
| High volume | Search/filter/pagination or deliberately bounded list with disclosed limit; stable selected identity, long identifiers, overflow, and no giant synchronous render/fetch loop |
| Recovery | REST reconciliation restores authoritative state after reconnect/restart; outcome is traceable without asking operator to guess if a write committed |

Design sequence: purpose -> operator questions -> information/action hierarchy ->
state model -> layout -> shared components -> visual language -> polish. Use the
existing tokens as a starting point, with restrained semantic severity/health
colour and text/icon equivalents. No decorative coloured card families or
repeated explanatory essays. Keep necessary safety explanations concise and
close to the action. Validate keyboard navigation, focus return, labels, contrast,
reduced motion and live announcements. Test 1366x768, normal laptops, large
desktops and narrow windows; mobile readiness is not claimed without its own scope.

## 9. M4 - Adversarial examination of the finished candidate

**Purpose:** Find reasons the system must not enter a company. **Current
evidence:** existing lifecycle/Layer 2/security/load suites are inputs, not this
phase's completed result. **Open risks:** correlated blind spots, environment
differences and failures after sustained use. **Required work:** independently
review the attack matrix below using strongest available reasoning/review
capability, with focused security/domain/database/browser perspectives where
available. Model branding is not a gate. **Pass gate:** every required attack has
an observed result and severity; missing critical tests remain open. **Stop
condition:** unsafe target, isolation breach, corrupt state or escalating load
beyond approved limits. **Exit criteria:** complete review plus reproducible
findings, including negative results and limits. **Required evidence:** attack
inputs, target revision, expected/actual invariants, runtime traces and final
ledger. **Next:** M5, including when no code defect is found.

| Attack family | SynapseCore-specific challenges and oracle |
| --- | --- |
| Architecture/state ownership | Follow one inbound through reservation, fulfillment, intelligence, event/dispatch and readback; challenge synchronous fan-out, ambient transactions, unbounded global work and stale cached authority |
| Auth and security | Session fixation/expiry/logout/rotation, browser cookies, login limits, Origin/CSRF/CORS, malformed content and errors; prove rejected operations leave no business mutation |
| Isolation/IDOR | Swap tenant/warehouse/object IDs across orders, inventory, products, replay, scenario history, audit, users and support; stale roles and scoped websocket subscriptions; no metadata or payload leak |
| Platform boundary | Tenant credentials/header fallback cannot access platform; platform session cannot operate customer business state; provisioning token remains private; public contact cannot enumerate tenants |
| Integrations | Token rotation/disable, duplicate identities with differing bodies, chunked/oversized/malformed imports, remote fetch SSRF/redirect/body limits, slow upstream, partial success accounting |
| Database/transactions | Real PostgreSQL unique/optimistic/pessimistic/advisory races, V14 same-parent contenders, deadlock victim and aborted transaction recovery, nested borrowing and rollback evidence |
| Operational correctness | Concurrent reserve/adjust/partial dispatch/cancel/return; exactly one accepted effect per identity; predictions don't fabricate demand; alert/recommendation resolution follows actual source truth |
| Governance | Forged actor, self-review, wrong assignment/stage/warehouse, approve/reject race, overdue acknowledgement versus decision; compatibility execute remains 410 with no operational effects |
| Performance/load | Declare dataset and operator mix; increase read/write/import/queue/realtime load in steps; measure latency, throughput, pool headroom, resource use, source-to-screen delay and errors |
| Stress | In isolated approved environment exceed the accepted envelope; prove visible rejection/degradation and bounded recovery without corruption; stop at defined resource/safety thresholds |
| Soak | At least one representative operating shift with scheduled jobs, session aging and history growth; duration and mix approved before execution; measure resource/backlog slope and post-run ledger |
| Mixed concurrency | Simultaneous scoped operators, sources, replay and scheduler work; examine duplicate events, tenant fairness, cache/future sharing, request retries and multiple process assumptions |
| Failure injection | Interrupt DB/Redis/network/browser and upstream response, restart backend during dispatch/replay, expire session during action; preserve committed state, failed evidence and safe recovery |
| Realtime | Forge STOMP SEND/destination, revoke authority, drop/duplicate/reorder updates, reconnect many clients, disable Pub/Sub temporarily; prove tenant isolation and REST convergence, not exactly-once delivery |
| UI safety | Every M3 abnormal state, high-volume selection, multi-tab updates, conflicting approval, disabled connector and slow response; no stale enablement or false success/empty state |
| Backup/recovery | Restore a sanitized V14 candidate backup into an isolated DB; validate migrations, identities, sequence state, ledger counts and post-restore intake/replay; measure RPO/RTO |
| Deployment/container | Production profiles, non-secret runtime config, image/runtime user and dependencies, TLS/proxy headers, health startup/shutdown, interrupted rollout, exact backend/frontend fingerprint and recovery path |
| Observability | Reconstruct a simulated 02:00 incident including early auth and audit failures with UTC/request/tenant/thread/pool/DB evidence; keep prod actuator restrictions intact |
| Dependencies/secrets | Date-bound Java/npm/image/transitive risk review and secret scan; classify reachability/exploitability; rotate exposed credentials safely; do not blanket-upgrade to make a scanner green |
| Test strategy | Challenge assertions, skipped/serial-aborted cases, unstable concurrency barriers, synthetic fixture bias, H2/prod differences, missing browser state coverage and absent durable artifacts |
| Documentation | Reconcile readiness claims, roles, source authority, Scenario wording, actual hosting, recovery/known limitations and company SOP against candidate behavior |

High-load and destructive fault/restore work targets an isolated production-shaped
environment with explicit resource limits and stop thresholds. A hosted E2E
authorization is not authorization to overload or erase the shared hosted DB.
After every assault, reconcile source inputs, orders/lines, inventory quantities,
fulfillment, replay outcomes, intelligence identities, governance, audit/event
counts, dispatch state and browser readback per tenant/warehouse.

## 10. M5 - Remediation and re-verification

**Purpose:** Resolve findings, not merely publish an audit. **Current evidence:**
prior focused correction history provides the working pattern. **Open risks:**
patching symptoms, combining unrelated fixes, or weakening assertions.
**Required work:** finding -> root cause -> minimal change -> failing-then-passing
targeted regression -> affected broader checks -> review -> commit/CI -> exact
deployment -> relevant hosted verification -> evidence update. **Pass gate:** all
pilot blockers/Critical/in-scope High findings closed; remaining limitations have
explicit owner, mitigation and scope. **Stop condition:** regression, unreviewed
authority/data change, or unexplained test intermittency. **Exit criteria:**
independent review validates closure and invalidated M2/M3/M4 checks are rerun.
**Required evidence:** finding ledger and linked verification per correction.
**Next:** M6; return to M1/M3/M4 whenever change impact requires it.

Finding fields: ID; severity; gate blocker yes/no; source/revision; trigger;
expected/actual behavior; affected tenant/domain; reproducibility; root cause;
owner; fix commit; local/CI/hosted regression; residual risk; acceptance decision.

Severity: Critical covers serious isolation/security/corruption/unsafe authority
or unrecoverable state. High can materially disrupt the approved operation.
Medium/Low require proportionate handling. A blocker is a gate decision, not a
replacement for severity. In-scope High/Critical defects cannot be waived as
cosmetic. Out-of-scope acceptance requires evidence that the defect cannot affect
in-scope operations. Future features are not readiness debts by default.

## 11. M6 - Final connected-engine acceptance and technical pilot-readiness gate

**Purpose:** After M1 hardening, M2 repeatable hosted proof, M3 final UI/operator
engineering, M4 adversarial examination and M5 remediation, prove that
SynapseCore operates as **one connected operational intelligence and control
engine**, not a set of separately passing modules. This is a mandatory final
integrated acceptance before the candidate may be called technically
pilot-ready. The historical August approval, green CI, isolated E2E passes,
and polished screens cannot replace this gate.

**Current status:** NOT RUN / BLOCKED by preceding gates. Neither an
individual domain pass nor the M2 pre-UI hosted proof establishes final
connected-engine acceptance.

### Full connected-engine verification scope

Freeze and record the exact post-remediation frontend/backend build and served
revisions, database schema/migrations, config, infrastructure, tenant/warehouse
fixtures, connector contracts, roles, data volume, scheduler load, concurrent
operators and clients, and predeclared correctness, p50/p95/max latency,
freshness, throughput, pool/resource headroom, queue age and recovery budgets.
Test on isolated production-shaped hosted infrastructure representative of
the declared pilot envelope; stress and destructive faults require an approved
isolated target. Maintain a full before/after operational ledger and
cross-system request/event correlation.

Verify *each supported, pilot-in-scope pipeline and their interactions*:

1. **Source and integration:** supported webhooks, CSV, scheduled pull and
   direct APIs as applicable; JSON/field normalization, credentials, duplicate
   identities, tenant/warehouse mapping, malformed/stale inputs, disabled
   connectors, rejected inputs and bounded retries.
2. **Operational truth and lifecycle:** catalog, orders/lines, stock
   reservation, inventory conservation, fulfillment/partial dispatch,
   delivery, cancel/return, reconciliation and authoritative source readback
   under normal and concurrent updates.
3. **Intelligence and control:** demand/pressure assessment, risk/prediction,
   alert condition creation/resolution, prioritization, advisory recommendation
   currentness, supported operator decision/routing/escalation and observed
   result. Recommendations cannot be presented as completed source actions.
4. **Governance:** hypothetical Scenario preview, save, revision, assignment,
   review, final approval where required, rejection and SLA escalation.
   Demonstrate authority and non-execution: planning cannot mutate live
   Orders, Inventory or Fulfillment or fabricate live alerts.
5. **Events, realtime and UI convergence:** persisted audit and dispatch,
   Redis/SockJS/STOMP, tenant-scoped REST snapshots and rendered browser
   state. Exercise dropped/duplicate/out-of-order events, reconnects,
   multiple tabs, scope/role changes, slow requests and authoritative REST
   reconciliation.
6. **Complete UI/UX:** every pilot-in-scope page and cross-page operator
   journey against real APIs and roles; navigation, information hierarchy,
   controls, accessibility and supported viewports, loading/empty/error,
   degraded/stale, high-volume, conflict, pending and recovery states.
   No false healthy status, unsupported execution affordance, hidden records
   or success before authoritative readback.
7. **Security and isolation:** session lifecycle, origin/CSRF/CORS,
   tenant/warehouse IDOR, role and approval boundaries, WebSocket and
   event authorization, platform/tenant separation, credentials, caches
   and background context. Rejected actions must not leak or mutate data.
8. **Performance, capacity and concurrency:** realistic mixed read/write/
   import/replay/scheduler/realtime/operator traffic with overlapping
   tenants/warehouses; measure user and source-to-screen latency, errors,
   CPU/heap/GC/threads, SQL/locks, Hikari acquisition/hold/headroom, Redis,
   queue growth and operating costs. Include approved ramp, bounded stress
   and representative operating-shift soak.
9. **Failure, replay, restoration and supportability:** safely interrupt
   sources, worker, DB, Redis, network, session and deployment; verify
   idempotent recovery, no lost/doubled effects, visible degraded state,
   backlog catch-up, current-schema backup and isolated restore, operational
   reconciliation, observability/request IDs, runbooks and rollback.

Execute at least five **complete connected journeys**, not just a checklist
of component tests: normal source-to-operator; cross-domain pressure and
intelligence; governed authorization without execution; integration failure
to Replay and reconciled readback; and mixed concurrency or dependency
failure through recovery. Run interacting pipelines together to expose
shared transaction, pool, scheduler, dispatch, security and UI interference.
Exercise two tenant scopes and multiple warehouse scopes as required by
the pilot contract. Clearly mark future/unsupported capabilities as out of
scope rather than pretending that they work.

**Acceptance evidence:** An independent reviewer inspects a coverage matrix
linking every in-scope pipeline and interconnection to exact-SHA/served-build
tests, request IDs, timestamps, input/output assertions, API/DB/event/browser
traces, security and UX findings, resource and load metrics, before/after
per-tenant operational ledger, recovery measurements, failures, reruns,
exclusions and residual limitations. All agreed numeric budgets must be
measured; a health endpoint, passing CI or visual demo alone is insufficient.

**Failure and correction:** Preserve the first failed run and stop on unsafe
state or unexplained degradation. Return to the relevant M1/M3/M4/M5 lane
for a smallest justified fix; rerun the affected tests and any invalidated
end-to-end journeys. Do not add hidden retries, relax criteria, bypass
security, shrink representative work or raise timeouts to manufacture a pass.

**Pass gate:** Every supported, pilot-in-scope connected journey passes
repeatably on the final frozen candidate with accepted correctness,
isolation, UX, performance, freshness, concurrency and recovery. Zero
unresolved pilot blockers, zero Critical and zero in-scope High defects;
remaining bounded limitations require explicit scope, mitigation, owner and
acceptance. Only then may the owner sign **technical pilot readiness** for
the specified build, environment and workload. This does not authorize
customer traffic.

**Exit artifact:** Signed *Final Connected-Engine Acceptance Record* with
build/config manifest, cross-pipeline coverage and causal traces, M1-M5
evidence, ledger reconciliation, measured capacity, findings and limits,
independent review, support/recovery plan and owner decision. Material
subsequent changes invalidate the affected proof.

**Next:** M7, including a scoped rerun of all applicable connected journeys
on the approved customer-specific, always-on pilot deployment before
owner/customer launch authorization.

## 12. M7 - Customer integration, packaging and launch

**Purpose:** Make the technical candidate usable by one real company. **Current
evidence:** extensive company runbooks and templates exist. **Open risks:**
customer mapping, identities, support and recovery commitments are not established
by synthetic test tenants. **Required work:** use the existing documents below to
record problem/scope, source ownership, supported connector, field mapping,
identities/units/timezones, duplicate semantics, warehouses, user/role assignments,
governance coverage, initial data reconciliation, policies, training and support.
**Pass gate:** customer data rehearsal succeeds within M6 envelope, credentials
are delivered/recoverable, no open blocker, and rollback/recovery/exit are agreed.
Before launch, owner-approved always-on paid sizing must cover the measured
company workload with justified headroom and cost; the frozen candidate must
pass the required hosted checks again on that environment. Specifically,
rerun applicable M6 connected-engine journeys using the real approved
customer source mappings, operators, tenant/warehouse scope, workload,
UI/realtime traffic, security, recovery and capacity criteria. Generic M6
proof does not substitute for this customer-specific launch proof.

Record the exact frozen frontend/backend revisions, migrations, configuration,
infrastructure, source mappings and data baseline actually serving that pilot
environment. Prove its end-to-end outcomes, operator readback, resource budgets
and recovery there, not merely on a similar staging tenant. A deployment or
customer-mapping change after the proof reopens the affected M7 checks; an
M7 discovery that contradicts M6 also reopens the affected M6 claim. No real
company reliance begins until the actual-environment record passes and the
owner and company authorize it.

**Stop condition:** unsupported source format/action, ambiguous ledger ownership,
missing reviewer coverage, no backup/support owner or expanding load without proof.
**Exit criteria:** owner and company approve the launch checklist and observation
window. **Required evidence:** completed customer templates, baseline and success
metrics, support contacts, restore/retention/RPO/RTO decisions, training/acceptance.
**Next:** M8.

Reuse [official pilot program](official-pilot-program.md),
[Company 1 intake](company-1-pilot-intake-pack.md),
[integration setup](company-integration-setup-runbook.md),
[data onboarding](company-data-onboarding-runbook.md),
[tenant provisioning](company-tenant-workspace-provisioning-runbook.md),
[user provisioning](company-user-provisioning-runbook.md),
[operational configuration](company-operational-configuration-runbook.md),
[pre-handover checklist](company-pre-handover-verification-checklist.md),
[handover procedure](company-customer-handover-procedure.md), and
[incident/recovery pack](company-incident-rollback-recovery-pack.md).

Explicitly resolve supported administrator recovery and proof-secret storage;
do not promise Platform Owner password impersonation or tenant deletion. Keep
synthetic proof tenants identifiable and excluded from company metrics. Agree
data access, retention, backup custody and end-of-pilot export/disposal with the
owner. Broad ERP adapters, AI agents, Kafka/microservices, HA/multi-region and
unproven horizontal workers remain FUTURE unless customer scope truly requires
them, in which case reopen architecture/readiness rather than promising them.

## 13. M8 - Real-company pilot and measured value

**Purpose:** Establish real operational usefulness inside approved limits.
**Current evidence:** no new company pilot is launched by this program.
**Open risks:** actual data quality, workload and operator behavior differ from
fixtures. **Required work:** supervised launch, daily correctness/freshness and
support review, incident handling, measured company outcome and feedback.
**Pass gate:** agreed observation period meets technical budgets and company
success measures without unresolved unsafe state. **Stop condition:** isolation
failure, corruption, unreliable authority, runtime collapse, irreconcilable source
data or loss of recovery capability; pause affected reliance and invoke incident
plan. **Exit criteria:** company/owner decide continue, remediate, stop or expand.
**Required evidence:** baseline versus measured detection/recovery/decision time,
recommendation usefulness, manual investigation burden and incident ledger.
**Next:** separate production-customer/expansion gate using actual pilot resource,
reliability, backup/recovery and cost evidence, never automatic conversion.

Do not invent business-value percentages. Use
[day-one guide](company-day-one-pilot-guide.md),
[daily operator SOP](company-daily-operator-sop.md), and
[pilot acceptance criteria](pilot-acceptance-criteria.md).

## 14. Verification commands and known blind spots

Run only the lane justified by the active gate. Commands below identify existing
tooling; they are not a request to run the whole program during documentation.

| Lane | Existing entrypoint | Proof limit |
| --- | --- | --- |
| Backend | `backend/mvnw.cmd test`; focused `-Dtest=...` | Default [test profile](../backend/src/test/resources/application-test.yml) uses H2, no Redis session store, scheduling off, header fallback on, rate limits/cache off; special tests override selected settings |
| CI | [workflow](../.github/workflows/ci.yml) | Backend tests, npm build, development/production Compose config; no automatic browser, hosted, load, restore or complete security gate |
| Frontend static/build | `npm.cmd run verify` in frontend | Check/build is not browser/component behavior proof |
| Convergence | `npm.cmd run test:convergence` | Focused script coverage; retain browser timing/readback evidence for actual rendered contracts |
| Controls | `npm.cmd run test:controls:inventory`, `npm.cmd run test:controls:execution` | Inventory and browser execution are separate; route/role/disabled controls need explicit oracles |
| Hosted | `scripts/check-live-connections.ps1`, `npm.cmd run test:e2e:prod` | Six serial tests; later tests may not run after failure. Preserve counts, exact build, setup latency, fixture state and cooldown. |
| Realtime | `npm.cmd run test:e2e:realtime`, `scripts/verify-realtime.ps1` | A focused dashboard update does not prove all topics/scopes/reconnects |
| Load | `npm.cmd run test:load:pilot` | Inspect options/target first; explicit approved envelope, no uncontrolled live stress |
| Release/security | `scripts/security-verify.ps1`, `scripts/secret-scan.ps1`, `scripts/env-sanity-check.ps1`, `scripts/check-prod-config.ps1` | Review actual script coverage and fixture findings; scans alone do not prove security |
| Migrations/recovery | `scripts/validate-flyway.ps1`, `scripts/backup-postgres.ps1`, `scripts/verify-restore-drill.ps1` | Isolated restore target, approved credentials and current schema required |
| Documentation | `scripts/docs-link-check.ps1`, `git diff --check` | Link/whitespace validity only; semantic claims still require evidence review |

Additional production-shaped tests must exercise PostgreSQL V14 constraints,
real Redis sessions/PubSub, enabled scheduler overlap and browser security where
H2/local profiles cannot. Do not claim a component-test suite exists merely
because frontend scripts pass. Existing Playwright `trace: on-first-retry` may
not preserve a first failure when no retry occurs; plan useful request evidence
before capture without adding retries as acceptance criteria.

## 15. Maintenance, evidence index and decision discipline

After each completed work item update: assessed revision/date, active H/M phase,
claim scope, open finding, proof links, served revision when verified, and one
next objective. Record changes to gates with rationale and owner decision. Never
overwrite a historical failed attempt or silently promote a local result to hosted.
The accepted protocol in [AGENTS.md](../AGENTS.md) remains in effect: targeted
Chrome usage, compact evidence, stop on first failure, preserve healthy controls,
and wait for actual deployment identity. Work on independent local analysis while
CI/deployment runs; do not use an older live revision as proof of a new fix.

Evidence navigation:

- Operational correctness: [inventory](evidence/inventory-final-operational-completeness-gate.md), [orders](evidence/order-lifecycle-phase-3-inbound-visibility-completeness.md), [fulfillment](evidence/fulfillment-lifecycle-phase-2-concurrency-surfaces-completeness.md), [Layer 2](evidence/layer-2-phase-7-full-technical-operational-acceptance.md).
- Ingestion/recovery: [integrations](evidence/integrations-lifecycle-phase-2-replay-recovery-completeness.md), [Replay](evidence/replay-recovery-bounded-domain-closure.md).
- Intelligence: [alerts](evidence/alerts-lifecycle-phase-2-condition-resolution-recovery.md), [recommendations](evidence/recommendations-lifecycle-phase-2-source-truth-recovery-completeness.md).
- Identity/authority: [auth](evidence/auth-sessions-phase-2-production-browser-infrastructure-completeness.md), [workspace](evidence/tenant-workspace-admin-phase-2-workspace-warehouse-security-completeness.md), [warehouse](evidence/warehouse-context-phase-2-browser-realtime-completeness.md), [platform](evidence/platform-owner-lifecycle-closure.md).
- Governance: [preview intelligence](evidence/scenario-lifecycle-phase-2-preview-intelligence.md), [SLA](evidence/scenario-lifecycle-phase-7-escalation-owner-sla.md), [revision](evidence/scenario-lifecycle-phase-8-rejection-revision.md), [non-execution](evidence/scenario-lifecycle-phase-9-approved-decision-boundary.md).
- Trust: [dashboard](evidence/dashboard-lifecycle-phase-2-currentness-degraded-completeness.md), [activity](evidence/activity-audit-lifecycle-closure.md), [runtime](evidence/runtime-lifecycle-closure.md), [realtime](evidence/realtime-lifecycle-closure.md), [platform freshness](evidence/platform-owner-live-freshness-hardening.md).
- UI foundations: [frontend flow](frontend-flow.md), [guidance/control depth](evidence/operational-page-guidance-control-depth-gate.md), [frontend QA](frontend-qa-checklist.md), [design tokens](../frontend/src/design-system.css).
- Operations: [runtime observability](runtime-observability.md), [Render runbook](render-ops-runbook.md), [backup/restore](backup-restore-runbook.md), [performance evidence](performance-scale-proof.md).

Locked decisions on 2026-09-27: current system is already built; current phase is
M1; no blanket readiness claim; governance is non-executing; capacity and frontend
hypotheses require separate causal evidence; recommendation reconciliation stays
ruled down for its healthy capture; historical documents retain their dates;
no broad implementation, hosted mutations, stress or customer onboarding is
authorized by creating this map.

**Next action remains the H1/H2/H7 incident reconstruction in section 6.** Once
its evidence selects a correction, apply the smallest change, verify it, update
this map, and clear the remaining M1 gates before M2 begins.
