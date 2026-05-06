export const platformTruth = {
  productName: "SynapseCore",
  generatedFor: "real operational fit analysis",
  currentScope: [
    "tenant-explicit auth, session control, and operator lane scoping",
    "catalog onboarding and warehouse-aware inventory control",
    "order ingestion through webhook, CSV, and scheduled pull",
    "alerts, recommendations, scenario approvals, and execution governance",
    "integration replay and recovery with manual-versus-automated eligibility rules",
    "tenant-scoped realtime visibility backed by Redis pub/sub on Render",
    "runtime, incident, audit, and event-tracing trust surfaces",
    "rate-limited auth and integration mutation paths"
  ],
  notClaimed: [
    "broad out-of-the-box ERP or carrier connector catalogs",
    "full transport management suite behavior outside the implemented ingestion and visibility lanes",
    "industry-specific billing, HR, payroll, or finance workflows"
  ],
  proofHighlights: [
    "full hosted proof passed end to end on Render twice in a row",
    "disabled connector CSV failures return structured CONNECTOR_DISABLED rows and create replay records",
    "manual replay records are not stolen by automated replay while the connector remains disabled",
    "dashboard, replay, scenario approval, role gating, and auth rate limiting are all browser-proven live"
  ],
  fitSignals: [
    "multiple operational systems feeding the same fulfillment or control process",
    "warehouse, fleet, fulfillment, field, or approval teams that need a shared operating picture",
    "teams that lose time reconciling spreadsheets, inboxes, exports, and delayed integration failures",
    "companies that need incident recovery and audit traceability without rebuilding their whole stack at once"
  ]
};

export const companyProfiles = [
  {
    id: "logistics-companies",
    label: "Logistics Companies",
    headline: "Best fit when dispatch, warehouse, fulfillment, and exception handling are split across tools and teams.",
    operationalPain: "Orders, shipment promises, dispatch changes, and exceptions move faster than operators can reconcile them manually.",
    fragmentedSystems: "TMS, spreadsheet lane boards, carrier portals, email escalations, and a separate stock or warehouse tool.",
    delayFailures: "Dispatch changes arrive late, exception ownership is unclear, and failed imports are noticed after downstream teams have already reacted.",
    visibilityBreaks: "Control teams see status snapshots after the fact instead of live pressure building by warehouse, connector, or order lane.",
    approvalBottlenecks: "Risky reallocations and promise changes wait for managers who have incomplete context or no shared review lane.",
    integrationFailures: "Carrier or partner feeds fail silently, CSV handoffs break, and support teams discover the gap only after customers escalate.",
    inventoryMismatch: "What sales promised, what the warehouse can actually ship, and what the dispatch board shows stop matching.",
    replayRecovery: "Replay records let operations recover failed inbound orders after connector issues are fixed instead of rekeying them from scratch.",
    realtimeVisibility: "Live connector, alert, and inventory pressure changes help operations re-route before SLA breaches spread.",
    auditTracing: "Runtime, incidents, audit, and business events show which lane failed, who replayed it, and what changed after recovery.",
    tenantIsolation: "Useful when one platform team supports multiple logistics clients or brands without mixing their operators or inbound records.",
    metrics: [
      "replay queue depth",
      "connector failure rate",
      "fulfillment backlog count",
      "delayed fulfillment count",
      "dispatch queue backlog",
      "orders ingested per hour"
    ],
    dashboards: [
      "live operations dashboard",
      "connector diagnostics and replay queue",
      "runtime and incident inbox",
      "warehouse inventory posture"
    ],
    alerts: [
      "connector disabled or degraded",
      "backlog growth by lane",
      "inventory risk on committed orders",
      "delayed fulfillment escalation"
    ],
    roi: [
      "less manual exception chasing",
      "faster recovery after ingestion failure",
      "fewer avoidable SLA misses",
      "clearer operational ownership during incidents"
    ],
    scenarios: [
      {
        title: "Carrier CSV lane pauses before shift change",
        before: "Teams discover the missing loads only after the warehouse asks why work dried up.",
        after: "Connector health, replay queue, and dashboard incidents surface the break quickly, then manual replay restores the lane after the connector is re-enabled."
      },
      {
        title: "Urgent allocation change needs approval",
        before: "Managers review partial screenshots and messages with no shared audit trail.",
        after: "Scenario approval and execution keep the review, decision, and result in one governed path."
      }
    ]
  },
  {
    id: "warehouses",
    label: "Warehouses",
    headline: "Best fit when warehouse teams need one control surface for inventory risk, inbound failures, and operator actions.",
    operationalPain: "Warehouse teams juggle receiving, picking, replenishment, and low-stock response while updates arrive from disconnected systems.",
    fragmentedSystems: "WMS exports, spreadsheet replenishment trackers, radio calls, and inbox-based exception handling.",
    delayFailures: "Receiving or order imports stall while floor teams keep working from stale priorities.",
    visibilityBreaks: "Supervisors cannot see which shortages are real, which are data lag, and which are connector failures.",
    approvalBottlenecks: "Reallocation and exception approval depend on supervisors reconstructing the event chain manually.",
    integrationFailures: "Inbound order or receiving updates fail without a shared recovery queue.",
    inventoryMismatch: "Available, reserved, and reorder-driven views drift apart, causing wasted picks or stockout surprises.",
    replayRecovery: "Failed inbound records stay visible until operators can intentionally recover them.",
    realtimeVisibility: "Low-stock signals and dashboard risk changes update without refresh when the floor situation changes.",
    auditTracing: "Teams can see when inventory changed, what replayed, and who executed or approved a risky scenario.",
    tenantIsolation: "Matters when 3PL or shared-service warehouse teams support multiple client workspaces.",
    metrics: [
      "active low-stock alerts",
      "inventory reservation pressure",
      "replay queue depth by warehouse",
      "failed dispatch count",
      "orders delayed by stock posture"
    ],
    dashboards: [
      "inventory and low-stock dashboard",
      "warehouse-scoped replay queue",
      "runtime incident inbox"
    ],
    alerts: [
      "threshold breach",
      "connector-disabled inbound lane",
      "warehouse-specific backlog spike"
    ],
    roi: [
      "fewer manual recount and reconciliation loops",
      "earlier shortage response",
      "less time lost chasing missing inbound records"
    ],
    scenarios: [
      {
        title: "Inbound connector disabled during morning receiving",
        before: "Warehouse staff stop trusting the queue and switch to ad hoc manual notes.",
        after: "The failed CSV row is classified, queued, and held for manual recovery once the connector is enabled."
      },
      {
        title: "Low stock threatens multiple active orders",
        before: "Operators refresh several tools to guess the real impact.",
        after: "Realtime dashboard risk and recommendations update immediately as inventory changes."
      }
    ]
  },
  {
    id: "retail-chains",
    label: "Retail Chains",
    headline: "Best fit when store, warehouse, and central operations need one tenant-safe control center instead of scattered spreadsheets and delayed reports.",
    operationalPain: "Store-level demand changes faster than central teams can rebalance stock and respond to fulfillment risk.",
    fragmentedSystems: "POS reports, store spreadsheets, email approvals, warehouse inventory views, and separate support inboxes.",
    delayFailures: "Store replenishment issues surface after shelves are already empty or store teams escalate manually.",
    visibilityBreaks: "Central ops see reports, not live operational pressure across stores and warehouse lanes.",
    approvalBottlenecks: "Transfers, substitutions, and urgent procurement waits for informal approval chains.",
    integrationFailures: "Store or partner order feeds fail without visible ownership.",
    inventoryMismatch: "Store-facing availability and warehouse reality drift apart.",
    replayRecovery: "Failed store or partner imports can be recovered deliberately rather than reentered from scratch.",
    realtimeVisibility: "Central dashboards show fresh risk posture while inventory and inbound data change.",
    auditTracing: "Useful for tracing who approved reallocation, who replayed inbound data, and when the state changed.",
    tenantIsolation: "Supports brand or franchise separation without shared-user leakage.",
    metrics: [
      "stockout risk by warehouse",
      "replay backlog by source system",
      "active recommendation count",
      "store-impacting alert count"
    ],
    dashboards: [
      "central dashboard summary",
      "catalog and inventory management",
      "alerts and recommendations"
    ],
    alerts: [
      "inventory below threshold",
      "connector degraded for a retail feed",
      "delayed fulfillment with store impact"
    ],
    roi: [
      "fewer lost sales from invisible stock pressure",
      "faster store-operations response",
      "better cross-store transfer governance"
    ],
    scenarios: [
      {
        title: "Promoted SKU falls below safe threshold",
        before: "Store teams find out after service complaints and manual phone calls.",
        after: "Central operations sees the risk live, reviews scenario options, and executes an approved response path."
      },
      {
        title: "Store-import lane fails overnight",
        before: "Morning teams manually compare exports and assume missing orders are already handled.",
        after: "Replay queue and incidents make the gap visible before store teams promise against bad data."
      }
    ]
  },
  {
    id: "ecommerce-fulfillment",
    label: "Ecommerce Fulfillment",
    headline: "Best fit when order velocity and SLA pressure make integration failures and stale dashboard data expensive.",
    operationalPain: "High-volume fulfillment teams cannot afford silent import failures or lag between stock posture and order commitments.",
    fragmentedSystems: "Commerce platform exports, marketplace feeds, shipping dashboards, warehouse notes, and support escalations.",
    delayFailures: "Failed order ingestion or delayed updates ripple into customer-facing promise misses quickly.",
    visibilityBreaks: "Operators cannot distinguish a real demand spike from a broken feed fast enough.",
    approvalBottlenecks: "Rush substitutions or inventory overrides move through chat rather than governed review.",
    integrationFailures: "Marketplace CSV and webhook lanes fail at the worst possible times and need visible recovery.",
    inventoryMismatch: "Oversell risk rises when commitments outrun visible stock posture.",
    replayRecovery: "Replay keeps failed inbound orders recoverable with traceability instead of forcing ad hoc reentry.",
    realtimeVisibility: "Live dashboard updates change triage speed when order pressure or stock risk shifts.",
    auditTracing: "Critical for proving whether an order failed to ingest, was replayed, and when the promise-relevant state changed.",
    tenantIsolation: "Useful for agencies or operators running multiple commerce brands in one platform.",
    metrics: [
      "orders ingested total",
      "fulfillment backlog",
      "delayed fulfillment count",
      "replay attempts",
      "recent import issues"
    ],
    dashboards: [
      "dashboard summary",
      "orders and fulfillment surfaces",
      "integrations and replay"
    ],
    alerts: [
      "connector disabled",
      "inventory shortage against active demand",
      "fulfillment delay escalation"
    ],
    roi: [
      "fewer missed customer promises",
      "faster operator triage",
      "less time reconciling order-import exceptions"
    ],
    scenarios: [
      {
        title: "Marketplace CSV lane fails mid-campaign",
        before: "Support sees angry customers before operations sees the broken feed.",
        after: "The failed rows are classified, queued, and recovered through a visible replay lane."
      },
      {
        title: "Warehouse stock drops during flash demand",
        before: "Teams refresh different tools and act on lagging numbers.",
        after: "Realtime dashboard risk and recommendations update immediately."
      }
    ]
  },
  {
    id: "distributors",
    label: "Distributors",
    headline: "Best fit when multi-customer stock movement and inbound partner feeds create constant reconciliation work.",
    operationalPain: "Distribution teams need clean views across customer demand, warehouse availability, and inbound supplier or sales feeds.",
    fragmentedSystems: "ERP extracts, partner CSVs, warehouse tools, and coordinator spreadsheets.",
    delayFailures: "Inbound orders or transfers fail quietly, then downstream teams overreact or duplicate work.",
    visibilityBreaks: "Distribution managers cannot see which warehouse or connector issue is actually driving backlog.",
    approvalBottlenecks: "High-impact moves such as reallocations and priority changes depend on ad hoc approval threads.",
    integrationFailures: "Partner source systems send inconsistent files that need clear failure codes and recovery paths.",
    inventoryMismatch: "Warehouse-on-hand, committed stock, and replenishment plans drift apart.",
    replayRecovery: "Operators can keep failed partner orders in view until the feed or connector issue is corrected.",
    realtimeVisibility: "Live operational summaries help distribution teams rebalance work faster.",
    auditTracing: "Needed when customer operations asks why a lane fell behind and who changed the recovery plan.",
    tenantIsolation: "Useful when one shared-service team manages multiple customer workspaces or business units.",
    metrics: [
      "warehouse-specific backlog",
      "connector health by source system",
      "inventory risk level",
      "replay queue depth"
    ],
    dashboards: [
      "distribution dashboard",
      "connector health rows",
      "inventory posture by warehouse"
    ],
    alerts: [
      "degraded connector",
      "rising replay queue",
      "stockout risk on priority SKUs"
    ],
    roi: [
      "less time spent reconciling partner feeds",
      "better warehouse balancing decisions",
      "fewer missed commitments"
    ],
    scenarios: [
      {
        title: "Partner import fails on disabled connector",
        before: "Teams retype the order or wait for support to investigate.",
        after: "The import comes back with CONNECTOR_DISABLED, creates a replay record, and stays ready for manual recovery."
      },
      {
        title: "Distribution center backlog spikes",
        before: "Managers piece together exports from different systems.",
        after: "Runtime, alerts, and dashboard surfaces show which lane is driving the pressure."
      }
    ]
  },
  {
    id: "manufacturers",
    label: "Manufacturers",
    headline: "Best fit when materials, internal approvals, and fulfillment risk need one governed control center.",
    operationalPain: "Manufacturing operations lose time when material availability, production commitments, and downstream order promises diverge.",
    fragmentedSystems: "MRP or ERP modules, local planners' spreadsheets, warehouse tools, and approval email chains.",
    delayFailures: "Changes to component availability and inbound demand do not reach planners quickly enough.",
    visibilityBreaks: "Teams cannot see whether a production risk is stock, approval, or connector driven.",
    approvalBottlenecks: "Production-plan or substitution changes wait for approvals with no shared operational context.",
    integrationFailures: "Inbound schedules or order feeds fail without a recovery-first operator path.",
    inventoryMismatch: "Raw material, finished goods, and promised delivery views drift apart.",
    replayRecovery: "Failed inbound schedules or orders remain recoverable after connector repairs.",
    realtimeVisibility: "Live risk posture helps planners act before shortages cascade into late delivery.",
    auditTracing: "Supports post-incident traceability around decisions, escalations, and replays.",
    tenantIsolation: "Useful for contract manufacturers supporting multiple brands or customer programs.",
    metrics: [
      "material-linked low-stock alerts",
      "replay backlog tied to inbound schedules",
      "scenario approval age",
      "delayed fulfillment count"
    ],
    dashboards: [
      "inventory and recommendation views",
      "scenario approval surfaces",
      "runtime and incidents"
    ],
    alerts: [
      "material shortage risk",
      "connector-disabled inbound lane",
      "approval escalation"
    ],
    roi: [
      "better production exception handling",
      "fewer surprises between material and fulfillment teams",
      "stronger auditability around change approval"
    ],
    scenarios: [
      {
        title: "Component shortage threatens committed orders",
        before: "Planners argue from stale exports and chat threads.",
        after: "Recommendations, approvals, and audit traces keep the response governed."
      },
      {
        title: "Inbound schedule import fails",
        before: "Operators manually reconstruct the day from spreadsheets.",
        after: "The failed inbound record stays recoverable through replay."
      }
    ]
  },
  {
    id: "procurement-heavy-businesses",
    label: "Procurement-Heavy Businesses",
    headline: "Best fit when approvals and stock visibility are central to protecting margin and service levels.",
    operationalPain: "Procurement and operations teams need tighter visibility between stock posture, demand changes, and approval decisions.",
    fragmentedSystems: "Procurement sheets, ERP extracts, inbox approvals, and warehouse updates that land too late.",
    delayFailures: "Purchase or replenishment decisions happen after stock pressure is already hurting execution.",
    visibilityBreaks: "Teams struggle to see which SKUs need action first and which issues come from bad data versus real demand.",
    approvalBottlenecks: "Buy/no-buy and transfer decisions queue up without shared operational context.",
    integrationFailures: "Inbound vendor or order feeds fail with no visible queue for recovery.",
    inventoryMismatch: "Demand and on-hand posture drift, creating either excess stock or missed commitments.",
    replayRecovery: "Recovery keeps failed inbound orders visible so procurement is not planning against missing demand.",
    realtimeVisibility: "Live stock-risk changes help prioritize the right approvals and replenishment moves.",
    auditTracing: "Shows who approved a move and what risk signal existed at the time.",
    tenantIsolation: "Supports holding-company or business-unit separation when teams share a platform.",
    metrics: [
      "active recommendation count",
      "inventory risk severity",
      "approval backlog",
      "connector failure count"
    ],
    dashboards: [
      "recommendations",
      "inventory and risk summary",
      "approval work surfaces"
    ],
    alerts: [
      "urgent replenishment need",
      "approval aging",
      "disabled connector blocking demand visibility"
    ],
    roi: [
      "better prioritization of scarce budget and stock",
      "less planning against incomplete demand data",
      "more defensible approval records"
    ],
    scenarios: [
      {
        title: "High-value SKU demand disappears from import lane",
        before: "Procurement under-buys because the demand feed failed silently.",
        after: "Runtime and replay surfaces show the failure before planning decisions lock in."
      },
      {
        title: "Replenishment approval stalls",
        before: "Ops teams escalate by email with no shared evidence trail.",
        after: "Scenario approval keeps the decision, escalation, and execution connected."
      }
    ]
  },
  {
    id: "operations-centers",
    label: "Operations Centers",
    headline: "Best fit when a central command team needs one tenant-safe console across alerts, incidents, replay, approvals, and live operational change.",
    operationalPain: "Central operations teams are expected to coordinate everything, but the signals arrive in different systems with different owners.",
    fragmentedSystems: "Monitoring tools, dashboards, spreadsheets, emails, chat, and separate business applications.",
    delayFailures: "Exception detection is fast in one tool and slow in another, so action starts late.",
    visibilityBreaks: "The center sees alerts but not the business context needed to act.",
    approvalBottlenecks: "Escalations stall because ownership and impact are unclear.",
    integrationFailures: "Inbound failures surface as symptoms rather than a visible queue with recovery actions.",
    inventoryMismatch: "Central teams are forced to mediate stock disputes without a shared state source.",
    replayRecovery: "Replay turns broken inbound orders into deliberate operational actions instead of mystery gaps.",
    realtimeVisibility: "Live dashboard updates help the center coordinate quickly across teams.",
    auditTracing: "Post-incident review becomes practical because events, approvals, and replays are all traceable.",
    tenantIsolation: "Useful for platform operations centers serving several customers or business units.",
    metrics: [
      "active incidents",
      "connector diagnostics",
      "replay queue depth",
      "dispatch failures",
      "latest failure age"
    ],
    dashboards: [
      "runtime and incidents",
      "dashboard summary",
      "integrations and replay"
    ],
    alerts: [
      "connector disabled",
      "replay backlog growth",
      "business-event failure spike"
    ],
    roi: [
      "faster cross-team incident response",
      "better shared understanding of active risk",
      "less time spent building manual status decks"
    ],
    scenarios: [
      {
        title: "Control center sees replay backlog growth",
        before: "Teams wait for support to diagnose whether the lane is safe to ignore.",
        after: "Connector diagnostics, incidents, and replay state show whether action is needed now."
      },
      {
        title: "Approval escalation overlaps with inbound failure",
        before: "Managers chase two disconnected workflows with no shared timeline.",
        after: "Runtime, scenarios, and audit surfaces keep the incident narrative coherent."
      }
    ]
  },
  {
    id: "supply-chain-coordinators",
    label: "Supply Chain Coordinators",
    headline: "Best fit when coordination is the job and the pain comes from bad handoffs, delayed data, and unclear ownership.",
    operationalPain: "Coordinators spend their day reconciling what vendors, warehouses, and customer-facing teams believe is true.",
    fragmentedSystems: "ERP exports, vendor spreadsheets, email chains, WMS or order portals, and support tickets.",
    delayFailures: "By the time a coordinator sees the real problem, downstream plans are already moving.",
    visibilityBreaks: "There is no single view tying connector state, inventory posture, and approvals together.",
    approvalBottlenecks: "Coordinators gather evidence for decisions instead of working from a governed review lane.",
    integrationFailures: "File-based and token-based imports fail without a clean operational queue.",
    inventoryMismatch: "Demand plans and warehouse reality diverge, especially during disruptions.",
    replayRecovery: "Replay keeps failed inbound demand recoverable and visible until corrected.",
    realtimeVisibility: "Live changes reduce the lag between event and response.",
    auditTracing: "Helps answer what failed, when it was replayed, and who approved the downstream action.",
    tenantIsolation: "Useful where one coordination team serves multiple tenants, brands, or customers.",
    metrics: [
      "connector incident count",
      "inventory risk hotspots",
      "approval turnaround",
      "replay queue age"
    ],
    dashboards: [
      "dashboard summary",
      "runtime and incidents",
      "scenario review"
    ],
    alerts: [
      "degraded inbound lane",
      "urgent stock risk",
      "stale replay backlog"
    ],
    roi: [
      "less manual reconciliation",
      "better timing on interventions",
      "cleaner traceability for cross-team decisions"
    ],
    scenarios: [
      {
        title: "Supplier file lands with disabled connector",
        before: "Coordinator manually checks whether orders were lost.",
        after: "Structured failure plus replay queue keeps the gap visible and actionable."
      },
      {
        title: "Warehouse pressure changes mid-day",
        before: "Coordinators wait for refreshes or calls from the floor.",
        after: "Realtime dashboard updates the risk posture immediately."
      }
    ]
  },
  {
    id: "multi-warehouse-businesses",
    label: "Multi-Warehouse Businesses",
    headline: "Best fit when visibility and replay must stay warehouse-aware instead of blending every lane together.",
    operationalPain: "Multiple warehouses create different risk profiles, but many systems flatten the view until problems are already expensive.",
    fragmentedSystems: "Warehouse-specific tools, central reporting, manual transfer sheets, and connector feeds with uneven quality.",
    delayFailures: "One location's connector or stock issue spreads confusion across the whole network.",
    visibilityBreaks: "Teams cannot tell which warehouse is actually constrained or which lane still has capacity.",
    approvalBottlenecks: "Transfers and priority changes need warehouse-aware approvals, not generic signoff.",
    integrationFailures: "A single warehouse-bound feed can fail and disappear into noise.",
    inventoryMismatch: "On-hand versus committed posture differs sharply by warehouse, but teams act on blended totals.",
    replayRecovery: "Replay records remain visible with warehouse context so the right lane can recover them.",
    realtimeVisibility: "Warehouse-specific dashboard changes drive faster balancing decisions.",
    auditTracing: "Useful for reviewing why inventory or transfer actions changed by location.",
    tenantIsolation: "Tenant safety matters when the same platform supports multiple warehouse networks.",
    metrics: [
      "risk by warehouse",
      "replay backlog by warehouse",
      "transfer-related approval count",
      "connector failures by source"
    ],
    dashboards: [
      "warehouse inventory view",
      "replay queue with warehouse context",
      "alerts and recommendations"
    ],
    alerts: [
      "warehouse-specific low stock",
      "connector disabled for one lane",
      "fulfillment delay tied to one location"
    ],
    roi: [
      "better rebalancing decisions",
      "less confusion caused by blended reporting",
      "faster warehouse-specific recovery"
    ],
    scenarios: [
      {
        title: "WH-NORTH import fails while WH-SOUTH stays healthy",
        before: "Central teams overreact across both sites.",
        after: "Replay queue and incidents isolate the failing lane and keep the healthy one moving."
      },
      {
        title: "Transfer approval affects a live shortage",
        before: "Decision history is scattered across messages.",
        after: "Scenario approval and audit keep the warehouse-specific rationale visible."
      }
    ]
  },
  {
    id: "transport-fleet-operations",
    label: "Transport and Fleet Operations",
    headline: "Best fit when dispatch visibility, operational incidents, and cross-team coordination matter more than generic dashboards.",
    operationalPain: "Fleet teams need to coordinate dispatch, exceptions, and fulfillment consequences in real time.",
    fragmentedSystems: "Dispatch tools, status spreadsheets, support inboxes, carrier portals, and manual alert escalation.",
    delayFailures: "A late or missing inbound event changes downstream routing before central ops knows it.",
    visibilityBreaks: "Teams see transport symptoms but not the upstream connector or inventory cause.",
    approvalBottlenecks: "Route or priority changes wait for signoff without a shared incident view.",
    integrationFailures: "Transport or delivery-import lanes fail and operators have no clean recovery queue.",
    inventoryMismatch: "Fleet plans may be built on orders or allocations that no longer match warehouse reality.",
    replayRecovery: "Replay gives transport ops a governed recovery path when inbound orders failed before dispatch.",
    realtimeVisibility: "Live dashboard state makes it easier to coordinate with warehouse and customer teams.",
    auditTracing: "Supports operational review when service failures span systems.",
    tenantIsolation: "Useful for fleet operators serving multiple customers or contracts.",
    metrics: [
      "dispatch backlog",
      "failed dispatch count",
      "connector incident count",
      "replay queue age"
    ],
    dashboards: [
      "runtime and incidents",
      "orders and dispatch surfaces",
      "replay queue"
    ],
    alerts: [
      "dispatch queue failure",
      "connector-disabled inbound lane",
      "delayed fulfillment with transport impact"
    ],
    roi: [
      "faster incident triage across transport and warehouse teams",
      "cleaner recovery after inbound failure",
      "less route churn caused by bad upstream state"
    ],
    scenarios: [
      {
        title: "Inbound order feed pauses before dispatch planning",
        before: "Dispatch plans against incomplete demand and discovers the gap later.",
        after: "The replay queue keeps the missing orders visible until recovery is performed."
      },
      {
        title: "Priority reroute needs approval",
        before: "Approvals happen in chat with no durable decision record.",
        after: "Scenario review keeps the action governed and traceable."
      }
    ]
  },
  {
    id: "field-operations",
    label: "Field Operations",
    headline: "Best fit when crews, inventory, and inbound work updates need a coordinated control layer rather than disconnected admin tools.",
    operationalPain: "Field teams suffer when central operations and local crews are working from different assumptions about stock, work intake, and approvals.",
    fragmentedSystems: "Ticketing, spreadsheets, warehouse views, crew notes, and manual dispatch coordination.",
    delayFailures: "Crew-impacting work arrives late or not at all because inbound lanes fail quietly.",
    visibilityBreaks: "Central teams cannot see whether a field issue is stock, dispatch, or integration related.",
    approvalBottlenecks: "Urgent changes need governed review but usually move through informal channels.",
    integrationFailures: "Imports fail and crews learn about it only when work packs are incomplete.",
    inventoryMismatch: "Field stock and central inventory assumptions drift apart.",
    replayRecovery: "Replay helps restore failed inbound work instructions without retyping them.",
    realtimeVisibility: "Live dashboard state helps central teams react while crews are still executing.",
    auditTracing: "Supports after-action review and customer communication.",
    tenantIsolation: "Useful for service providers operating multiple contracts or customer tenants.",
    metrics: [
      "work intake failures",
      "replay backlog",
      "inventory risk on field-critical SKUs",
      "approval turnaround"
    ],
    dashboards: [
      "dashboard summary",
      "inventory and alerts",
      "runtime and replay"
    ],
    alerts: [
      "failed inbound work import",
      "critical field stock risk",
      "approval escalation"
    ],
    roi: [
      "less field downtime caused by central data failures",
      "better control over urgent changes",
      "clearer service traceability"
    ],
    scenarios: [
      {
        title: "Field work order CSV import fails",
        before: "Dispatch manually re-enters requests or delays crews.",
        after: "The failed import is classified and held for manual recovery."
      },
      {
        title: "Critical part stock changes mid-day",
        before: "Crews get stale commitments from central ops.",
        after: "Realtime updates keep the control center current."
      }
    ]
  },
  {
    id: "enterprise-admin-teams",
    label: "Enterprise Admin Teams",
    headline: "Best fit when the real pain is access governance, tenant separation, connector administration, and operational trust surfaces.",
    operationalPain: "Admin teams have to keep operations moving without letting access, settings, or connector mistakes create hidden risk.",
    fragmentedSystems: "Identity admin, spreadsheets, operations tools, support tickets, and disconnected audit sources.",
    delayFailures: "Bad connector or user-state changes are discovered only after operators complain or proof flows fail.",
    visibilityBreaks: "Admins lack one runtime view combining session, connector, replay, and incident posture.",
    approvalBottlenecks: "Sensitive changes have no shared operational context when different teams approve them.",
    integrationFailures: "Connector misconfiguration causes business failures that look like user error until traced properly.",
    inventoryMismatch: "Admin teams may not own stock directly, but they still need to understand the impact of access or connector changes on downstream inventory truth.",
    replayRecovery: "Manual replay remains available to authorized operators after admins correct connector posture.",
    realtimeVisibility: "Admins can confirm whether the control center is live, reconnecting, or degraded.",
    auditTracing: "Critical for explaining who changed a connector, who reset access, and what impact followed.",
    tenantIsolation: "This is a core fit area when one admin team supports multiple tenants, clients, or operating units.",
    metrics: [
      "recent audit failures",
      "disabled connector count",
      "replay queue depth",
      "active incident count",
      "auth and mutation rate-limit signals"
    ],
    dashboards: [
      "runtime",
      "incidents",
      "users and access",
      "integrations"
    ],
    alerts: [
      "connector disabled",
      "auth rate-limit pressure",
      "audit or runtime failure spike"
    ],
    roi: [
      "safer administration of live operations",
      "better incident explanation and change accountability",
      "cleaner tenant separation"
    ],
    scenarios: [
      {
        title: "Connector disabled during support change",
        before: "Admins learn about business impact from downstream complaints.",
        after: "Runtime, incidents, and replay queue show the exact operational consequence immediately."
      },
      {
        title: "User access needs role-gated verification",
        before: "Teams rely on screenshots or assumptions about who can see what.",
        after: "Hosted proof and role-gated UI behavior prove the access model live."
      }
    ]
  }
];

export const companyProfileMap = new Map(companyProfiles.map((profile) => [profile.id, profile]));
