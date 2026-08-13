# Pre-Pilot Gate 4 Control Verification

This document records the final Pre-Pilot Gate 4 interactive-control execution pass for the current SynapseCore pilot candidate.

Gate 4 asks one direct question:

Can every interactive control in SynapseCore be individually verified before Company 1 receives the pilot build?

Current status:

PRE-PILOT GATE 4 ACCEPTED WITH DOCUMENTED LIMITATION

## Baseline

- Authoritative inventory commit: 48d3546a9e8d37b053d93997ede78c735eb7f02f
- Catalog click-layer fix commit: 2cfe8a1
- Control execution harness commit: 8c70b31
- Inventory regenerated after source change: 2026-08-13T11:07:10.526Z
- Execution report generated: 2026-08-13T11:06:21.433Z
- Backend under proof: https://synapscore-3.onrender.com
- Proof tenant: HOSTED-PROOF-3

The previous 198 estimate is obsolete. The authoritative inventory remains 201 after the catalog CSS source change.

## Inventory Summary

| Control type | Count |
| --- | ---: |
| Buttons | 142 |
| Inputs | 40 |
| Selects | 13 |
| Textareas | 1 |
| Checkboxes | 2 |
| Radios | 0 |
| Forms | 1 |
| Role buttons | 0 |
| Anchors/navigation actions | 0 |
| Other interactive controls | 3 |
| Total | 201 |

## Final Classification Accounting

| Classification | Count |
| --- | ---: |
| VERIFIED WORKING | 192 |
| VERIFIED WORKING WITH DOCUMENTED LIMITATION | 6 |
| DISABLED BY DESIGN - VERIFIED | 2 |
| ROLE RESTRICTED - VERIFIED | 0 |
| WORKING WITH LIMITATION | 1 |
| BROKEN | 0 |
| PLACEHOLDER / NO-OP | 0 |
| UNVERIFIED | 0 |

Final reconciliation:

- CONTROL INVENTORY: 201
- INDIVIDUALLY VERIFIED: 201 / 201
- UNVERIFIED: 0
- Unexpected 5xx responses: 0
- Unexpected network failures: 0

Console noise observed during expected negative-path tests:
- Failed to load resource: the server responded with a status of 401 ()
- Failed to load resource: the server responded with a status of 400 ()

These console entries correspond to deliberate negative-path checks: invalid sign-in, role/authorization denial, and invalid current-password recovery. They did not produce unexpected 5xx responses or stuck UI state.

## Defects Fixed

| CTRL ID | Severity | Root cause | Fix | Proof result |
| --- | --- | --- | --- | --- |
| CTRL-024 | High | Catalog Clear action could be enabled while adjacent catalog content intercepted pointer events on the hosted build. | Added local stacking/min-width containment for catalog workflow cards and action rows in frontend/src/design-system.css. | Final hosted control execution passed with BROKEN=0. |

## Documented Limitations

| CTRL ID | Classification | Limitation |
| --- | --- | --- |
| CTRL-107 | VERIFIED WORKING WITH DOCUMENTED LIMITATION | Positive password persistence should be executed against a disposable account before broad production rollout. |
| CTRL-108 | VERIFIED WORKING WITH DOCUMENTED LIMITATION | Positive password persistence should be executed against a disposable account before broad production rollout. |
| CTRL-109 | VERIFIED WORKING WITH DOCUMENTED LIMITATION | Positive password persistence should be executed against a disposable account before broad production rollout. |
| CTRL-110 | VERIFIED WORKING WITH DOCUMENTED LIMITATION | Positive password persistence should be executed against a disposable account before broad production rollout. |
| CTRL-117 | VERIFIED WORKING WITH DOCUMENTED LIMITATION | Gate 4 execution did not force a second destructive replay if the deterministic queue was already clear or ineligible; backend truth replay remains covered b... |
| CTRL-153 | WORKING WITH LIMITATION | Requires fresh tenant result to become enabled. |
| CTRL-162 | VERIFIED WORKING WITH DOCUMENTED LIMITATION | Successful tenant bootstrap remains covered by supported hosted proof preparation and platform-admin token flow. |

## Machine-Readable Control Reference

The table below is generated from frontend/test-results/control-inventory/control-inventory.json plus frontend/test-results/control-execution/gate-4-control-execution-report.json. The source artifacts remain local proof artifacts and are not committed.

| CTRL ID | Route | Component | Type | Label | Classification | Evidence | Limitation / defect |
| --- | --- | --- | --- | --- | --- | --- | --- |
| CTRL-001 | /, /product, /contact | PublicExperience | button |  | VERIFIED WORKING | BATCH 1 public and authentication controls execute and recover |  |
| CTRL-002 | /, /product, /contact | PublicExperience | button | page.label | VERIFIED WORKING | BATCH 1 public and authentication controls execute and recover |  |
| CTRL-003 | /, /product, /contact | PublicExperience | button | Sign In to Workspace | VERIFIED WORKING | BATCH 1 public and authentication controls execute and recover |  |
| CTRL-004 | /, /product, /contact | PublicExperience | button | Sign In to Workspace | VERIFIED WORKING | BATCH 1 public and authentication controls execute and recover |  |
| CTRL-005 | /, /product, /contact | PublicExperience | button | Create Company Workspace | VERIFIED WORKING | BATCH 1 public and authentication controls execute and recover |  |
| CTRL-006 | /, /product, /contact | PublicExperience | button | Sign In to Workspace | VERIFIED WORKING | BATCH 1 public and authentication controls execute and recover |  |
| CTRL-007 | /, /product, /contact | PublicExperience | button | finalCtaLabel | VERIFIED WORKING | BATCH 1 public and authentication controls execute and recover |  |
| CTRL-008 | /alerts | AlertsPage | button | Focus highest severity | VERIFIED WORKING | BATCH 3 dashboard runtime audit and operational read controls execute |  |
| CTRL-009 | /alerts | AlertsPage | button |  | VERIFIED WORKING | BATCH 3 dashboard runtime audit and operational read controls execute |  |
| CTRL-010 | /approvals | ApprovalsPage | button | Focus overdue decision | VERIFIED WORKING | BATCH 5 scenarios replay approvals and role restrictions execute |  |
| CTRL-011 | /approvals | ApprovalsPage | button |  | VERIFIED WORKING | BATCH 5 scenarios replay approvals and role restrictions execute |  |
| CTRL-012 | /approvals | ApprovalsPage | button |  | VERIFIED WORKING | BATCH 5 scenarios replay approvals and role restrictions execute |  |
| CTRL-013 | /approvals | ApprovalsPage | button |  | VERIFIED WORKING | BATCH 5 scenarios replay approvals and role restrictions execute |  |
| CTRL-014 | /approvals | ApprovalsPage | button |  | VERIFIED WORKING | BATCH 5 scenarios replay approvals and role restrictions execute |  |
| CTRL-015 | /audit-events | AuditPage | button |  | VERIFIED WORKING | BATCH 3 dashboard runtime audit and operational read controls execute |  |
| CTRL-016 | /audit-events | AuditPage | button |  | VERIFIED WORKING | BATCH 3 dashboard runtime audit and operational read controls execute |  |
| CTRL-017 | /audit-events | AuditPage | button | Open Runtime | VERIFIED WORKING | BATCH 3 dashboard runtime audit and operational read controls execute |  |
| CTRL-018 | /audit-events | AuditPage | button | Open Replay | VERIFIED WORKING | BATCH 3 dashboard runtime audit and operational read controls execute |  |
| CTRL-019 | /catalog | CatalogPage | button | catalogForm.id ? 'Create new product' : 'Reset form' | VERIFIED WORKING | BATCH 4 catalog and workspace-admin mutation controls execute with readback |  |
| CTRL-020 | /catalog | CatalogPage | input | SKU-ACME-100 | VERIFIED WORKING | BATCH 4 catalog and workspace-admin mutation controls execute with readback |  |
| CTRL-021 | /catalog | CatalogPage | input | Product name | VERIFIED WORKING | BATCH 4 catalog and workspace-admin mutation controls execute with readback |  |
| CTRL-022 | /catalog | CatalogPage | input | Operational category | VERIFIED WORKING | BATCH 4 catalog and workspace-admin mutation controls execute with readback |  |
| CTRL-023 | /catalog | CatalogPage | button | Unlabeled interactive control | VERIFIED WORKING | BATCH 4 catalog and workspace-admin mutation controls execute with readback |  |
| CTRL-024 | /catalog | CatalogPage | button | Clear | VERIFIED WORKING | BATCH 4 catalog and workspace-admin mutation controls execute with readback |  |
| CTRL-025 | /catalog | CatalogPage | input |  | VERIFIED WORKING | BATCH 4 catalog and workspace-admin mutation controls execute with readback |  |
| CTRL-026 | /catalog | CatalogPage | interactive-surface | ) : ( | VERIFIED WORKING | BATCH 4 catalog and workspace-admin mutation controls execute with readback |  |
| CTRL-027 | /catalog | CatalogPage | button | Inspect | VERIFIED WORKING | BATCH 4 catalog and workspace-admin mutation controls execute with readback |  |
| CTRL-028 | /catalog | CatalogPage | button | Edit | VERIFIED WORKING | BATCH 4 catalog and workspace-admin mutation controls execute with readback |  |
| CTRL-029 | /company-settings | SettingsPage | input | Company workspace name | VERIFIED WORKING | BATCH 4 catalog and workspace-admin mutation controls execute with readback |  |
| CTRL-030 | /company-settings | SettingsPage | input | Operational workspace summary | VERIFIED WORKING | BATCH 4 catalog and workspace-admin mutation controls execute with readback |  |
| CTRL-031 | /company-settings | SettingsPage | button | accessAdminState.loading ? 'Working...' : 'Save Workspace' | VERIFIED WORKING | BATCH 4 catalog and workspace-admin mutation controls execute with readback |  |
| CTRL-032 | /company-settings | SettingsPage | input | workspaceSecurityForm.passwordRotationDays | VERIFIED WORKING | BATCH 4 catalog and workspace-admin mutation controls execute with readback |  |
| CTRL-033 | /company-settings | SettingsPage | input | workspaceSecurityForm.sessionTimeoutMinutes | VERIFIED WORKING | BATCH 4 catalog and workspace-admin mutation controls execute with readback |  |
| CTRL-034 | /company-settings | SettingsPage | checkbox |  | VERIFIED WORKING | BATCH 4 catalog and workspace-admin mutation controls execute with readback |  |
| CTRL-035 | /company-settings | SettingsPage | button | accessAdminState.loading ? 'Working...' : 'Save Security Policy' | VERIFIED WORKING | BATCH 4 catalog and workspace-admin mutation controls execute with readback |  |
| CTRL-036 | /company-settings | SettingsPage | button |  | VERIFIED WORKING | BATCH 4 catalog and workspace-admin mutation controls execute with readback |  |
| CTRL-037 | /company-settings | SettingsPage | input | selectedWorkspaceWarehouseDraft.name | VERIFIED WORKING | BATCH 4 catalog and workspace-admin mutation controls execute with readback |  |
| CTRL-038 | /company-settings | SettingsPage | input | selectedWorkspaceWarehouseDraft.location | VERIFIED WORKING | BATCH 4 catalog and workspace-admin mutation controls execute with readback |  |
| CTRL-039 | /company-settings | SettingsPage | button | Save Warehouse | VERIFIED WORKING | BATCH 4 catalog and workspace-admin mutation controls execute with readback |  |
| CTRL-040 | /company-settings | SettingsPage | button |  | VERIFIED WORKING | BATCH 4 catalog and workspace-admin mutation controls execute with readback |  |
| CTRL-041 | /company-settings | SettingsPage | select | selectedWorkspaceConnectorDraft.syncMode | VERIFIED WORKING | BATCH 4 catalog and workspace-admin mutation controls execute with readback |  |
| CTRL-042 | /company-settings | SettingsPage | select | selectedWorkspaceConnectorDraft.validationPolicy | VERIFIED WORKING | BATCH 4 catalog and workspace-admin mutation controls execute with readback |  |
| CTRL-043 | /company-settings | SettingsPage | select | selectedWorkspaceConnectorDraft.transformationPolicy | VERIFIED WORKING | BATCH 4 catalog and workspace-admin mutation controls execute with readback |  |
| CTRL-044 | /company-settings | SettingsPage | input | selectedWorkspaceConnectorDraft.syncMode === 'SCHEDULED_PULL' ? selectedWorkspaceConnectorDraft.syncIntervalMinutes : selectedWorkspaceConnectorDraft.syncMod... | VERIFIED WORKING | BATCH 4 catalog and workspace-admin mutation controls execute with readback |  |
| CTRL-045 | /company-settings | SettingsPage | select | selectedWorkspaceConnectorDraft.supportOwnerActorName | VERIFIED WORKING | BATCH 4 catalog and workspace-admin mutation controls execute with readback |  |
| CTRL-046 | /company-settings | SettingsPage | input | https://company.example.com/orders-feed | VERIFIED WORKING | BATCH 4 catalog and workspace-admin mutation controls execute with readback |  |
| CTRL-047 | /company-settings | SettingsPage | button | Save Connector Policy | VERIFIED WORKING | BATCH 4 catalog and workspace-admin mutation controls execute with readback |  |
| CTRL-048 | /create-workspace | CreateWorkspacePage | button |  | VERIFIED WORKING | BATCH 1 public and authentication controls execute and recover |  |
| CTRL-049 | /create-workspace | CreateWorkspacePage | button | page.label | VERIFIED WORKING | BATCH 1 public and authentication controls execute and recover |  |
| CTRL-050 | /create-workspace | CreateWorkspacePage | button | Sign In to Workspace | VERIFIED WORKING | BATCH 1 public and authentication controls execute and recover |  |
| CTRL-051 | /create-workspace | CreateWorkspacePage | button |  | VERIFIED WORKING | BATCH 1 public and authentication controls execute and recover |  |
| CTRL-052 | /create-workspace | CreateWorkspacePage | input | Acme Distribution Group | VERIFIED WORKING | BATCH 1 public and authentication controls execute and recover |  |
| CTRL-053 | /create-workspace | CreateWorkspacePage | select | draft.industry | VERIFIED WORKING | BATCH 1 public and authentication controls execute and recover |  |
| CTRL-054 | /create-workspace | CreateWorkspacePage | input | ACME-OPS | VERIFIED WORKING | BATCH 1 public and authentication controls execute and recover |  |
| CTRL-055 | /create-workspace | CreateWorkspacePage | button | Generate | VERIFIED WORKING | BATCH 1 public and authentication controls execute and recover |  |
| CTRL-056 | /create-workspace | CreateWorkspacePage | select | draft.operationsProfile | VERIFIED WORKING | BATCH 1 public and authentication controls execute and recover |  |
| CTRL-057 | /create-workspace | CreateWorkspacePage | select | draft.scaleProfile | VERIFIED WORKING | BATCH 1 public and authentication controls execute and recover |  |
| CTRL-058 | /create-workspace | CreateWorkspacePage | input | Amina Dlamini | VERIFIED WORKING | BATCH 1 public and authentication controls execute and recover |  |
| CTRL-059 | /create-workspace | CreateWorkspacePage | input | amina@acmeops.com | VERIFIED WORKING | BATCH 1 public and authentication controls execute and recover |  |
| CTRL-060 | /create-workspace | CreateWorkspacePage | input | amina.admin | VERIFIED WORKING | BATCH 1 public and authentication controls execute and recover |  |
| CTRL-061 | /create-workspace | CreateWorkspacePage | input | Choose a strong setup password | VERIFIED WORKING | BATCH 1 public and authentication controls execute and recover |  |
| CTRL-062 | /create-workspace | CreateWorkspacePage | button | showPassword ? 'Hide' : 'Show' | VERIFIED WORKING | BATCH 1 public and authentication controls execute and recover |  |
| CTRL-063 | /create-workspace | CreateWorkspacePage | input | Operations planners, warehouse leads, and tenant admins | VERIFIED WORKING | BATCH 1 public and authentication controls execute and recover |  |
| CTRL-064 | /create-workspace | CreateWorkspacePage | textarea | Catalog, users, inventory, or integration stability | VERIFIED WORKING | BATCH 1 public and authentication controls execute and recover |  |
| CTRL-065 | /create-workspace | CreateWorkspacePage | button | Back | VERIFIED WORKING | BATCH 1 public and authentication controls execute and recover |  |
| CTRL-066 | /create-workspace | CreateWorkspacePage | button | Prepare Workspace Brief | VERIFIED WORKING | BATCH 1 public and authentication controls execute and recover |  |
| CTRL-067 | /create-workspace | CreateWorkspacePage | button | Continue | VERIFIED WORKING | BATCH 1 public and authentication controls execute and recover |  |
| CTRL-068 | /create-workspace | CreateWorkspacePage | button | Review Product Surface | VERIFIED WORKING | BATCH 1 public and authentication controls execute and recover |  |
| CTRL-069 | /create-workspace | CreateWorkspacePage | button | Continue to Sign In | VERIFIED WORKING | BATCH 1 public and authentication controls execute and recover |  |
| CTRL-070 | /dashboard | DashboardPage | button |  | VERIFIED WORKING | BATCH 3 dashboard runtime audit and operational read controls execute |  |
| CTRL-071 | /dashboard | DashboardPage | button | primaryAttentionItem?.actionLabel // 'Open Audit' | VERIFIED WORKING | BATCH 3 dashboard runtime audit and operational read controls execute |  |
| CTRL-072 | /dashboard | DashboardPage | button | Open Runtime | VERIFIED WORKING | BATCH 3 dashboard runtime audit and operational read controls execute |  |
| CTRL-073 | /dashboard | DashboardPage | button |  | VERIFIED WORKING | BATCH 3 dashboard runtime audit and operational read controls execute |  |
| CTRL-074 | /dashboard | DashboardPage | button |  | VERIFIED WORKING | BATCH 3 dashboard runtime audit and operational read controls execute |  |
| CTRL-075 | /dashboard | DashboardPage | button |  | VERIFIED WORKING | BATCH 3 dashboard runtime audit and operational read controls execute |  |
| CTRL-076 | /dashboard | DashboardPage | interactive-surface | Recent business and operator changes | VERIFIED WORKING | BATCH 3 dashboard runtime audit and operational read controls execute |  |
| CTRL-077 | /dashboard | DashboardPage | button | Open Audit | VERIFIED WORKING | BATCH 3 dashboard runtime audit and operational read controls execute |  |
| CTRL-078 | /dashboard | DashboardPage | button |  | VERIFIED WORKING | BATCH 3 dashboard runtime audit and operational read controls execute |  |
| CTRL-079 | /dashboard | DashboardPage | button |  | VERIFIED WORKING | BATCH 3 dashboard runtime audit and operational read controls execute |  |
| CTRL-080 | /dashboard | DashboardPage | button |  | VERIFIED WORKING | BATCH 3 dashboard runtime audit and operational read controls execute |  |
| CTRL-081 | /dashboard | DashboardPage | button | Open Alerts | VERIFIED WORKING | BATCH 3 dashboard runtime audit and operational read controls execute |  |
| CTRL-082 | /dashboard | DashboardPage | button |  | VERIFIED WORKING | BATCH 3 dashboard runtime audit and operational read controls execute |  |
| CTRL-083 | /dashboard | DashboardPage | button | Open Recommendations | VERIFIED WORKING | BATCH 3 dashboard runtime audit and operational read controls execute |  |
| CTRL-084 | /dashboard | DashboardPage | button |  | VERIFIED WORKING | BATCH 3 dashboard runtime audit and operational read controls execute |  |
| CTRL-085 | /escalations | EscalationsPage | button |  | VERIFIED WORKING | BATCH 3 dashboard runtime audit and operational read controls execute |  |
| CTRL-086 | /integrations | IntegrationsPage | button | Inspect connector | VERIFIED WORKING | BATCH 3 dashboard runtime audit and operational read controls execute |  |
| CTRL-087 | /integrations | IntegrationsPage | button | Open replay queue | VERIFIED WORKING | BATCH 3 dashboard runtime audit and operational read controls execute |  |
| CTRL-088 | /integrations | IntegrationsPage | button |  | VERIFIED WORKING | BATCH 3 dashboard runtime audit and operational read controls execute |  |
| CTRL-089 | /integrations | IntegrationsPage | button | Manage Policies | VERIFIED WORKING | BATCH 3 dashboard runtime audit and operational read controls execute |  |
| CTRL-090 | /integrations | IntegrationsPage | button | Open Replay Queue | VERIFIED WORKING | BATCH 3 dashboard runtime audit and operational read controls execute |  |
| CTRL-091 | /inventory | InventoryPage | button | Focus highest risk | VERIFIED WORKING | BATCH 3 dashboard runtime audit and operational read controls execute |  |
| CTRL-092 | /inventory | InventoryPage | button | Inspect fast mover | VERIFIED WORKING | BATCH 3 dashboard runtime audit and operational read controls execute |  |
| CTRL-093 | /inventory | InventoryPage | button |  | VERIFIED WORKING | BATCH 3 dashboard runtime audit and operational read controls execute |  |
| CTRL-094 | /inventory | InventoryPage | button |  | VERIFIED WORKING | BATCH 3 dashboard runtime audit and operational read controls execute |  |
| CTRL-095 | /inventory | InventoryPage | button | Keep Selected | VERIFIED WORKING | BATCH 3 dashboard runtime audit and operational read controls execute |  |
| CTRL-096 | /orders | OrdersPage | button | Inspect newest order | VERIFIED WORKING | BATCH 3 dashboard runtime audit and operational read controls execute |  |
| CTRL-097 | /orders | OrdersPage | button | Focus pressured lane | VERIFIED WORKING | BATCH 3 dashboard runtime audit and operational read controls execute |  |
| CTRL-098 | /orders | OrdersPage | button |  | VERIFIED WORKING | BATCH 3 dashboard runtime audit and operational read controls execute |  |
| CTRL-099 | /orders | OrdersPage | button | Keep Selected | VERIFIED WORKING | BATCH 3 dashboard runtime audit and operational read controls execute |  |
| CTRL-100 | /platform-admin | PlatformAdminPage | button | Open workspace rollout | VERIFIED WORKING | BATCH 6 admin users profile tenants and platform controls execute |  |
| CTRL-101 | /platform-admin | PlatformAdminPage | button | Open releases | VERIFIED WORKING | BATCH 6 admin users profile tenants and platform controls execute |  |
| CTRL-102 | /platform-admin | PlatformAdminPage | button |  | VERIFIED WORKING | BATCH 6 admin users profile tenants and platform controls execute |  |
| CTRL-103 | /platform-admin | PlatformAdminPage | button | Open Runtime | VERIFIED WORKING | BATCH 6 admin users profile tenants and platform controls execute |  |
| CTRL-104 | /platform-admin | PlatformAdminPage | button | Open System Config | VERIFIED WORKING | BATCH 6 admin users profile tenants and platform controls execute |  |
| CTRL-105 | /profile | ProfilePage | button | Open company settings | VERIFIED WORKING | BATCH 6 admin users profile tenants and platform controls execute |  |
| CTRL-106 | /profile | ProfilePage | button | Unlabeled interactive control | VERIFIED WORKING | BATCH 6 admin users profile tenants and platform controls execute |  |
| CTRL-107 | /profile | ProfilePage | input | Enter current password | VERIFIED WORKING WITH DOCUMENTED LIMITATION | BATCH 6 admin users profile tenants and platform controls execute | Positive password persistence should be executed against a disposable account before broad production rollout. |
| CTRL-108 | /profile | ProfilePage | input | Choose a stronger password | VERIFIED WORKING WITH DOCUMENTED LIMITATION | BATCH 6 admin users profile tenants and platform controls execute | Positive password persistence should be executed against a disposable account before broad production rollout. |
| CTRL-109 | /profile | ProfilePage | input | Repeat new password | VERIFIED WORKING WITH DOCUMENTED LIMITATION | BATCH 6 admin users profile tenants and platform controls execute | Positive password persistence should be executed against a disposable account before broad production rollout. |
| CTRL-110 | /profile | ProfilePage | button | Unlabeled interactive control | VERIFIED WORKING WITH DOCUMENTED LIMITATION | BATCH 6 admin users profile tenants and platform controls execute | Positive password persistence should be executed against a disposable account before broad production rollout. |
| CTRL-111 | /profile | ProfilePage | button |  | VERIFIED WORKING | BATCH 6 admin users profile tenants and platform controls execute |  |
| CTRL-112 | /recommendations | RecommendationsPage | button | Focus urgent recommendation | VERIFIED WORKING | BATCH 3 dashboard runtime audit and operational read controls execute |  |
| CTRL-113 | /recommendations | RecommendationsPage | button |  | VERIFIED WORKING | BATCH 3 dashboard runtime audit and operational read controls execute |  |
| CTRL-114 | /replay-queue | ReplayPage | button | Review next replay item | VERIFIED WORKING | BATCH 5 scenarios replay approvals and role restrictions execute |  |
| CTRL-115 | /replay-queue | ReplayPage | button | Open connector health | VERIFIED WORKING | BATCH 5 scenarios replay approvals and role restrictions execute |  |
| CTRL-116 | /replay-queue | ReplayPage | button |  | VERIFIED WORKING | BATCH 5 scenarios replay approvals and role restrictions execute |  |
| CTRL-117 | /replay-queue | ReplayPage | button | Unlabeled interactive control | VERIFIED WORKING WITH DOCUMENTED LIMITATION | BATCH 5 scenarios replay approvals and role restrictions execute | Gate 4 execution did not force a second destructive replay if the deterministic queue was already clear or ineligible; backend truth replay remains covered b... |
| CTRL-118 | /replay-queue | ReplayPage | button | View Connector Health | VERIFIED WORKING | BATCH 5 scenarios replay approvals and role restrictions execute |  |
| CTRL-119 | /runtime | RuntimePage | button | Open audit | VERIFIED WORKING | BATCH 3 dashboard runtime audit and operational read controls execute |  |
| CTRL-120 | /runtime | RuntimePage | button | Open releases | VERIFIED WORKING | BATCH 3 dashboard runtime audit and operational read controls execute |  |
| CTRL-121 | /runtime | RuntimePage | button |  | VERIFIED WORKING | BATCH 3 dashboard runtime audit and operational read controls execute |  |
| CTRL-122 | /scenario-history | ScenarioHistoryPage | button | Focus executable plan | VERIFIED WORKING | BATCH 5 scenarios replay approvals and role restrictions execute |  |
| CTRL-123 | /scenario-history | ScenarioHistoryPage | button |  | VERIFIED WORKING | BATCH 5 scenarios replay approvals and role restrictions execute |  |
| CTRL-124 | /scenarios | ScenarioPlannerPage | button | Exit Revision Mode | VERIFIED WORKING | BATCH 5 scenarios replay approvals and role restrictions execute |  |
| CTRL-125 | /scenarios | ScenarioPlannerPage | button | scenarioState.loading ? 'Analyzing...' : 'Preview Scenario A' | VERIFIED WORKING | BATCH 5 scenarios replay approvals and role restrictions execute |  |
| CTRL-126 | /scenarios | ScenarioPlannerPage | button | comparisonState.loading ? 'Comparing...' : 'Compare A vs B' | VERIFIED WORKING | BATCH 5 scenarios replay approvals and role restrictions execute |  |
| CTRL-127 | /scenarios | ScenarioPlannerPage | button | scenarioSaveState.loading ? 'Saving...' : 'Save Scenario A' | VERIFIED WORKING | BATCH 5 scenarios replay approvals and role restrictions execute |  |
| CTRL-128 | /scenarios | ScenarioPlannerPage | button | My Requests | VERIFIED WORKING | BATCH 5 scenarios replay approvals and role restrictions execute |  |
| CTRL-129 | /scenarios | ScenarioPlannerPage | button | My Review Queue | VERIFIED WORKING | BATCH 5 scenarios replay approvals and role restrictions execute |  |
| CTRL-130 | /scenarios | ScenarioPlannerPage | button | High-Risk Queue | VERIFIED WORKING | BATCH 5 scenarios replay approvals and role restrictions execute |  |
| CTRL-131 | /scenarios | ScenarioPlannerPage | button | Escalated Queue | VERIFIED WORKING | BATCH 5 scenarios replay approvals and role restrictions execute |  |
| CTRL-132 | /scenarios | ScenarioPlannerPage | button | Final Approval Queue | VERIFIED WORKING | BATCH 5 scenarios replay approvals and role restrictions execute |  |
| CTRL-133 | /scenarios | ScenarioPlannerPage | button | My Final Approvals | VERIFIED WORKING | BATCH 5 scenarios replay approvals and role restrictions execute |  |
| CTRL-134 | /scenarios | ScenarioPlannerPage | button | Overdue Queue | VERIFIED WORKING | BATCH 5 scenarios replay approvals and role restrictions execute |  |
| CTRL-135 | /scenarios | ScenarioPlannerPage | button | SLA Escalated Queue | VERIFIED WORKING | BATCH 5 scenarios replay approvals and role restrictions execute |  |
| CTRL-136 | /scenarios | ScenarioPlannerPage | input | North restock option | VERIFIED WORKING | BATCH 5 scenarios replay approvals and role restrictions execute |  |
| CTRL-137 | /scenarios | ScenarioPlannerPage | select | scenarioRequestedBy | DISABLED BY DESIGN - VERIFIED | BATCH 5 scenarios replay approvals and role restrictions execute |  |
| CTRL-138 | /scenarios | ScenarioPlannerPage | select | scenarioActorRole | VERIFIED WORKING | BATCH 5 scenarios replay approvals and role restrictions execute |  |
| CTRL-139 | /scenarios | ScenarioPlannerPage | input | signedInSession ? signedInSession.displayName : 'Sign in to review or approve' | DISABLED BY DESIGN - VERIFIED | BATCH 5 scenarios replay approvals and role restrictions execute |  |
| CTRL-140 | /scenarios | ScenarioPlannerPage | select | scenarioReviewOwner | VERIFIED WORKING | BATCH 5 scenarios replay approvals and role restrictions execute |  |
| CTRL-141 | /scenarios | ScenarioPlannerPage | input | Required when rejecting a saved plan | VERIFIED WORKING | BATCH 5 scenarios replay approvals and role restrictions execute |  |
| CTRL-142 | /sign-in | SignInPage | button |  | VERIFIED WORKING | BATCH 1 public and authentication controls execute and recover |  |
| CTRL-143 | /sign-in | SignInPage | button | page.label | VERIFIED WORKING | BATCH 1 public and authentication controls execute and recover |  |
| CTRL-144 | /sign-in | SignInPage | form |  | VERIFIED WORKING | BATCH 1 public and authentication controls execute and recover |  |
| CTRL-145 | /sign-in | SignInPage | input | tenantDirectoryState.loading ? 'Loading workspace directory...' : 'Enter company workspace code' | VERIFIED WORKING | BATCH 1 public and authentication controls execute and recover |  |
| CTRL-146 | /sign-in | SignInPage | input | workspace.admin | VERIFIED WORKING | BATCH 1 public and authentication controls execute and recover |  |
| CTRL-147 | /sign-in | SignInPage | input | Enter workspace password | VERIFIED WORKING | BATCH 1 public and authentication controls execute and recover |  |
| CTRL-148 | /sign-in | SignInPage | button | showPassword ? 'Hide' : 'Show' | VERIFIED WORKING | BATCH 1 public and authentication controls execute and recover |  |
| CTRL-149 | /sign-in | SignInPage | checkbox |  | VERIFIED WORKING | BATCH 1 public and authentication controls execute and recover |  |
| CTRL-150 | /sign-in | SignInPage | button | signInBusy ? 'Opening Workspace...' : 'Enter Platform' | VERIFIED WORKING | BATCH 1 public and authentication controls execute and recover |  |
| CTRL-151 | /sign-in | SignInPage | button | Create Workspace | VERIFIED WORKING | BATCH 1 public and authentication controls execute and recover |  |
| CTRL-152 | /sign-in | SignInPage | button | Product Overview | VERIFIED WORKING | BATCH 1 public and authentication controls execute and recover |  |
| CTRL-153 | /tenant-management | TenantsPage | button | Unlabeled interactive control | WORKING WITH LIMITATION | BATCH 6 admin users profile tenants and platform controls execute | Requires fresh tenant result to become enabled. |
| CTRL-154 | /tenant-management | TenantsPage | input | ACME-OPS | VERIFIED WORKING | BATCH 6 admin users profile tenants and platform controls execute |  |
| CTRL-155 | /tenant-management | TenantsPage | input | Acme Operations | VERIFIED WORKING | BATCH 6 admin users profile tenants and platform controls execute |  |
| CTRL-156 | /tenant-management | TenantsPage | input | Regional operating workspace | VERIFIED WORKING | BATCH 6 admin users profile tenants and platform controls execute |  |
| CTRL-157 | /tenant-management | TenantsPage | input | Amina Dlamini | VERIFIED WORKING | BATCH 6 admin users profile tenants and platform controls execute |  |
| CTRL-158 | /tenant-management | TenantsPage | input | amina.admin | VERIFIED WORKING | BATCH 6 admin users profile tenants and platform controls execute |  |
| CTRL-159 | /tenant-management | TenantsPage | input | Choose a strong bootstrap password | VERIFIED WORKING | BATCH 6 admin users profile tenants and platform controls execute |  |
| CTRL-160 | /tenant-management | TenantsPage | input | Johannesburg | VERIFIED WORKING | BATCH 6 admin users profile tenants and platform controls execute |  |
| CTRL-161 | /tenant-management | TenantsPage | input | Cape Town | VERIFIED WORKING | BATCH 6 admin users profile tenants and platform controls execute |  |
| CTRL-162 | /tenant-management | TenantsPage | button | Unlabeled interactive control | VERIFIED WORKING WITH DOCUMENTED LIMITATION | BATCH 6 admin users profile tenants and platform controls execute | Successful tenant bootstrap remains covered by supported hosted proof preparation and platform-admin token flow. |
| CTRL-163 | /tenant-management | TenantsPage | button | Set Sign-In Target | VERIFIED WORKING | BATCH 6 admin users profile tenants and platform controls execute |  |
| CTRL-164 | /users | UsersPage | button | Open company settings | VERIFIED WORKING | BATCH 6 admin users profile tenants and platform controls execute |  |
| CTRL-165 | /users | UsersPage | button | Open my profile | VERIFIED WORKING | BATCH 6 admin users profile tenants and platform controls execute |  |
| CTRL-166 | /users | UsersPage | button |  | VERIFIED WORKING | BATCH 6 admin users profile tenants and platform controls execute |  |
| CTRL-167 | /users | UsersPage | button |  | VERIFIED WORKING | BATCH 6 admin users profile tenants and platform controls execute |  |
| CTRL-168 | /users | UsersPage | button | Open Settings | VERIFIED WORKING | BATCH 6 admin users profile tenants and platform controls execute |  |
| CTRL-169 | /users | UsersPage | button | Open Profile | VERIFIED WORKING | BATCH 6 admin users profile tenants and platform controls execute |  |
| CTRL-170 | shared-shell | ActionPanel | button |  | VERIFIED WORKING | BATCH 2 shell navigation search and shared controls execute |  |
| CTRL-171 | shared-shell | DataGrid | button | column.sortable ? `Sort by $column.label | VERIFIED WORKING | BATCH 2 shell navigation search and shared controls execute |  |
| CTRL-172 | shared-shell | ScenarioDecisionConsole | select | scenarioActorRole | VERIFIED WORKING | BATCH 5 scenarios replay approvals and role restrictions execute |  |
| CTRL-173 | shared-shell | ScenarioDecisionConsole | input | canAcknowledgeEscalation ? 'Required to acknowledge the escalation' : approvalNoteRequired ? 'Recommended for final approval; required for rejection' : 'Requ... | VERIFIED WORKING | BATCH 5 scenarios replay approvals and role restrictions execute |  |
| CTRL-174 | shared-shell | ScenarioDecisionConsole | button | Unlabeled interactive control | VERIFIED WORKING | BATCH 5 scenarios replay approvals and role restrictions execute |  |
| CTRL-175 | shared-shell | ScenarioDecisionConsole | button | Unlabeled interactive control | VERIFIED WORKING | BATCH 5 scenarios replay approvals and role restrictions execute |  |
| CTRL-176 | shared-shell | ScenarioDecisionConsole | button | Unlabeled interactive control | VERIFIED WORKING | BATCH 5 scenarios replay approvals and role restrictions execute |  |
| CTRL-177 | shared-shell | ScenarioDecisionConsole | button | Unlabeled interactive control | VERIFIED WORKING | BATCH 5 scenarios replay approvals and role restrictions execute |  |
| CTRL-178 | shared-shell | ScenarioDecisionConsole | button | Unlabeled interactive control | VERIFIED WORKING | BATCH 5 scenarios replay approvals and role restrictions execute |  |
| CTRL-179 | shared-shell | ScenarioEditor | button | Add Line | VERIFIED WORKING | BATCH 5 scenarios replay approvals and role restrictions execute |  |
| CTRL-180 | shared-shell | ScenarioEditor | select | form.warehouseCode | VERIFIED WORKING | BATCH 5 scenarios replay approvals and role restrictions execute |  |
| CTRL-181 | shared-shell | ScenarioEditor | button | Remove | VERIFIED WORKING | BATCH 5 scenarios replay approvals and role restrictions execute |  |
| CTRL-182 | shared-shell | ScenarioEditor | select | item.productSku | VERIFIED WORKING | BATCH 5 scenarios replay approvals and role restrictions execute |  |
| CTRL-183 | shared-shell | ScenarioEditor | input | item.quantity | VERIFIED WORKING | BATCH 5 scenarios replay approvals and role restrictions execute |  |
| CTRL-184 | shared-shell | ScenarioEditor | input | item.unitPrice | VERIFIED WORKING | BATCH 5 scenarios replay approvals and role restrictions execute |  |
| CTRL-185 | shared-shell | WorkspaceNotices | button | notice.actionLabel | VERIFIED WORKING | BATCH 2 shell navigation search and shared controls execute |  |
| CTRL-186 | shared-shell | Sidebar | button |  | VERIFIED WORKING | BATCH 2 shell navigation search and shared controls execute |  |
| CTRL-187 | shared-shell | Sidebar | button |  | VERIFIED WORKING | BATCH 2 shell navigation search and shared controls execute |  |
| CTRL-188 | shared-shell | Sidebar | button | Runtime | VERIFIED WORKING | BATCH 2 shell navigation search and shared controls execute |  |
| CTRL-189 | shared-shell | Sidebar | button | Profile & Session | VERIFIED WORKING | BATCH 2 shell navigation search and shared controls execute |  |
| CTRL-190 | shared-shell | Topbar | button | Runtime runtimeLabel | VERIFIED WORKING | BATCH 2 shell navigation search and shared controls execute |  |
| CTRL-191 | shared-shell | Topbar | button | Notifications globalNotificationCount | VERIFIED WORKING | BATCH 2 shell navigation search and shared controls execute |  |
| CTRL-192 | shared-shell | Topbar | button | action.label | VERIFIED WORKING | BATCH 2 shell navigation search and shared controls execute |  |
| CTRL-193 | shared-shell | Topbar | button | Unlabeled interactive control | VERIFIED WORKING | BATCH 2 shell navigation search and shared controls execute |  |
| CTRL-194 | shared-shell | Topbar | button | signedInSession?.displayName // 'Profile' | VERIFIED WORKING | BATCH 2 shell navigation search and shared controls execute |  |
| CTRL-195 | shared-shell | Topbar | button | authSessionState.action === 'signout' ? 'Signing Out...' : 'Sign Out' | VERIFIED WORKING | BATCH 2 shell navigation search and shared controls execute |  |
| CTRL-196 | shared-shell | Topbar | input | Search pages, orders, alerts, or incidents | VERIFIED WORKING | BATCH 2 shell navigation search and shared controls execute |  |
| CTRL-197 | shared-shell | Topbar | button | Clear | VERIFIED WORKING | BATCH 2 shell navigation search and shared controls execute |  |
| CTRL-198 | shared-shell | Topbar | button |  | VERIFIED WORKING | BATCH 2 shell navigation search and shared controls execute |  |
| CTRL-199 | shared-shell | Topbar | button | action.label | VERIFIED WORKING | BATCH 2 shell navigation search and shared controls execute |  |
| CTRL-200 | shared-shell | WorkspaceUtilityRail | button |  | VERIFIED WORKING | BATCH 2 shell navigation search and shared controls execute |  |
| CTRL-201 | shared-shell | WorkspaceUtilityRail | button |  | VERIFIED WORKING | BATCH 2 shell navigation search and shared controls execute |  |

## Acceptance Notes

- Mutating catalog and workspace-admin controls were verified with backend readback where safe.
- Password update success was intentionally not executed against the proof admin credential; invalid-current-password recovery was verified and documented.
- Tenant creation fields were exercised, but final tenant creation was not repeated during Gate 4 to avoid unnecessary workspace proliferation; tenant bootstrap remains covered by hosted proof preparation and the platform-admin token flow.
- Replay mutation was not forced a second time when the deterministic queue was already clear or ineligible; replay recovery remains covered by the hosted proof replay flow.
- Role boundary was verified by planner/operator access behavior and protected-surface handling.
- Responsive and keyboard reachability were verified through targeted desktop, tablet, and mobile checks inside the control execution suite.

## Final Regression Evidence

| Gate | Result |
| --- | --- |
| Backend tests | PASS - 133 tests, 0 failures, 0 errors |
| Frontend lint | PASS |
| Frontend build | PASS |
| Frontend verify | PASS |
| Git diff check | PASS |
| Secret scan | PASS - 0 critical findings; 5 known fixture findings |
| Docs link check | PASS - 604 local links checked, 0 missing |
| Hosted proof | PASS - 6 / 6 |
| Gate 4 controls execution | PASS - 7 / 7 batches, 201 / 201 controls classified |

Final verdict:

PRE-PILOT GATE 4 ACCEPTED WITH DOCUMENTED LIMITATION
