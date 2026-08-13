# Company Data Onboarding Record

Use this template for Company 1 Pilot Phase 6 evidence. Do not store customer row data, payloads, credentials, private files, or screenshots containing sensitive details in this record.

## Record Metadata

| Field | Value |
| --- | --- |
| Company | Company 1 |
| Tenant code |  |
| Tenant name |  |
| Data domain | Catalog / Inventory / Orders |
| Source system |  |
| Source environment | Synthetic / sample / controlled live |
| Source-of-truth owner |  |
| Prepared by |  |
| Reviewed by |  |
| Date prepared |  |
| Phase | Phase 6 - Company Data Onboarding |

## Scope

| Field | Value |
| --- | --- |
| Approved Phase 2 scope reference |  |
| In-scope records |  |
| Out-of-scope records |  |
| Approximate volume |  |
| Update frequency |  |
| Approved fields |  |
| Data minimization confirmed |  |
| Customer authorization confirmed |  |

## Environment Safety

| Check | Value |
| --- | --- |
| Environment | Local / Render / other |
| Backend URL |  |
| Frontend URL |  |
| Tenant code displayed before import |  |
| Tenant display name displayed before import |  |
| Operator username |  |
| Operator role |  |
| Test/live mode |  |
| Explicit operator confirmation captured |  |

## Mapping Reference

| Field | Value |
| --- | --- |
| Mapping record path/reference |  |
| Mapping version/reference |  |
| SKU normalization approved |  |
| Warehouse/location mapping approved |  |
| External order id strategy approved |  |
| Unsupported fields excluded |  |

## Pre-Import Data Quality Summary

| Metric | Count / Result |
| --- | --- |
| Total records |  |
| Valid records |  |
| Invalid records |  |
| Duplicates |  |
| Missing required field |  |
| Unknown SKU |  |
| Unknown location |  |
| Invalid status |  |
| Invalid quantity |  |
| Malformed records |  |
| Unsupported fields excluded |  |
| Other |  |

## Import Execution

| Field | Value |
| --- | --- |
| Import method |  |
| Start time |  |
| End time |  |
| Source count |  |
| Accepted/imported count |  |
| Rejected count |  |
| Duplicate count |  |
| Resulting readback count |  |
| Backup/checkpoint reference |  |
| Import status | Pass / partial / failed |
| Operator notes |  |

## Reconciliation

| Check | Result | Evidence |
| --- | --- | --- |
| Source total minus approved rejections equals expected SynapseCore total |  |  |
| Catalog count reconciled |  |  |
| Inventory count reconciled |  |  |
| Order count reconciled |  |  |
| Identifier readback reconciled |  |  |
| Quantity readback reconciled |  |  |
| Unexplained mismatch exists |  |  |

## Integrity Checks

| Check | Result | Evidence |
| --- | --- | --- |
| No wrong-tenant records |  |  |
| No orphan inventory |  |  |
| No order item references missing product |  |  |
| No invalid warehouse relation |  |  |
| No duplicate product/location inventory row |  |  |
| No duplicate external order id |  |  |
| No duplicate replay outcome |  |  |

## Functional Readback

| Surface | Result | Evidence |
| --- | --- | --- |
| Catalog page/API |  |  |
| Inventory page/API |  |  |
| Orders page/API |  |  |
| Dashboard snapshot |  |  |
| Integrations page/API |  |  |
| Replay page/API |  |  |
| Realtime update |  |  |

## Failure And Replay Evidence

| Check | Result | Evidence |
| --- | --- | --- |
| Deterministic invalid record submitted |  |  |
| Failure visible |  |  |
| Prerequisite corrected through supported path |  |  |
| Replay completed if eligible |  |  |
| Final business state correct |  |  |

## Sensitive Data Handling

| Check | Result |
| --- | --- |
| No customer data committed to Git |  |
| No customer payload in docs |  |
| No customer payload in public reports |  |
| Temporary files deleted or retained by explicit approval |  |
| Screenshots redacted |  |
| Inbound/replay payload sensitivity acknowledged |  |

## Issues And Decisions

| Issue/decision | Impact | Owner | Resolution |
| --- | --- | --- | --- |
|  |  |  |  |

## Phase 7 Authorization

| Field | Value |
| --- | --- |
| Data domain accepted | Yes / No / accepted with limitation |
| Limitation summary |  |
| Approved for Phase 7 operational configuration |  |
| Approver |  |
| Date |  |
