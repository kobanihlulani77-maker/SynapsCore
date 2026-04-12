import { SummaryCard } from '../components/Card'
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
  } = context

  if (!isAuthenticated || !isUsersPage) return null

  return (
    <section className="content-grid">
      <Panel wide>
        <div className="panel-header">
          <div><p className="panel-kicker">Access control</p><h2>Manage operators, roles, and user access</h2></div>
          <span className="panel-badge integration-badge">{accessAdminOperators.length + accessAdminUsers.length}</span>
        </div>
        <div className="summary-grid compact-summary-grid">
          <SummaryCard label="Operators" value={accessAdminOperators.length} accent="teal" />
          <SummaryCard label="Users" value={accessAdminUsers.length} accent="blue" />
          <SummaryCard label="Warehouses" value={workspaceAdmin?.warehouses?.length || 0} accent="amber" />
          <SummaryCard label="Support incidents" value={workspaceAdmin?.supportIncidents?.length || 0} accent="rose" />
        </div>
        <div className="experience-grid experience-grid-three">
          <article className="stack-card section-card">
            <div className="stack-title-row"><strong>Operator lanes</strong><span className="scenario-type-tag">{accessAdminOperators.length}</span></div>
            <div className="signal-list">
              {accessAdminOperators.length ? accessAdminOperators.slice(0, 5).map((operator) => (
                <button key={operator.id} className={`signal-list-item selectable-card ${selectedAccessSubject?.subjectKey === `operator-${operator.id}` ? 'is-selected' : ''}`} onClick={() => setSelectedAccessSubjectKey(`operator-${operator.id}`)} type="button">
                  <div className="stack-title-row">
                    <strong>{operator.displayName}</strong>
                    <span className={`status-tag ${operator.active ? 'status-success' : 'status-failure'}`}>{operator.active ? 'Active' : 'Inactive'}</span>
                  </div>
                  <p>{operator.actorName}</p>
                  <p className="muted-text">{operator.roles.map((role) => formatCodeLabel(role)).join(', ') || 'No roles assigned'}</p>
                  <p className="muted-text">{operator.warehouseScopes.length ? `Warehouse scope ${operator.warehouseScopes.join(', ')}` : 'Tenant-wide lane'}</p>
                </button>
              )) : <EmptyState>Operator lanes appear here once tenant admins start assigning roles and warehouse scope.</EmptyState>}
            </div>
          </article>
          <article className="stack-card section-card">
            <div className="stack-title-row"><strong>User roster</strong><span className="scenario-type-tag">{accessAdminUsers.length}</span></div>
            <div className="signal-list">
              {accessAdminUsers.length ? accessAdminUsers.slice(0, 5).map((user) => (
                <button key={user.id} className={`signal-list-item selectable-card ${selectedAccessSubject?.subjectKey === `user-${user.id}` ? 'is-selected' : ''}`} onClick={() => setSelectedAccessSubjectKey(`user-${user.id}`)} type="button">
                  <div className="stack-title-row">
                    <strong>{user.fullName}</strong>
                    <span className={`status-tag ${user.active ? 'status-success' : 'status-failure'}`}>{user.active ? 'Enabled' : 'Disabled'}</span>
                  </div>
                  <p>{user.username}</p>
                  <p className="muted-text">Operator lane {user.operatorDisplayName || user.operatorActorName}</p>
                </button>
              )) : <EmptyState>User accounts will appear here once access lifecycle flows are active.</EmptyState>}
            </div>
          </article>
          <article className="stack-card section-card">
            <div className="stack-title-row"><strong>Access posture</strong><span className="scenario-type-tag">{workspaceAdmin?.recentSupportActivity?.length || 0} activity</span></div>
            <div className="utility-metric-grid">
              <div><span>Reset required</span><strong>{workspaceAdmin?.supportDiagnostics?.activeUsersRequiringPasswordChange || 0}</strong></div>
              <div><span>Rotation overdue</span><strong>{workspaceAdmin?.supportDiagnostics?.activeUsersPastPasswordRotation || 0}</strong></div>
              <div><span>Blocked by lane</span><strong>{workspaceAdmin?.supportDiagnostics?.activeUsersBlockedByInactiveOperator || 0}</strong></div>
              <div><span>High severity</span><strong>{workspaceAdmin?.supportDiagnostics?.highSeverityIncidentCount || 0}</strong></div>
            </div>
            <p className="muted-text">Tenant admins can create, scope, deactivate, rotate, and recover access without leaving the workspace.</p>
          </article>
        </div>
        <div className="experience-grid experience-grid-split">
          <article className="stack-card section-card">
            <div className="stack-title-row"><strong>Access focus</strong><span className="scenario-type-tag">{selectedAccessSubject ? formatCodeLabel(selectedAccessSubject.subjectType) : 'Waiting'}</span></div>
            {selectedAccessSubject ? (
              <div className="signal-list">
                <div className="signal-list-item">
                  <strong>{selectedAccessSubject.subjectType === 'operator' ? selectedAccessSubject.displayName : selectedAccessSubject.fullName}</strong>
                  <p>{selectedAccessSubject.subjectType === 'operator' ? selectedAccessSubject.actorName : selectedAccessSubject.username}</p>
                  <p className="muted-text">{selectedAccessSubject.subjectType === 'operator' ? (selectedAccessSubject.roles.map((role) => formatCodeLabel(role)).join(', ') || 'No roles assigned') : `Operator lane ${selectedAccessSubject.operatorDisplayName || selectedAccessSubject.operatorActorName}`}</p>
                  <p className="muted-text">{selectedAccessSubject.warehouseScopes.length ? `Warehouse scope ${selectedAccessSubject.warehouseScopes.join(', ')}` : 'Tenant-wide lane'}</p>
                  <p className="muted-text">{selectedAccessSubject.active ? 'Active access posture' : 'Inactive access posture'}{selectedAccessSubject.passwordChangeRequired ? ' | Password reset required' : ''}</p>
                </div>
              </div>
            ) : <EmptyState>Select an operator lane or user account to inspect the exact scope and access posture.</EmptyState>}
          </article>
          <article className="stack-card section-card">
            <div className="stack-title-row"><strong>Access response posture</strong><span className="scenario-type-tag">{workspaceAdmin?.recentSupportActivity?.length || 0} activity</span></div>
            <div className="utility-metric-grid">
              <div><span>Resets</span><strong>{workspaceAdmin?.supportDiagnostics?.activeUsersRequiringPasswordChange || 0}</strong></div>
              <div><span>Rotation overdue</span><strong>{workspaceAdmin?.supportDiagnostics?.activeUsersPastPasswordRotation || 0}</strong></div>
              <div><span>Blocked</span><strong>{workspaceAdmin?.supportDiagnostics?.activeUsersBlockedByInactiveOperator || 0}</strong></div>
              <div><span>Unowned connectors</span><strong>{workspaceAdmin?.supportDiagnostics?.connectorsWithoutSupportOwner || 0}</strong></div>
            </div>
            <div className="history-action-row">
              <button className="ghost-button" onClick={() => navigateToPage('settings')} type="button">Open Settings</button>
              <button className="ghost-button" onClick={() => navigateToPage('profile')} type="button">Open Profile</button>
            </div>
            <p className="muted-text">This page should help tenant admins understand who can act, where they can act, and which access lanes need cleanup first.</p>
          </article>
        </div>
      </Panel>
    </section>
  )
}

