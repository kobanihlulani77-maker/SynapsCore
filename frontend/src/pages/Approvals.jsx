import Panel from '../components/Panel'
import ScenarioDecisionConsole from '../components/ScenarioDecisionConsole'

export default function ApprovalsPage({ context }) {
  const {
    isAuthenticated,
    isApprovalsPage,
    pendingApprovalScenarios,
    approvedScenarios,
    rejectedScenarios,
    overdueScenarios,
    selectedApprovalScenario,
    setSelectedScenarioId,
    formatCodeLabel,
    formatTimestamp,
    snapshot,
    scenarioDecisionContext,
  } = context

  if (!isAuthenticated || !isApprovalsPage) return null

  return (
    <section className="content-grid approvals-center-grid">
      <Panel wide>
        <div className="panel-header">
          <div><p className="panel-kicker">Approvals center</p><h2>Pending, approved, rejected, and overdue decisions</h2></div>
          <span className="panel-badge scenario-badge">{pendingApprovalScenarios.length}</span>
        </div>
        <div className="approval-status-grid">
          <article className="stack-card">
            <div className="stack-title-row"><strong>Pending</strong><span className="status-tag status-partial">{pendingApprovalScenarios.length}</span></div>
            <div className="stack-list compact-stack-list">
              {pendingApprovalScenarios.slice(0, 4).map((scenario) => (
                <button key={scenario.id} className={`stack-card stack-card-compact selectable-card ${selectedApprovalScenario?.id === scenario.id ? 'is-selected' : ''}`} onClick={() => setSelectedScenarioId(scenario.id)} type="button">
                  <strong>{scenario.title}</strong>
                  <p className="muted-text">{scenario.reviewOwner || 'Review owner'} | {formatCodeLabel(scenario.approvalStage || 'pending_approval')}</p>
                </button>
              ))}
              {!pendingApprovalScenarios.length ? <div className="empty-state">No pending approvals right now.</div> : null}
            </div>
          </article>
          <article className="stack-card">
            <div className="stack-title-row"><strong>Approved</strong><span className="status-tag status-success">{approvedScenarios.length}</span></div>
            <div className="stack-list compact-stack-list">
              {approvedScenarios.slice(0, 4).map((scenario) => (
                <button key={scenario.id} className={`stack-card stack-card-compact selectable-card ${selectedApprovalScenario?.id === scenario.id ? 'is-selected' : ''}`} onClick={() => setSelectedScenarioId(scenario.id)} type="button">
                  <strong>{scenario.title}</strong>
                  <p className="muted-text">{scenario.approvedBy || 'Approved'} | {formatTimestamp(scenario.createdAt)}</p>
                </button>
              ))}
              {!approvedScenarios.length ? <div className="empty-state">Approved plans will appear here.</div> : null}
            </div>
          </article>
          <article className="stack-card">
            <div className="stack-title-row"><strong>Rejected</strong><span className="status-tag status-failure">{rejectedScenarios.length}</span></div>
            <div className="stack-list compact-stack-list">
              {rejectedScenarios.slice(0, 4).map((scenario) => (
                <button key={scenario.id} className={`stack-card stack-card-compact selectable-card ${selectedApprovalScenario?.id === scenario.id ? 'is-selected' : ''}`} onClick={() => setSelectedScenarioId(scenario.id)} type="button">
                  <strong>{scenario.title}</strong>
                  <p className="muted-text">{scenario.rejectedBy || 'Rejected'} | {scenario.rejectionReason || 'Reason recorded in plan history'}</p>
                </button>
              ))}
              {!rejectedScenarios.length ? <div className="empty-state">Rejected plans will appear here.</div> : null}
            </div>
          </article>
          <article className="stack-card">
            <div className="stack-title-row"><strong>Overdue</strong><span className="status-tag status-failure">{overdueScenarios.length}</span></div>
            <div className="stack-list compact-stack-list">
              {overdueScenarios.slice(0, 4).map((scenario) => (
                <button key={scenario.id} className={`stack-card stack-card-compact selectable-card ${selectedApprovalScenario?.id === scenario.id ? 'is-selected' : ''}`} onClick={() => setSelectedScenarioId(scenario.id)} type="button">
                  <strong>{scenario.title}</strong>
                  <p className="muted-text">Due {formatTimestamp(scenario.approvalDueAt)} | {scenario.slaEscalated ? 'Escalated' : 'Awaiting action'}</p>
                </button>
              ))}
              {!overdueScenarios.length ? <div className="empty-state">No overdue approval items.</div> : null}
            </div>
          </article>
        </div>
        <div className="experience-grid experience-grid-split">
          <article className="stack-card section-card">
            <div className="stack-title-row">
              <strong>Approval path focus</strong>
              <span className="scenario-type-tag">{selectedApprovalScenario ? formatCodeLabel(selectedApprovalScenario.approvalStage || selectedApprovalScenario.approvalStatus || 'pending_approval') : 'Clear'}</span>
            </div>
            {selectedApprovalScenario ? (
              <div className="signal-list">
                <div className="signal-list-item">
                  <strong>{selectedApprovalScenario.title}</strong>
                  <p>{selectedApprovalScenario.summary}</p>
                  <p className="muted-text">Requester {selectedApprovalScenario.requestedBy || 'Unknown'}{selectedApprovalScenario.reviewOwner ? ` | Review owner ${selectedApprovalScenario.reviewOwner}` : ''}</p>
                  <p className="muted-text">Final approver {selectedApprovalScenario.finalApprovalOwner || 'Not assigned'}{selectedApprovalScenario.approvalDueAt ? ` | Due ${formatTimestamp(selectedApprovalScenario.approvalDueAt)}` : ''}</p>
                  <p className="muted-text">Approval {formatCodeLabel(selectedApprovalScenario.approvalStatus)}{selectedApprovalScenario.approvalPolicy ? ` | Policy ${formatCodeLabel(selectedApprovalScenario.approvalPolicy)}` : ''}{selectedApprovalScenario.reviewPriority ? ` | ${formatCodeLabel(selectedApprovalScenario.reviewPriority)} priority` : ''}</p>
                  {selectedApprovalScenario.overdue ? <p className="error-text">This approval lane has breached its expected decision timing.</p> : null}
                </div>
              </div>
            ) : <div className="empty-state">When a plan is waiting on approval, its decision path and due pressure will appear here.</div>}
          </article>
          <ScenarioDecisionConsole
            scenario={selectedApprovalScenario}
            title="Approval action console"
            emptyMessage="Select a queued decision to review it, approve it, reject it, or route it back into the planner."
            context={scenarioDecisionContext}
          />
        </div>
        <div className="experience-grid experience-grid-split">
          <article className="stack-card section-card">
            <div className="stack-title-row"><strong>Decision workload</strong><span className="scenario-type-tag">{pendingApprovalScenarios.length + overdueScenarios.length}</span></div>
            <div className="signal-list">
              <div className="signal-list-item">
                <strong>Pending review</strong>
                <p>{pendingApprovalScenarios.length} plans are waiting on review or final approval.</p>
                <p className="muted-text">Use this page to balance volume before the queue turns into escalation pressure.</p>
              </div>
              <div className="signal-list-item">
                <strong>Overdue risk</strong>
                <p>{overdueScenarios.length} plans have breached the expected approval timing.</p>
                <p className="muted-text">{overdueScenarios.filter((scenario) => scenario.slaEscalated).length} are already escalated.</p>
              </div>
            </div>
          </article>
          <article className="stack-card section-card">
            <div className="stack-title-row"><strong>Ops notices</strong><span className="scenario-type-tag">{snapshot.scenarioNotifications.length}</span></div>
            <div className="signal-list">
              {snapshot.scenarioNotifications.length ? snapshot.scenarioNotifications.slice(0, 4).map((notification) => (
                <div key={`${notification.type}-${notification.scenarioRunId}-${notification.createdAt}`} className="signal-list-item">
                  <strong>{notification.title}</strong>
                  <p>{notification.message}</p>
                  <p className="muted-text">{notification.warehouseCode ? `${notification.warehouseCode} | ` : ''}{notification.approvalStage ? `Stage ${formatCodeLabel(notification.approvalStage)} | ` : ''}{notification.actor ? `${notification.actionRequired ? 'Assigned to' : 'Handled by'} ${notification.actor}` : 'Monitoring'}</p>
                  <p className={`muted-text${notification.actionRequired ? ' overdue-text' : ''}`}>{notification.actionRequired && notification.dueAt ? `Due ${formatTimestamp(notification.dueAt)} | ` : ''}{formatTimestamp(notification.createdAt)}</p>
                </div>
              )) : <div className="empty-state">Approval-related notifications appear here when plans are rerouted, accepted, or need faster ownership.</div>}
            </div>
          </article>
        </div>
      </Panel>
    </section>
  )
}
