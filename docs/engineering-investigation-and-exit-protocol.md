# SynapseCore Engineering Investigation and Exit Protocol

This protocol selects the next justified engineering action across M0-M8. The
[master readiness map](SYNAPSCORE-MASTER-ENGINEERING-READINESS-MAP.md) owns the
gate criteria; [AGENTS.md](../AGENTS.md) owns repository-wide constraints. Build
a trustworthy operational engine, not a dashboard with no warnings. Neither a
successful response nor an attractive metric excuses wrong state, lost work,
authority leakage, unbounded resource demand, or an unproved gate.

## The bounded decision cycle

1. **Name the operational contract.** Specify the actor, tenant and warehouse
   scope, workflow, authoritative state, expected client-visible outcome, and
   deadline/freshness budget. If the owner has not approved a budget, mark it
   `TBD`; a 2-second warning threshold is not a service-level agreement.
2. **Separate observation from explanation.** Record exact SHA served, instance,
   UTC, workload/fixture scale, request ID or scheduler unit, HTTP status,
   client result, state before/after, and resource context. Mark missing fields
   `not captured`, never inferred from an unrelated sample.
3. **Reuse the record.** Read relevant source, tests, prior failures, CI,
   deployment and existing bounded logs before producing new traffic. Preserve
   contradictory and failing evidence. Treat a healthy window as a control.
4. **Rank a finite cause tree.** Include only mechanisms that can produce this
   symptom through the inspected path. For the top one or two, state what
   observation would confirm or falsify them and why the answer affects a gate.
5. **Run one discriminator.** Set the safe fixture, maximum probes/time, stop
   condition and data/secret limits first. Prefer request-ID/client correlation,
   a focused test, query census, or one warm read to broad hosted E2E or new
   instrumentation. Stop at the first relevant failure and retain its context.
6. **Decide and move.** Classify confirmed defect, measured capacity limit,
   owner-pending limitation, gate evidence gap, or monitored optimization. Fix
   only a proven cause with a narrow regression. Record what remains unproved,
   update the map, and choose the next material contract. Exhausted evidence
   budgets are not permission to relabel an unknown as healthy.

## Causal discriminators

| Candidate on the actual path | Discriminator | Unsafe shortcut |
| --- | --- | --- |
| Browser cancellation, navigation, proxy disconnect | Same-ID client network outcome and backend abort reason; did the browser receive a DTO? | Calling server 204 a successful read or raising timeouts |
| Session, authority, tenant/warehouse resolution | Identity-stage duration and query count with role-safe fixture | Bypassing revalidation or caching stale authority |
| Serial composition and duplicate reads | Ordered service spans, count per query family and response sections | Parallelizing write-capable or scoped calls blindly |
| Connector telemetry or support incident amplification | Count and duration as connector/incident cardinality changes | Rewriting ORM from one slow request |
| SQL plan, database I/O, locks and transaction lifetime | Same-window SQL timing, plan, wait event, blockers and transaction age | Claiming a JDBC stack frame proves slow SQL |
| Hikari acquisition and holder overlap | In-flight active/idle/waiting plus holder lifetime at failure | Increasing pool from end-of-request counts |
| JVM threads, GC, CPU, memory or scheduler overlap | Same-window thread state, GC/host metrics and worker timeline | Buying capacity from low request-thread CPU alone |
| Redis, connector or other dependency | Prove the call is in this request path and time its wait | Debugging unrelated services |
| Serialization, network and frontend convergence | Backend completion versus browser response, payload and render/freshness | Changing UI when authoritative HTTP itself is slow |
| Cold start, deploy cutover or changed data shape | Exact Live SHA/instance, boot timeline and comparable warm fixture | Comparing different revisions or cold and warm windows |

PostgreSQL `ClientRead` can indicate the server is waiting for its client; it
does not by itself identify the Java method or every elapsed millisecond.
Hibernate `HHH000444` is a query-locking warning, not a demonstrated deadlock.
`200` proves an HTTP result, not that the operator saw fresh correct state.
`204` from a GET normally returning a DTO is not proof of delivery. A low
current-thread CPU/wall ratio does not rule out container-level throttling.
Sparse Hikari samples with no waiters rule down saturation only at sample time.
Never convert temporal coincidence into exact request-to-PostgreSQL ownership.

## M1 worked example: snapshots versus workspace

The [H2 snapshot record](evidence/dashboard-snapshot-auth-query-bound-2026-10-08.md)
contains warm 200 snapshot pairs taking seconds, low request-thread CPU, and
sparse zero-waiter pool samples. These narrow sampled saturation; they neither
prove an operator deadline breach nor close historical 10/10 starvation.
The October 13 owner log window separately reported workspace GETs at 8.131,
15.364 and 16.650 seconds with 200, and 18.408 and 19.128 seconds with 204.
The exact served SHA and browser outcome for those historical IDs remain
unverified. In current source, `AccessController.getWorkspace()` returns the
`TenantWorkspaceAdministrationService.getWorkspace()` DTO. That service
composes tenant/users/warehouses, per-connector `describeConnector()`, active
incidents, support audit and counts without a read-only wrapper because incident
SLA transitions can write. `RequestTraceFilter` and `ApiExceptionHandler` can
emit 204 for abort-like exceptions. The frontend's workspace fetch participates
in a `Promise.all` with operators and users and has no explicit fetch timeout;
that does not exclude navigation, proxy or transport cancellation.

The first question is whether the operator received correct workspace data,
abandoned a pending request, or encountered a client failure. Existing backend
logs alone cannot establish which. Correlate one of the recorded request IDs
with a preserved client outcome and abort reason if available. If not, record
this as the precise M1 evidence gap and bound one representative warm tenant-
admin workspace read, including client completion and response content. Only
if it shows material delay or failure should query amplification versus DB,
resource, or transport wait become the next focused discriminator. Do not make
the 2-second snapshot warning the active defect merely because it is visible.

## Classification and exit

| Decision | Required proof and next move |
| --- | --- |
| Confirmed defect/blocker | Wrong/unsafe state, lost work, authority breach, failed client task, harmful resource exhaustion or accepted deadline breach. Preserve failure; smallest causal fix; focused and affected broader regression. |
| Measured infrastructure ceiling | Name the resource, same-window metrics and work blocked; propose smallest costed experiment. Owner approval precedes a paid change; compare identical workload before/after. |
| Known limitation, acceptance proposed | Correct client/engine outcome, bounded impact, measured distribution/headroom, mitigation, owner and revisit trigger. No acceptance until the owner approves the workflow budget; never waive Critical/High correctness/security issues. |
| Gate evidence gap | State exact required missing proof and minimum safe method; keep the gate open without inventing a code defect. |
| Monitor/defer | Noncritical optimization outside approved gate with no credible material harm; record owner and trigger, then move. |

Budgets are workflow-specific: login, tenant workspace, snapshot display and
freshness, action acknowledgement, ingest/order/fulfillment, alert/realtime,
and replay/recovery have different clocks and consequences. Propose the
representative tenant/warehouse/role, concurrent workload, max and tail latency,
abort/error allowance, freshness, resource headroom and degraded behavior for
owner approval. Until accepted, use `TBD`; no universal 2, 5 or 10 seconds.

An M-stage closes only against all of its [mapped criteria](SYNAPSCORE-MASTER-ENGINEERING-READINESS-MAP.md):
exact revision, relevant local/CI/PostgreSQL/hosted/representative evidence,
negative and concurrency paths, declared workload, repeatability and accepted
limitations. M1 is not merely snapshot speed; it includes connected source
ingest, order/reservation/fulfillment state, intelligence/dispatch, tenant and
warehouse authority, database/pool/worker headroom, realtime convergence and
recovery under its declared envelope. M2 repeatability, M6 connected-engine
proof and M7 frozen pilot-environment verification are separate; preparation
does not earn early gate credit.

For a proven fix, protect existing transaction, isolation, audit and recovery
contracts. Review the diff and failure regression; run relevant focused tests,
then the required broader/CI checks. Verify the exact deployed SHA and a
like-for-like hosted result when runtime claims depend on hosting. Keep a
rollback/stop condition for a material regression. Do not suppress warnings,
weaken assertions, shrink work, add retries, raise timeouts/pool limits, disable
schedulers or purchase infrastructure to manufacture a pass. Never include
credentials, tenant payloads or connection strings in evidence or commits.

Stop an investigation when its question is answered, a defect is fixed and
re-proven, an owner accepts a bounded limitation, or its declared evidence
budget is exhausted. Reopen on materially new client failure, stale/wrong data,
resource growth, lock/pool wait, security/recovery defect, workload or revision
change, or budget breach, not on another warning within an accepted envelope.

## One-page investigation record

```text
ID / date / owner / branch SHA / served SHA / environment / safe fixture:
Operator or engine contract; owner-approved deadline (or TBD):
Observed failure and client outcome; UTC and request/work-unit IDs:
Prior evidence and scope it rules down:
Top hypotheses and their falsifying observations:
Single next discriminator, maximum probes/time, stop condition:
Measured result: state, client/server/DB/resource timings and failures:
Classification and gate effect:
Smallest fix, if proven; focused/broader/CI/hosted verification:
Residual unknown, owner decision and reopen trigger:
Next highest-impact contract:
```

Daily progress should state the contract, new evidence, actual change, checks
that ran, uncertainty and one next action. More logs, probes or PRs alone do
not constitute progress.
