import { SummaryCard } from '../components/Card'
import Panel from '../components/Panel'

export default function ProfilePage({ context }) {
  const {
    isAuthenticated,
    isProfilePage,
    passwordChangeRequired,
    passwordRotationRequired,
    activeAlerts,
    pendingApprovalScenarios,
    systemIncidents,
    signedInSession,
    signedInRoles,
    signedInWarehouseScopes,
    signedInSessionExpiresAt,
    signedInPasswordExpiresAt,
    formatCodeLabel,
    formatTimestamp,
    passwordChangeState,
    setPasswordChangeState,
    changeSignedInPassword,
    signOutOperator,
    authSessionState,
    navigateToPage,
  } = context

  if (!isAuthenticated || !isProfilePage) return null

  const sessionHealthNeedsAction = passwordChangeRequired || passwordRotationRequired
  const sessionQuickActions = [
    { title: 'Open alerts', note: `${activeAlerts.length} active alert${activeAlerts.length === 1 ? '' : 's'} in the workspace`, target: 'alerts' },
    { title: 'Open approvals', note: `${pendingApprovalScenarios.length} decision${pendingApprovalScenarios.length === 1 ? '' : 's'} waiting on review`, target: 'approvals' },
    { title: 'Open runtime', note: `${systemIncidents.length} runtime incident${systemIncidents.length === 1 ? '' : 's'} visible`, target: 'runtime' },
  ]

  return (
    <section className="content-grid">
      <Panel wide>
        <div className="panel-header">
          <div><p className="panel-kicker">Profile</p><h2>Personal access and session posture</h2></div>
          <span className="panel-badge audit-badge">{signedInSession ? 'Active' : 'Signed Out'}</span>
        </div>
        <div className="summary-grid compact-summary-grid">
          <SummaryCard label="Roles" value={signedInRoles.length} accent="blue" />
          <SummaryCard label="Warehouse scopes" value={signedInWarehouseScopes.length || 'All'} accent="teal" />
          <SummaryCard label="Action pressure" value={sessionHealthNeedsAction ? 'Attention' : 'Healthy'} accent="amber" />
          <SummaryCard label="Live notices" value={activeAlerts.length + systemIncidents.length} accent="rose" />
        </div>
        <div className="profile-grid">
          <article className="stack-card">
            <div className="stack-title-row">
              <strong>{signedInSession?.displayName || 'No active session'}</strong>
              <span className={`status-tag ${sessionHealthNeedsAction ? 'status-failure' : 'status-success'}`}>{sessionHealthNeedsAction ? 'Action Needed' : 'Healthy'}</span>
            </div>
            <p>{signedInSession ? `${signedInSession.username} in ${signedInSession.tenantName || signedInSession.tenantCode}` : 'Sign in to manage your operator identity.'}</p>
            <p className="muted-text">Actor {signedInSession?.actorName || 'Unavailable'} | Roles {signedInRoles.length ? signedInRoles.map((role) => formatCodeLabel(role)).join(', ') : 'None'}</p>
            <p className="muted-text">Warehouse scope {signedInWarehouseScopes.length ? signedInWarehouseScopes.join(', ') : 'All warehouses in tenant scope'}.</p>
            <p className="muted-text">Session expires {signedInSessionExpiresAt ? formatTimestamp(signedInSessionExpiresAt) : 'per tenant policy'}.</p>
            <p className="muted-text">Password expires {signedInPasswordExpiresAt ? formatTimestamp(signedInPasswordExpiresAt) : 'per tenant policy'}.</p>
          </article>
          <article className="stack-card">
            <div className="stack-title-row">
              <strong>Change password</strong>
              <span className="scenario-type-tag">Secure session</span>
            </div>
            <div className="session-control-row">
              <label className="field session-field">
                <span>Current Password</span>
                <input
                  type="password"
                  value={passwordChangeState.form.currentPassword}
                  onChange={(event) => setPasswordChangeState((current) => ({ ...current, form: { ...current.form, currentPassword: event.target.value } }))}
                  placeholder="Enter current password"
                  disabled={passwordChangeState.loading}
                />
              </label>
              <label className="field session-field">
                <span>New Password</span>
                <input
                  type="password"
                  value={passwordChangeState.form.newPassword}
                  onChange={(event) => setPasswordChangeState((current) => ({ ...current, form: { ...current.form, newPassword: event.target.value } }))}
                  placeholder="Choose a stronger password"
                  disabled={passwordChangeState.loading}
                />
              </label>
              <label className="field session-field">
                <span>Confirm Password</span>
                <input
                  type="password"
                  value={passwordChangeState.form.confirmPassword}
                  onChange={(event) => setPasswordChangeState((current) => ({ ...current, form: { ...current.form, confirmPassword: event.target.value } }))}
                  placeholder="Repeat new password"
                  disabled={passwordChangeState.loading}
                />
              </label>
            </div>
            <div className="history-action-row">
              <button className="secondary-button" onClick={changeSignedInPassword} disabled={passwordChangeState.loading || passwordChangeState.form.currentPassword.trim().length < 8 || passwordChangeState.form.newPassword.trim().length < 8 || passwordChangeState.form.newPassword !== passwordChangeState.form.confirmPassword} type="button">
                {passwordChangeState.loading ? 'Updating Password...' : 'Update Password'}
              </button>
              <button className="ghost-button" onClick={signOutOperator} disabled={authSessionState.loading} type="button">
                {authSessionState.action === 'signout' ? 'Signing Out...' : 'Sign Out'}
              </button>
            </div>
            {passwordChangeState.error ? <p className="error-text">{passwordChangeState.error}</p> : null}
            {passwordChangeState.success ? <p className="success-text">{passwordChangeState.success}</p> : null}
          </article>
        </div>
        <div className="experience-grid experience-grid-split">
          <article className="stack-card section-card">
            <div className="stack-title-row">
              <strong>Session hygiene</strong>
              <span className={`status-tag ${sessionHealthNeedsAction ? 'status-failure' : 'status-success'}`}>{sessionHealthNeedsAction ? 'Needs action' : 'Healthy'}</span>
            </div>
            <div className="signal-list">
              <div className="signal-list-item">
                <strong>Password posture</strong>
                <p>{passwordChangeRequired ? 'Password change required before the next secure action.' : passwordRotationRequired ? 'Password rotation window has elapsed.' : 'Password is inside policy.'}</p>
                <p className="muted-text">Expires {signedInPasswordExpiresAt ? formatTimestamp(signedInPasswordExpiresAt) : 'per tenant policy'}.</p>
              </div>
              <div className="signal-list-item">
                <strong>Session expiry</strong>
                <p>{signedInSessionExpiresAt ? formatTimestamp(signedInSessionExpiresAt) : 'Controlled by tenant security policy.'}</p>
                <p className="muted-text">Use sign out when leaving a live operations console to avoid stale control access.</p>
              </div>
            </div>
          </article>
          <article className="stack-card section-card">
            <div className="stack-title-row">
              <strong>Quick routes</strong>
              <span className="scenario-type-tag">{sessionQuickActions.length}</span>
            </div>
            <div className="signal-list">
              {sessionQuickActions.map((action) => (
                <button key={action.title} className="utility-action" onClick={() => navigateToPage(action.target)} type="button">
                  <span>Workspace action</span>
                  <strong>{action.title}</strong>
                  <p>{action.note}</p>
                </button>
              ))}
            </div>
          </article>
        </div>
      </Panel>
    </section>
  )
}
