# SynapseCore — Product, Problem and Market Research Foundation
**Established:** 2026-10-03 (Africa/Johannesburg)  
**Status:** Research operating framework; not a capability certification, competitive verdict or customer qualification.  
**Engineering source of truth:** [SYNAPSCORE-MASTER-ENGINEERING-READINESS-MAP.md](SYNAPSCORE-MASTER-ENGINEERING-READINESS-MAP.md).  
**Pilot implementation boundary:** [company-integration-setup-runbook.md](company-integration-setup-runbook.md).  
**Company-level evidence:** [pilot-prospect-research-2026-10-03.md](pilot-prospect-research-2026-10-03.md).

## Governing distinction: end-M product, not the M1 snapshot

**SynapseCore's identity is the complete operational intelligence and control system the founder has defined.** The M0-M8 engineering and adoption programme is the route to delivering, hardening, proving and introducing it; the system's meaning does not shrink to whatever the currently deployed M1 revision can do.

**Our primary research baseline is the finished, connected product as specified in the canonical master roadmap:** supported company facts and trustworthy operational state; intelligence, risk/pressure, prioritisation and advice; operator visibility and control; explicit authority, review, approval and escalation; supported real operational actions/external handoff; verification of outcomes; integrations and failure/replay/recovery; real-time convergence, audit, security and tenant isolation. Understand these functions together and in ordinary operations, not as a stack of isolated feature pitches. The finished product is not merely a blueprint or a dashboard: it has a working operational lifecycle, human roles, correct state transitions, enforcement, observability, and measurable results.

**Stages have distinct meanings:** M1-M5 complete reliability, repeatable proof, UI/operator experience, adversarial review and remediation; **M6 accepts the finished connected engine** against every supported pilot-in-scope interaction; **M7** establishes and re-verifies the selected company configuration/integration on the approved operating environment; **M8** observes actual company usefulness during the controlled pilot. Completing stages does not silently add unspecified adapters, execute hypothetical scenarios, or make every future industry-specific domain native; separately envisioned capabilities require an explicit engineering scope decision.

**Research order:** investigate the full end-M product and the entire fragmentation/coordination/control problem first; research incumbents, adjacent alternatives and the market on equal terms; then test how a company could benefit and plan a technically credible pilot. The current M1 connector posture belongs in a delivery/readiness ledger, not in the definition of SynapseCore or a premature restriction of its market.

---

## 1. Correct mandate

Research must proceed in this order, iteratively:
**Understand the complete product and intended direction -> understand the full operating problem and its economic/organizational causes -> understand the whole available market, incumbent products and alternative processes -> reconstruct each company's operational model -> test a concrete customer value proposition and feasible pilot.**

Do not reverse-engineer a small market niche from a conveniently available connector or force a differentiation from a named rival. Do not invent a gap merely because a system is old, department-specific or does not use AI. Competing products may address the same end-to-end problem; an existing solution does not eliminate market entry, but a customer still needs a credible reason to adopt or add SynapseCore.

Working ERP, WMS, supply planning, transport, BI, middleware, control tower and human processes are all possible inputs and alternatives. The research object is the **complete operating outcome** and how different systems, people, functions, authorities, decisions and actions achieve it. A good existing departmental subsystem is neither proof of fragmentation nor a reason to exclude a prospect.

## 2. Full connected product — intended finished M-programme system and implementation ledger

### Complete end-to-end operational loop of the specified finished product
1. **External facts and trust:** supported inbound order webhook/CSV/scheduled pull and direct operational APIs; source identity, validation, tenant/warehouse authorization, connector telemetry and freshness.
2. **Operational truth:** accepted order, catalog, inventory, reservation, fulfillment and relevant integration facts maintained with correct lifecycle and tenant/warehouse isolation.
3. **Operational synthesis:** tenant/role/scope-relevant combined snapshot, status, condition, dependency and cross-domain pressure. Real-time signals are notifications, not the durable source of truth.
4. **Intelligence:** condition-based alerts, pressure/risk prediction where implemented, prioritization and *advisory* recommendations, with evidence/context and explicit distinction between actual and hypothetical state.
5. **Operator control:** intelligible working surfaces for the responsible authorized people, role/assignment/warehouse restrictions and clear exception attention.
6. **Governed decisions:** hypothetical scenario preview/save, review, approval/rejection/revision and overdue escalation under correct authority; approved hypothetical scenarios do **not** automatically execute operational writes.
7. **Real-world action/handoff:** only individually implemented and authorized live operations are performed in SynapseCore; other customer actions occur in authoritative source systems through approved human/external handoff.
8. **Observed outcome:** authoritative source readback, REST reconciliation, events, scoped realtime updates, audit/history and correction of degraded/missed updates.
9. **Resilience and recovery:** connector health and failed-input evidence, replay under permissions, safe retries/transaction boundaries, observability and bounded runtime resource demand.
10. **Multi-company control:** tenant isolation, warehouse/role scopes, company onboarding and a separate metadata-only platform support identity. Do not conflate a platform owner with customer operational authority.

### Broader problem/direction that should be investigated
The strategic need spans *the connection* among orders, inventory, demand, fulfillment, procurement/supply, production, logistics, external partners, operator decisions, customer commitments, approvals, integration reliability, recovery, operational trust and organizational oversight as relevant to the industry. These are domains to research, **not claims that every domain is currently implemented or ingestible**. More source systems and domains may be needed for the full future proposition in some industries.

### Implementation and delivery evidence — not the product definition
- Canonical engineering map states Java/Spring Boot, PostgreSQL, Redis/realtime, operational snapshots, supported order/inventory/fulfillment operations, condition alerts, recommendations, governance, Replay, platform/workspace UI and bounded multi-tenant/role controls.
- Company integration runbook states connector types are `WEBHOOK_ORDER` and `CSV_ORDER_IMPORT`; modes include realtime push, batch CSV and limited scheduled pull; this is **not** a general ETL connector platform or universal ERP/WMS/production/fleet/finance integration. No arbitrary mapping UI and no dedicated outbound auth for scheduled pulls. Additional approved direct operational APIs and catalog/inventory onboarding have separate contracts.
- Recommendation is not proof of action. Hypothetical Scenario approval never creates an order, mutates inventory or performs fulfillment; its execute endpoint returns 410. Real company systems remain authoritative unless a specific supported workflow is approved.
- Do not infer accepted hosted performance, supported enterprise scale or pilot clearance from local builds or earlier tests. Engineering master records M1 open; M2-M6 acceptance and M7 customer-specific verification are required before a live pilot.
- All capability claims need a current source code/API/evidence check by Codex; this document is the research frame, not a replacement for that check.

## 3. The problem: full causal taxonomy, not a single symptom

Research must investigate whether and how an organization experiences each relevant category:
- **Truth fragmentation:** duplicated/inconsistent identities, state, timing, freshness and source authority across systems.
- **Context fragmentation:** each system can be correct locally while cross-domain consequences and dependencies are difficult to understand collectively.
- **Priority fragmentation:** competing customer, cost, capacity, risk and service objectives with no adequate common context for making trade-offs.
- **Decision fragmentation:** data exists but authority, approvals, alternatives, accountability and handoffs are spread over roles and applications.
- **Action fragmentation:** recommendations or decisions fail to reach the appropriate execution owners/source systems, or completion is not proven.
- **Exception fragmentation:** incidents, source failures, stale signals, retries and recovery can be invisible across operating boundaries.
- **Organizational fragmentation:** different teams, locations, legal entities and third-party partners have different permissions, incentives and responsibilities.
- **Economic fragmentation:** an apparent local optimisation may worsen a wider service, working-capital, throughput, revenue, quality or contractual outcome.
- **Operational trust:** unreliable technology, unbounded latency, stale displays, missing audit or uncertain source authority undermine decision quality.
- **Existing satisfactory coordination:** sometimes the organization already has good integrated processes and tools; document those as positive evidence, not as a problem to manufacture.

Study how these categories interact in **ordinary operating days and exceptional periods**, including growth, seasonal peaks, outages, supplier shocks, and strategic operational change. Do not assume that a company must be in crisis to obtain value.

## 4. Market boundary: study the whole competitive and substitute landscape

Include competing and adjacent categories without assuming false separation:
- enterprise operational intelligence, decision intelligence and process orchestration;
- supply-chain command centres/control towers, multi-party coordination and execution;
- concurrent planning, demand/supply planning, forecasting and cross-functional decision systems;
- ERP and ERP suites, warehouse and transport management, MES/production and industry-specific operations platforms;
- data integration, iPaaS/event platforms, data observability, process mining and workflow systems;
- BI/analytics, incident operations and industry command-centre systems;
- built-in integrations, custom enterprise software, spreadsheets, email, calls and existing human operating routines.

Named market references for overlapping claims:
- Aera Decision Cloud: https://www.aeratechnology.com/aera-decision-cloud/
- Blue Yonder Supply Chain Command Center: https://blueyonder.com/solutions/supply-chain-command-center
- Kinaxis Maestro/control tower: https://www.kinaxis.com/en/solutions/control-tower
- SAPICS, 2026-08-18, collaborative supply-chain operating models and accountable decisions: https://www.sapics.org.za/news/silos-shared-solutions-rethinking-supply-chain-resilience-africa
- KPMG, September 2026, South Africa-hosted global report on enterprise supply-chain integration, decision intelligence and governance: https://kpmg.com/za/en/insights/2026/09/Transforming-the-Enterprise-supply-chain.html
Vendor websites express vendor claims; do not infer independently validated performance or competitor shortcomings from them. Industry/global evidence is not proof of a specific South African company's pain.

### Competitive research questions
For every relevant vendor, assess documented data/source breadth, industry/operating scope, truth model, event/exception lifecycle, prediction and recommendations, authority/governance, simulation vs execution semantics, action/writeback, cross-party participation, audit/recovery, resilience, deployment/integration requirements, customer-size assumptions, commercial model and documented proof/customer cases. Mark **verified, marketed claim, not established, planned or unsupported** as appropriate. Comparison can show overlap and different trade-offs; never force a manufactured unique selling proposition or promise unverified feature parity.

## 5. Customer operating-model research

Start from the **operating chain**, not company name or warehouse/store count. Follow critical decisions across internal systems, people, physical sites, customer/supplier relationships and third parties. Reconstruct one routine high-volume flow, one compound exception, one integration failure/recovery and one governed decision.

For a company dossier, document:
1. the actual business model, stakeholders, value chain and operational objective;
2. the authoritative systems and processes, their connected interfaces and source-of-truth ownership;
3. routine and exceptional event flows and where actual cross-system choices are made;
4. company-wide consequence of competing decisions (include human and financial effects only if grounded);
5. operating authority, escalation and governance;
6. what existing tools and teams already do adequately;
7. what might improve and how to test that **without assuming the company has a defect**;
8. technical/data/security constraints, integration rights, measurable baseline and sponsor;
9. full strategic market fit, separate from near-term supported pilot feasibility;
10. adoption burden, alternatives, economics and the customer's reason to buy or not buy.

Maintain three evidence labels: **verified public fact, research hypothesis, company-confirmed finding**. No qualified-company or buying-intent claim until appropriate discovery.

## 6. Research-to-product and sales decisions

- First prove our product identity and supported contracts; then assess operational need and alternatives.
- We may compete directly, complement existing technology, or identify a use case requiring further product development. Do not artificially squeeze into a narrow gap.
- A first pilot should test a meaningful connected operating journey with multiple relevant decision/operational dimensions, **not** a single dashboard or a fictional full-enterprise deployment.
- The pilot source-data contract must fit today's supported APIs or wait for separately authorized engineering. An order webhook alone is not production or finance observability. Neither governed scenarios nor advisory recommendations imply external execution.
- Agree measurement against customer baseline (detection, investigation, decision, coordination, throughput/service impact, recovery, source freshness, audit and total operating burden as applicable).
- Compete on empirically demonstrated effectiveness, trust, fit, implementation burden, value and economics; never on invented exclusive capabilities.
- Research and conditional discovery run in parallel with M1; no unsupported technical claims, customer data intake or live pilot before M6 technical acceptance and M7 company-specific authorization.

## 7. Immediate research work packages

**P1 — Product X-ray:** First reconstruct the complete specified end-M product and its fully connected operational loop from product intent, architecture and the canonical acceptance contract. Then inspect code, APIs and evidence to track which pieces have been implemented, which require hardening/proof, which are accepted at M6 and which are explicitly separate future expansion. Include provenance, governance, recovery, authority and operational outcomes. Never use the current delivery ledger to redefine the finished product.

**P2 — Problem X-ray:** Build causal maps of fragmented truth, priority, decision, action and recovery across at least industrial distribution, integrated production/agribusiness, multi-party logistics and critical-service coordination. Distinguish structural opportunity from company-verified pain.

**P3 — Market X-ray:** Map overlapping platforms and substituting approaches by actual functions, customer adoption patterns, economics, integration burden and relevant case studies, including strong incumbents. Do not assume SynapseCore must be categorically different.

**P4 — Prospect operations:** Continue BMG, VKB/Multi Green, Masterparts, Plumblink, Matus, Bidvest Afcom and Renttech under the complete control-loop lens; expand where better real-world operating models emerge. No name becomes a selection by repetition.

**P5 — Customer validation:** Prepare consequence-focused discovery, map decision makers, validate existing satisfactory capability and possible incremental value, and document supported minimum connected pilot data and acceptance criteria for the owner.

### Governing test
**Does this work improve our understanding of the full intended end-M SynapseCore operational mechanism, the enterprise-wide operating problem, the entire realistic market, and the evidence required for customer adoption?** If not, change the research approach—not SynapseCore's identity. Current code-readiness findings inform release engineering and factual promises; they do not restrict the market thesis.
