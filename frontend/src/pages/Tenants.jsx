import { MetricCard } from '../components/Card'
import EmptyState from '../components/EmptyState'
import Panel from '../components/Panel'

const ROLE_OPTIONS = [
  'TENANT_ADMIN',
  'INTEGRATION_ADMIN',
  'INTEGRATION_OPERATOR',
  'REVIEW_OWNER',
  'FINAL_APPROVER',
  'ESCALATION_OWNER',
]

function roleLabel(role) {
  return role.replaceAll('_', ' ')
}

function updateListItem(setTenantOnboardingForm, collection, index, changes) {
  setTenantOnboardingForm((current) => ({
    ...current,
    [collection]: current[collection].map((item, itemIndex) => itemIndex === index ? { ...item, ...changes } : item),
  }))
}

export default function TenantsPage({ context }) {
  const {
    isAuthenticated,
    isTenantsPage,
    tenantDirectoryState,
    signedInSession,
    signedInRoles,
    tenantOnboardingState,
    tenantOnboardingForm,
    setTenantOnboardingForm,
    onboardTenant,
    signInOperator,
    authSessionState,
    setAuthSessionState,
  } = context

  if (!isAuthenticated || !isTenantsPage) return null

  const currentTenant = tenantDirectoryState.items.find((tenant) => tenant.code === signedInSession?.tenantCode)
  const canCreateWorkspace = signedInRoles.includes('PLATFORM_OWNER')
  const warehouses = tenantOnboardingForm.warehouses || []
  const users = tenantOnboardingForm.users || []
  const hasTenantAdmin = users.some((user) => user.roles.includes('TENANT_ADMIN'))
  const warehousesReady = warehouses.length > 0 && warehouses.every((warehouse) => (
    warehouse.code.trim() && warehouse.name.trim() && warehouse.location.trim()
  ))
  const usersReady = users.length > 0 && users.every((user) => (
    user.username.trim()
      && user.fullName.trim()
      && user.operatorActorName.trim()
      && user.roles.length > 0
      && (user.tenantWide || user.warehouseScopes.length > 0)
      && user.initialPassword.trim().length >= 8
  ))
  const tenantFormReady = Boolean(
    tenantOnboardingForm.tenantCode.trim()
      && tenantOnboardingForm.tenantName.trim()
      && warehousesReady
      && usersReady
      && hasTenantAdmin
      && tenantOnboardingForm.requiredRolesConfigured,
  )
  const updateWarehouse = (index, changes) => updateListItem(setTenantOnboardingForm, 'warehouses', index, changes)
  const updateUser = (index, changes) => updateListItem(setTenantOnboardingForm, 'users', index, changes)
  const toggleUserRole = (index, role) => {
    const user = users[index]
    const roles = user.roles.includes(role)
      ? user.roles.filter((currentRole) => currentRole !== role)
      : [...user.roles, role]
    updateUser(index, { roles })
  }
  const toggleUserScope = (index, warehouseCode) => {
    const user = users[index]
    const warehouseScopes = user.warehouseScopes.includes(warehouseCode)
      ? user.warehouseScopes.filter((currentCode) => currentCode !== warehouseCode)
      : [...user.warehouseScopes, warehouseCode]
    updateUser(index, { warehouseScopes, tenantWide: false })
  }

  return (
    <section className="content-grid">
      <Panel wide>
        <div className="panel-header">
          <div>
            <p className="panel-kicker">Workspace rollout</p>
            <h2>Tenant onboarding and workspace rollout</h2>
          </div>
          <span className="panel-badge scenario-badge">{tenantDirectoryState.items.length}</span>
        </div>

        <div className="workflow-decision-hero admin-trust-hero">
          <div className="workflow-decision-copy">
            <strong>Explicit company workspace provisioning</strong>
            <p>
              Describe the actual company boundary, warehouses, people, roles, and scopes. Nothing is created unless it is
              included in this request and accepted by the backend policy.
            </p>
            <div className="ops-pill-row">
              <span className="workspace-meta-pill">{canCreateWorkspace ? 'Admin provisioning enabled' : 'Provisioning restricted'}</span>
              <span className="workspace-meta-pill">{tenantFormReady ? 'Configuration ready' : 'Configuration incomplete'}</span>
              <span className="workspace-meta-pill">{tenantOnboardingState.result?.tenantCode || 'No new workspace yet'}</span>
            </div>
          </div>
          <div className="workflow-action-console">
            <div className="workflow-action-card">
              <span>Tenant isolation</span>
              <strong>{tenantOnboardingForm.tenantCode.trim() || 'Workspace code required'}</strong>
              <p>Provisioning creates one isolated company boundary and does not create operational data.</p>
            </div>
            <div className="workflow-action-card">
              <span>Configuration supplied</span>
              <strong>{warehouses.length} warehouses / {users.length} users</strong>
              <p>Role and warehouse scope assignments are submitted exactly as entered.</p>
            </div>
          </div>
        </div>

        <div className="summary-grid compact-summary-grid">
          <MetricCard label="Visible workspaces" value={tenantDirectoryState.items.length} accent="blue" note="Company environments currently visible inside the platform portfolio." />
          <MetricCard label="Provisioning access" value={canCreateWorkspace ? 'Granted' : 'Restricted'} accent="teal" note="Whether the current operator can create a new workspace rollout." />
          <MetricCard label="Current workspace" value={currentTenant?.code || 'None'} accent="amber" note="The company environment currently associated with the signed-in operator." />
          <MetricCard label="Last rollout" value={tenantOnboardingState.result?.tenantCode || 'Waiting'} accent="rose" note="The most recent workspace created through this rollout surface." />
        </div>

        <div className="experience-grid experience-grid-split">
          <article className="stack-card section-card admin-form-panel">
            <div className="stack-title-row">
              <strong>Create company workspace</strong>
              <span className={`status-tag ${canCreateWorkspace ? 'status-success' : 'status-failure'}`}>{canCreateWorkspace ? 'Ready' : 'Restricted'}</span>
            </div>
            <p className="muted-text integration-note">
              This is an explicit production configuration surface. It does not add default warehouses, synthetic personas,
              starter inventory, connectors, orders, or other operational records.
            </p>

            <div className="admin-form-section">
              <div>
                <strong>Company boundary</strong>
                <p className="muted-text">Defines the isolated workspace operators will select at sign-in.</p>
              </div>
              <div className="session-control-row">
                <label className="field session-field"><span>Workspace code</span><input value={tenantOnboardingForm.tenantCode} onChange={(event) => setTenantOnboardingForm((current) => ({ ...current, tenantCode: event.target.value }))} placeholder="ACME-OPS" disabled={tenantOnboardingState.loading || !canCreateWorkspace} /></label>
                <label className="field session-field"><span>Company workspace name</span><input value={tenantOnboardingForm.tenantName} onChange={(event) => setTenantOnboardingForm((current) => ({ ...current, tenantName: event.target.value }))} placeholder="Acme Operations" disabled={tenantOnboardingState.loading || !canCreateWorkspace} /></label>
                <label className="field session-field"><span>Description (optional)</span><input value={tenantOnboardingForm.description} onChange={(event) => setTenantOnboardingForm((current) => ({ ...current, description: event.target.value }))} placeholder="Regional operating workspace" disabled={tenantOnboardingState.loading || !canCreateWorkspace} /></label>
              </div>
            </div>

            <div className="admin-form-section">
              <div>
                <strong>Warehouses</strong>
                <p className="muted-text">Add the actual sites. Warehouse codes are also used when assigning scoped operators.</p>
              </div>
              {warehouses.map((warehouse, index) => (
                <div className="session-control-row" key={`warehouse-${index}`}>
                  <label className="field session-field"><span>Warehouse code</span><input value={warehouse.code} onChange={(event) => updateWarehouse(index, { code: event.target.value })} placeholder="WH-JHB" disabled={tenantOnboardingState.loading || !canCreateWorkspace} /></label>
                  <label className="field session-field"><span>Warehouse name</span><input value={warehouse.name} onChange={(event) => updateWarehouse(index, { name: event.target.value })} placeholder="Johannesburg Hub" disabled={tenantOnboardingState.loading || !canCreateWorkspace} /></label>
                  <label className="field session-field"><span>Location</span><input value={warehouse.location} onChange={(event) => updateWarehouse(index, { location: event.target.value })} placeholder="Johannesburg" disabled={tenantOnboardingState.loading || !canCreateWorkspace} /></label>
                  {warehouses.length > 1 ? <button className="ghost-button" onClick={() => setTenantOnboardingForm((current) => ({ ...current, warehouses: current.warehouses.filter((_, itemIndex) => itemIndex !== index) }))} disabled={tenantOnboardingState.loading || !canCreateWorkspace} type="button">Remove</button> : null}
                </div>
              ))}
              <div className="history-action-row">
                <button className="ghost-button" onClick={() => setTenantOnboardingForm((current) => ({ ...current, warehouses: [...current.warehouses, { code: '', name: '', location: '' }] }))} disabled={tenantOnboardingState.loading || !canCreateWorkspace} type="button">Add warehouse</button>
              </div>
            </div>

            <div className="admin-form-section">
              <div>
                <strong>Users, roles, and scopes</strong>
                <p className="muted-text">Only people listed here are created. Empty scope is tenant-wide, so select it deliberately.</p>
              </div>
              {users.map((user, index) => (
                <div className="stack-card section-card" key={`user-${index}`}>
                  <div className="stack-title-row"><strong>User {index + 1}</strong><span className="status-tag status-partial">Explicit identity</span></div>
                  <div className="session-control-row">
                    <label className="field session-field"><span>Username</span><input value={user.username} onChange={(event) => updateUser(index, { username: event.target.value })} placeholder="operator.name" disabled={tenantOnboardingState.loading || !canCreateWorkspace} /></label>
                    <label className="field session-field"><span>Full name</span><input value={user.fullName} onChange={(event) => updateUser(index, { fullName: event.target.value })} placeholder="Operator name" disabled={tenantOnboardingState.loading || !canCreateWorkspace} /></label>
                    <label className="field session-field"><span>Operator actor name</span><input value={user.operatorActorName} onChange={(event) => updateUser(index, { operatorActorName: event.target.value })} placeholder="Warehouse Review Owner" disabled={tenantOnboardingState.loading || !canCreateWorkspace} /></label>
                  </div>
                  <div className="session-control-row">
                    <label className="field session-field"><span>Display name (optional)</span><input value={user.operatorDisplayName} onChange={(event) => updateUser(index, { operatorDisplayName: event.target.value })} placeholder="Same as actor name" disabled={tenantOnboardingState.loading || !canCreateWorkspace} /></label>
                    <label className="field session-field"><span>Description (optional)</span><input value={user.operatorDescription} onChange={(event) => updateUser(index, { operatorDescription: event.target.value })} placeholder="Responsibility" disabled={tenantOnboardingState.loading || !canCreateWorkspace} /></label>
                    <label className="field session-field"><span>Initial password (required)</span><input type="password" value={user.initialPassword} onChange={(event) => updateUser(index, { initialPassword: event.target.value })} placeholder="At least 8 characters" disabled={tenantOnboardingState.loading || !canCreateWorkspace} /></label>
                  </div>
                  <div>
                    <strong>Roles</strong>
                    <div className="ops-pill-row">
                      {ROLE_OPTIONS.map((role) => <label className="workspace-meta-pill" key={role}><input type="checkbox" checked={user.roles.includes(role)} onChange={() => toggleUserRole(index, role)} disabled={tenantOnboardingState.loading || !canCreateWorkspace} /> {roleLabel(role)}</label>)}
                    </div>
                  </div>
                  <div>
                    <strong>Warehouse scope</strong>
                    <div className="ops-pill-row">
                      <label className="workspace-meta-pill"><input type="checkbox" checked={user.tenantWide} onChange={(event) => updateUser(index, { tenantWide: event.target.checked, warehouseScopes: event.target.checked ? [] : user.warehouseScopes })} disabled={tenantOnboardingState.loading || !canCreateWorkspace} /> Tenant-wide</label>
                      {warehouses.filter((warehouse) => warehouse.code.trim()).map((warehouse) => <label className="workspace-meta-pill" key={warehouse.code}><input type="checkbox" checked={!user.tenantWide && user.warehouseScopes.includes(warehouse.code.trim().toUpperCase())} onChange={() => toggleUserScope(index, warehouse.code.trim().toUpperCase())} disabled={tenantOnboardingState.loading || !canCreateWorkspace || user.tenantWide} /> {warehouse.code.trim().toUpperCase()}</label>)}
                    </div>
                  </div>
                  {users.length > 1 ? <div className="history-action-row"><button className="ghost-button" onClick={() => setTenantOnboardingForm((current) => ({ ...current, users: current.users.filter((_, itemIndex) => itemIndex !== index) }))} disabled={tenantOnboardingState.loading || !canCreateWorkspace} type="button">Remove user</button></div> : null}
                </div>
              ))}
              <div className="history-action-row">
                <button className="ghost-button" onClick={() => setTenantOnboardingForm((current) => ({ ...current, users: [...current.users, { username: '', fullName: '', operatorActorName: '', operatorDisplayName: '', operatorDescription: '', roles: [], warehouseScopes: [], tenantWide: false, initialPassword: '' }] }))} disabled={tenantOnboardingState.loading || !canCreateWorkspace} type="button">Add user</button>
              </div>
            </div>

            <div className="admin-form-section">
              <div>
                <strong>Required-role policy</strong>
                <p className="muted-text">Choose which roles must have valid authority for every configured warehouse, or explicitly choose no mandatory role policy.</p>
              </div>
              <div className="ops-pill-row">
                {['REVIEW_OWNER', 'FINAL_APPROVER', 'ESCALATION_OWNER', 'INTEGRATION_ADMIN', 'INTEGRATION_OPERATOR'].map((role) => <label className="workspace-meta-pill" key={role}><input type="checkbox" checked={tenantOnboardingForm.requiredRoles.includes(role)} onChange={() => setTenantOnboardingForm((current) => ({ ...current, requiredRoles: current.requiredRoles.includes(role) ? current.requiredRoles.filter((currentRole) => currentRole !== role) : [...current.requiredRoles, role], requiredRolesConfigured: true }))} disabled={tenantOnboardingState.loading || !canCreateWorkspace} /> {roleLabel(role)}</label>)}
                <label className="workspace-meta-pill"><input type="checkbox" checked={tenantOnboardingForm.requiredRolesConfigured && tenantOnboardingForm.requiredRoles.length === 0} onChange={() => setTenantOnboardingForm((current) => ({ ...current, requiredRoles: [], requiredRolesConfigured: true }))} disabled={tenantOnboardingState.loading || !canCreateWorkspace} /> No mandatory role policy</label>
              </div>
            </div>

            <div className="admin-review-card">
              <strong>Review before creating</strong>
              <p>{tenantFormReady ? `${tenantOnboardingForm.tenantName} will be created as ${tenantOnboardingForm.tenantCode.toUpperCase()} with ${warehouses.length} warehouse(s) and ${users.length} explicitly supplied user(s).` : 'Complete the company, warehouse, user, role, scope, and required-policy fields before creating the workspace.'}</p>
              <p className="muted-text">The backend performs the final tenant isolation, role coverage, duplicate, and readiness validation. Provisioning completion does not mean operational data is ready.</p>
            </div>
            <div className="history-action-row">
              <button className="secondary-button" onClick={onboardTenant} disabled={tenantOnboardingState.loading || !canCreateWorkspace || !tenantFormReady} type="button">
                {tenantOnboardingState.loading ? 'Creating Workspace...' : 'Create Company Workspace'}
              </button>
              {tenantOnboardingState.result ? <button className="ghost-button" onClick={signInOperator} type="button">Open workspace sign-in</button> : null}
            </div>
            {tenantOnboardingState.error ? <p className="error-text">{tenantOnboardingState.error}</p> : null}
            {tenantOnboardingState.success ? <p className="success-text">{tenantOnboardingState.success}</p> : null}
            {tenantOnboardingState.result ? <div className="muted-text"><p>Created configuration: {tenantOnboardingState.result.starterWarehouseCodes.join(', ') || 'No warehouses reported'}.</p><p>Provisioned identities:</p><ul>{(tenantOnboardingState.result.provisionedUsers || []).map((user) => <li key={user.username}>{user.username} | {user.roles.join(', ')} | {user.warehouseScopes.length ? user.warehouseScopes.join(', ') : 'Tenant-wide'}{user.temporaryCredential ? ' | One-time temporary credential returned in this response' : ''}</li>)}</ul></div> : null}
          </article>

          <article className="stack-card section-card admin-list-panel">
            <div className="stack-title-row"><strong>Existing workspaces</strong><span className="status-tag status-partial">Portfolio</span></div>
            <p className="muted-text">Workspace portfolio visibility. Setting the sign-in target changes the sign-in form target only; it does not switch tenant data in place.</p>
            <div className="signal-list">
              {tenantDirectoryState.items.length ? tenantDirectoryState.items.map((tenant) => (
                <div key={tenant.code} className="signal-list-item">
                  <div className="stack-title-row">
                    <strong>{tenant.name}</strong>
                    <span className={`status-tag ${signedInSession?.tenantCode === tenant.code ? 'status-success' : 'status-partial'}`}>{signedInSession?.tenantCode === tenant.code ? 'Current' : tenant.code}</span>
                  </div>
                  <p>{tenant.description || 'Operational workspace ready for rollout.'}</p>
                  <div className="history-action-row">
                    <button className="ghost-button" onClick={() => setAuthSessionState((current) => ({ ...current, tenantCode: tenant.code }))} disabled={authSessionState.loading} type="button">Set Sign-In Target</button>
                  </div>
                </div>
              )) : <EmptyState>{tenantDirectoryState.loading ? 'Loading workspace portfolio...' : 'Company workspaces will appear here after the first rollout.'}</EmptyState>}
            </div>
          </article>
        </div>
      </Panel>
    </section>
  )
}
