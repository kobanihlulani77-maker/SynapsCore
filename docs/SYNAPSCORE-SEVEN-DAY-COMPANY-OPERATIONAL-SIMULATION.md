# SynapseCore - Seven-Day Company Operational Simulation and Business Value Report

**Evidence date:** 2026-10-24. **Source baseline:**
`917c6a1aa7af59504838d2b83d53f3ab57b04648` (`origin/main`).
**Company:** Ubuntu Retail Distribution, entirely fictional. **Environment used for
execution:** isolated Spring `test` profile with an ephemeral H2 database. No
hosted tenant, customer record, credential, or production configuration was
changed for this report.

## 1. Executive overview

SynapseCore is the operating layer between incoming business activity and the
people who must respond to it. In this example, an order enters from a configured
source; stock is reserved; fulfillment work is created; low stock and other
conditions become visible; a failed import is retained for repair; and an
authorized operator can replay it. Management can see the resulting state and
evidence without interpreting each source system in isolation.

The **continuous seven-day company story below is an illustrative allocation**
of supported events to a working week. The linked full-operational integration
test executes a connected subset in seconds, not across seven elapsed days, and
in a different event order. Therefore the daily timestamps, store identities,
staff conversations, staffing levels, and business counterfactuals are
**simulation assumptions**, not captured application facts. The test's exact
stock arithmetic, durable records, role denials, and final counts are actual
assertions. Focused lifecycle tests substantiate additional behaviors but do
not turn every narrated weekly event into an executed seven-day trace.

This document demonstrates product purpose and current capability. It does not
authorize a customer pilot, establish an operating-week load envelope, or close
[M1](SYNAPSCORE-MASTER-ENGINEERING-READINESS-MAP.md).

## 2. The example company

Ubuntu Retail Distribution sells household essentials through an online channel
and supplies six stores. Six stores, an online channel, daily trading volume,
and the names below are **fictional business context**; stores and transport
vehicles are not asserted to be first-class SynapseCore records. The controlled
software slice uses two company warehouses, `L2-P7-WH-NORTH` (main DC) and
`L2-P7-WH-SOUTH` (regional hub), and a second isolated tenant with warehouse
`L2-P7-WH-BETA` for boundary checks. The executed fixture has four company
products (`P7-SKU-A`, `B`, `C`, and `RECOVERY`), two North connectors (webhook and
CSV), and scoped operator accounts. These intentionally small counts are a
demonstration fixture, **not** a proposed daily throughput or capacity promise.

The business uses its existing ordering and warehouse processes. SynapseCore
does not replace their ERP, WMS, purchasing system, carrier tracking, or human
judgment. A store code can be a customer reference on an order; it is not a
SynapseCore store master record. Physical stock receiving and a cycle count
must be supplied through authorized inventory operations; SynapseCore cannot
discover a shelf count by itself.

## 3. Deployment and integrations

In a real installation the tenant has named warehouses, products, users,
connector policies, operational thresholds and supported source credentials.
Webhook order intake, CSV order import and scheduled pull are implemented entry
paths. This scenario **executes webhook and CSV** through isolated MockMvc tests;
it does not connect to a real retailer system or exercise a live pull endpoint.
The connector authenticates and maps incoming order identity, warehouse and
lines; an order write reserves stock and creates fulfillment. Failed inbound
work and import evidence can enter Replay. [Webhook implementation](../backend/src/main/java/com/synapsecore/integration/ExternalOrderWebhookService.java),
[CSV implementation](../backend/src/main/java/com/synapsecore/integration/ExternalOrderCsvImportService.java),
[pull worker](../backend/src/main/java/com/synapsecore/integration/IntegrationScheduledPullWorkerService.java).

The operator workspace is React, backed by authenticated APIs. Operational
changes produce a durable dispatch item; after commit, dispatch can publish a
tenant realtime signal. The browser uses that signal to refresh authoritative
REST state, and has periodic repair behavior. This is not a guarantee that
every browser sees an update immediately or exactly once. [Dispatch queue](../backend/src/main/java/com/synapsecore/event/OperationalDispatchQueueService.java),
[browser reconciliation](../frontend/src/hooks/useWorkspaceRealtime.js).

## 4. Complete system and page inventory

The route list and initial navigation policy are implemented in
[pageRegistry.js](../frontend/src/config/pageRegistry.js). Unless a role is
specified below, the page is available to an authenticated workspace user,
subject to **backend** tenant/warehouse/role checks. Navigation visibility is
not permission to mutate. A realtime signal can refresh the common snapshot;
specialist pages can also use their own API reads. A failed read must not be
interpreted as a healthy empty result.

| Route / view | Who uses it; what it shows | Actual supported action and boundary |
| --- | --- | --- |
| `/dashboard` | Company operator sees summary, pressure, recent operational state and freshness. | Inspect and navigate; not an execution console. |
| `/alerts` | Operator sees active warning, severity, SKU/location and response guidance. | Inspect condition and source evidence; no general alert acknowledgement or stock purchase action is established here. |
| `/recommendations` | Operator sees current guidance, urgency and rationale. | Evaluate guidance; a recommendation does not create a transfer or purchase order. |
| `/orders` | Operator sees recent order identities, lines, status and warehouse. | Investigate; order intake/transitions use the supported authorized API/integration paths rather than an invented order-entry button here. |
| `/inventory` | Operator sees available/reserved/on-hand, risk and depletion estimate. | A `TENANT_ADMIN` with warehouse access can submit a reasoned, idempotency-keyed adjustment and refresh the readback. This is a broad admin authority, not a dedicated inventory role. [Page](../frontend/src/pages/Inventory.jsx). |
| `/catalog` | Tenant operator sees owned SKUs and import results. | Supported create/update/CSV product onboarding under backend authority. |
| `/locations` | Operator compares visible warehouse pressure. | Inspect site state; it is not a transport or store management system. |
| `/fulfillment` | Operator sees backlog, dispatch/delivery pressure and affected orders. | **Current page is observational** and states that no fulfillment mutation is exposed there; authorized transitions exist through backend/integration paths. [Page](../frontend/src/pages/Fulfillment.jsx). |
| `/scenarios` | Operator previews order impact and prepares a governed plan. | Preview/compare/save; projections are not live orders. |
| `/scenario-history` | Operator sees plans, revisions and decisions. | Inspect, compare and take only lifecycle-eligible next steps. |
| `/approvals` | `REVIEW_OWNER` or `FINAL_APPROVER` sees assigned decision work. | Approve/reject an eligible stage; cannot self-assign arbitrary authority. |
| `/escalations` | `ESCALATION_OWNER` sees overdue governance attention. | Acknowledge an escalation; not approve a plan merely by being escalation owner. |
| `/integrations` | `INTEGRATION_ADMIN`/`INTEGRATION_OPERATOR` sees connector and import state. | Capability- and scope-checked connector/import operations; secrets are not an information panel. |
| `/replay-queue` | Integration roles see failed inbound identities, cause and eligibility. | Authorized retry; a disabled connector or already-replayed item is not silently bypassed. |
| `/runtime` | Company operator sees application/runtime trust and incidents. | Inspect/refresh; no implication that opening this page repairs a queue. |
| `/audit-events` | Company operator reconstructs business events and audit entries. | Inspect supported history, not edit it. |
| `/users` | `TENANT_ADMIN` sees users, roles and warehouse scopes. | Supported user/operator changes with coverage safeguards. |
| `/company-settings` | `TENANT_ADMIN` sees workspace metadata, policies, warehouses and connector ownership. | Authorized administration; conflicts and coverage rules still apply. |
| `/profile` | Signed-in user sees own identity, session/password posture. | Own supported password/session actions. |
| `/platform-admin` | Platform Owner sees metadata-only portfolio posture and support signals. | Inspect platform overview, not tenant operational payload or inventory. |
| `/tenant-management` | Platform Owner sees tenant directory and provisioning. | Supported controlled tenant onboarding; not an invented tenant deletion or tenant-user password reset. |
| `/system-config` | Platform Owner sees display-safe runtime/config posture. | Inspect implemented defaults; not unrestricted live server editing. |
| `/platform-activity` | Platform Owner sees metadata-only activity. | Investigate platform changes without opening raw customer business records. |
| `/releases` | Platform Owner sees release/build trust. | Compare served identity and readiness; it does not deploy code from this page. |
| `/platform-sign-in` | Platform Owner signs into a separate session. | Tenant credentials cannot substitute. |
| `/sign-in`; `/`; `/product`; `/contact` | Tenant sign-in and public product/pilot pages. | Authentication or information/handoff, not operational controls. |

The first visible six inventory rows are a UI preview, not the complete
inventory ledger. Orders similarly emphasize recent records. The complete
operator experience must distinguish a bounded overview from all company
history. The [UI engineering inventory](SYNAPSCORE-MASTER-ENGINEERING-READINESS-MAP.md#8-m3---operator-experience-engineering)
records additional abnormal-state and high-volume questions; its phase labels
should not be read as proof that every interface has completed final design.

**Visual evidence limit:** Docker was unavailable during this report, so no
isolated frontend/backend browser session or authentic fictional-company
screenshot was captured. Hosted SynapseCore pages would show a different
tenant; presenting them as Ubuntu Retail Distribution would be misleading.
The linked components describe the implemented screens, not fabricated images.

## 5. Operator responsibilities

Business titles are personas, **not new access roles**. The implemented company
roles are `TENANT_ADMIN`, `INTEGRATION_ADMIN`, `INTEGRATION_OPERATOR`,
`REVIEW_OWNER`, `FINAL_APPROVER` and `ESCALATION_OWNER`.
[Access policy](../backend/src/main/java/com/synapsecore/access/AccessControlService.java).

| Fictional employee | Actual role and scope | Daily responsibility; cannot do |
| --- | --- | --- |
| Nomsa, operations manager | `TENANT_ADMIN`, tenant-wide | Sees the company picture, manages workspace/users, performs approved inventory corrections. Admin status does not confer the integration operator's fulfillment write or a review owner's independent decision. |
| Priya, integration lead | `INTEGRATION_ADMIN`, North | Configures the North sources, observes imports and may act as integration operator in that warehouse. Cannot alter South operational lanes without scope. |
| Kabelo, DC order and fulfillment operator | `INTEGRATION_OPERATOR`, North | Investigates and performs authorized ingestion/fulfillment/replay operations through supported paths. Cannot manage tenant users or perform an inventory-admin adjustment. |
| Lerato, regional operator | `INTEGRATION_OPERATOR`, South | Sees and acts within South. A North correction or fulfillment action is denied. |
| Amina, independent reviewer | `REVIEW_OWNER`, North | Reviews an assigned North scenario; cannot create a live order by approving it. |
| Sipho, final approver | `FINAL_APPROVER`, North | Handles a final approval stage when the policy requires it. Not every standard plan reaches that stage. |
| Thabo, escalation owner | `ESCALATION_OWNER`, North | Handles overdue governance attention; cannot become the reviewer by acknowledging it. |
| Platform support | Separate `PLATFORM_OWNER` session | Views portfolio metadata, rollout, runtime posture and release evidence. Cannot use platform authority to change Ubuntu stock or browse raw tenant orders. |

The isolated connected-flow fixture actually creates and signs in equivalent
roles, including another tenant for separation. The names here are illustrative;
the test uses synthetic account identities.

## 6. Monday - baseline and first work

**Illustrative business day.** Nomsa opens Dashboard and Inventory. The
test-backed baseline is four tenant products; North has `A=100`, `B=80`,
`C=60` on hand and no row yet for `RECOVERY`; South has `A/B/C=50` each.
The two North order sources are provisioned; no company orders or fulfillment
tasks exist before intake. A separate tenant has its own `A=70` in Beta.
These are **fixture numbers**, not a customer's measured stock.

Priya confirms the North webhook/CSV sources in Integrations. Kabelo sees no
order work yet. The Platform Owner, in a separate control plane, can inspect
the new workspace's metadata and release/runtime posture, but does not open
Ubuntu's raw order lines. The company has a shared starting position rather
than spreadsheet totals compiled from several systems.

**End-Monday ledger:** North `A 100/0/100`, `B 80/0/80`, `C 60/0/60`
(`on-hand/reserved/available`); two connectors, zero orders. Actual baseline
assertions: [connected-flow test](../backend/src/test/java/com/synapsecore/Layer2Phase7FullOperationalAcceptanceIntegrationTest.java).

## 7. Tuesday - an order consumes promiseable stock

A North webhook supplies order `L2-P7-ORDER-001` with eight units of A and six
of B. The connector policy maps source, warehouse and lines; the order service
reserves stock and opens queued fulfillment. Actual test assertions show:

| North SKU | Before on-hand / reserved / available | After accepted order |
| --- | --- | --- |
| A | `100 / 0 / 100` | `100 / 8 / 92` |
| B | `80 / 0 / 80` | `80 / 6 / 74` |

Kabelo can inspect the new order and its queued fulfillment. Nomsa sees less
available stock while physical on-hand remains unchanged until dispatch. This
distinction matters: the company must not promise reserved units to another
customer. The focused inventory test also challenges two concurrent orders
for a single available unit and expects one success, one conflict and no
oversell, but that is **a separate local test**, not a second Tuesday event.
[Order service](../backend/src/main/java/com/synapsecore/domain/service/OrderService.java),
[concurrency test](../backend/src/test/java/com/synapsecore/InventoryConcurrencyIntegrationTest.java).

**End-Tuesday narrative state:** one received order, 14 units reserved, one
queued fulfillment. The explicit weekday assignment is illustrative; the
before/after ledger is an executed assertion.

## 8. Wednesday - stock truth and failed intake

Nomsa records North C at ten units against a threshold of twenty through the
authorized inventory baseline path. This is a **reported stock correction**;
the test does not establish whether an actual store consumed fifty units, a
cycle count found a discrepancy, or a source had been wrong. SynapseCore's
alert feed then contains at least one active North-scoped condition; the
full-story test does not assert that the alert's SKU is C. Its
recommendations endpoint returns an array, but this particular full-story test
does not assert a named recommendation for C. Focused advisory lifecycle tests
cover specific reorder/transfer/retirement rules separately.

The North CSV source submits `L2-P7-RECOVERY-001` for two units of `RECOVERY`.
There is no North inventory row for that SKU, so the import reports **zero
imported, one failed**, with `INVENTORY_NOT_FOUND`; inbound status becomes
`REPLAY_QUEUED`, Replay becomes `PENDING`, and no order or fulfillment task is
created for that input. Priya or Kabelo can inspect the failure in Integrations
and Replay Queue. An operator must not report that failed CSV line as a sale
already accepted into stock. [Connected-flow test](../backend/src/test/java/com/synapsecore/Layer2Phase7FullOperationalAcceptanceIntegrationTest.java).

## 9. Thursday - connected pressure, not separate dashboard cards

The illustrative operations team works the accepted A/B order through picking,
packing and partial dispatch. The actual test checks a three-unit A dispatch:
`A 97 on-hand / 5 reserved / 92 available`, with three fulfilled and five
still reserved on that line. Dispatching the remaining five A units yields
`92 / 0 / 92`; dispatching six B units yields `74 / 0 / 74`. The available
number need not jump when a reserved unit is dispatched: one unit leaves both
physical on-hand and the outstanding reservation.

Meanwhile C is below its threshold, and the failed `RECOVERY` import is still
pending. Dashboard, Orders, Inventory, Fulfillment, Alerts, Integrations and
Replay expose different aspects of the **same operational picture**: an
accepted order progressing, a newly constrained product, and an unaccepted
source event. The system does not prove that the C condition caused the A/B
order or that the replay is automatically safe before stock is established.
The operator's value is seeing those distinctions together, then choosing
the correct authorized path for each one.

## 10. Friday - repair and governed decision

Nomsa establishes ten North units of `RECOVERY` through an authorized
inventory update. Kabelo then retries the pending import. The test checks a
`REPLAYED` status and exactly one newly created order with two reserved units,
a queued fulfillment task, and inventory `10 / 2 / 8`. A second replay
attempt is rejected. This is a real supported recovery flow, **not** a direct
database repair. [Replay service](../backend/src/main/java/com/synapsecore/integration/IntegrationReplayService.java).

For a proposed additional order, Kabelo previews impact and saves a North
scenario. Amina reviews the assigned plan. The test checks requester and
review-owner identity, non-executable saved state, approval stage, and **no
change** to live orders, inventory, fulfillment, alerts, recommendations or
replay counts from preview/save/approval. A legacy `/execute` request receives
`410 Gone`. Nomsa must separately authorize and carry out any real business
action through an appropriate operational path. Approval is a governed
decision record, not autonomous purchase, replenishment or order execution.
[Scenario test assertions](../backend/src/test/java/com/synapsecore/Layer2Phase7FullOperationalAcceptanceIntegrationTest.java).

## 11. Saturday - continuity with fewer people present

**Implemented behavior, not an executed seven-day Saturday observation:**
scheduled recommendation reconciliation, automated replay eligibility checks,
scheduled pull where configured, and dispatch queue draining can continue
without a staff member holding a page open. Recommendation reconciliation
re-evaluates authoritative inventory and active fulfillment inputs. The browser
has a live-refresh and degraded-refresh path, but a missed message can delay
display until an authoritative repair read. [Scheduler](../backend/src/main/java/com/synapsecore/decision/RecommendationReconciliationService.java),
[dispatch](../backend/src/main/java/com/synapsecore/event/OperationalDispatchQueueService.java),
[frontend repair](../frontend/src/hooks/useWorkspaceRealtime.js).

No controlled dependency interruption or actual overnight queue accumulation
was run in this simulation. Existing disposable-PostgreSQL worker-kill tests
prove particular lease-reclaim boundaries, not uninterrupted weekend service
or a precise time-to-recovery. A fictional employee checking Runtime on
Saturday is a workflow illustration, not a captured login.

## 12. Sunday - weekly review

The connected-flow test ends with **two** tenant A orders, **four** products,
**two** tenant A warehouses, committed business-event and successful audit
evidence, and no remaining `PENDING` or `PROCESSING` dispatch work item in the
test fixture. Its first order is delivered; the recovered order remains
received with a queued fulfillment task. This is not a claim that all of the
fictional company's weekly demand was fulfilled.

| North SKU | Monday on-hand / reserved / available | Sunday test-backed state | Explanation supported by fixture |
| --- | --- | --- | --- |
| A | `100 / 0 / 100` | `92 / 0 / 92` | Eight units reserved then fulfilled. |
| B | `80 / 0 / 80` | `74 / 0 / 74` | Six units reserved then fulfilled. |
| C | `60 / 0 / 60` | `10 / 0 / 10` | Absolute inventory correction to ten; not attributed to a demonstrated sale. |
| RECOVERY | No North inventory row | `10 / 2 / 8` | Ten units established, then two reserved by successful Replay. |

South A/B/C stay at 50 in this fixture. The separate tenant Beta's A stays
`70 / 0 / 70`; wrong-tenant attempts cannot mutate it. A manager can use the
existing pages and audit timeline to reconstruct these facts. SynapseCore does
**not** have a dedicated tested weekly business report that automatically
produces this table; this document derives it from test assertions.

## 13. Complete operator experience

Nomsa begins in Dashboard to locate pressure, opens Inventory to verify
on-hand versus reserved stock, checks Alerts/Recommendations for advisory
conditions, then makes a reasoned correction only after confirming the source
of truth. She sees a success and refreshed readback. Her Tenant Admin role is
broad; it must not be described as a fine-grained inventory-clerk role.

Kabelo starts from Orders/Fulfillment for accepted work and Integrations/Replay
for failed source work. The Fulfillment page shows backlog and timing but does
not expose a mutation button; supported fulfillment transitions are integration-
authorized backend operations. Replay is not a way to bypass missing stock or
a disabled connector. He can also preview a scenario, but cannot turn that
preview or its approval into an ungoverned live order.

Priya maintains North connector configuration and investigates source attempts.
Amina receives a North review assignment and sees evidence before making an
eligible decision. Sipho is involved only when a final stage is required;
Thabo handles SLA escalation attention without inheriting approval authority.
Lerato can operate South but cannot correct North merely because she can see
company-level context. Their work connects through shared authoritative
order, inventory, fulfillment, replay and governance records, not through
informal reinterpretation of dashboard cards.

## 14. Platform Owner experience

The Platform Owner signs into `/platform-sign-in`, not Ubuntu's tenant login.
The control plane has Platform Overview, Tenant Directory, Platform Runtime,
Platform Activity and Release Trust. It can show workspace metadata, rollout
posture, display-safe operational health and activity, and support controlled
tenant provisioning. It cannot use platform authority to make Ubuntu's
inventory correction or inspect arbitrary tenant order payloads. The isolated
test explicitly denies a platform session's inventory write.
[Platform application](../frontend/src/components/PlatformApplication.jsx),
[platform overview](../frontend/src/pages/PlatformAdmin.jsx).

Platform health is not a substitute for the company's business ledger. A green
platform readiness indicator does not tell Nomsa that the CSV import failed;
the tenant's Integrations/Replay evidence does.

## 15. End-to-end processing evidence

The following are **executed assertion chains**, not raw production event IDs
or seven dated audit exports. Event labels `E01`-`E05` are document identifiers.
The source test records actual synthetic order identities, business events,
audit evidence, and before/after values. It does not print every request ID
or a chronological seven-day trace.

| Event | Origin / entry / validation | Authoritative effect and derived information | Operator result, action, evidence status |
| --- | --- | --- | --- |
| E01: first accepted order | North webhook, `L2-P7-ORDER-001`, A x8 and B x6; authenticated connector and mapped warehouse. | One received order, A and B reservations, one queued fulfillment. `ORDER_INGESTED` business event and successful audit target later asserted. | Orders/Inventory/Fulfillment can display resulting state. Exact ledger is **executed locally**. |
| E02: partial then full dispatch | Authorized integration operator submits PICKING, PACKED, DISPATCHED units, then DELIVERED through the fulfillment API. | A after first three dispatched: `97/5/92`; after all eight: `92/0/92`. B ends `74/0/74`. Repeated delivery does not consume twice. | Fulfillment and Orders can show changed status. Ledger and final delivered state are **executed locally**; live browser rendering is not part of this test. |
| E03: low stock | Tenant Admin reports C North at ten with threshold twenty through inventory update. | Active alert feed has at least one North-scoped condition; recommendations endpoint is an array. | Alerts/Inventory show a condition to investigate. **Executed locally**, but the full-story test does **not** assert that the alert targets C or that a named C recommendation exists. |
| E04: failed CSV and repair | North CSV order `L2-P7-RECOVERY-001`, RECOVERY x2, initially no stock row. | Import 0 accepted/1 failed; `INVENTORY_NOT_FOUND`; inbound `REPLAY_QUEUED`, Replay `PENDING`; no order/task. After inventory 10 and authorized replay, one received order, queued task, `10/2/8`; repeat retry denied. | Integrations and Replay identify cause and supported corrective path. State/replay identity is **executed locally**. |
| E05: governed plan | North order-impact preview, save and Review Owner approval. | Scenario evidence changes; live order, stock, fulfillment, alert, recommendation and replay counts do **not** change. Legacy execute is denied. | Scenarios/History/Approvals expose decision history. Non-execution and authority are **executed locally**. |

The main assertion source is
[Layer2Phase7FullOperationalAcceptanceIntegrationTest](../backend/src/test/java/com/synapsecore/Layer2Phase7FullOperationalAcceptanceIntegrationTest.java).
Additional distinct tests are
[inventory contention](../backend/src/test/java/com/synapsecore/InventoryConcurrencyIntegrationTest.java),
[alert identity](../backend/src/test/java/com/synapsecore/AlertLifecyclePhase1IntegrationTest.java),
[recommendation lifecycle](../backend/src/test/java/com/synapsecore/RecommendationLifecyclePhase2IntegrationTest.java),
and [Replay recovery](../backend/src/test/java/com/synapsecore/Layer2Phase4ReplayRecoveryIntegrationTest.java).
Their results must be read as separate fixtures, not extra events added to this
company's weekly totals.

## 16. Demonstrated business benefits

| Business problem | What SynapseCore does in this evidence-backed slice | What an operator gains; limitation |
| --- | --- | --- |
| Promising stock already allocated | E01 reserves A/B and distinguishes on-hand from available; E02 converts reservations into physical consumption. | Nomsa/Kabelo can see why available stock fell before dispatch. Prevention of every oversell at hosted scale is **not** established by this slice. |
| A source event fails outside the normal flow | E04 preserves failed inbound and Replay evidence instead of pretending an order exists. | Priya sees the cause and Kabelo has a controlled recovery path. Automatic repair is not implied. |
| A condition needs attention | E03 surfaces a low-stock condition tied to North stock and threshold. | Nomsa knows which lane requires investigation. The system does not know a physical shelf count absent supplied data. |
| People disagree about order progress | E02 links order status, line quantities, inventory and fulfillment task. | The team can inspect a common state rather than reconcile unrelated status messages. This assumes the source transitions are truthful. |
| An attractive proposal might be mistaken for an approved live action | E05 keeps projection and approval non-executing and attributed to authorized people. | Management can consider the plan without accidental stock/order mutation. They still need a separate real operational action. |
| Accountability after change | E01/E02/E04/E05 leave business-event, import/replay, scenario and audit evidence. | An investigator can reconstruct a supported path. This is not a claim of a cryptographically immutable audit ledger. |
| Separate warehouse/tenant lanes | The fixture denies South-to-North and Beta-to-Ubuntu operations. | Managers retain shared context without granting arbitrary write authority. Broader adversarial isolation remains a later gate. |

No labor savings, revenue gain, lost-sale prevention percentage or return on
investment was measured. Those would require a real company baseline and pilot.

## 17. With SynapseCore versus without it

The counterfactual assumes a competent company with an ERP/WMS and local alerts,
not a business with no systems.

| Incident | With SynapseCore in this supported slice | Without SynapseCore; plausible existing practice | Incremental advantage and uncertainty |
| --- | --- | --- | --- |
| A/B order pressure | One order identity links reservations and fulfillment state, with a common operator view. | ERP/WMS likely has order and stock state; teams may already have an equivalent joined view. | Advantage exists only if SynapseCore makes the cross-domain relationship easier/fresher for these operators; no time saved measured. |
| C falls below threshold | An active warehouse-scoped alert ties a condition to reported stock. | WMS may already alert on reorder thresholds; a manager could see it in a stock report. | SynapseCore adds value if it puts the condition alongside orders and fulfillment attention; its alert alone may not be differentiated. |
| CSV cannot create an order | Rejected inbound and pending Replay remain visible, with a controlled retry after stock is corrected. | Integration middleware may already have a dead-letter queue; otherwise staff might compare import logs to ERP orders. | The cross-system failure-to-order relationship and safe operator retry are concrete product strengths, but comparative recovery time is unmeasured. |
| Partial dispatch | Reserved, fulfilled and on-hand units remain reconcilable. | A competent WMS already tracks pick/ship quantities. | SynapseCore's incremental value is the joined operational picture and evidence, not invention of shipment accounting. |
| Proposed follow-up order | Preview and independent approval create a decision record without live execution. | Company may use spreadsheets, ERP approvals, or a planning module. | SynapseCore can make the reasoning visible in its operational context; superiority over an incumbent approval workflow needs customer validation. |

## 18. Weekly results and conservation ledger

**These totals belong only to the executed isolated connected-flow fixture.**
They are not totals for a real six-store working week, and focused test fixtures
are excluded.

| Supported measurement | Result and provenance |
| --- | --- |
| Company tenant products / warehouses | 4 / 2, asserted final dashboard summary. |
| Accepted orders | 2: webhook primary and repaired CSV order; final dashboard summary. |
| Failed CSV before repair | 1 failed, 0 imported in that attempt; error `INVENTORY_NOT_FOUND`. |
| Replay outcomes | One pending record repaired to replayed; repeated retry rejected. |
| Primary order fulfillment | A x8 and B x6 dispatched and delivered; repeat delivery idempotent in fixture. |
| Recovered order fulfillment | Two units reserved, task still queued; **not delivered**. |
| Low-stock alerts | At least one active North condition; exact weekly creation/resolution count not asserted here. |
| Recommendations | Endpoint returns an array; exact fixture count/currentness not asserted here. Separate lifecycle tests exercise rules. |
| Scenario operations | Preview, save and review approval; zero live operational-count changes attributable to them. |
| Dispatch | No tenant A `PENDING` or `PROCESSING` work items at the test's final wait boundary; actual browser delivery not measured. |
| Business/audit evidence | Specified order, fulfillment, replay and scenario event types plus an order audit target asserted; exhaustive weekly audit count not asserted. |
| Other daily volume, cycle-time and financial measures | Not established by this isolated test. |

Stock conservation for the primary order is direct: A began with 100 on hand,
eight fulfilled, and ends with 92; B began with 80, six fulfilled, and ends
with 74. The recovered order begins with ten on hand, reserves two, and ends
`10 on-hand - 2 reserved = 8 available`. C's `60 -> 10` is an absolute reported
change, not a calculated sale. No test-backed claim is made about all possible
commercial stock movements across a real week.

## 19. Current capabilities and gaps

| Classification | Capability or expectation |
| --- | --- |
| **Implemented and executed in the isolated test** | Tenant/product/warehouse onboarding, webhook accepted order, reservation, fulfillment transitions, low-stock alert presence, failed CSV evidence, repair and Replay, Scenario non-execution and approval, key role/scope denials, final dashboard/event/audit readback. |
| **Implemented but not verified in this company-week run** | Scheduled external pull, actual browser/WebSocket delivery, overnight reconciliation, exact recommendation produced for C, final-stage/escalation case, multiple concurrent real operators, platform UI navigation and live support action. Other tests/evidence address portions separately. |
| **Partially implemented as an end-to-end operator journey** | Fulfillment has backend-authorized transitions but its current page is observational. Inventory maintenance uses the broad Tenant Admin role rather than a dedicated inventory-only permission. Recent/bounded views are not a full historical weekly report. |
| **Not implemented or not established here** | A store master/transport fleet/purchasing transaction as an independent SynapseCore operational domain; automatic purchase order or stock transfer execution from a recommendation; automatic live order execution after Scenario approval; dedicated weekly business report; measured ROI. |
| **Blocked by this execution environment** | Isolated Docker/PostgreSQL/Redis frontend session and authentic screenshots. Docker Desktop engine did not respond. H2/MockMvc tests remain useful functional evidence, but not production-shaped browser, PostgreSQL or hosted proof. |

An implemented path can still have an M1 reliability or M2 repeatability gap.
The classification above describes the **simulation run**, not a blanket final
readiness assessment.

## 20. Final product assessment

SynapseCore gives this fictional company a **connected operational view and
controlled response path**. It accepts supported business activity, preserves
order and stock relationships, highlights reported conditions, retains failed
intake for investigation and retry, and separates live operational changes
from governed hypothetical plans. The most persuasive demonstration is the
`E01 -> E02 -> E04` chain: accepted order and reservation, honest partial
fulfillment arithmetic, then a failed import that becomes one valid order only
after stock is established and an authorized replay occurs. Show Nomsa's
Dashboard/Inventory/Alerts view and Kabelo's Orders/Replay investigation next
to the actual ledger, not a generic collection of metric cards.

An operator can genuinely inspect a scoped company picture; a Tenant Admin can
make a controlled inventory adjustment; integration-authorized personnel can
use the supported intake/fulfillment/replay APIs; reviewers can make governed
Scenario decisions. The system does **not** autonomously procure, dispatch,
change the company's ERP, or execute an approved scenario.

This is compelling enough for a **truthful product walkthrough** with a
prospective company, clearly labeled as a synthetic isolated demonstration.
A real pilot still requires the M-stage gates: repeatable hosted connected-flow
proof on a frozen candidate, accepted workload and recovery behavior, final
operator UX and security review, and actual customer-source mappings. The
right product claim is not that SynapseCore replaces capable existing systems.
It is that it can help the people using them understand **what changed, what
matters, who may act, and whether the attempted recovery really worked**.

## Verification record

Run from a clean `origin/main` worktree on 2026-10-24:

```text
Backend: Java 21, Spring test profile, H2 PostgreSQL compatibility mode
Command: .\mvnw.cmd -B '-Dtest=Layer2Phase7FullOperationalAcceptanceIntegrationTest,InventoryConcurrencyIntegrationTest,AlertLifecyclePhase1IntegrationTest,RecommendationLifecyclePhase2IntegrationTest,Layer2Phase4ReplayRecoveryIntegrationTest' test
Selected tests: full operational acceptance; inventory concurrency;
alert identity; recommendation lifecycle; Replay recovery.
Result: 21 tests passed, 0 failures, 0 errors, 0 skipped.
Breakdown: full operational acceptance 1; inventory concurrency 11;
alert lifecycle 3; recommendation lifecycle 3; Replay recovery 3.
```

No hosted tenant was used for mutations. No authentic fictional-company
screenshot was produced. The daily narrative is a business-facing simulation;
its executed facts are exactly the isolated test assertions identified above.
