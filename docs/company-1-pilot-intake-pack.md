# Company 1 Pilot Intake Pack

This is the authoritative Phase 2 intake pack for Company 1 and future controlled SynapseCore pilots.

Phase 2 begins only after the company has passed the Phase 1 presentation/discovery stage. It collects the business, operational, technical, data, security, and decision information SynapseCore needs before any tenant, workspace, user, connector, or data provisioning begins.

This document is not a legal contract, a CRM form, a technical provisioning runbook, a secret-storage document, or a feature wishlist.

## Phase Boundary

Phase 2 is intake only.

Do not perform:

- tenant creation
- workspace creation
- user creation
- role assignment
- connector configuration
- data import
- replay setup
- operational policy configuration
- customer handoff

Those belong to later phases.

## Repository Truth Used

This intake pack follows the accepted Phase 1 handoff from:

- [company-1-presentation-pack.md](company-1-presentation-pack.md)

It is also grounded in:

- [final-pre-pilot-release-gate.md](final-pre-pilot-release-gate.md)
- [verification-status.md](verification-status.md)
- [official-pilot-program.md](official-pilot-program.md)
- [security-and-trust-model.md](security-and-trust-model.md)
- [performance-scale-proof.md](performance-scale-proof.md)
- [backup-restore-runbook.md](backup-restore-runbook.md)
- backend roles: `backend/src/main/java/com/synapsecore/access/SynapseAccessRole.java`
- tenant/user/operator models: `backend/src/main/java/com/synapsecore/domain/entity/`
- product/inventory/order DTOs: `backend/src/main/java/com/synapsecore/domain/dto/`
- connector/replay DTOs: `backend/src/main/java/com/synapsecore/integration/dto/`

Do not add intake requirements based on imaginary product capability.

## Operating Model

The customer does not provision SynapseCore.

Customer provides:

- approved business and technical contacts
- selected pilot problem
- approved pilot users
- source-system information
- authorized sample data
- data scope and exclusions
- success metrics
- support and escalation contacts

SynapseCore later handles:

- tenant/workspace provisioning
- user creation
- role assignment
- connector configuration
- data onboarding
- operational configuration
- verification
- access handoff

## Secret Handling Rule

Do not put secrets in this document.

Never record:

- passwords
- API keys
- database passwords
- private tokens
- OAuth client secrets
- session secrets
- connector credentials
- customer private credentials

The intake may record:

| Field | Allowed |
| --- | --- |
| Secret required | Yes / No / Unknown |
| Secret type | API key, bearer token, OAuth credential, basic auth, file-transfer credential, other |
| Secret owner | Company technical contact or SynapseCore operator |
| Secure handoff status | Not started / Required later / Received through approved secure channel / Rotated |

Actual credential handling belongs to Phase 5: Integration Setup.

## Intake Completeness States

| State | Meaning |
| --- | --- |
| DRAFT | SynapseCore is collecting information; no readiness decision exists. |
| AWAITING COMPANY INFORMATION | Critical information is missing from the company. |
| TECHNICAL REVIEW REQUIRED | The intake is mostly complete but feasibility, data, integration, or security needs technical review. |
| READY FOR INTERNAL APPROVAL | Critical fields are complete and SynapseCore can decide whether provisioning may begin. |
| APPROVED FOR PROVISIONING | Internal approval confirms Phase 3 may begin. |
| BLOCKED | A critical issue prevents provisioning. |

Current intake state:

`DRAFT`

## Company Identity

Customer provides:

| Field | Required | Notes |
| --- | --- | --- |
| Company legal name | Yes | Use official customer name where available. |
| Trading/display name | Yes | Used for human-readable pilot records. |
| Business unit / department | Yes | The first pilot should be departmentally bounded. |
| Industry | Yes | Retail, logistics, distribution, manufacturing, ecommerce, warehouse operations, or other. |
| Primary operational location | If relevant | Needed when location/warehouse context matters. |
| Pilot sponsor | Yes | Executive or accountable business sponsor. |
| Primary business contact | Yes | Person accountable for pilot business value. |
| Primary technical contact | Yes | Person accountable for source-system and access coordination. |
| Primary operational contact | Yes | Person accountable for daily pilot workflow. |
| Escalation contact | Yes | Person to contact for urgent pause/security/data issues. |

Do not collect unnecessary personal information. Business name, business email, role, and contact path are enough unless the company requires another channel.

## Pilot Problem Statement

The pilot problem must be measurable and bounded.

Vague statements are not sufficient:

- "Improve operations"
- "Use AI"
- "Make the warehouse better"
- "Connect everything"

Required fields:

| Field | Required Prompt |
| --- | --- |
| Current process | What happens today, step by step? |
| Current pain | What specifically is slow, hidden, manual, risky, or unreliable? |
| Current systems | Which systems are involved today? |
| Current failure / visibility issue | What fails or becomes invisible? |
| Desired outcome | What should be easier, faster, clearer, safer, or more auditable? |
| Why this workflow | Why is this the right first pilot lane? |
| Bounded test statement | What exact slice will the pilot test? |

Acceptable example shape:

```text
The pilot will test whether the warehouse operations team can see, classify, and recover failed inbound order records for one order lane from one source system, while monitoring related inventory pressure and approval/replay governance.
```

## Pilot Scope

The intake must prevent scope creep.

### In Scope

Capture:

- business unit / department
- locations or warehouses included
- product groups included
- order types included
- inventory records included
- connector lane included
- users included
- workflows included
- reporting/review cadence included

### Not Included In Company 1 Pilot

Explicitly list:

- departments not included
- locations/warehouses not included
- product groups not included
- order types not included
- integrations not included
- users not included
- data categories not included
- workflows not included
- decisions/actions not included

If the company cannot name what is out of scope, the pilot is not bounded enough for Phase 3.

## Pilot Operating Envelope

Current standard pilot envelope:

| Area | Standard Company 1 Envelope |
| --- | --- |
| Workspaces | 1 |
| Operators | 3 to 5 |
| Connector lanes | 1 initially |
| Data | Bounded operational slice |
| Systems of record | Existing company systems remain authoritative |

Classification:

| Proposed Scope | Result |
| --- | --- |
| Fits envelope | WITHIN STANDARD PILOT ENVELOPE |
| Slightly exceeds envelope but may be feasible | REQUIRES TECHNICAL REVIEW |
| Exceeds pilot assumptions materially | OUTSIDE CURRENT PILOT ENVELOPE |

Do not silently accept larger requests.

## Workspace Requirements

Customer provides:

| Field | Required | Notes |
| --- | --- | --- |
| Desired workspace display name | Yes | Human-readable only. |
| Business unit / department | Yes | Used to describe pilot scope. |
| Agreed pilot purpose | Yes | Should match problem statement. |
| Timezone | Yes | For operational review and scheduling. |
| Primary operating region | If relevant | Do not over-collect. |
| Company naming conventions | If relevant | Helpful for user/warehouse/source-system labels. |

SynapseCore completes internally later:

- tenant code
- internal identifiers
- workspace creation
- security settings
- warehouse/workspace records
- final provisioning notes

Do not ask the customer to choose internal database IDs.

## Approved Pilot Users

Customer provides one row per proposed user.

| Field | Required | Notes |
| --- | --- | --- |
| Name | Yes | Business identity. |
| Business email | Yes | Use company-approved address. |
| Job responsibility | Yes | Current real-world job. |
| Operational responsibility in pilot | Yes | What they will do in SynapseCore. |
| Makes operational decisions? | Yes / No | Used for role mapping later. |
| Approves decisions? | Yes / No | Used for approval role mapping later. |
| Needs administrative capability? | Yes / No / Unknown | Do not assume. |
| Primary or backup operator | Yes | Primary / backup / observer. |
| Warehouse/location scope | If relevant | Use business location names first. |
| Access restrictions | If relevant | e.g. read-only, location-limited, no approvals. |

Do not assign technical roles in the customer-facing row. Collect responsibility first.

## Role Mapping Preparation

Actual supported SynapseCore access roles:

| Role | Current Meaning For Intake |
| --- | --- |
| `TENANT_ADMIN` | High-impact workspace/user/admin capability. Assign cautiously. |
| `REVIEW_OWNER` | Can participate in scenario review ownership and review-stage governance. |
| `FINAL_APPROVER` | Can perform final approval where required. |
| `ESCALATION_OWNER` | Can handle escalation ownership where configured. |
| `INTEGRATION_ADMIN` | Can administer integration connector configuration. |
| `INTEGRATION_OPERATOR` | Can operate integration/replay flows where authorized. |

Scenario actor roles also include:

- `REQUESTER`
- `REVIEW_OWNER`
- `FINAL_APPROVER`
- `ESCALATION_OWNER`

Customer-facing responsibility mapping:

| Business Responsibility | Possible Internal Role Later |
| --- | --- |
| Workspace access/admin owner | `TENANT_ADMIN` |
| Reviews proposed operational plan | `REVIEW_OWNER` |
| Gives final approval for high-impact decision | `FINAL_APPROVER` |
| Owns escalated/overdue decision | `ESCALATION_OWNER` |
| Configures connector/support ownership | `INTEGRATION_ADMIN` |
| Reviews/replays failed inbound records | `INTEGRATION_OPERATOR` or `INTEGRATION_ADMIN` |

Final role assignment is performed by SynapseCore in Phase 4.

## Access Approval

Customer must confirm listed users are approved to access pilot data.

Required fields:

- access approver name
- access approver business role
- approval date
- approved user list
- access restrictions
- confidentiality considerations
- external business/legal prerequisite status, if applicable

Do not invent legal contract terms in this intake. If a formal legal agreement is required, mark it as an external prerequisite.

## Current System Landscape

Create one row per relevant system.

| Field | Required | Notes |
| --- | --- | --- |
| System name | Yes | e.g. ERP, WMS, OMS, POS, spreadsheet, CSV process. |
| System purpose | Yes | What operational job it performs. |
| Data it owns | Yes | Catalog, inventory, orders, fulfillment, approvals, etc. |
| Source-of-truth status | Yes | Authoritative / reference / derived / manual. |
| Technical owner | Yes | Person/team. |
| Integration method available | Yes | API, webhook, CSV, scheduled export, manual file, none. |
| Environment | Yes | Test / staging / production / unknown. |
| Update frequency | Yes | Event, minutes, hourly, daily, manual. |
| Access restrictions | If relevant | Network, auth, export, data restrictions. |

Do not imply SynapseCore supports a named vendor integration just because the system exists. The supported method matters.

## Initial Connector Lane

Current recommended pilot: 1 initial connector lane.

Collect:

| Field | Required | Notes |
| --- | --- | --- |
| Source system | Yes | Must be later mapped to `sourceSystem`. |
| Business purpose | Yes | Why this lane matters. |
| Connector/integration method | Yes | Webhook order, CSV order import, scheduled pull candidate, or manual/other for review. |
| Direction of data flow | Yes | Source to SynapseCore, SynapseCore to source, or reference-only. |
| Inbound data type | Yes | Orders, catalog, inventory, or other. |
| Update/event frequency | Yes | Event, every X minutes, hourly, daily, manual file. |
| Sample data availability | Yes | Available / redacted sample needed / unavailable. |
| Expected volume | Yes | Daily/weekly estimates. |
| Technical contact | Yes | Person/team. |
| Authentication mechanism category | Yes | API key required, bearer token, OAuth, basic auth, file access, none, unknown. |
| Network restrictions | If relevant | IP restrictions, VPN, allowlist, private network, none. |
| Test environment availability | Yes | Available / not available / unknown. |
| Error/retry expectations | Yes | What should happen when import fails? |

Supported connector-related repository truth:

- Connector types: `WEBHOOK_ORDER`, `CSV_ORDER_IMPORT`
- Sync modes: `REALTIME_PUSH`, `BATCH_FILE_DROP`, `SCHEDULED_PULL`
- Validation policies: `STANDARD`, `STRICT`, `RELAXED`
- Transformation policies: `NONE`, `NORMALIZE_CODES`
- Mapping version currently supports version `1`
- Connector metadata may include `sourceSystem`, `displayName`, `enabled`, sync mode, optional pull endpoint, default warehouse fallback, default warehouse code, notes, support owner, and inbound token status.

Do not collect actual tokens in this document.

## Data Ownership

For every major data domain, identify:

| Data Domain | System Of Record | SynapseCore Role |
| --- | --- | --- |
| Catalog / products | To be completed | READ / IMPORT / MIRROR / RECEIVE EVENTS / OPERATE ON BOUNDED COPY |
| Inventory | To be completed | READ / IMPORT / MIRROR / RECEIVE EVENTS / OPERATE ON BOUNDED COPY |
| Orders | To be completed | READ / IMPORT / MIRROR / RECEIVE EVENTS / OPERATE ON BOUNDED COPY |

During Company 1, SynapseCore should not be assumed to become the master system unless explicitly agreed and supported by later technical review.

## Catalog / Product Data Intake

Actual supported product shape:

| Field | Status | Notes |
| --- | --- | --- |
| SKU / catalog SKU | REQUIRED | Max 64; normalized tenant-visible SKU. |
| Product name | REQUIRED | Max 120. |
| Category | REQUIRED | Max 120. |
| Internal SKU | SYNAPSCORE INTERNAL | Built from tenant code and catalog SKU; customer should not supply this. |
| Product status | NOT CURRENTLY SUPPORTED AS PRODUCT FIELD | Capture only as external context if needed. |
| Unit of measure | NOT CURRENTLY SUPPORTED AS PRODUCT FIELD | Capture only as external context if needed. |

Product CSV/import should be planned around:

- `sku`
- `name`
- `category`

## Inventory Data Intake

Actual supported inventory shape:

| Field | Status | Notes |
| --- | --- | --- |
| Product SKU | REQUIRED | Must match catalog/product. |
| Warehouse/location code | REQUIRED | Must map to SynapseCore warehouse. |
| Quantity available | REQUIRED for update | Non-negative. |
| Reorder threshold | REQUIRED for update | Non-negative. |
| Quantity on hand | SUPPORTED INTERNALLY | Used by stock model. |
| Quantity reserved | SUPPORTED INTERNALLY | Used by stock model. |
| Quantity inbound | SUPPORTED INTERNALLY | Used by stock model. |
| Last received/adjusted/reconciled | SYSTEM-MAINTAINED | Do not require from customer unless needed for baseline context. |
| Reconciliation variance | SYSTEM-MAINTAINED | Created by reconciliation flows. |

Current inventory operations include update, receive, adjust, and reconcile.

## Order Data Intake

Actual supported order shape:

| Field | Status | Notes |
| --- | --- | --- |
| External order ID | REQUIRED for external systems | Max 80 in internal create DTO; required by webhook/CSV import. |
| Warehouse code | REQUIRED | Max 40. |
| Items | REQUIRED | At least one item. |
| Item product SKU | REQUIRED | Must map to catalog. |
| Item quantity | REQUIRED | Positive integer. |
| Unit price | REQUIRED | Positive decimal. |
| Status | SUPPORTED | Current values include `CREATED`, `RECEIVED`, `PROCESSING`, `PARTIALLY_FULFILLED`, `FULFILLED`, `DELIVERED`, `CANCELLED`, `RETURNED`, `FAILED`, `BLOCKED`. |
| Customer reference | SUPPORTED IN WEBHOOK REQUEST CONTEXT | Capture if useful; do not depend on it unless reviewed. |
| Occurred at timestamp | SUPPORTED IN WEBHOOK REQUEST CONTEXT | Capture if available. |

CSV order import should be planned around:

- `sourceSystem`
- `externalOrderId`
- `warehouseCode`
- `productSku`
- `quantity`
- `unitPrice`

## Sample Data Requirements

Request non-sensitive or redacted samples first.

Required before provisioning:

- sample product/catalog file or records
- sample inventory records
- sample order record or file
- field definitions
- representative valid record
- representative invalid record
- expected order/status values
- identifier examples
- duplicate/retry example if relevant
- redaction notes

Real operational data should only be used after authorization and pilot environment readiness.

## Data Volume

Collect measurable estimates:

| Volume Field | Estimate |
| --- | --- |
| Products in pilot scope |  |
| Inventory rows in pilot scope |  |
| Warehouses/locations in pilot scope |  |
| Orders per day |  |
| Inbound events per day |  |
| Files per day |  |
| Records per file |  |
| Peak expected inbound burst |  |
| Expected concurrent operators |  |

Do not ask "Is your data large?" Collect estimates.

## Update Frequency

For each relevant data domain, choose:

- REALTIME / EVENT
- EVERY X MINUTES
- HOURLY
- DAILY
- MANUAL FILE
- OTHER

Do not promise realtime if the source system produces nightly files.

## Failure / Recovery Requirements

Ask:

- What failures currently occur?
- How are failures detected today?
- How are failures recovered today?
- Can failed inbound data safely be replayed?
- Is duplicate delivery possible?
- What constitutes a dangerous duplicate?
- Who may authorize recovery?
- Should replay require approval?
- What failed records should never be replayed?
- How should recovery evidence be reviewed?

Current replay-related truth:

- failed inbound records can create replay records
- statuses include `PENDING`, `REPLAY_FAILED`, `DEAD_LETTERED`, `REPLAYED`
- failure codes include connector missing/disabled, invalid token, missing fields, missing product/warehouse/inventory, duplicate external order ID, insufficient inventory, and unknown

## Approval / Governance Requirements

Collect:

- which actions require approval
- which business role may approve
- which business role may reject
- whether initiator may approve own action
- escalation expectations
- SLA/overdue expectations
- audit/evidence needs
- final approval owner
- review owner
- escalation owner

Use current SynapseCore governance capability only. Do not promise an arbitrary workflow engine.

## Alert Requirements

Collect requested attention conditions:

| Field | Prompt |
| --- | --- |
| Condition | What should operators notice? |
| Severity | Low / medium / high / critical business impact. |
| Audience | Who should see it? |
| Expected response | What should they do? |
| Operational owner | Who owns response? |
| Evidence source | What data proves the condition? |

Later Phase 7 classification:

- SUPPORTED
- CONFIGURABLE
- REQUIRES PRODUCT CHANGE
- OUT OF PILOT

## Recommendation Requirements

Collect decision-support needs:

- decision currently made manually
- evidence currently used
- operator responsible
- desired recommendation context
- approval requirement
- consequence of incorrect recommendation
- examples of useful and unhelpful recommendations

Do not ask the customer to design algorithms.

## Scenario Requirements

Where relevant, collect:

- what-if decisions worth comparing
- inputs operators currently consider
- decision owner
- approval requirement
- execution authority
- expected review owner
- final approver

If scenarios are irrelevant to Company 1, mark:

`OUT OF SCOPE`

Do not force every SynapseCore feature into every pilot.

## Realtime Requirements

Ask:

- Which changes need rapid visibility?
- What delay is acceptable?
- Which users need rapid updates?
- What currently happens when data is stale?
- Which updates can be manual or scheduled?
- Which updates are business-critical?

Do not promise zero-latency delivery.

## Security / Data Sensitivity

Collect categories, not unnecessary sensitive values.

Data sensitivity categories:

- commercially sensitive data
- employee-related data
- customer-related data
- internal identifiers
- financial information
- confidential operational information
- regulated data category, if identified by customer
- prohibited data category for pilot

Access/security constraints:

- authorized user requirements
- network restrictions
- data retention expectation if provided
- confidentiality considerations
- source-system access restrictions
- data export restrictions

Avoid collecting unnecessary sensitive data.

## Data Minimization Rule

Only data necessary for the pilot should be onboarded.

For every requested data field ask:

```text
Is this field necessary to prove the pilot objective?
```

If not, exclude it.

## Data Authorization

Required confirmation:

- company authorizes SynapseCore to receive/use the agreed pilot data
- authorization is limited to the agreed pilot purpose
- approved data domains are listed
- out-of-scope/prohibited data is listed
- external legal/contract approval status is known
- operational approver is named
- approval date is recorded

Do not invent legal terms. If legal approval is required, mark it as external prerequisite.

## Pilot Environment

Capture expected starting data environment:

- sample data first
- test/synthetic data first
- production-derived bounded data
- dedicated integration test
- production source connector

Preferred staged approach:

```text
SAMPLE -> TEST -> VERIFIED -> CONTROLLED LIVE PILOT
```

Do not automatically connect a production source on day one.

## Success Metrics

Agree on measurable success before provisioning.

| Area | Question |
| --- | --- |
| Visibility | Can operators see relevant state more effectively? |
| Detection | Are operational issues surfaced appropriately? |
| Recovery | Can supported failed inbound activity be recovered? |
| Decision support | Are recommendations useful and evidence understandable? |
| Governance | Can required decisions follow appropriate authorization? |
| Realtime | Are supported changes delivered within useful pilot timing? |
| Reliability | Can operators use the platform consistently? |
| Operator experience | Can users complete agreed tasks? |
| Integration | Does agreed data arrive correctly? |
| Data integrity | Does SynapseCore maintain correct pilot state? |

## Baseline Comparison

Collect current-state baseline where possible:

| Baseline Field | Current Measurement |
| --- | --- |
| Current time to detect issue |  |
| Current recovery effort |  |
| Current number of manual checks |  |
| Current systems/screens involved |  |
| Current response time |  |
| Current failure frequency |  |
| Current approval delay |  |
| Current replay/reprocessing method |  |

If unavailable, mark:

`NOT CURRENTLY MEASURED`

Do not invent baseline values.

## Dates And Schedule

Capture each date with `PROPOSED` or `CONFIRMED`.

| Date | Status | Owner |
| --- | --- | --- |
| Proposed setup window |  |  |
| Target pilot start |  |  |
| Target pilot end |  |  |
| Operator training/handover date |  |  |
| Review checkpoint 1 |  |  |
| Review checkpoint 2 |  |  |
| Final pilot review date |  |  |

Do not promise dates until technical feasibility is confirmed.

## Support Expectations

Collect:

- company support contact
- normal operating hours
- critical escalation method
- expected response expectations
- planned maintenance constraints
- business-critical periods to avoid

Clearly distinguish:

- pilot support expectation
- contractual SLA

Do not invent formal SLA commitments.

## Incident Communication

Record who should be contacted if:

- integration fails
- pilot must pause
- data integrity issue occurs
- access must be revoked
- critical security issue occurs
- source system becomes unavailable
- replay produces unexpected behavior
- customer asks for emergency scope change

This feeds Phase 12.

## Backup / Recovery Expectations

Current SynapseCore recovery truth:

- application-level PostgreSQL backup/restore is proven
- provider-level Render restore evidence remains a documented operating condition before reliance expands

Collect:

- customer recovery expectation
- maximum acceptable pilot data loss expectation
- maximum acceptable downtime expectation
- whether source systems remain fallback
- whether company requires backup evidence before live data
- recovery contact

Do not claim more than current evidence.

## Change Authority

Identify who may request:

| Change | Authorized Company Role/Person |
| --- | --- |
| Add user |  |
| Remove user |  |
| Role change |  |
| Connector change |  |
| Expanded data scope |  |
| New workflow |  |
| Pilot pause |  |
| Pilot termination |  |
| Emergency access revocation |  |

SynapseCore should not accept high-impact changes from arbitrary users.

## Customer Responsibilities

Company 1 is responsible for:

- providing accurate pilot requirements
- nominating approved users
- approving user access
- providing authorized sample/data access
- providing technical contact
- identifying source-system owners
- communicating source-system changes
- reporting observed operational issues
- participating in pilot reviews
- confirming data authorization and exclusions
- keeping existing systems of record authoritative during pilot

Customer is not responsible for SynapseCore infrastructure provisioning.

## SynapseCore Responsibilities

SynapseCore is responsible for later phases:

- provisioning tenant/workspace
- setting up access
- creating users
- assigning roles
- configuring connector lane
- onboarding agreed data
- verifying tenant/workspace/user/access state
- performing readiness checks
- supporting pilot operations
- executing backup operations where applicable
- classifying incidents
- controlling changes

Do not specify technical commands in Phase 2.

## Internal Risk Review

Classify each risk:

- LOW
- MEDIUM
- HIGH
- BLOCKER

| Risk Area | Classification | Notes |
| --- | --- | --- |
| Integration complexity |  |  |
| Data sensitivity |  |  |
| Unclear data ownership |  |  |
| Excessive pilot scope |  |  |
| Unsupported workflow |  |  |
| Operator count |  |  |
| Source-system instability |  |  |
| Unclear authorization |  |  |
| Unclear success criteria |  |  |
| Recovery expectations |  |  |
| Unrealistic availability expectations |  |  |
| Provider restore expectation |  |  |

## Intake Validation Gate

Before Phase 3 may begin, these critical fields must be complete:

- company identified
- business owner identified
- technical contact identified
- operational contact identified
- pilot problem defined
- pilot scope defined
- out-of-scope definition completed
- users identified
- user access approved
- current systems identified
- initial connector lane identified
- data domains identified
- sample data available or plan agreed
- data volume estimated
- update frequency known
- data authorization confirmed
- success metrics agreed
- support/escalation contact identified
- pilot envelope accepted or technical review required
- major security/data constraints known
- change authority known
- risk review completed

If a critical item is missing:

`DO NOT PROVISION`

## Pilot Intake Decision

At the end of Phase 2, classify the candidate:

| Decision | Meaning |
| --- | --- |
| APPROVED FOR PHASE 3 PROVISIONING | Intake is complete and internal approval allows tenant/workspace provisioning to begin. |
| APPROVED WITH PRE-PROVISION CONDITIONS | Provisioning may begin only after listed conditions are resolved. |
| TECHNICAL DISCOVERY REQUIRED | More technical clarification is needed before provisioning. |
| NOT CURRENTLY SUITABLE FOR PILOT | The current request does not fit the controlled pilot envelope or supported capability. |

No provisioning should begin without this decision.

## Customer Provides Vs SynapseCore Completes Internally

| Customer Provides | SynapseCore Completes Internally |
| --- | --- |
| Company information | Risk score/classification |
| Business problem | Supported role mapping |
| Approved users | Technical feasibility review |
| Source systems | Pilot envelope result |
| Authorized data | Provisioning approval |
| Technical contacts | Internal workspace/tenant naming |
| Data exclusions | Connector setup notes |
| Success metrics | Phase 3 handoff package |
| Support contacts | Internal implementation notes |

Do not expose unnecessary internal architecture to the customer.

## Phase 3 Handoff

Phase 3 is Tenant + Workspace Provisioning Runbook.

Phase 3 receives only approved provisioning inputs:

- approved company name
- approved workspace display name
- agreed operational scope
- business unit / department
- timezone/settings where supported
- approved pilot operator envelope
- approved user list reference
- initial connector scope reference
- approved data domains
- data sensitivity/exclusion summary
- support/escalation contacts
- internal provisioning approval
- risk classification
- pre-provision conditions

Do not write Phase 3 procedures in this document.

## Reusable Intake Template

Copy this section for each company.

### Intake Header

| Field | Value |
| --- | --- |
| Company |  |
| Intake owner |  |
| Intake state | DRAFT |
| Date opened |  |
| Last updated |  |
| Phase 1 fit result |  |
| Target pilot decision date |  |

### Company

| Field | Value |
| --- | --- |
| Legal name |  |
| Trading/display name |  |
| Business unit / department |  |
| Industry |  |
| Primary operational location |  |
| Pilot sponsor |  |

### Contacts

| Contact Type | Name | Business Role | Business Email | Notes |
| --- | --- | --- | --- | --- |
| Business owner |  |  |  |  |
| Technical contact |  |  |  |  |
| Operational contact |  |  |  |  |
| Escalation contact |  |  |  |  |
| Change authority |  |  |  |  |

### Pilot Problem

| Field | Value |
| --- | --- |
| Current process |  |
| Current pain |  |
| Current systems |  |
| Failure / visibility issue |  |
| Desired outcome |  |
| Why this workflow was selected |  |
| Bounded test statement |  |

### Scope

| In Scope | Notes |
| --- | --- |
| Locations/warehouses |  |
| Departments |  |
| Product groups |  |
| Order types |  |
| Inventory records |  |
| Connector lane |  |
| Users |  |
| Workflows |  |

### Not Included In Company 1 Pilot

| Out Of Scope | Reason |
| --- | --- |
|  |  |

### Users

| Name | Business Email | Job Responsibility | Pilot Responsibility | Decides? | Approves? | Admin Need? | Primary/Backup | Scope/Restriction |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
|  |  |  |  |  |  |  |  |  |

### Internal Role Mapping

SynapseCore completes internally.

| User | Business Responsibility | Proposed SynapseCore Role(s) | Warehouse Scope | Notes |
| --- | --- | --- | --- | --- |
|  |  |  |  |  |

### Access Approval

| Field | Value |
| --- | --- |
| Access approver |  |
| Approver business role |  |
| Approval date |  |
| Approved user list attached/recorded |  |
| Access restrictions |  |
| Confidentiality considerations |  |
| External legal prerequisite status |  |

### Systems Inventory

| System | Purpose | Data Owned | Source Of Truth? | Technical Owner | Integration Method | Environment | Update Frequency | Restrictions |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
|  |  |  |  |  |  |  |  |  |

### Initial Connector Lane

| Field | Value |
| --- | --- |
| Source system |  |
| Business purpose |  |
| Method |  |
| Direction |  |
| Inbound data type |  |
| Frequency |  |
| Sample data availability |  |
| Expected volume |  |
| Technical contact |  |
| Auth category |  |
| Secret required |  |
| Secret type |  |
| Secure handoff status |  |
| Network restrictions |  |
| Test environment availability |  |
| Error/retry expectations |  |

### Data Ownership

| Domain | System Of Record | SynapseCore Role | In Scope? | Notes |
| --- | --- | --- | --- | --- |
| Catalog / products |  |  |  |  |
| Inventory |  |  |  |  |
| Orders |  |  |  |  |

### Data Mapping

| Domain | Required Fields | Optional/Context Fields | Not Supported / Excluded |
| --- | --- | --- | --- |
| Catalog | SKU, name, category |  | product status, unit of measure unless retained outside supported model |
| Inventory | product SKU, warehouse code, quantity available, reorder threshold | on hand, reserved, inbound, update frequency | unsupported custom stock attributes |
| Orders | external order ID, warehouse code, product SKU, quantity, unit price | customer reference, occurred at | arbitrary enterprise order schema |

### Sample Data

| Sample | Status | Notes |
| --- | --- | --- |
| Product/catalog sample |  |  |
| Inventory sample |  |  |
| Order sample |  |  |
| Valid record example |  |  |
| Invalid record example |  |  |
| Expected status values |  |  |
| Identifier examples |  |  |
| Redaction confirmed |  |  |

### Volumes And Timing

| Field | Estimate |
| --- | --- |
| Products |  |
| Inventory rows |  |
| Orders/day |  |
| Inbound events/day |  |
| Files/day |  |
| Records/file |  |
| Concurrent operators |  |
| Update frequency |  |
| Acceptable visibility delay |  |

### Failure And Recovery

| Question | Answer |
| --- | --- |
| Common current failures |  |
| Current detection method |  |
| Current recovery method |  |
| Safe to replay failed inbound data? |  |
| Duplicate delivery possible? |  |
| Dangerous duplicate definition |  |
| Recovery approver |  |
| Replay requires approval? |  |
| Never-replay cases |  |

### Governance

| Question | Answer |
| --- | --- |
| Actions requiring approval |  |
| Business role that may approve |  |
| Business role that may reject |  |
| Initiator may approve own action? |  |
| Escalation expectation |  |
| Audit/evidence need |  |

### Alerts, Recommendations, Scenarios, Realtime

| Area | Requirement | Owner | Notes |
| --- | --- | --- | --- |
| Alerts |  |  |  |
| Recommendations |  |  |  |
| Scenarios |  |  |  |
| Realtime |  |  |  |

### Security And Data Sensitivity

| Area | Notes |
| --- | --- |
| Sensitive data categories |  |
| Prohibited data categories |  |
| Authorized users only? |  |
| Network restrictions |  |
| Retention expectation |  |
| Data minimization confirmed |  |
| Data authorization confirmed |  |
| External legal prerequisite |  |

### Success Metrics And Baseline

| Metric Area | Success Definition | Baseline |
| --- | --- | --- |
| Visibility |  |  |
| Detection |  |  |
| Recovery |  |  |
| Decision support |  |  |
| Governance |  |  |
| Realtime |  |  |
| Reliability |  |  |
| Operator experience |  |  |
| Integration |  |  |
| Data integrity |  |  |

### Schedule

| Date | Proposed/Confirmed | Owner |
| --- | --- | --- |
| Setup window |  |  |
| Pilot start |  |  |
| Pilot end |  |  |
| Handover/training |  |  |
| Review checkpoint 1 |  |  |
| Review checkpoint 2 |  |  |
| Final review |  |  |

### Support And Incident Communication

| Event | Contact | Method | Notes |
| --- | --- | --- | --- |
| Normal support |  |  |  |
| Critical escalation |  |  |  |
| Integration failure |  |  |  |
| Pilot pause |  |  |  |
| Data integrity issue |  |  |  |
| Access revocation |  |  |  |
| Security issue |  |  |  |
| Source system unavailable |  |  |  |

### Backup / Recovery Expectations

| Field | Value |
| --- | --- |
| Customer recovery expectation |  |
| Acceptable pilot data loss expectation |  |
| Acceptable downtime expectation |  |
| Existing systems remain fallback? |  |
| Backup evidence required before live data? |  |
| Recovery contact |  |

### Change Authority

| Change | Authorized Person/Role |
| --- | --- |
| Add user |  |
| Remove user |  |
| Role change |  |
| Connector change |  |
| Expanded data scope |  |
| New workflow |  |
| Pilot pause |  |
| Pilot termination |  |

### Internal Risk Review

| Risk Area | LOW / MEDIUM / HIGH / BLOCKER | Notes |
| --- | --- | --- |
| Integration complexity |  |  |
| Data sensitivity |  |  |
| Data ownership clarity |  |  |
| Pilot scope |  |  |
| Workflow support |  |  |
| Operator count |  |  |
| Source-system stability |  |  |
| Authorization clarity |  |  |
| Success criteria clarity |  |  |
| Recovery expectations |  |  |
| Availability expectations |  |  |

### Intake Validation Gate

| Critical Field | Complete? | Notes |
| --- | --- | --- |
| Company identified |  |  |
| Business owner |  |  |
| Technical contact |  |  |
| Pilot problem |  |  |
| Pilot scope |  |  |
| Out-of-scope definition |  |  |
| Users identified |  |  |
| User access approved |  |  |
| Systems identified |  |  |
| Initial connector lane |  |  |
| Data domains |  |  |
| Sample data plan |  |  |
| Data volume estimate |  |  |
| Data authorization |  |  |
| Success metrics |  |  |
| Support/escalation contact |  |  |
| Pilot envelope result |  |  |
| Security/data constraints |  |  |

### Final Intake Decision

Decision:

```text
APPROVED FOR PHASE 3 PROVISIONING
APPROVED WITH PRE-PROVISION CONDITIONS
TECHNICAL DISCOVERY REQUIRED
NOT CURRENTLY SUITABLE FOR PILOT
```

Decision notes:

```text
TBD
```

Phase 3 handoff approved by:

```text
TBD
```

## Phase 2 Verdict Standard

Phase 2 is acceptable when:

- the customer has provided all required business, technical, data, and access inputs
- the pilot problem is bounded and measurable
- the standard pilot envelope is accepted or technical review is explicitly required
- data scope and exclusions are clear
- user access is approved
- secrets are not stored in the intake
- success metrics and pause conditions are defined
- internal risk review is complete
- final intake decision is recorded
- Phase 3 receives only approved provisioning inputs

Current Phase 2 document verdict:

`COMPANY PILOT PHASE 2 ACCEPTED`
