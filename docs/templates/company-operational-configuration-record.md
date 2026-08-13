# Company Operational Configuration Record

Use this template after completing Company 1 Phase 7 operational configuration. Do not store secrets, passwords, raw connector tokens, private customer payloads, database credentials, or sensitive row-level customer data in this record.

## Record Control

| Field | Value |
| --- | --- |
| Company | Company 1 |
| Tenant code |  |
| Tenant name |  |
| Configuration date |  |
| Configuration owner |  |
| Previous phase evidence | Phase 3 tenant/workspace, Phase 4 users, Phase 5 connector, Phase 6 data onboarding |
| Phase 8 handoff owner |  |
| Final status | DRAFT / FROZEN / SUPERSEDED |

## Pilot Feature Scope

| Feature | In pilot? | Customer visible? | Role required | Dependency | Limitation |
| --- | --- | --- | --- | --- | --- |
| Dashboard |  |  |  |  |  |
| Catalog |  |  |  |  |  |
| Inventory |  |  |  |  |  |
| Orders |  |  |  |  |  |
| Alerts |  |  |  |  |  |
| Recommendations |  |  |  |  |  |
| Integrations |  |  |  |  |  |
| Replay |  |  |  |  |  |
| Approvals |  |  |  |  |  |
| Scenarios |  |  |  |  |  |
| Runtime |  |  |  |  |  |
| Settings |  |  |  |  |  |
| Admin capabilities |  |  |  |  |  |

## Alerts

| Requested condition | Supported state | Final configuration | Verification plan/result | Limitation | Approval |
| --- | --- | --- | --- | --- | --- |
| Low stock |  |  |  |  |  |
| Depletion risk |  |  |  |  |  |
| Fulfillment backlog |  |  |  |  |  |
| Delivery delay risk |  |  |  |  |  |
| Fulfillment anomaly |  |  |  |  |  |
| External notifications |  |  |  |  |  |
| Custom alert rule |  |  |  |  |  |

## Recommendations

| Recommendation family | Supported state | Final priority/policy | Verification plan/result | Limitation | Approval |
| --- | --- | --- | --- | --- | --- |
| Reorder stock |  |  |  |  |  |
| Reorder urgently |  |  |  |  |  |
| Transfer stock |  |  |  |  |  |
| Prioritize fulfillment |  |  |  |  |  |
| Escalate logistics |  |  |  |  |  |
| Investigate logistics anomaly |  |  |  |  |  |
| Dismissal/suppression |  |  |  |  |  |
| Automatic execution |  |  |  |  |  |

## Tenant Operational Policy

| Field | Default value | Company 1 value | Why changed | Approval | Verification |
| --- | --- | --- | --- | --- | --- |
| lowStockCriticalRatio | 0.5 |  |  |  |  |
| depletionRiskHoursThreshold | 8 |  |  |  |  |
| urgentDepletionRiskHoursThreshold | 4 |  |  |  |  |
| rapidConsumptionUnitsMinimum | 5 |  |  |  |  |
| rapidConsumptionThresholdRatio | 0.5 |  |  |  |  |
| backlogRiskCount | 4 |  |  |  |  |
| backlogCriticalCount | 6 |  |  |  |  |
| backlogClearHoursThreshold | 6 |  |  |  |  |
| delayedShipmentCountThreshold | 2 |  |  |  |  |
| overdueDispatchCountThreshold | 2 |  |  |  |  |
| deliveryDelayToleranceHours | 2 |  |  |  |  |
| highRiskScoreThreshold | 40 |  |  |  |  |
| criticalRiskScoreThreshold | 100 |  |  |  |  |
| lowStockSeverity | HIGH |  |  |  |  |
| lowStockCriticalSeverity | CRITICAL |  |  |  |  |
| depletionRiskSeverity | HIGH |  |  |  |  |
| urgentDepletionRiskSeverity | CRITICAL |  |  |  |  |
| backlogRiskSeverity | HIGH |  |  |  |  |
| backlogCriticalSeverity | CRITICAL |  |  |  |  |
| deliveryDelaySeverity | HIGH |  |  |  |  |
| fulfillmentAnomalySeverity | CRITICAL |  |  |  |  |
| lowStockRecommendationPriority | MEDIUM |  |  |  |  |
| criticalLowStockRecommendationPriority | CRITICAL |  |  |  |  |
| depletionRiskRecommendationPriority | MEDIUM |  |  |  |  |
| urgentDepletionRiskRecommendationPriority | HIGH |  |  |  |  |
| backlogRecommendationPriority | MEDIUM |  |  |  |  |
| deliveryDelayRecommendationPriority | HIGH |  |  |  |  |
| fulfillmentAnomalyRecommendationPriority | CRITICAL |  |  |  |  |
| escalatedApprovalMinimumPriority | CRITICAL |  |  |  |  |
| reviewHoursMedium | 8 |  |  |  |  |
| reviewHoursHigh | 4 |  |  |  |  |
| reviewHoursCritical | 2 |  |  |  |  |
| finalApprovalHoursMedium | 4 |  |  |  |  |
| finalApprovalHoursHigh | 2 |  |  |  |  |
| finalApprovalHoursCritical | 1 |  |  |  |  |
| reviewOwnerRole | REVIEW_OWNER |  |  |  |  |
| finalApproverRole | FINAL_APPROVER |  |  |  |  |
| escalationOwnerRole | ESCALATION_OWNER |  |  |  |  |

## Replay and Recovery

| Rule | Final Company 1 value | Evidence required | Limitation | Approval |
| --- | --- | --- | --- | --- |
| Who may inspect replay queue |  |  |  |  |
| Who may manually replay |  |  |  |  |
| Required pre-replay checks |  |  |  |  |
| Replay stop conditions acknowledged |  |  |  |  |
| Dead-letter handling |  |  |  |  |
| Duplicate-risk handling |  |  |  |  |
| Customer/source-system confirmation needed? |  |  |  |  |

## Approval Governance

| Business decision | SynapseCore action | Initiator role | Review role | Final authority | Escalation role | Evidence required |
| --- | --- | --- | --- | --- | --- | --- |
| Scenario saved plan review |  |  |  |  |  |  |
| Escalated scenario final approval |  |  |  |  |  |  |
| Scenario rejection |  |  |  |  |  |  |
| Replay procedural approval, if required by Company 1 |  |  |  |  |  |  |

## Separation of Duty

| Control | Technical enforcement | Procedural enforcement | Company 1 final decision | Evidence |
| --- | --- | --- | --- | --- |
| Requester differs from approver for escalated scenario | Yes |  |  |  |
| Review owner differs from final approver for escalated scenario | Yes |  |  |  |
| Standard scenario self-approval prevention | Partial/procedural |  |  |  |
| Same operator holding multiple governance roles | Not globally prevented |  |  |  |
| Tenant admin also being approver | Not globally prevented |  |  |  |

## Scenarios

| Field | Value |
| --- | --- |
| Scenario workflow in Company 1 pilot? | YES / NO |
| Approved use case |  |
| Approved warehouse(s) |  |
| Approved products/SKUs |  |
| Execution allowed? | YES / NO |
| Execution conditions |  |
| If out of scope, reason |  |

## Integration Policies

| Connector | Source system | Type | Enabled | Sync mode | Cadence | Validation | Transformation | Mapping version | Fallback | Default warehouse | Support owner | Token hint only |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
|  |  |  |  |  |  |  |  |  |  |  |  |  |

## Tenant Settings

| Setting | Final value | Verification | Approval |
| --- | --- | --- | --- |
| Tenant name |  |  |  |
| Description |  |  |  |
| Password rotation days |  |  |  |
| Session timeout minutes |  |  |  |
| Invalidate existing sessions performed? | YES / NO |  |  |

## Warehouse Metadata

| Warehouse code | Name | Location | Changed in Phase 7? | Verification |
| --- | --- | --- | --- | --- |
|  |  |  |  |  |

## Operator Scopes

| User/operator | Role(s) | Warehouse scope | Connector scope exists? | Governance scope | Customer visible? | Verification |
| --- | --- | --- | --- | --- | --- | --- |
|  |  |  | No connector-specific scope model |  |  |  |

## Realtime Expectations

| Business event | Expected realtime screen | Supported? | Fallback if disconnected | Verification |
| --- | --- | --- | --- | --- |
| Inventory update |  |  |  |  |
| New order |  |  |  |  |
| Connector update |  |  |  |  |
| Replay state change |  |  |  |  |
| Scenario approval/escalation |  |  |  |  |
| Runtime incident |  |  |  |  |

## Runtime and Trust Notes

| Runtime signal | Expected interpretation | Action if degraded | Verification |
| --- | --- | --- | --- |
| Readiness |  |  |  |
| Liveness |  |  |  |
| Realtime broker |  |  |  |
| Dispatch queue |  |  |  |
| Failed dispatch |  |  |  |
| Connector diagnostics |  |  |  |
| Active incidents |  |  |  |

## Customer-Visible vs Internal

| Area | Customer operator visible | Customer admin visible | SynapseCore internal only | Notes |
| --- | --- | --- | --- | --- |
| Dashboard |  |  |  |  |
| Orders |  |  |  |  |
| Inventory |  |  |  |  |
| Alerts |  |  |  |  |
| Recommendations |  |  |  |  |
| Integrations |  |  |  |  |
| Replay |  |  |  |  |
| Approvals |  |  |  |  |
| Scenarios |  |  |  |  |
| Runtime |  |  |  |  |
| Settings |  |  |  |  |
| Tenant management |  |  |  |  |
| Infrastructure controls |  |  |  |  |

## Known Operating Conditions

| Condition | Accepted? | Owner | Notes |
| --- | --- | --- | --- |
| No MFA/SSO/invitation flow |  |  |  |
| No customer forgot-password flow |  |  |  |
| No alert recipients/notifications |  |  |  |
| No recommendation dismissal/suppression |  |  |  |
| No generic approval workflow builder |  |  |  |
| No connector-specific operator scope |  |  |  |
| No inventory CSV import |  |  |  |
| No per-import rollback |  |  |  |
| No automatic retention cleanup control |  |  |  |
| No provider-level restore proof beyond documented limitation |  |  |  |

## Verification Handoff to Phase 8

| Check | Ready for Phase 8? | Evidence location | Notes |
| --- | --- | --- | --- |
| Tenant configuration understood |  |  |  |
| User/operator scopes understood |  |  |  |
| Approved roles aligned |  |  |  |
| Alert capability mapped |  |  |  |
| Recommendation capability mapped |  |  |  |
| Replay operating rules defined |  |  |  |
| Governance matrix mapped |  |  |  |
| Scenario in/out decision recorded |  |  |  |
| Integration policies recorded |  |  |  |
| Settings recorded |  |  |  |
| Realtime expectations mapped |  |  |  |
| Runtime/trust behavior understood |  |  |  |
| Cross-tenant negative test planned |  |  |  |
| Operating limitations documented |  |  |  |
| Baseline frozen |  |  |  |
| No unresolved critical blocker |  |  |  |
| No unresolved high blocker |  |  |  |

## Change Freeze

| Freeze item | Value |
| --- | --- |
| Configuration frozen? | YES / NO |
| Freeze timestamp |  |
| Freeze approver |  |
| Further changes require | REQUEST -> APPROVAL -> IMPLEMENT -> VERIFY -> RECORD |
| Linked change records |  |

## Final Phase 7 Decision

| Decision | Mark one |
| --- | --- |
| COMPANY PILOT PHASE 7 ACCEPTED |  |
| COMPANY PILOT PHASE 7 ACCEPTED WITH DOCUMENTED LIMITATION |  |
| COMPANY PILOT PHASE 7 NOT ACCEPTED - SAFE OPERATIONAL CONFIGURATION INCOMPLETE |  |

Decision notes:

```text

```
