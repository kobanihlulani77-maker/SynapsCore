import { SummaryCard } from '../components/Card'
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
  } = context

  if (!isAuthenticated || !isSettingsPage) return null

  const supportedWorkspaceConnectorModes = selectedWorkspaceConnector?.supportedSyncModes?.length
    ? selectedWorkspaceConnector.supportedSyncModes
    : ['REALTIME_PUSH']

  return (
    <section className="content-grid">
      <Panel wide>
        <div className="panel-header">
          <div><p className="panel-kicker">Company settings</p><h2>Workspace profile, security, and connector policy</h2></div>
          <span className="panel-badge scenario-badge">{workspaceAdmin?.warehouses?.length || 0}</span>
        </div>
        <div className="summary-grid compact-summary-grid">
          <SummaryCard label="Rotation days" value={workspaceAdmin?.securitySettings?.passwordRotationDays || 0} accent="blue" />
          <SummaryCard label="Session timeout" value={workspaceAdmin?.securitySettings?.sessionTimeoutMinutes || 0} accent="teal" />
          <SummaryCard label="Warehouse lanes" value={workspaceAdmin?.warehouses?.length || 0} accent="amber" />
          <SummaryCard label="Connectors" value={workspaceAdmin?.connectors?.length || 0} accent="rose" />
        </div>
        {accessAdminState.error ? <p className="error-text">{accessAdminState.error}</p> : null}
        {accessAdminState.success ? <p className="success-text">{accessAdminState.success}</p> : null}
        <div className="experience-grid experience-grid-split">
          <article className="stack-card section-card">
            <div className="stack-title-row"><strong>Workspace profile</strong><span className="status-tag status-success">Editable</span></div>
            <div className="session-control-row">
              <label className="field planner-name-field">
                <span>Tenant Name</span>
                <input value={workspaceSettingsForm.tenantName} onChange={(event) => setWorkspaceSettingsForm((current) => ({ ...current, tenantName: event.target.value }))} placeholder="Tenant workspace name" disabled={accessAdminState.loading || !canManageTenantAccess} />
              </label>
              <label className="field planner-name-field">
                <span>Description</span>
                <input value={workspaceSettingsForm.description} onChange={(event) => setWorkspaceSettingsForm((current) => ({ ...current, description: event.target.value }))} placeholder="Operational workspace summary" disabled={accessAdminState.loading || !canManageTenantAccess} />
              </label>
            </div>
            <div className="history-action-row">
              <button className="secondary-button" onClick={saveWorkspaceSettings} disabled={accessAdminState.loading || !canManageTenantAccess || !workspaceSettingsForm.tenantName.trim()} type="button">
                {accessAdminState.loading ? 'Working...' : 'Save Workspace'}
              </button>
            </div>
          </article>
          <article className="stack-card section-card">
            <div className="stack-title-row"><strong>Security policy</strong><span className="status-tag status-partial">Tenant policy</span></div>
            <div className="session-control-row">
              <label className="field planner-name-field">
                <span>Password Rotation Days</span>
                <input value={workspaceSecurityForm.passwordRotationDays} onChange={(event) => setWorkspaceSecurityForm((current) => ({ ...current, passwordRotationDays: event.target.value }))} inputMode="numeric" disabled={accessAdminState.loading || !canManageTenantAccess} />
              </label>
              <label className="field planner-name-field">
                <span>Session Timeout Minutes</span>
                <input value={workspaceSecurityForm.sessionTimeoutMinutes} onChange={(event) => setWorkspaceSecurityForm((current) => ({ ...current, sessionTimeoutMinutes: event.target.value }))} inputMode="numeric" disabled={accessAdminState.loading || !canManageTenantAccess} />
              </label>
              <label className="field checkbox-field">
                <span>Invalidate Other Sessions</span>
                <input className="checkbox-input" type="checkbox" checked={workspaceSecurityForm.invalidateOtherSessions} onChange={(event) => setWorkspaceSecurityForm((current) => ({ ...current, invalidateOtherSessions: event.target.checked }))} disabled={accessAdminState.loading || !canManageTenantAccess} />
              </label>
            </div>
            <div className="history-action-row">
              <button className="ghost-button" onClick={saveWorkspaceSecuritySettings} disabled={accessAdminState.loading || !canManageTenantAccess} type="button">
                {accessAdminState.loading ? 'Working...' : 'Save Security Policy'}
              </button>
            </div>
          </article>
        </div>
        <div className="experience-grid experience-grid-split">
          <article className="stack-card section-card">
            <div className="stack-title-row"><strong>Warehouse focus</strong><span className="scenario-type-tag">{selectedWorkspaceWarehouse?.code || 'Waiting'}</span></div>
            <div className="signal-list">
              {workspaceAdmin?.warehouses?.length ? workspaceAdmin.warehouses.map((warehouse) => (
                <button key={warehouse.id} className={`signal-list-item selectable-card ${selectedWorkspaceWarehouse?.id === warehouse.id ? 'is-selected' : ''}`} onClick={() => setSelectedWorkspaceWarehouseId(warehouse.id)} type="button">
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
          <article className="stack-card section-card">
            <div className="stack-title-row"><strong>Connector focus</strong><span className="scenario-type-tag">{selectedWorkspaceConnector ? formatCodeLabel(selectedWorkspaceConnector.syncMode) : 'Waiting'}</span></div>
            <div className="signal-list">
              {workspaceAdmin?.connectors?.length ? workspaceAdmin.connectors.map((connector) => (
                <button key={connector.id} className={`signal-list-item selectable-card ${selectedWorkspaceConnector?.id === connector.id ? 'is-selected' : ''}`} onClick={() => setSelectedWorkspaceConnectorId(connector.id)} type="button">
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
                    <span>Sync Mode</span>
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
                    <span>Support Owner</span>
                    <select value={selectedWorkspaceConnectorDraft.supportOwnerActorName} onChange={(event) => setWorkspaceConnectorDrafts((current) => ({ ...current, [selectedWorkspaceConnector.id]: { ...selectedWorkspaceConnectorDraft, supportOwnerActorName: event.target.value } }))} disabled={accessAdminState.loading || !canManageTenantAccess}>
                      <option value="">Unassigned</option>
                      {selectedWorkspaceConnectorOwnerOptions.map((operator) => <option key={operator.id} value={operator.actorName}>{operator.displayName}</option>)}
                    </select>
                  </label>
                </div>
                {selectedWorkspaceConnectorDraft.syncMode === 'SCHEDULED_PULL' ? (
                  <label className="field planner-name-field">
                    <span>Pull Endpoint URL</span>
                    <input value={selectedWorkspaceConnectorDraft.pullEndpointUrl} onChange={(event) => setWorkspaceConnectorDrafts((current) => ({ ...current, [selectedWorkspaceConnector.id]: { ...selectedWorkspaceConnectorDraft, pullEndpointUrl: event.target.value } }))} placeholder="https://company.example.com/orders-feed" disabled={accessAdminState.loading || !canManageTenantAccess} />
                  </label>
                ) : null}
                <p className="muted-text">{selectedWorkspaceConnector.supportBoundary || 'Connector support boundaries are enforced by the backend and mirrored here so workspace admins only configure real modes.'}</p>
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
