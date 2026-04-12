import { SummaryCard } from '../components/Card'
import Panel from '../components/Panel'
import EmptyState from '../components/EmptyState'

export default function ScenarioControlPage({ context }) {
  const {
    isAuthenticated,
    isScenariosPage,
    isScenarioHistoryPage,
    isApprovalsPage,
    isEscalationsPage,
    scenarioHistoryItems,
    pendingApprovalScenarios,
    approvedScenarios,
    rejectedScenarios,
    overdueScenarios,
    approvalBoard,
    formatCodeLabel,
  } = context

  if (!isAuthenticated || !(isScenariosPage || isScenarioHistoryPage || isApprovalsPage || isEscalationsPage)) {
    return null
  }

  return (
    <section className="content-grid">
      <Panel wide>
        <div className="panel-header">
          <div><p className="panel-kicker">Control center</p><h2>Planning, approvals, and escalations</h2></div>
          <span className="panel-badge scenario-badge">{scenarioHistoryItems.length}</span>
        </div>
        <div className="summary-grid compact-summary-grid">
          <SummaryCard label="Pending approvals" value={pendingApprovalScenarios.length} accent="amber" />
          <SummaryCard label="Approved" value={approvedScenarios.length} accent="teal" />
          <SummaryCard label="Rejected" value={rejectedScenarios.length} accent="rose" />
          <SummaryCard label="Overdue" value={overdueScenarios.length} accent="orange" />
        </div>
        <div className="approval-board">
          {approvalBoard.length ? approvalBoard.map((scenario) => (
            <div key={scenario.id} className="stack-card">
              <div className="stack-title-row">
                <strong>{scenario.title}</strong>
                <div className="stack-tag-row">
                  {scenario.reviewPriority ? <span className={`priority-tag priority-${scenario.reviewPriority.toLowerCase()}`}>{formatCodeLabel(scenario.reviewPriority)}</span> : null}
                  {scenario.overdue ? <span className="status-tag status-failure">Overdue</span> : null}
                </div>
              </div>
              <p>{scenario.summary}</p>
              <p className="muted-text">{scenario.warehouseCode || 'Tenant-wide'} | {formatCodeLabel(scenario.approvalStage || 'not_required')} | Review owner {scenario.reviewOwner || 'Unassigned'}</p>
            </div>
          )) : <EmptyState>Saved plans and approval queues will appear here once operators start routing decisions.</EmptyState>}
        </div>
      </Panel>
    </section>
  )
}
