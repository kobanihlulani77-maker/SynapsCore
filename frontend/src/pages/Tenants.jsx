import { SummaryCard } from '../components/Card'
import Panel from '../components/Panel'

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

  return (
    <section className="content-grid">
      <Panel wide>
        <div className="panel-header">
          <div><p className="panel-kicker">Tenant management</p><h2>Bootstrap and monitor workspace rollout</h2></div>
          <span className="panel-badge scenario-badge">{tenantDirectoryState.items.length}</span>
        </div>
        <p className="muted-text integration-note">
          {signedInRoles.includes('TENANT_ADMIN')
            ? 'Create a new tenant workspace with controlled warehouses, an admin account, and an approval lane. Executive access is created without exposing a raw bootstrap password.'
            : 'Tenant creation is restricted to tenant admins with rollout authority.'}
        </p>
        <div className="summary-grid compact-summary-grid">
          <SummaryCard label="Visible workspaces" value={tenantDirectoryState.items.length} accent="blue" />
          <SummaryCard label="Admin access" value={signedInRoles.includes('TENANT_ADMIN') ? 'Granted' : 'Restricted'} accent="teal" />
          <SummaryCard label="Current workspace" value={currentTenant?.code || 'None'} accent="amber" />
          <SummaryCard label="Last rollout" value={tenantOnboardingState.result?.tenantCode || 'Waiting'} accent="rose" />
        </div>
        <div className="tenant-rollout-grid">
          <article className="stack-card">
            <div className="stack-title-row">
              <strong>New workspace</strong>
              <span className={`status-tag ${signedInRoles.includes('TENANT_ADMIN') ? 'status-success' : 'status-failure'}`}>{signedInRoles.includes('TENANT_ADMIN') ? 'Ready' : 'Restricted'}</span>
            </div>
            <div className="session-control-row">
              <label className="field session-field"><span>Tenant Code</span><input value={tenantOnboardingForm.tenantCode} onChange={(event) => setTenantOnboardingForm((current) => ({ ...current, tenantCode: event.target.value }))} placeholder="ACME-OPS" disabled={tenantOnboardingState.loading || !signedInRoles.includes('TENANT_ADMIN')} /></label>
              <label className="field session-field"><span>Tenant Name</span><input value={tenantOnboardingForm.tenantName} onChange={(event) => setTenantOnboardingForm((current) => ({ ...current, tenantName: event.target.value }))} placeholder="Acme Operations" disabled={tenantOnboardingState.loading || !signedInRoles.includes('TENANT_ADMIN')} /></label>
              <label className="field session-field"><span>Description</span><input value={tenantOnboardingForm.description} onChange={(event) => setTenantOnboardingForm((current) => ({ ...current, description: event.target.value }))} placeholder="Regional operating workspace" disabled={tenantOnboardingState.loading || !signedInRoles.includes('TENANT_ADMIN')} /></label>
              <label className="field session-field"><span>Admin Full Name</span><input value={tenantOnboardingForm.adminFullName} onChange={(event) => setTenantOnboardingForm((current) => ({ ...current, adminFullName: event.target.value }))} placeholder="Amina Dlamini" disabled={tenantOnboardingState.loading || !signedInRoles.includes('TENANT_ADMIN')} /></label>
              <label className="field session-field"><span>Admin Username</span><input value={tenantOnboardingForm.adminUsername} onChange={(event) => setTenantOnboardingForm((current) => ({ ...current, adminUsername: event.target.value }))} placeholder="amina.admin" disabled={tenantOnboardingState.loading || !signedInRoles.includes('TENANT_ADMIN')} /></label>
              <label className="field session-field"><span>Admin Password</span><input type="password" value={tenantOnboardingForm.adminPassword} onChange={(event) => setTenantOnboardingForm((current) => ({ ...current, adminPassword: event.target.value }))} placeholder="Choose a strong bootstrap password" disabled={tenantOnboardingState.loading || !signedInRoles.includes('TENANT_ADMIN')} /></label>
              <label className="field session-field"><span>Primary Location</span><input value={tenantOnboardingForm.primaryLocation} onChange={(event) => setTenantOnboardingForm((current) => ({ ...current, primaryLocation: event.target.value }))} placeholder="Johannesburg" disabled={tenantOnboardingState.loading || !signedInRoles.includes('TENANT_ADMIN')} /></label>
              <label className="field session-field"><span>Secondary Location</span><input value={tenantOnboardingForm.secondaryLocation} onChange={(event) => setTenantOnboardingForm((current) => ({ ...current, secondaryLocation: event.target.value }))} placeholder="Cape Town" disabled={tenantOnboardingState.loading || !signedInRoles.includes('TENANT_ADMIN')} /></label>
            </div>
            <div className="history-action-row">
              <button className="secondary-button" onClick={onboardTenant} disabled={tenantOnboardingState.loading || !signedInRoles.includes('TENANT_ADMIN') || !tenantOnboardingForm.tenantCode.trim() || !tenantOnboardingForm.tenantName.trim() || !tenantOnboardingForm.adminFullName.trim() || !tenantOnboardingForm.adminUsername.trim() || !tenantOnboardingForm.adminPassword.trim() || !tenantOnboardingForm.primaryLocation.trim()} type="button">
                {tenantOnboardingState.loading ? 'Creating Workspace...' : 'Create Tenant Workspace'}
              </button>
              {tenantOnboardingState.result ? (
                <button className="ghost-button" onClick={signInOperator} disabled={authSessionState.loading || authSessionState.username.trim() !== tenantOnboardingState.result.adminUsername || authSessionState.tenantCode.trim() !== tenantOnboardingState.result.tenantCode} type="button">
                  {authSessionState.action === 'signin' ? 'Opening Workspace...' : 'Continue As Workspace Admin'}
                </button>
              ) : null}
            </div>
            {tenantOnboardingState.error ? <p className="error-text">{tenantOnboardingState.error}</p> : null}
            {tenantOnboardingState.success ? <p className="success-text">{tenantOnboardingState.success}</p> : null}
            {tenantOnboardingState.result ? <p className="muted-text">Executive approver account: {tenantOnboardingState.result.executiveUsername} | Reset the password from Users before first use. Warehouses {tenantOnboardingState.result.starterWarehouseCodes.join(', ')}</p> : null}
          </article>
          <article className="stack-card">
            <div className="stack-title-row"><strong>Existing workspaces</strong><span className="status-tag status-partial">Portfolio</span></div>
            <div className="stack-list tenant-list-stack">
              {tenantDirectoryState.items.length ? tenantDirectoryState.items.map((tenant) => (
                <div key={tenant.code} className="stack-card stack-card-compact">
                  <div className="stack-title-row">
                    <strong>{tenant.name}</strong>
                    <span className={`status-tag ${signedInSession?.tenantCode === tenant.code ? 'status-success' : 'status-partial'}`}>{signedInSession?.tenantCode === tenant.code ? 'Current' : tenant.code}</span>
                  </div>
                  <p>{tenant.code}</p>
                  <div className="history-action-row">
                    <button className="ghost-button" onClick={() => setAuthSessionState((current) => ({ ...current, tenantCode: tenant.code }))} type="button">Set Sign-In Target</button>
                  </div>
                </div>
              )) : <div className="empty-state">{tenantDirectoryState.loading ? 'Loading tenant portfolio...' : 'Tenant workspaces will appear here after the first rollout.'}</div>}
            </div>
          </article>
        </div>
      </Panel>
    </section>
  )
}
