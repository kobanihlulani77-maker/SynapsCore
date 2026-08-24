import { MetricCard } from '../components/Card'
import Panel from '../components/Panel'
import EmptyState from '../components/EmptyState'
import LoadingState from '../components/LoadingState'
import OperationalGuidance from '../components/OperationalGuidance'

const roleDefinitions = [
  ['TENANT_ADMIN', 'Tenant workspace access, user, role, and scope administration.'],
  ['REVIEW_OWNER', 'Reviews assigned operational decisions before final approval.'],
  ['FINAL_APPROVER', 'Provides the final governance decision for an eligible plan.'],
  ['ESCALATION_OWNER', 'Owns escalation acknowledgement and follow-up, not approval.'],
  ['INTEGRATION_ADMIN', 'Manages supported connector and replay operations.'],
  ['INTEGRATION_OPERATOR', 'Works supported integration and recovery tasks within assigned scope.'],
]

export default function UsersPage({ context }) {
  const {
    isAuthenticated,
    isUsersPage,
    accessAdminOperators,
    accessAdminUsers,
    accessAdminState,
    workspaceAdmin,
    selectedAccessSubject,
    setSelectedAccessSubjectKey,
    formatCodeLabel,
    navigateToPage,
    canManageTenantAccess,
    pageError,
  } = context

  if (!isAuthenticated || !isUsersPage) return null

  const adminOperators = accessAdminOperators.filter((operator) => operator.roles.includes('TENANT_ADMIN')).length
  const integrationOperators = accessAdminOperators.filter((operator) => operator.roles.some((role) => role.startsWith('INTEGRATION_'))).length
  const reviewOperators = accessAdminOperators.filter((operator) => operator.roles.includes('REVIEW_OWNER') || operator.roles.includes('FINAL_APPROVER')).length
  const disabledAccounts = accessAdminUsers.filter((user) => !user.active).length
  const inactiveOperators = accessAdminOperators.filter((operator) => !operator.active).length
  const elevatedOperators = accessAdminOperators.filter((operator) => operator.roles.includes('TENANT_ADMIN') || operator.roles.includes('FINAL_APPROVER'))
  const accessReviewCount = disabledAccounts
    + inactiveOperators
    + (workspaceAdmin?.supportDiagnostics?.activeUsersRequiringPasswordChange || 0)
    + (workspaceAdmin?.supportDiagnostics?.activeUsersBlockedByInactiveOperator || 0)
  const selectedWarehouseScopes = selectedAccessSubject?.warehouseScopes || []
  const selectedLabel = selectedAccessSubject
    ? selectedAccessSubject.subjectType === 'operator'
      ? 'Operator lane'
      : 'User account'
    : 'Waiting'
  const accessDataError = accessAdminState.error || pageError
  const accessDataLoading = accessAdminState.loading

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

        <OperationalGuidance
          stateLabel={accessDataLoading ? 'Loading' : accessDataError ? 'Unavailable' : 'Live roster'}
          stateTone={accessDataLoading ? 'status-partial' : accessDataError ? 'status-failure' : 'status-success'}
          stateDetail={accessDataLoading ? 'User accounts, operator lanes, and workspace scope are still being loaded.' : accessDataError ? 'The access administration read failed; the visible roster must not be treated as complete.' : 'The roster separates sign-in identities from the operator lanes that carry roles and warehouse scope.'}
          attention={accessDataError ? accessDataError : accessReviewCount ? `${accessReviewCount} access condition${accessReviewCount === 1 ? '' : 's'} need Tenant Admin review.` : 'No disabled, inactive, password, or linked-lane review signal is currently reported.'}
          nextAction={canManageTenantAccess ? 'Before a material role or scope change, record the intended state, save it, then verify the persisted role, scope, and fresh-session behavior.' : 'Use Profile for personal account actions and ask a Tenant Admin to change roles or warehouse scope.'}
          evidence="Backend access administration remains authoritative. An empty warehouse-scope list means tenant-wide authority, not no access."
          role="Tenant Admin controls this surface; other tenant roles must not use it as an administration path."
          limitation="This page does not grant platform-owner authority, change tenant provisioning, or revoke a session unless the backend confirms that behavior."
        />

        <div className="workflow-decision-hero admin-trust-hero">
          <div className="workflow-decision-copy">
            <strong>Operator access control surface</strong>
            <p>
              Users authenticate into the workspace. Operator lanes define what operational work a person can perform.
              Keep those two concepts visible before changing access.
            </p>
            <div className="ops-pill-row">
              <span className="workspace-meta-pill">{accessReviewCount} review signals</span>
              <span className="workspace-meta-pill">{adminOperators} tenant admins</span>
              <span className="workspace-meta-pill">{canManageTenantAccess ? 'Admin editing enabled' : 'Read-only access'}</span>
            </div>
          </div>
          <div className="workflow-action-console">
            <div className="workflow-action-card">
              <span>User account</span>
              <strong>Can sign in</strong>
              <p>Account status, password posture, and linked operator lane determine whether access is usable.</p>
            </div>
            <div className="workflow-action-card">
              <span>Operator lane</span>
              <strong>Can act operationally</strong>
              <p>Roles and warehouse scope determine what work the signed-in person can perform.</p>
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
        </div>

        <div className="summary-grid compact-summary-grid">
          <MetricCard label="Operators" value={accessAdminOperators.length} accent="teal" note="Operational actors that carry company roles and warehouse scope." />
          <MetricCard label="User accounts" value={accessAdminUsers.length} accent="blue" note="Workspace sign-in identities linked to operator lanes." />
          <MetricCard label="Tenant admins" value={adminOperators} accent="amber" note="Admins currently able to manage workspace policy and access." />
          <MetricCard label="Review signals" value={accessReviewCount} accent="rose" note="Disabled, inactive, password, or linked-lane access conditions that deserve admin review." />
        </div>

        <div className="experience-grid experience-grid-three">
          <article className="stack-card section-card admin-list-panel">
            <div className="stack-title-row"><strong>Operator lanes</strong><span className="scenario-type-tag">{accessAdminOperators.length}</span></div>
            <p className="muted-text">Operational identities, roles, and warehouse scope. These lanes explain who can act.</p>
            <div className="signal-list">
              {accessDataLoading ? <LoadingState label="Loading operator lanes..." /> : accessDataError ? <p className="error-text">Operator lanes are unavailable because the access administration read failed.</p> : accessAdminOperators.length ? accessAdminOperators.slice(0, 5).map((operator) => (
                <button key={operator.id} className={`signal-list-item selectable-card system-select-card admin-subject-card ${selectedAccessSubject?.subjectKey === `operator-${operator.id}` ? 'is-selected' : ''}`} onClick={() => setSelectedAccessSubjectKey(`operator-${operator.id}`)} type="button">
                  <div className="stack-title-row">
                    <strong>{operator.displayName}</strong>
                    <span className={`status-tag ${operator.active ? 'status-success' : 'status-failure'}`}>{operator.active ? 'Active' : 'Inactive'}</span>
                  </div>
                  <p>{operator.actorName}</p>
                  <p className="muted-text">{operator.roles.map((role) => formatCodeLabel(role)).join(', ') || 'No roles assigned'}</p>
                  <p className="muted-text">{operator.warehouseScopes.length ? `Warehouse scope ${operator.warehouseScopes.join(', ')}` : 'Tenant-wide lane'}</p>
                  <div className="attention-card-meta">
                    <span>{operator.roles.includes('TENANT_ADMIN') ? 'High impact role' : 'Operational role'}</span>
                    <span>{operator.active ? 'Can act' : 'Action blocked'}</span>
                  </div>
                </button>
              )) : <EmptyState>Operator lanes appear here once company admins start assigning roles and warehouse scope.</EmptyState>}
            </div>
          </article>

          <article className="stack-card section-card admin-list-panel">
            <div className="stack-title-row"><strong>User roster</strong><span className="scenario-type-tag">{accessAdminUsers.length}</span></div>
            <p className="muted-text">Sign-in identities linked to operator lanes. These accounts explain who can authenticate.</p>
            <div className="signal-list">
              {accessDataLoading ? <LoadingState label="Loading user accounts..." /> : accessDataError ? <p className="error-text">User accounts are unavailable because the access administration read failed.</p> : accessAdminUsers.length ? accessAdminUsers.slice(0, 5).map((user) => (
                <button key={user.id} className={`signal-list-item selectable-card system-select-card admin-subject-card ${selectedAccessSubject?.subjectKey === `user-${user.id}` ? 'is-selected' : ''}`} onClick={() => setSelectedAccessSubjectKey(`user-${user.id}`)} type="button">
                  <div className="stack-title-row">
                    <strong>{user.fullName}</strong>
                    <span className={`status-tag ${user.active ? 'status-success' : 'status-failure'}`}>{user.active ? 'Enabled' : 'Disabled'}</span>
                  </div>
                  <p>{user.username}</p>
                  <p className="muted-text">Operator lane {user.operatorDisplayName || user.operatorActorName}</p>
                  <div className="attention-card-meta">
                    <span>{user.passwordChangeRequired ? 'Password change required' : 'Password policy current'}</span>
                    <span>{user.active ? 'Can sign in' : 'Sign-in blocked'}</span>
                  </div>
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
              <div><span>Disabled users</span><strong>{disabledAccounts}</strong></div>
              <div><span>Inactive lanes</span><strong>{inactiveOperators}</strong></div>
            </div>
            <p className="muted-text">Make role boundaries obvious so operators understand whether access is tenant-wide, warehouse-specific, or limited to support and integration workflows.</p>
          </article>
        </div>

        <article className="stack-card section-card">
          <div className="stack-title-row"><strong>Role lanes</strong><span className="scenario-type-tag">Six tenant roles</span></div>
          <p className="muted-text">A role describes the operational responsibility a person carries. It does not make that person a Platform Owner or bypass warehouse scope.</p>
          <div className="signal-list">
            {roleDefinitions.map(([role, description]) => (
              <div key={role} className="signal-list-item">
                <strong>{formatCodeLabel(role)}</strong>
                <p>{description}</p>
              </div>
            ))}
          </div>
        </article>

        <div className="experience-grid experience-grid-split">
          <article className="stack-card section-card workflow-selected-panel admin-focus-panel">
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
                  <p className="muted-text">{selectedWarehouseScopes.length ? `Warehouse scope ${selectedWarehouseScopes.join(', ')}` : 'Tenant-wide lane'}</p>
                  <p className="muted-text">{selectedAccessSubject.active ? 'Active access posture' : 'Inactive access posture'}{selectedAccessSubject.passwordChangeRequired ? ' | Password reset required' : ''}</p>
                </div>
                <div className="utility-metric-grid">
                  <div><span>Signs in</span><strong>{selectedAccessSubject.subjectType === 'user' && selectedAccessSubject.active ? 'Yes' : selectedAccessSubject.subjectType === 'user' ? 'No' : 'N/A'}</strong></div>
                  <div><span>Can act</span><strong>{selectedAccessSubject.subjectType === 'operator' && selectedAccessSubject.active ? 'Yes' : selectedAccessSubject.subjectType === 'operator' ? 'No' : 'Linked'}</strong></div>
                  <div><span>Scope</span><strong>{selectedWarehouseScopes.length || 'All'}</strong></div>
                  <div><span>Impact</span><strong>{selectedAccessSubject.roles?.includes?.('TENANT_ADMIN') ? 'High' : 'Standard'}</strong></div>
                </div>
              </div>
            ) : <EmptyState>Select an operator lane or user account to inspect exact scope, status, and access posture.</EmptyState>}
          </article>

          <article className="stack-card section-card">
            <div className="stack-title-row"><strong>Users requiring review</strong><span className="scenario-type-tag">{canManageTenantAccess ? 'Admin controlled' : 'Review only'}</span></div>
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
            <div className="signal-list">
              {elevatedOperators.slice(0, 3).map((operator) => (
                <div key={operator.id} className="signal-list-item">
                  <strong>{operator.displayName}</strong>
                  <p>{operator.roles.map((role) => formatCodeLabel(role)).join(', ')}</p>
                  <p className="muted-text">High-impact access. Review owner, approver, and tenant-admin roles before pilot rollout.</p>
                </div>
              ))}
              {!elevatedOperators.length ? <EmptyState>No high-impact operator roles are visible in the current workspace data.</EmptyState> : null}
            </div>
          </article>
        </div>
      </Panel>
    </section>
  )
}
