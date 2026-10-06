# SynapseCore Research Report 14 — Sidecar Advisory Pilot Model and Prospect Requalification
**Date:** 2026-10-07 (Africa/Johannesburg)
**Mode:** JARVIS / responsible-partner execution
**Purpose:** Correct the prospect-selection model so current enterprise technology, company scale, or specialist-system maturity do not incorrectly disqualify a pilot candidate.

---

## 1. Founder correction

The pilot must be understood as a **sidecar operational intelligence and control deployment**.

SynapseCore does not need to enter, replace, or deeply orchestrate every system in the company.

For the current pilot model:
1. approved company source systems remain authoritative;
2. SynapseCore receives the **minimum supported operational facts** needed for the selected control scope;
3. it maintains its own tenant-scoped operational state;
4. it evaluates conditions, pressure, dependencies and priorities within the implemented model;
5. it produces alerts and **advisory recommendations**;
6. responsible human operators retain decision authority;
7. actions that are not individually supported SynapseCore operations remain in the company's authoritative systems or human/external workflows;
8. SynapseCore observes/readbacks the resulting state where an approved source path exists.

> **A sophisticated SAP/WMS/TMS/ERP environment is not a prospect disqualifier.**

The correct question is:

> **Can we obtain the minimum authoritative facts needed for a bounded pilot control scope, and can SynapseCore turn those facts into useful, trustworthy operational judgment for the responsible operators?**

---

## 2. Current implementation boundary

The current engineering records state:
- SynapseCore is a tenant-scoped operational intelligence and control layer **above company source systems**.
- Source systems remain authoritative for real-company business action unless a specific supported direct-operation workflow is explicitly approved.
- Recommendations remain advisory.
- Scenario approval does not execute business effects.
- The current integration connector is primarily an **order-ingestion lane**, not a universal ETL platform.
- Supported order intake includes WEBHOOK_ORDER and CSV_ORDER_IMPORT; WEBHOOK_ORDER can use realtime push and limited scheduled pull.
- Catalog, inventory and other operational onboarding/updates use their own supported contracts and must be confirmed for the chosen pilot.
- The first company connector can intentionally be **one controlled lane**.
- Customer-specific integration code is not automatically part of the first pilot.

Engineering sources:
- docs/SYNAPSCORE-MASTER-ENGINEERING-READINESS-MAP.md
- docs/company-integration-setup-runbook.md

This means **sidecar** does not mean 'no integration.' It means **minimum approved data connection without replacement or takeover of the customer's operating stack**.

---

## 3. Correct prospect qualification model

### Old, incorrect commercial filter
Do not avoid or delay a company merely because:
- it has SAP;
- it has a strong WMS;
- it has warehouse automation;
- it has a modern control platform;
- it is large;
- it is currently modernising one specialist system.

Those facts may affect discovery or integration details, but they are not by themselves reasons to reject the company.

### Correct filter

A prospect is attractive when a meaningful bounded control surface can be represented from the minimum authoritative data SynapseCore needs, through a supported or explicitly approved path, with a real operator who can use the result and a measurable outcome.

Required questions:
- What bounded operating scope matters?
- Which few facts are actually needed?
- Which source owns each fact?
- Can the facts be delivered through webhook, CSV, approved pull, or another supported onboarding/API path?
- What freshness is needed?
- Which operator uses the control picture and recommendations?
- Which decisions stay outside SynapseCore?
- How does the resulting outcome become observable again?
- What metric proves usefulness?

---

## 4. Requalification of previously held prospects

### BMG — move to active discovery

**Previous hold reason:** active Manhattan SCALE/WMS transformation appeared to create overlap or timing resistance.

**Correction:** the WMS transformation is not a technical disqualifier for a sidecar pilot. BMG's systems remain authoritative. The relevant test is whether BMG can expose a bounded set of operational facts and whether SynapseCore's advisory control picture adds value across branches, availability, priorities and fulfillment.

**Action:** product-first sidecar outreach sent 2026-10-07 to customercare@bmgworld.net, requesting routing to Managing Director Robin Briggs or the appropriate senior operations/supply-chain owner.

### Goldwagen — move to active discovery

**Previous hold reason:** warehouse automation/current technology investment made timing appear poor.

**Correction:** automation and strong internal IT are not reasons to exclude Goldwagen. A bounded pilot can use only the approved order/inventory/fulfillment facts needed for a defined network scope.

**Action:** product-first sidecar outreach sent 2026-10-07 to info@goldwagen.com, requesting the senior central-distribution/supply-chain/network-operations owner.

### Food Lover's Market / FVC — retain as strong candidate, change the reason for waiting

The company should not be held because fresh-chain technology or safety systems are complex. A pilot can deliberately remain advisory and exclude food-safety/quality authority while using selected operational facts.

The current hold is only **contact/ownership quality and exact bounded scope**, not technology maturity or scale.

### VKB / Multi Green — elevate

The prior concern that production/distribution requires too many integrations was too broad. The first pilot would not need full production/MRP observability if a useful advisory control lane can be built from the minimum supported facts.

Multi Green remains a high-priority discovery candidate pending an appropriate public business route / owner confirmation.

### AECI / Dis-Chem and other regulated environments

Regulation and safety still matter, but the correct conclusion is not that SynapseCore must integrate the whole regulated environment. A bounded advisory scope may still be possible if safety/quality authority stays outside SynapseCore and only permitted operational facts are ingested.

The main disadvantage is likely approval/data-governance burden, not their existing enterprise technology.

---

## 5. What this changes about company research

From now on, researching a company's systems serves only to:
1. identify authoritative sources;
2. understand what minimum data might be obtainable;
3. avoid contradicting source ownership;
4. understand operator context;
5. identify mapping/security/freshness constraints.

It is **not** used to conclude that because a company already has SAP, WMS, automation or strong internal IT, SynapseCore is unnecessary.

---

## 6. Outreach language correction

Where useful, explain:

> SynapseCore works alongside your existing systems. A controlled pilot does not require access to every platform or replacement of the systems that run the business. We connect only the approved operational facts needed for a bounded scope; SynapseCore builds the operational control picture and advisory intelligence while your people and source systems retain authority.

Do not turn first contact into an integration-architecture discussion.

---

## 7. Strategic conclusion

The first pilot is not an enterprise integration programme.

It is a **controlled proof that SynapseCore can sit beside an existing operation, receive enough authoritative operational truth, judge what is happening, prioritize and recommend coherently, support the responsible operator, and preserve a trustworthy record of the operating outcome.**

The customer's technology stack matters only insofar as it affects the **minimum approved data contract and source authority**.