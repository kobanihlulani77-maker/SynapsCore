import { MetricCard } from '../components/Card'
import EmptyState from '../components/EmptyState'
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
  const canCreateWorkspace = signedInRoles.includes('TENANT_ADMIN')

  return (
    <section className="content-grid">
      <Panel wide>
        <div className="panel-header">
          <div>
            <p className="panel-kicker">Workspace rollout</p>
            <h2>Bootstrap and monitor company workspace rollout</h2>
          </div>
          <span className="panel-badge scenario-badge">{tenantDirectoryState.items.length}</span>
        </div>

        <div className="ops-command-hero">
          <div className="ops-command-copy">
            <strong>Company workspace provisioning</strong>
            <p>
              This surface is for controlled workspace rollout. It should explain who can create a new company environment,
              what bootstrap information is required, and how operators continue into the new workspace safely.
            </p>
            <div className="ops-pill-row">
              <span className="workspace-meta-pill">{canCreateWorkspace ? 'Admin provisioning enabled' : 'Provisioning restricted'}</span>
              <span className="workspace-meta-pill">Workspace code required</span>
              <span className="workspace-meta-pill">Operator handoff ready</span>
            </div>
          </div>
          <div className="ops-command-actions">
            {tenantOnboardingState.result ? (
              <button className="secondary-button" onClick={signInOperator} disabled={authSessionState.loading || authSessionState.username.trim() !== tenantOnboardingState.result.adminUsername || authSessionState.tenantCode.trim() !== tenantOnboardingState.result.tenantCode} type="button">
                {authSessionState.action === 'signin' ? 'Opening Workspace...' : 'Continue As Workspace Admin'}
              </button>
            ) : null}
          </div>
        </div>

        <div className="summary-grid compact-summary-grid">
          <MetricCard label="Visible workspaces" value={tenantDirectoryState.items.length} accent="blue" note="Company environments currently visible inside the platform portfolio." />
          <MetricCard label="Provisioning access" value={canCreateWorkspace ? 'Granted' : 'Restricted'} accent="teal" note="Whether the current operator can create a new workspace rollout." />
          <MetricCard label="Current workspace" value={currentTenant?.code || 'None'} accent="amber" note="The company environment currently associated with the signed-in operator." />
          <MetricCard label="Last rollout" value={tenantOnboardingState.result?.tenantCode || 'Waiting'} accent="rose" note="The most recent workspace created through this rollout surface." />
        </div>

        <div className="experience-grid experience-grid-split">
          <article className="stack-card section-card">
            <div className="stack-title-row">
              <strong>Create company workspace</strong>
              <span className={`status-tag ${canCreateWorkspace ? 'status-success' : 'status-failure'}`}>{canCreateWorkspace ? 'Ready' : 'Restricted'}</span>
            </div>
            <p className="muted-text integration-note">
              {canCreateWorkspace
                ? 'Create a new company workspace with controlled sites, an admin operator, and a clean operational boundary. Executive access is created without forcing a developer-style bootstrap flow.'
                : 'Workspace creation is restricted to platform or tenant admins with rollout authority.'}
            </p>
            <div className="session-control-row">
              <label className="field session-field"><span>Workspace code</span><input value={tenantOnboardingForm.tenantCode} onChange={(event) => setTenantOnboardingForm((current) => ({ ...current, tenantCode: event.target.value }))} placeholder="ACME-OPS" disabled={tenantOnboardingState.loading || !canCreateWorkspace} /></label>
              <label className="field session-field"><span>Company workspace name</span><input value={tenantOnboardingForm.tenantName} onChange={(event) => setTenantOnboardingForm((current) => ({ ...current, tenantName: event.target.value }))} placeholder="Acme Operations" disabled={tenantOnboardingState.loading || !canCreateWorkspace} /></label>
              <label className="field session-field"><span>Description</span><input value={tenantOnboardingForm.description} onChange={(event) => setTenantOnboardingForm((current) => ({ ...current, description: event.target.value }))} placeholder="Regional operating workspace" disabled={tenantOnboardingState.loading || !canCreateWorkspace} /></label>
              <label className="field session-field"><span>Admin full name</span><input value={tenantOnboardingForm.adminFullName} onChange={(event) => setTenantOnboardingForm((current) => ({ ...current, adminFullName: event.target.value }))} placeholder="Amina Dlamini" disabled={tenantOnboardingState.loading || !canCreateWorkspace} /></label>
              <label className="field session-field"><span>Admin username</span><input value={tenantOnboardingForm.adminUsername} onChange={(event) => setTenantOnboardingForm((current) => ({ ...current, adminUsername: event.target.value }))} placeholder="amina.admin" disabled={tenantOnboardingState.loading || !canCreateWorkspace} /></label>
              <label className="field session-field"><span>Admin password</span><input type="password" value={tenantOnboardingForm.adminPassword} onChange={(event) => setTenantOnboardingForm((current) => ({ ...current, adminPassword: event.target.value }))} placeholder="Choose a strong bootstrap password" disabled={tenantOnboardingState.loading || !canCreateWorkspace} /></label>
              <label className="field session-field"><span>Primary location</span><input value={tenantOnboardingForm.primaryLocation} onChange={(event) => setTenantOnboardingForm((current) => ({ ...current, primaryLocation: event.target.value }))} placeholder="Johannesburg" disabled={tenantOnboardingState.loading || !canCreateWorkspace} /></label>
              <label className="field session-field"><span>Secondary location</span><input value={tenantOnboardingForm.secondaryLocation} onChange={(event) => setTenantOnboardingForm((current) => ({ ...current, secondaryLocation: event.target.value }))} placeholder="Cape Town" disabled={tenantOnboardingState.loading || !canCreateWorkspace} /></label>
            </div>
            <div className="history-action-row">
              <button className="secondary-button" onClick={onboardTenant} disabled={tenantOnboardingState.loading || !canCreateWorkspace || !tenantOnboardingForm.tenantCode.trim() || !tenantOnboardingForm.tenantName.trim() || !tenantOnboardingForm.adminFullName.trim() || !tenantOnboardingForm.adminUsername.trim() || !tenantOnboardingForm.adminPassword.trim() || !tenantOnboardingForm.primaryLocation.trim()} type="button">
                {tenantOnboardingState.loading ? 'Creating Workspace...' : 'Create Company Workspace'}
              </button>
            </div>
            {tenantOnboardingState.error ? <p className="error-text">{tenantOnboardingState.error}</p> : null}
            {tenantOnboardingState.success ? <p className="success-text">{tenantOnboardingState.success}</p> : null}
            {tenantOnboardingState.result ? <p className="muted-text">Executive approver account: {tenantOnboardingState.result.executiveUsername} | Reset the password from Users before first use. Warehouses {tenantOnboardingState.result.starterWarehouseCodes.join(', ')}</p> : null}
          </article>

          <article className="stack-card section-card">
            <div className="stack-title-row"><strong>Existing workspaces</strong><span className="status-tag status-partial">Portfolio</span></div>
            <div className="signal-list">
              {tenantDirectoryState.items.length ? tenantDirectoryState.items.map((tenant) => (
                <div key={tenant.code} className="signal-list-item">
                  <div className="stack-title-row">
                    <strong>{tenant.name}</strong>
                    <span className={`status-tag ${signedInSession?.tenantCode === tenant.code ? 'status-success' : 'status-partial'}`}>{signedInSession?.tenantCode === tenant.code ? 'Current' : tenant.code}</span>
                  </div>
                  <p>{tenant.description || 'Operational workspace ready for rollout.'}</p>
                  <div className="history-action-row">
                    <button className="ghost-button" onClick={() => setAuthSessionState((current) => ({ ...current, tenantCode: tenant.code }))} type="button">Set Sign-In Target</button>
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
