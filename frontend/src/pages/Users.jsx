import { MetricCard } from '../components/Card'
import Panel from '../components/Panel'
import EmptyState from '../components/EmptyState'

export default function UsersPage({ context }) {
  const {
    isAuthenticated,
    isUsersPage,
    accessAdminOperators,
    accessAdminUsers,
    workspaceAdmin,
    selectedAccessSubject,
    setSelectedAccessSubjectKey,
    formatCodeLabel,
    navigateToPage,
    canManageTenantAccess,
  } = context

  if (!isAuthenticated || !isUsersPage) return null

  const adminOperators = accessAdminOperators.filter((operator) => operator.roles.includes('TENANT_ADMIN')).length
  const integrationOperators = accessAdminOperators.filter((operator) => operator.roles.some((role) => role.startsWith('INTEGRATION_'))).length
  const reviewOperators = accessAdminOperators.filter((operator) => operator.roles.includes('REVIEW_OWNER') || operator.roles.includes('FINAL_APPROVER')).length
  const selectedLabel = selectedAccessSubject
    ? selectedAccessSubject.subjectType === 'operator'
      ? 'Operator lane'
      : 'User account'
    : 'Waiting'

  return (
    <section className="content-grid">
      <Panel wide>
        <div className="panel-header">
          <div>
            <p className="panel-kicker">Access control</p>
            <h2>Manage operators, roles, and user access</h2>
          </div>
          <span className="panel-badge integration-badge">{accessAdminOperators.length + accessAdminUsers.length}</span>
        </div>

        <div className="ops-command-hero">
          <div className="ops-command-copy">
            <strong>Operator access control surface</strong>
            <p>
              This page should help company admins understand who can act, where they can act, and which access lanes
              need cleanup before they affect live operations.
            </p>
            <div className="ops-pill-row">
              <span className="workspace-meta-pill">Role scoped</span>
              <span className="workspace-meta-pill">Warehouse aware</span>
              <span className="workspace-meta-pill">Admin controlled</span>
            </div>
          </div>
          <div className="ops-command-actions">
            <button className="secondary-button" onClick={() => navigateToPage('settings')} type="button">
              Open company settings
            </button>
            <button className="ghost-button" onClick={() => navigateToPage('profile')} type="button">
              Open my profile
            </button>
          </div>
        </div>

        <div className="summary-grid compact-summary-grid">
          <MetricCard label="Operators" value={accessAdminOperators.length} accent="teal" note="Operational actors that carry company roles and warehouse scope." />
          <MetricCard label="User accounts" value={accessAdminUsers.length} accent="blue" note="Workspace sign-in identities linked to operator lanes." />
          <MetricCard label="Tenant admins" value={adminOperators} accent="amber" note="Admins currently able to manage workspace policy and access." />
          <MetricCard label="Support attention" value={workspaceAdmin?.supportIncidents?.length || 0} accent="rose" note="Access or support incidents already visible to the workspace." />
        </div>

        <div className="experience-grid experience-grid-three">
          <article className="stack-card section-card">
            <div className="stack-title-row"><strong>Operator lanes</strong><span className="scenario-type-tag">{accessAdminOperators.length}</span></div>
            <div className="signal-list">
              {accessAdminOperators.length ? accessAdminOperators.slice(0, 5).map((operator) => (
                <button key={operator.id} className={`signal-list-item selectable-card system-select-card ${selectedAccessSubject?.subjectKey === `operator-${operator.id}` ? 'is-selected' : ''}`} onClick={() => setSelectedAccessSubjectKey(`operator-${operator.id}`)} type="button">
                  <div className="stack-title-row">
                    <strong>{operator.displayName}</strong>
                    <span className={`status-tag ${operator.active ? 'status-success' : 'status-failure'}`}>{operator.active ? 'Active' : 'Inactive'}</span>
                  </div>
                  <p>{operator.actorName}</p>
                  <p className="muted-text">{operator.roles.map((role) => formatCodeLabel(role)).join(', ') || 'No roles assigned'}</p>
                  <p className="muted-text">{operator.warehouseScopes.length ? `Warehouse scope ${operator.warehouseScopes.join(', ')}` : 'Tenant-wide lane'}</p>
                </button>
              )) : <EmptyState>Operator lanes appear here once company admins start assigning roles and warehouse scope.</EmptyState>}
            </div>
          </article>

          <article className="stack-card section-card">
            <div className="stack-title-row"><strong>User roster</strong><span className="scenario-type-tag">{accessAdminUsers.length}</span></div>
            <div className="signal-list">
              {accessAdminUsers.length ? accessAdminUsers.slice(0, 5).map((user) => (
                <button key={user.id} className={`signal-list-item selectable-card system-select-card ${selectedAccessSubject?.subjectKey === `user-${user.id}` ? 'is-selected' : ''}`} onClick={() => setSelectedAccessSubjectKey(`user-${user.id}`)} type="button">
                  <div className="stack-title-row">
                    <strong>{user.fullName}</strong>
                    <span className={`status-tag ${user.active ? 'status-success' : 'status-failure'}`}>{user.active ? 'Enabled' : 'Disabled'}</span>
                  </div>
                  <p>{user.username}</p>
                  <p className="muted-text">Operator lane {user.operatorDisplayName || user.operatorActorName}</p>
                </button>
              )) : <EmptyState>User accounts will appear here once operator access is provisioned for the workspace.</EmptyState>}
            </div>
          </article>

          <article className="stack-card section-card">
            <div className="stack-title-row"><strong>Permission posture</strong><span className="scenario-type-tag">{canManageTenantAccess ? 'Admin tools' : 'Read only'}</span></div>
            <div className="utility-metric-grid">
              <div><span>Tenant admins</span><strong>{adminOperators}</strong></div>
              <div><span>Integration roles</span><strong>{integrationOperators}</strong></div>
              <div><span>Approval roles</span><strong>{reviewOperators}</strong></div>
              <div><span>Warehouses</span><strong>{workspaceAdmin?.warehouses?.length || 0}</strong></div>
            </div>
            <p className="muted-text">Make role boundaries obvious so operators understand whether access is tenant-wide, warehouse-specific, or limited to support and integration workflows.</p>
          </article>
        </div>

        <div className="experience-grid experience-grid-split">
          <article className="stack-card section-card">
            <div className="stack-title-row"><strong>Access focus</strong><span className="scenario-type-tag">{selectedLabel}</span></div>
            {selectedAccessSubject ? (
              <div className="signal-list">
                <div className="signal-list-item">
                  <strong>{selectedAccessSubject.subjectType === 'operator' ? selectedAccessSubject.displayName : selectedAccessSubject.fullName}</strong>
                  <p>{selectedAccessSubject.subjectType === 'operator' ? selectedAccessSubject.actorName : selectedAccessSubject.username}</p>
                  <p className="muted-text">
                    {selectedAccessSubject.subjectType === 'operator'
                      ? (selectedAccessSubject.roles.map((role) => formatCodeLabel(role)).join(', ') || 'No roles assigned')
                      : `Operator lane ${selectedAccessSubject.operatorDisplayName || selectedAccessSubject.operatorActorName}`}
                  </p>
                  <p className="muted-text">{selectedAccessSubject.warehouseScopes.length ? `Warehouse scope ${selectedAccessSubject.warehouseScopes.join(', ')}` : 'Tenant-wide lane'}</p>
                  <p className="muted-text">{selectedAccessSubject.active ? 'Active access posture' : 'Inactive access posture'}{selectedAccessSubject.passwordChangeRequired ? ' | Password reset required' : ''}</p>
                </div>
              </div>
            ) : <EmptyState>Select an operator lane or user account to inspect exact scope, status, and access posture.</EmptyState>}
          </article>

          <article className="stack-card section-card">
            <div className="stack-title-row"><strong>Access response posture</strong><span className="scenario-type-tag">{canManageTenantAccess ? 'Admin controlled' : 'Review only'}</span></div>
            <div className="utility-metric-grid">
              <div><span>Reset required</span><strong>{workspaceAdmin?.supportDiagnostics?.activeUsersRequiringPasswordChange || 0}</strong></div>
              <div><span>Rotation overdue</span><strong>{workspaceAdmin?.supportDiagnostics?.activeUsersPastPasswordRotation || 0}</strong></div>
              <div><span>Blocked by lane</span><strong>{workspaceAdmin?.supportDiagnostics?.activeUsersBlockedByInactiveOperator || 0}</strong></div>
              <div><span>Unowned connectors</span><strong>{workspaceAdmin?.supportDiagnostics?.connectorsWithoutSupportOwner || 0}</strong></div>
            </div>
            <div className="history-action-row">
              <button className="ghost-button" onClick={() => navigateToPage('settings')} type="button">Open Settings</button>
              <button className="ghost-button" onClick={() => navigateToPage('profile')} type="button">Open Profile</button>
            </div>
            <p className="muted-text">Empty-state onboarding here should make it clear that company admins add operators first, then users inherit safe workspace access through those operator lanes.</p>
          </article>
        </div>
      </Panel>
    </section>
  )
}
