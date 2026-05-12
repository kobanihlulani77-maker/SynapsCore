import { MetricCard } from '../components/Card'
import EmptyState from '../components/EmptyState'
import Panel from '../components/Panel'

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
  } = context

  if (!isAuthenticated || !isSettingsPage) return null

  const supportedWorkspaceConnectorModes = selectedWorkspaceConnector?.supportedSyncModes?.length
    ? selectedWorkspaceConnector.supportedSyncModes
    : ['REALTIME_PUSH']

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

        <div className="ops-command-hero">
          <div className="ops-command-copy">
            <strong>Company workspace configuration</strong>
            <p>
              This surface should explain what company-level settings affect operations, security, warehouse scope, and connector behavior
              without turning workspace configuration into a raw admin console.
            </p>
            <div className="ops-pill-row">
              <span className="workspace-meta-pill">{signedInSession?.tenantCode || 'Workspace code pending'}</span>
              <span className="workspace-meta-pill">{canManageTenantAccess ? 'Admin editing enabled' : 'Read-only access'}</span>
              <span className="workspace-meta-pill">Tenant boundary enforced</span>
            </div>
          </div>
          <div className="ops-command-actions">
            <button className="secondary-button" onClick={saveWorkspaceSettings} disabled={accessAdminState.loading || !canManageTenantAccess || !workspaceSettingsForm.tenantName.trim()} type="button">
              {accessAdminState.loading ? 'Working...' : 'Save Workspace'}
            </button>
            <button className="ghost-button" onClick={saveWorkspaceSecuritySettings} disabled={accessAdminState.loading || !canManageTenantAccess} type="button">
              {accessAdminState.loading ? 'Working...' : 'Save Security Policy'}
            </button>
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
          <article className="stack-card section-card" id="settings-profile">
            <div className="stack-title-row"><strong>Workspace profile</strong><span className="status-tag status-success">Admin controlled</span></div>
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
            <p className="muted-text">Workspace identity explains what company environment operators are entering and what operational boundary this configuration governs.</p>
          </article>

          <article className="stack-card section-card" id="settings-security">
            <div className="stack-title-row"><strong>Security policy</strong><span className="status-tag status-partial">Workspace policy</span></div>
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
            <p className="muted-text">These settings directly shape session safety and password hygiene for everyone inside the company workspace.</p>
          </article>
        </div>

        <div className="experience-grid experience-grid-split">
          <article className="stack-card section-card">
            <div className="stack-title-row"><strong>Warehouse focus</strong><span className="scenario-type-tag">{selectedWorkspaceWarehouse?.code || 'Waiting'}</span></div>
            <div className="signal-list">
              {workspaceAdmin?.warehouses?.length ? workspaceAdmin.warehouses.map((warehouse) => (
                <button key={warehouse.id} className={`signal-list-item selectable-card system-select-card ${selectedWorkspaceWarehouse?.id === warehouse.id ? 'is-selected' : ''}`} onClick={() => setSelectedWorkspaceWarehouseId(warehouse.id)} type="button">
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

          <article className="stack-card section-card" id="settings-connectors">
            <div className="stack-title-row"><strong>Connector focus</strong><span className="scenario-type-tag">{selectedWorkspaceConnector ? formatCodeLabel(selectedWorkspaceConnector.syncMode) : 'Waiting'}</span></div>
            <div className="signal-list">
              {workspaceAdmin?.connectors?.length ? workspaceAdmin.connectors.map((connector) => (
                <button key={connector.id} className={`signal-list-item selectable-card system-select-card ${selectedWorkspaceConnector?.id === connector.id ? 'is-selected' : ''}`} onClick={() => setSelectedWorkspaceConnectorId(connector.id)} type="button">
                  <strong>{connector.displayName}</strong>
                  <p>{connector.sourceSystem} | {formatCodeLabel(connector.syncMode)}</p>
                  <p className="muted-text">{connector.supportOwnerDisplayName || 'No support owner assigned yet'}</p>
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
