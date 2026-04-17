import LoadingState from '../components/LoadingState'

export default function SignInPage({ context }) {
  const {
    effectivePageMeta,
    navigateToPage,
    publicPages,
    tenantDirectoryState,
    authSessionState,
    setAuthSessionState,
    rememberWorkspace,
    setRememberWorkspace,
    handleSignInSubmit,
    selectedTenantOption,
    wsUrl,
    signInWorkspaceHint,
    signInConfigHint,
  } = context

  const featureCards = [
    { title: 'Live visibility', body: 'Orders, inventory, locations, fulfillment, incidents, and connectors pulled into one operational picture.' },
    { title: 'Prediction and guidance', body: 'Detect risk early, estimate near-term impact, and surface what the team should do next.' },
    { title: 'Control and trust', body: 'Run scenarios, route approvals, recover failed inbound work, and keep runtime confidence visible.' },
  ]
  const signInBusy = authSessionState.loading && authSessionState.action === 'signin'

  return (
    <main className={`public-shell public-page-${effectivePageMeta.key}`}>
      <header className="public-topbar">
        <button className="brand-lockup brand-button" onClick={() => navigateToPage('landing')} type="button">
          <span className="brand-mark">S</span>
          <span><strong>SynapseCore</strong><small>Operational intelligence operating system</small></span>
        </button>
        <nav className="public-nav">
          {publicPages.filter((page) => page.key !== 'sign-in').map((page) => (
            <button key={page.key} className="ghost-button" onClick={() => navigateToPage(page.key)} type="button">{page.label}</button>
          ))}
        </nav>
      </header>
      <section className="public-signin-shell enterprise-signin-shell">
        <article className="public-signin-story enterprise-signin-story">
          <p className="eyebrow">Secure company entry</p>
          <h1>{effectivePageMeta.title}</h1>
          <p>{effectivePageMeta.description}</p>
          <div className="enterprise-status-row">
            <span className="enterprise-status-pill">{tenantDirectoryState.loading ? 'Checking workspace directory' : `${tenantDirectoryState.items.length} workspaces visible`}</span>
            <span className={`enterprise-status-pill ${wsUrl ? 'status-live' : 'status-missing'}`}>{wsUrl ? 'Realtime path ready' : 'Realtime not configured'}</span>
          </div>
          <div className="public-feature-stack">
            {featureCards.map((feature) => (
              <article key={feature.title} className="public-feature-card">
                <strong>{feature.title}</strong>
                <p>{feature.body}</p>
              </article>
            ))}
          </div>
        </article>
        <article className="public-signin-card">
          <p className="panel-kicker">Company sign in</p>
          <h2>Enter the operational platform</h2>
          {tenantDirectoryState.loading && !tenantDirectoryState.items.length ? <LoadingState label="Loading available workspaces..." /> : null}
          <form className="signin-form-shell" onSubmit={handleSignInSubmit}>
            <div className="signin-form-grid">
              <label className="field">
                <span>Tenant workspace</span>
                <input
                  type="text"
                  list="tenant-workspace-options"
                  value={authSessionState.tenantCode}
                  onChange={(event) => setAuthSessionState((current) => ({ ...current, tenantCode: event.target.value.toUpperCase() }))}
                  placeholder={tenantDirectoryState.loading ? 'Loading workspace directory...' : 'Enter tenant code'}
                  autoComplete="organization"
                  disabled={signInBusy}
                />
                <datalist id="tenant-workspace-options">
                  {tenantDirectoryState.items.map((tenant) => <option key={tenant.code} value={tenant.code}>{tenant.name}</option>)}
                </datalist>
              </label>
              <label className="field">
                <span>Username</span>
                <input
                  type="text"
                  value={authSessionState.username}
                  onChange={(event) => setAuthSessionState((current) => ({ ...current, username: event.target.value }))}
                  placeholder="workspace.admin"
                  autoComplete="username"
                  disabled={signInBusy}
                />
              </label>
              <label className="field">
                <span>Password</span>
                <input
                  type="password"
                  value={authSessionState.password}
                  onChange={(event) => setAuthSessionState((current) => ({ ...current, password: event.target.value }))}
                  placeholder="Enter workspace password"
                  autoComplete="current-password"
                  disabled={signInBusy}
                />
              </label>
            </div>
            <div className="signin-meta-row">
              <label className="checkbox-field inline-checkbox">
                <input className="checkbox-input" type="checkbox" checked={rememberWorkspace} onChange={(event) => setRememberWorkspace(event.target.checked)} />
                <span>Remember tenant code and username on this device</span>
              </label>
              <span className="muted-text">Password recovery is managed by your tenant admin.</span>
            </div>
            <div className="history-action-row">
              <button className="primary-button" disabled={signInBusy || !authSessionState.tenantCode.trim() || !authSessionState.username.trim() || !authSessionState.password.trim()} type="submit">
                {signInBusy ? 'Opening Workspace...' : 'Enter Platform'}
              </button>
              <button className="ghost-button" onClick={() => navigateToPage('product')} type="button">Product Overview</button>
            </div>
          </form>
          <div className="signin-trust-grid">
            <article className="signin-trust-card">
              <span>Tenant scope</span>
              <strong>{selectedTenantOption?.name || authSessionState.tenantCode.trim() || 'Workspace required'}</strong>
              <p>Operators enter a company workspace, not a generic app account.</p>
            </article>
            <article className="signin-trust-card">
              <span>Session model</span>
              <strong>Secure browser session</strong>
              <p>Protected actions, approvals, replay, and realtime access all follow the signed-in operator identity.</p>
            </article>
            <article className="signin-trust-card">
              <span>Realtime posture</span>
              <strong>{wsUrl ? 'Live transport configured' : 'Realtime path missing'}</strong>
              <p>SynapseCore opens the command workspace with live operational updates when the session is valid.</p>
            </article>
          </div>
          <p className="muted-text integration-note">{signInWorkspaceHint}</p>
          <p className="muted-text integration-note">{signInConfigHint}</p>
          {tenantDirectoryState.error ? <p className="error-text">{tenantDirectoryState.error}</p> : null}
          {authSessionState.error ? <p className="error-text">{authSessionState.error}</p> : null}
        </article>
      </section>
    </main>
  )
}
