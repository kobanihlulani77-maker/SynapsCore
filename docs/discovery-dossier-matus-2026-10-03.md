# Founder Discovery Dossier — Matus
**Date:** 2026-10-03  
**Purpose:** Prepare a founder-level operational discovery conversation.  
**Status:** Public research only; no company-confirmed problem, sponsor or pilot.

## Why Matus is first
Matus is a national South African wholesaler with a large Johannesburg/Germiston warehouse, 220+ brands, national distribution, local/international sourcing and explicit CRM/ERP investment. It also uses RapidTrade for B2B ordering.

Public evidence shows a useful operating tension:
- customers can see current stock through RapidTrade;
- Matus warns that stock can be depleted by demand and recommends confirmation where necessary;
- public website stock is not reservable through the online experience;
- special stock can be sourced by air freight;
- buy-out orders can take 5–7 working days;
- the general delivery policy is 24 hours but varies for outlying areas.

Sources:
- https://www.matus.co.za/about-us/
- https://www.matus.co.za/faq/
- https://www.matus.co.za/
- https://www.rapidtrade.com/industries/

This does **not** prove Matus has a problem. It proves there is enough operational complexity to ask intelligent questions about customer commitments, sourcing, stock confidence and delivery outcomes.

## Full-loop hypothesis
**B2B customer demand -> current stock / availability confidence -> sourcing or buy-out option -> delivery implication -> priority -> accountable decision -> warehouse/procurement handoff -> confirmed fulfillment/delivery -> customer status -> audit/recovery**

The SynapseCore question is whether Matus already has a sufficiently coherent way to manage that whole chain when normal online ordering is not enough.

## What we need to learn
- ERP identity and source-of-truth ownership.
- What RapidTrade writes to / reads from.
- Warehouse system and stock reservation semantics.
- How customer urgency is represented.
- How buy-out and special-stock decisions are made.
- Whether regional stock can be reallocated.
- Who owns an order exception end-to-end.
- How delivery changes are fed back to customer-facing teams.
- Current exception/control tooling.
- What happens when stock/integration information is stale.
- Whether a bounded Johannesburg operating slice has accessible data.

## Likely sponsor functions
Start with functions, not named individuals:
1. Distribution / operations leadership.
2. Customer service / commercial operations.
3. Procurement / buying.
4. Business systems / ERP / CRM.
5. Warehouse/logistics leadership.

## Official business route
Matus Johannesburg / Germiston: No. 1 Randport Industrial Park, 1 Suzuka Road, Gosforth Park, Germiston. Main line published by Matus: 011 681 9100.

Source: https://www.matus.co.za/about-us/

## Founder opening
Do not begin with a product demo. Explain that SynapseCore is an operational intelligence and control system and ask to understand one real order journey that becomes difficult to fulfill normally across stock, sourcing and delivery.

## Seven high-value discovery questions
1. When a customer order cannot be fulfilled exactly as expected, where is the complete operational situation assembled?
2. How do staff distinguish stock shown to a customer from stock that is truly available for that commitment?
3. When buy-out, special-stock or another location is an option, who compares the consequences and decides?
4. Who owns the order until the customer outcome is confirmed?
5. Where are the decision, handoffs and pending actions visible?
6. What happens if stock or another source feed becomes stale or inconsistent?
7. Which parts already work extremely well and should remain exactly where they are?

## Evidence not to claim
- Do not say Matus has inaccurate stock.
- Do not say its ERP/RapidTrade are fragmented or inadequate.
- Do not say SynapseCore will automate procurement or delivery execution.
- Do not present public FAQ caveats as failures.
- Do not invent savings.

## Potential bounded pilot shape
Johannesburg/Germiston warehouse + selected B2B customer/order journeys + one sourcing/fulfillment dependency, with several real operator roles.

The pilot must test a connected loop, not merely show stock or orders.

## Useful baseline metrics if company confirms relevance
- order exception investigation time;
- customer-commitment decision latency;
- number of manual handoffs;
- order exception age;
- time from decision to source-confirmed fulfillment;
- stale/inconsistent source incidents;
- manual reconciliation effort;
- late commitment / changed-promise count for the scoped journey.

## Disqualifiers for the proposed scope
- Existing control process already satisfies the need with little measurable burden.
- No accessible approved source path.
- No operational sponsor.
- Problem is too rare or low consequence.
- Scope requires unsupported autonomous procurement/transport action before value can be shown.
