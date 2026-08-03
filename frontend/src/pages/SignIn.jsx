import { useState } from 'react'
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

  const [showPassword, setShowPassword] = useState(false)
  const [attemptedSubmit, setAttemptedSubmit] = useState(false)
  const [visitedFields, setVisitedFields] = useState({
    tenantCode: false,
    username: false,
    password: false,
  })

  const featureCards = [
    { title: 'Workspace first', body: 'Start with the company workspace code so SynapseCore opens the correct tenant boundary.' },
    { title: 'Operator identity', body: 'Username and password confirm the person signing in and the operational permissions they carry.' },
    { title: 'Live command entry', body: 'After validation, alerts, replay, approvals, runtime, orders, and inventory open under that session.' },
  ]
  const signInBusy = authSessionState.loading && authSessionState.action === 'signin'
  const workspaceCodeMissing = (attemptedSubmit || visitedFields.tenantCode) && !authSessionState.tenantCode.trim()
  const usernameMissing = (attemptedSubmit || visitedFields.username) && !authSessionState.username.trim()
  const passwordMissing = (attemptedSubmit || visitedFields.password) && !authSessionState.password.trim()
  const authError = authSessionState.error || tenantDirectoryState.error
  const authIssueTone = authError?.toLowerCase().includes('expired') ? 'warning' : 'danger'
  const authIssueTitle = authError?.toLowerCase().includes('expired')
    ? 'Session expired'
    : 'Workspace sign-in needs attention'

  const handleFieldBlur = (field) => {
    setVisitedFields((current) => ({ ...current, [field]: true }))
  }

  const submitWithValidation = (event) => {
    setAttemptedSubmit(true)
    handleSignInSubmit(event)
  }

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
          <div className="public-positioning-card signin-positioning-card">
            <strong>Daily operator path</strong>
            <p>Use the workspace code from your company admin. SynapseCore checks tenant context and operator credentials before protected actions unlock.</p>
          </div>
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
          <p className="muted-text">Sign in with workspace code, username, and password. If access fails, the company workspace admin manages resets and role changes.</p>
          {tenantDirectoryState.loading && !tenantDirectoryState.items.length ? <LoadingState label="Loading available workspaces..." /> : null}
          <form className="signin-form-shell" onSubmit={submitWithValidation}>
            <div className="signin-form-grid">
              <label className="field">
                <span>Company workspace code</span>
                <input
                  type="text"
                  list="tenant-workspace-options"
                  value={authSessionState.tenantCode}
                  onChange={(event) => setAuthSessionState((current) => ({ ...current, tenantCode: event.target.value.toUpperCase() }))}
                  placeholder={tenantDirectoryState.loading ? 'Loading workspace directory...' : 'Enter company workspace code'}
                  autoComplete="organization"
                  disabled={signInBusy}
                  aria-invalid={workspaceCodeMissing}
                  onBlur={() => handleFieldBlur('tenantCode')}
                />
                <datalist id="tenant-workspace-options">
                  {tenantDirectoryState.items.map((tenant) => <option key={tenant.code} value={tenant.code}>{tenant.name}</option>)}
                </datalist>
                <span className={`field-hint ${workspaceCodeMissing ? 'field-validation-warning' : ''}`}>
                  {workspaceCodeMissing
                    ? 'Enter the workspace code so SynapseCore can open the correct company environment.'
                    : 'Use the workspace code your SynapseCore company admin gave your team.'}
                </span>
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
                  aria-invalid={usernameMissing}
                  onBlur={() => handleFieldBlur('username')}
                />
                <span className={`field-hint ${usernameMissing ? 'field-validation-warning' : ''}`}>
                  {usernameMissing
                    ? 'Enter the operator username tied to this company workspace.'
                    : 'Operator usernames stay inside the company workspace boundary.'}
                </span>
              </label>
              <label className="field">
                <span>Password</span>
                <div className="field-control">
                  <input
                    type={showPassword ? 'text' : 'password'}
                    value={authSessionState.password}
                    onChange={(event) => setAuthSessionState((current) => ({ ...current, password: event.target.value }))}
                    placeholder="Enter workspace password"
                    autoComplete="current-password"
                    disabled={signInBusy}
                    aria-invalid={passwordMissing}
                    onBlur={() => handleFieldBlur('password')}
                  />
                  <button className="field-inline-button" onClick={() => setShowPassword((current) => !current)} type="button">
                    {showPassword ? 'Hide' : 'Show'}
                  </button>
                </div>
                <span className={`field-hint ${passwordMissing ? 'field-validation-warning' : ''}`}>
                  {passwordMissing
                    ? 'Enter the password for this operator account.'
                    : 'Passwords are verified against the signed company workspace before protected actions unlock.'}
                </span>
              </label>
            </div>
            <div className="signin-status-grid">
              <article className="signin-status-card">
                <span>Workspace target</span>
                <strong>{selectedTenantOption?.name || authSessionState.tenantCode.trim() || 'Choose workspace code'}</strong>
                <p>{selectedTenantOption ? 'The live workspace directory recognizes this company environment.' : 'Workspace codes point operators into the correct company operations environment.'}</p>
              </article>
              <article className="signin-status-card">
                <span>Access flow</span>
                <strong>{signInBusy ? 'Validating access' : 'Operator sign-in'}</strong>
                <p>{signInBusy ? 'SynapseCore is validating workspace identity, operator credentials, and session posture.' : 'Operators enter with workspace code, username, and password. Company admins manage reset and access policy.'}</p>
              </article>
            </div>
            <div className="signin-meta-row">
              <label className="checkbox-field inline-checkbox">
                <input className="checkbox-input" type="checkbox" checked={rememberWorkspace} onChange={(event) => setRememberWorkspace(event.target.checked)} />
                <span>Remember workspace code and username on this device</span>
              </label>
              <span className="muted-text">Password recovery is managed by your company workspace admin.</span>
            </div>
            <div className="history-action-row">
              <button className="primary-button" disabled={signInBusy || !authSessionState.tenantCode.trim() || !authSessionState.username.trim() || !authSessionState.password.trim()} type="submit">
                {signInBusy ? 'Opening Workspace...' : 'Enter Platform'}
              </button>
              <button className="secondary-button" onClick={() => navigateToPage('create-workspace')} type="button">Create Workspace</button>
              <button className="ghost-button" onClick={() => navigateToPage('product')} type="button">Product Overview</button>
            </div>
          </form>
          <div className="signin-trust-grid">
            <article className="signin-trust-card">
              <span>Workspace scope</span>
              <strong>{selectedTenantOption?.name || authSessionState.tenantCode.trim() || 'Workspace required'}</strong>
              <p>Operators sign into a specific company workspace, not a generic application account.</p>
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
          {signInBusy ? (
            <div className="signin-feedback-card tone-info">
              <strong>Opening your operations workspace</strong>
              <p>We're validating the company workspace, operator identity, and live session before the command center opens.</p>
            </div>
          ) : null}
          {authError ? (
            <div className={`signin-feedback-card tone-${authIssueTone}`}>
              <strong>{authIssueTitle}</strong>
              <p>{authError}</p>
            </div>
          ) : null}
          <p className="muted-text integration-note">{signInWorkspaceHint}</p>
          <p className="muted-text integration-note">{signInConfigHint}</p>
        </article>
      </section>
    </main>
  )
}
