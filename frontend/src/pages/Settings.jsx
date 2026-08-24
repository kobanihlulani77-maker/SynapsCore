import { MetricCard } from '../components/Card'
import EmptyState from '../components/EmptyState'
import Panel from '../components/Panel'
import LoadingState from '../components/LoadingState'
import OperationalGuidance from '../components/OperationalGuidance'

export default function SettingsPage({ context }) {
  const {
    isAuthenticated,
    isSettingsPage,
    workspaceAdmin,
    accessAdminState,
    canManageTenantAccess,
    workspaceSettingsForm,
    setWorkspaceSettingsForm,
    workspaceSecurityForm,
    setWorkspaceSecurityForm,
    saveWorkspaceSettings,
    saveWorkspaceSecuritySettings,
    selectedWorkspaceWarehouse,
    selectedWorkspaceWarehouseDraft,
    selectedWorkspaceConnector,
    selectedWorkspaceConnectorDraft,
    selectedWorkspaceConnectorOwnerOptions,
    setSelectedWorkspaceWarehouseId,
    setSelectedWorkspaceConnectorId,
    setWorkspaceWarehouseDrafts,
    setWorkspaceConnectorDrafts,
    saveWorkspaceWarehouse,
    saveWorkspaceConnectorSupport,
    formatCodeLabel,
    integrationValidationPolicies,
    integrationTransformationPolicies,
    signedInSession,
    pageError,
  } = context

  if (!isAuthenticated || !isSettingsPage) return null

  const supportedWorkspaceConnectorModes = selectedWorkspaceConnector?.supportedSyncModes?.length
    ? selectedWorkspaceConnector.supportedSyncModes
    : ['REALTIME_PUSH']
  const settingsReviewCount = (workspaceAdmin?.supportDiagnostics?.connectorsWithoutSupportOwner || 0)
    + (workspaceAdmin?.supportDiagnostics?.activeUsersRequiringPasswordChange || 0)
  const workspaceScopeLabel = signedInSession?.tenantName || signedInSession?.tenantCode || 'Workspace unavailable'
  const settingsDataError = accessAdminState.error || pageError
  const settingsDataLoading = accessAdminState.loading && !workspaceAdmin

  return (
    <section className="content-grid">
      <Panel wide>
        <div className="panel-header">
          <div>
            <p className="panel-kicker">Company settings</p>
            <h2>Workspace profile, security, and connector policy</h2>
          </div>
          <span className="panel-badge scenario-badge">{workspaceAdmin?.warehouses?.length || 0}</span>
        </div>

        <OperationalGuidance
          stateLabel={settingsDataLoading ? 'Loading' : settingsDataError ? 'Unavailable' : 'Configured'}
          stateTone={settingsDataLoading ? 'status-partial' : settingsDataError ? 'status-failure' : 'status-success'}
          stateDetail={settingsDataLoading ? 'Tenant workspace settings, warehouse lanes, and connector policy are still being loaded.' : settingsDataError ? 'The tenant configuration read failed; visible values must not be treated as current persisted settings.' : 'This page exposes supported tenant configuration for workspace identity, security policy, warehouses, and connector support.'}
          attention={settingsDataError ? settingsDataError : settingsReviewCount ? `${settingsReviewCount} configuration or access condition${settingsReviewCount === 1 ? '' : 's'} need review.` : 'No password-reset or connector-ownership review signal is currently reported.'}
          nextAction={canManageTenantAccess ? 'For a material change, capture the before state, save one control, confirm the persisted readback, then verify the affected sign-in, warehouse, or connector behavior.' : 'This page is Tenant Admin controlled. Use Profile for personal actions and ask a Tenant Admin to change tenant configuration.'}
          evidence="Successful save actions reload the supported workspace administration payload. This page does not prove external source-system reconciliation or deployment health."
          role="Tenant Admin controls these changes; settings do not grant Platform Owner authority or change another tenant."
          limitation="Platform infrastructure, PostgreSQL, Redis, MFA/SSO, tenant creation, and connector secrets are outside this tenant settings surface unless a supported control is shown here."
        />

        <div className="workflow-decision-hero admin-trust-hero">
          <div className="workflow-decision-copy">
            <strong>Company workspace configuration</strong>
            <p>
              Every control on this page affects the signed-in company workspace. Keep the affected scope, consequence,
              and save path visible before changing security, warehouse, or connector policy.
            </p>
            <div className="ops-pill-row">
              <span className="workspace-meta-pill">{workspaceScopeLabel}</span>
              <span className="workspace-meta-pill">{canManageTenantAccess ? 'Admin editing enabled' : 'Read-only access'}</span>
              <span className="workspace-meta-pill">{settingsReviewCount} review signals</span>
            </div>
          </div>
          <div className="workflow-action-console">
            <div className="workflow-action-card">
              <span>Workspace scope</span>
              <strong>{signedInSession?.tenantCode || 'Not reported'}</strong>
              <p>Changes are tenant-scoped and apply inside this company workspace boundary.</p>
            </div>
            <div className="workflow-action-card">
              <span>High-impact controls</span>
              <strong>Security and connectors</strong>
              <p>Session policy, connector mode, and support ownership can affect operational access or data flow.</p>
            </div>
          </div>
        </div>

        <div className="summary-grid compact-summary-grid">
          <MetricCard label="Rotation days" value={workspaceAdmin?.securitySettings?.passwordRotationDays || 0} accent="blue" note="How often workspace passwords are expected to rotate." />
          <MetricCard label="Session timeout" value={workspaceAdmin?.securitySettings?.sessionTimeoutMinutes || 0} accent="teal" note="Minutes before inactive sessions age out of the workspace." />
          <MetricCard label="Warehouse lanes" value={workspaceAdmin?.warehouses?.length || 0} accent="amber" note="Operational locations currently configured for the company workspace." />
          <MetricCard label="Connectors" value={workspaceAdmin?.connectors?.length || 0} accent="rose" note="Integration lanes currently configured inside this workspace boundary." />
        </div>

        {accessAdminState.error ? <p className="error-text">{accessAdminState.error}</p> : null}
        {accessAdminState.success ? <p className="success-text">{accessAdminState.success}</p> : null}

        <div className="experience-grid experience-grid-split">
          <article className="stack-card section-card admin-form-panel" id="settings-profile">
            <div className="stack-title-row"><strong>Workspace profile</strong><span className="status-tag status-success">Admin controlled</span></div>
            <p className="muted-text">Descriptive company identity. Applies to this workspace after the save request succeeds.</p>
            <div className="session-control-row">
              <label className="field planner-name-field">
                <span>Company workspace name</span>
                <input value={workspaceSettingsForm.tenantName} onChange={(event) => setWorkspaceSettingsForm((current) => ({ ...current, tenantName: event.target.value }))} placeholder="Company workspace name" disabled={accessAdminState.loading || !canManageTenantAccess} />
              </label>
              <label className="field planner-name-field">
                <span>Workspace description</span>
                <input value={workspaceSettingsForm.description} onChange={(event) => setWorkspaceSettingsForm((current) => ({ ...current, description: event.target.value }))} placeholder="Operational workspace summary" disabled={accessAdminState.loading || !canManageTenantAccess} />
              </label>
            </div>
            <div className="history-action-row">
              <button className="secondary-button" onClick={saveWorkspaceSettings} disabled={accessAdminState.loading || !canManageTenantAccess || !workspaceSettingsForm.tenantName.trim()} type="button">
                {accessAdminState.loading ? 'Working...' : 'Save Workspace'}
              </button>
            </div>
          </article>

          <article className="stack-card section-card admin-risk-panel" id="settings-security">
            <div className="stack-title-row"><strong>Security policy</strong><span className="status-tag status-partial">Workspace policy</span></div>
            <p className="muted-text">High-impact settings. These values affect workspace session safety and password hygiene after the backend confirms the update.</p>
            <div className="session-control-row">
              <label className="field planner-name-field">
                <span>Password rotation days</span>
                <input value={workspaceSecurityForm.passwordRotationDays} onChange={(event) => setWorkspaceSecurityForm((current) => ({ ...current, passwordRotationDays: event.target.value }))} inputMode="numeric" disabled={accessAdminState.loading || !canManageTenantAccess} />
              </label>
              <label className="field planner-name-field">
                <span>Session timeout minutes</span>
                <input value={workspaceSecurityForm.sessionTimeoutMinutes} onChange={(event) => setWorkspaceSecurityForm((current) => ({ ...current, sessionTimeoutMinutes: event.target.value }))} inputMode="numeric" disabled={accessAdminState.loading || !canManageTenantAccess} />
              </label>
              <label className="field checkbox-field">
                <span>Invalidate other sessions</span>
                <input className="checkbox-input" type="checkbox" checked={workspaceSecurityForm.invalidateOtherSessions} onChange={(event) => setWorkspaceSecurityForm((current) => ({ ...current, invalidateOtherSessions: event.target.checked }))} disabled={accessAdminState.loading || !canManageTenantAccess} />
              </label>
            </div>
            <div className="history-action-row">
              <button className="secondary-button" onClick={saveWorkspaceSecuritySettings} disabled={accessAdminState.loading || !canManageTenantAccess} type="button">
                {accessAdminState.loading ? 'Working...' : 'Save Security Policy'}
              </button>
            </div>
          </article>
        </div>

        <div className="experience-grid experience-grid-split">
          <article className="stack-card section-card admin-form-panel">
            <div className="stack-title-row"><strong>Warehouse focus</strong><span className="scenario-type-tag">{selectedWorkspaceWarehouse?.code || 'Waiting'}</span></div>
            <p className="muted-text">Operational site identity. Save only updates the selected warehouse lane.</p>
            <div className="signal-list">
              {settingsDataLoading ? <LoadingState label="Loading warehouse lanes..." /> : settingsDataError ? <p className="error-text">Warehouse settings are unavailable because the tenant configuration read failed.</p> : workspaceAdmin?.warehouses?.length ? workspaceAdmin.warehouses.map((warehouse) => (
                  <button key={warehouse.id} className={`signal-list-item selectable-card system-select-card admin-subject-card ${selectedWorkspaceWarehouse?.id === warehouse.id ? 'is-selected' : ''}`} onClick={() => setSelectedWorkspaceWarehouseId(warehouse.id)} type="button">
                  <strong>{warehouse.name}</strong>
                  <p>{warehouse.code}</p>
                  <p className="muted-text">{warehouse.location || 'Location not defined yet'}</p>
                </button>
              )) : <EmptyState>Warehouse and site defaults will appear here when the workspace is configured.</EmptyState>}
            </div>
            {selectedWorkspaceWarehouse && selectedWorkspaceWarehouseDraft ? (
              <>
                <div className="session-control-row">
                  <label className="field planner-name-field">
                    <span>Name</span>
                    <input value={selectedWorkspaceWarehouseDraft.name} onChange={(event) => setWorkspaceWarehouseDrafts((current) => ({ ...current, [selectedWorkspaceWarehouse.id]: { ...selectedWorkspaceWarehouseDraft, name: event.target.value } }))} disabled={accessAdminState.loading || !canManageTenantAccess} />
                  </label>
                  <label className="field planner-name-field">
                    <span>Location</span>
                    <input value={selectedWorkspaceWarehouseDraft.location} onChange={(event) => setWorkspaceWarehouseDrafts((current) => ({ ...current, [selectedWorkspaceWarehouse.id]: { ...selectedWorkspaceWarehouseDraft, location: event.target.value } }))} disabled={accessAdminState.loading || !canManageTenantAccess} />
                  </label>
                </div>
                <div className="history-action-row">
                  <button className="ghost-button" onClick={() => saveWorkspaceWarehouse(selectedWorkspaceWarehouse.id)} disabled={accessAdminState.loading || !canManageTenantAccess} type="button">Save Warehouse</button>
                </div>
              </>
            ) : null}
          </article>

          <article className="stack-card section-card admin-risk-panel" id="settings-connectors">
            <div className="stack-title-row"><strong>Connector focus</strong><span className="scenario-type-tag">{selectedWorkspaceConnector ? formatCodeLabel(selectedWorkspaceConnector.syncMode) : 'Waiting'}</span></div>
            <p className="muted-text">High-impact integration settings. Changes can affect inbound data handling, validation, cadence, and recovery ownership.</p>
            <div className="signal-list">
              {settingsDataLoading ? <LoadingState label="Loading connector policy..." /> : settingsDataError ? <p className="error-text">Connector policy is unavailable because the tenant configuration read failed.</p> : workspaceAdmin?.connectors?.length ? workspaceAdmin.connectors.map((connector) => (
                <button key={connector.id} className={`signal-list-item selectable-card system-select-card admin-subject-card ${selectedWorkspaceConnector?.id === connector.id ? 'is-selected' : ''}`} onClick={() => setSelectedWorkspaceConnectorId(connector.id)} type="button">
                  <strong>{connector.displayName}</strong>
                  <p>{connector.sourceSystem} | {formatCodeLabel(connector.syncMode)}</p>
                  <p className="muted-text">{connector.supportOwnerDisplayName || 'No support owner assigned yet'}</p>
                  <div className="attention-card-meta">
                    <span>{connector.supportOwnerDisplayName ? 'Owner assigned' : 'Ownership needed'}</span>
                    <span>{formatCodeLabel(connector.validationPolicy)}</span>
                  </div>
                </button>
              )) : <EmptyState>Connector policy cards will appear here once integration lanes are created for the workspace.</EmptyState>}
            </div>
            {selectedWorkspaceConnector && selectedWorkspaceConnectorDraft ? (
              <>
                <div className="session-control-row">
                  <label className="field planner-name-field">
                    <span>Sync mode</span>
                    <select value={selectedWorkspaceConnectorDraft.syncMode} onChange={(event) => setWorkspaceConnectorDrafts((current) => ({ ...current, [selectedWorkspaceConnector.id]: { ...selectedWorkspaceConnectorDraft, syncMode: event.target.value, syncIntervalMinutes: event.target.value === 'SCHEDULED_PULL' ? (selectedWorkspaceConnectorDraft.syncIntervalMinutes || '15') : '' } }))} disabled={accessAdminState.loading || !canManageTenantAccess}>
                      {supportedWorkspaceConnectorModes.map((mode) => <option key={mode} value={mode}>{formatCodeLabel(mode)}</option>)}
                    </select>
                  </label>
                  <label className="field planner-name-field">
                    <span>Validation</span>
                    <select value={selectedWorkspaceConnectorDraft.validationPolicy} onChange={(event) => setWorkspaceConnectorDrafts((current) => ({ ...current, [selectedWorkspaceConnector.id]: { ...selectedWorkspaceConnectorDraft, validationPolicy: event.target.value } }))} disabled={accessAdminState.loading || !canManageTenantAccess}>
                      {integrationValidationPolicies.map((policy) => <option key={policy} value={policy}>{formatCodeLabel(policy)}</option>)}
                    </select>
                  </label>
                  <label className="field planner-name-field">
                    <span>Transform</span>
                    <select value={selectedWorkspaceConnectorDraft.transformationPolicy} onChange={(event) => setWorkspaceConnectorDrafts((current) => ({ ...current, [selectedWorkspaceConnector.id]: { ...selectedWorkspaceConnectorDraft, transformationPolicy: event.target.value } }))} disabled={accessAdminState.loading || !canManageTenantAccess}>
                      {integrationTransformationPolicies.map((policy) => <option key={policy} value={policy}>{formatCodeLabel(policy)}</option>)}
                    </select>
                  </label>
                </div>
                <div className="session-control-row">
                  <label className="field planner-name-field">
                    <span>Connector cadence</span>
                    <input value={selectedWorkspaceConnectorDraft.syncMode === 'SCHEDULED_PULL' ? selectedWorkspaceConnectorDraft.syncIntervalMinutes : selectedWorkspaceConnectorDraft.syncMode === 'REALTIME_PUSH' ? 'Event-driven push' : 'File-drop batch'} onChange={(event) => setWorkspaceConnectorDrafts((current) => ({ ...current, [selectedWorkspaceConnector.id]: { ...selectedWorkspaceConnectorDraft, syncIntervalMinutes: event.target.value } }))} disabled={accessAdminState.loading || !canManageTenantAccess || selectedWorkspaceConnectorDraft.syncMode !== 'SCHEDULED_PULL'} inputMode="numeric" />
                  </label>
                  <label className="field planner-name-field">
                    <span>Support owner</span>
                    <select value={selectedWorkspaceConnectorDraft.supportOwnerActorName} onChange={(event) => setWorkspaceConnectorDrafts((current) => ({ ...current, [selectedWorkspaceConnector.id]: { ...selectedWorkspaceConnectorDraft, supportOwnerActorName: event.target.value } }))} disabled={accessAdminState.loading || !canManageTenantAccess}>
                      <option value="">Unassigned</option>
                      {selectedWorkspaceConnectorOwnerOptions.map((operator) => <option key={operator.id} value={operator.actorName}>{operator.displayName}</option>)}
                    </select>
                  </label>
                </div>
                {selectedWorkspaceConnectorDraft.syncMode === 'SCHEDULED_PULL' ? (
                  <label className="field planner-name-field">
                    <span>Pull endpoint URL</span>
                    <input value={selectedWorkspaceConnectorDraft.pullEndpointUrl} onChange={(event) => setWorkspaceConnectorDrafts((current) => ({ ...current, [selectedWorkspaceConnector.id]: { ...selectedWorkspaceConnectorDraft, pullEndpointUrl: event.target.value } }))} placeholder="https://company.example.com/orders-feed" disabled={accessAdminState.loading || !canManageTenantAccess} />
                  </label>
                ) : null}
                <p className="muted-text">{selectedWorkspaceConnector.supportBoundary || 'Connector support boundaries are enforced by the backend and mirrored here so admins only configure real operating modes.'}</p>
                <div className="history-action-row">
                  <button className="ghost-button" onClick={() => saveWorkspaceConnectorSupport(selectedWorkspaceConnector.id)} disabled={accessAdminState.loading || !canManageTenantAccess || (selectedWorkspaceConnectorDraft.syncMode === 'SCHEDULED_PULL' && !selectedWorkspaceConnectorDraft.pullEndpointUrl.trim())} type="button">Save Connector Policy</button>
                </div>
              </>
            ) : null}
          </article>
        </div>
      </Panel>
    </section>
  )
}
