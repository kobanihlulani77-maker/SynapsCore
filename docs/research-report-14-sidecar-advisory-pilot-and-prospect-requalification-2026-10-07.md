# SynapseCore Research Report 14 — Whole-Picture Control Pilot and Prospect Requalification
**Date:** 2026-10-07 (Africa/Johannesburg)
**Mode:** JARVIS / responsible-partner execution
**Purpose:** Correct the prospect-selection and pilot language so reduced data volume/integration burden is never confused with a reduced SynapseCore operating role.

---

## 1. Founder correction

SynapseCore must not be positioned as a small analytics sidecar, a dashboard, or a single-domain order/inventory/fulfillment aid.

The pilot principle is:

> **Whole-picture control system, selective data footprint.**

This means:
- company source systems remain authoritative for the facts/actions they own;
- SynapseCore does not need every row, every historical record or every enterprise system;
- but the pilot data set must be sufficient to exercise the **connected SynapseCore operating picture**, not one isolated feed;
- SynapseCore ingests the relevant supported facts into its own tenant-scoped operational model;
- it synthesizes cross-domain state, conditions and pressure;
- it produces alerts, prioritisation, risk/pressure intelligence and advisory recommendations;
- it exposes that picture to authorized operators;
- governance, authority, audit, realtime convergence, replay/recovery and observed outcomes remain part of the system;
- human operators and approved source systems retain the applicable real-world execution authority.

Therefore **bounded** refers to company footprint, workload and unnecessary data volume — not to shrinking SynapseCore into one domain.

Examples of valid bounded scope:
- selected region rather than entire national company;
- selected DC + connected branches rather than every site;
- selected operational period/volume rather than all history;
- only the fields/records required from each authoritative source rather than full database replication.

Within that scope, the goal is still a **whole connected control picture**.

---

## 2. Canonical engineering alignment

The master readiness map already requires M6 to execute complete connected journeys rather than component tests:
- normal source-to-operator;
- cross-domain pressure and intelligence;
- governed authorization without pretending advisory state is execution;
- integration failure through Replay and reconciled readback;
- mixed concurrency/dependency failure through recovery.

The currently implemented connected engine includes, as applicable to the supported pilot scope:
- catalog/product state;
- inventory and reservation state;
- orders;
- fulfillment;
- integration/connector state and freshness;
- alerts and recommendations;
- operational intelligence/pressure;
- governance/authority and scenarios;
- activity/audit/events;
- realtime + REST readback;
- replay/recovery;
- tenant/warehouse/role isolation.

The connector model is currently strongest for order ingestion, but the **product proof is not the order connector**. The connector is only one ingress path into the larger connected operating engine.

Industry-specific future domains such as arbitrary procurement, finance, production/MES, fleet or every external platform are not silently claimed as native. Where those facts are not needed for the selected pilot, they remain outside the data contract. Where they are essential, support must be explicitly engineered/approved rather than faked.

---

## 3. Correct meaning of selective/minimum data

Do **not** say:
> We only need a small order feed to show value.

That makes SynapseCore sound like analytics on top of an ERP/WMS.

Say instead:
> We do not need to replicate your entire technology estate or all of your data. For the agreed pilot scope, we identify the authoritative operational facts needed across the relevant connected domains, ingest only the required records/fields, and use them to build the whole SynapseCore control picture for that operating scope.

So **minimum data** means:
- no unnecessary history;
- no unrelated departments;
- no full-database replication;
- no redundant fields;
- no integration merely for completeness.

It does **not** mean:
- one order only;
- fulfillment only;
- inventory only;
- one dashboard metric;
- one isolated domain.

---

## 4. Prospect qualification rule

Existing SAP, WMS, TMS, ERP, automation, BI or control-tower technology is neither a disqualifier nor the definition of the problem.

The qualification questions are:
1. Can we identify a meaningful company slice in which the connected SynapseCore control loop matters?
2. Can the authoritative facts required across the relevant supported domains be delivered to SynapseCore?
3. Are those facts sufficiently fresh and correctly mapped to create trustworthy cross-domain operational state?
4. Can SynapseCore exercise the connected intelligence/control/governance/recovery loop over that scope?
5. Is there a real operator/sponsor who can use and judge the whole control picture?
6. Can we observe and measure the operational result?

The customer can keep a giant ERP/WMS estate. SynapseCore sits **above the relevant facts from those systems** and forms its own whole operational intelligence/control layer.

---

## 5. Requalification decisions

### BMG
Move to active discovery. Manhattan SCALE is not a reason to hold BMG. The question is whether the relevant order/inventory/fulfillment/integration facts for a meaningful operating scope can feed the connected SynapseCore engine and support a whole operational picture across the chosen network slice.

### Goldwagen
Move to active discovery. Warehouse automation is not a blocker. The pilot should not be described as a small data side-tool; the target is a whole connected control picture across the chosen DC/franchise scope using only the required company data.

### Food Lover's Market / FVC
Remain a strong candidate. Food-safety/quality authority can stay external. The pilot still needs a meaningful connected operational picture across the selected supported domains; the hold is only correct ownership/contact and exact safe scope.

### VKB / Multi Green
Elevate. We do not need full MRP/MES integration simply because production exists. But we also should not pretend an order-only feed is enough if the chosen control picture depends materially on production facts. Pick a company slice whose relevant connected facts are actually supportable.

### AECI / Dis-Chem
Do not exclude because they are technologically sophisticated. Their disadvantage is approval/data-governance burden and safety boundaries. Any pilot must still exercise the whole connected SynapseCore picture for its selected safe scope.

---

## 6. Outreach language

Preferred explanation:

> SynapseCore sits above the operational systems you already use and builds one connected control picture for the agreed operating scope. We do not need to replicate your entire technology estate or ingest all of your data. We connect only the authoritative records and fields needed across the relevant operational domains, and SynapseCore uses that combined state to understand what is happening, identify pressure and priorities, produce recommendations, support governed decisions, and keep the outcome and recovery visible.

Then say:

> Your existing ERP/WMS/other systems remain authoritative for the functions they already own. SynapseCore's role is the whole-operation intelligence and control picture across them — not another departmental application.

Do not lead with 'sidecar', 'small feed', 'minimum facts', 'one order lane' or similar language unless the technical discussion specifically needs those terms.

---

## 7. Pilot acceptance implication

The first company pilot must not become a connector demonstration.

It must prove:
**authoritative source facts -> connected operational state -> cross-domain intelligence/pressure -> prioritisation/recommendations -> authorized operator control/governance -> external/supported action or handoff -> outcome readback -> audit/realtime convergence -> replay/recovery**

for the agreed company slice and supported domains.

That is how we reduce data burden without reducing SynapseCore.