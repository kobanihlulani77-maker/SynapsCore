import Panel from '../components/Panel'
import EmptyState from '../components/EmptyState'
import ScenarioDecisionConsole from '../components/ScenarioDecisionConsole'

export default function EscalationsPage({ context }) {
  const {
    isAuthenticated,
    isEscalationsPage,
    snapshot,
    systemIncidents,
    escalatedScenarios,
    selectedEscalationScenario,
    setSelectedScenarioId,
    formatCodeLabel,
    formatTimestamp,
    getIncidentStatusClassName,
    scenarioDecisionContext,
  } = context

  if (!isAuthenticated || !isEscalationsPage) return null

  return (
    <section className="content-grid">
      <Panel wide>
        <div className="panel-header">
          <div><p className="panel-kicker">Escalation inbox</p><h2>Urgent operational items needing ownership</h2></div>
          <span className="panel-badge notification-badge">{snapshot.slaEscalations.length + systemIncidents.length}</span>
        </div>
        <div className="approval-board">
          {escalatedScenarios.map((scenario) => (
            <button key={scenario.id} className={`stack-card selectable-card ${selectedEscalationScenario?.id === scenario.id ? 'is-selected' : ''}`} onClick={() => setSelectedScenarioId(scenario.id)} type="button">
              <div className="stack-title-row">
                <strong>{scenario.title}</strong>
                <div className="stack-tag-row">
                  <span className="policy-tag policy-escalated">SLA Escalated</span>
                  {scenario.reviewPriority ? <span className={`priority-tag priority-${scenario.reviewPriority.toLowerCase()}`}>{formatCodeLabel(scenario.reviewPriority)}</span> : null}
                </div>
              </div>
              <p>{scenario.summary}</p>
              <p className="muted-text">Stage {formatCodeLabel(scenario.approvalStage)} | Final approver {scenario.finalApprovalOwner || 'Monitoring'}</p>
            </button>
          ))}
          {systemIncidents.slice(0, 4).map((incident) => (
            <div key={incident.incidentKey} className="stack-card">
              <div className="stack-title-row">
                <strong>{incident.title}</strong>
                <span className={`status-tag ${getIncidentStatusClassName(incident.severity)}`}>{formatCodeLabel(incident.severity)}</span>
              </div>
              <p>{incident.detail}</p>
              <p className="muted-text">{incident.context}</p>
            </div>
          ))}
          {!snapshot.slaEscalations.length && !systemIncidents.length ? <EmptyState>No escalations are active right now. This page becomes the operational inbox when SLA pressure or trust incidents need faster ownership.</EmptyState> : null}
        </div>
        <div className="experience-grid experience-grid-split">
          <article className="stack-card section-card">
            <div className="stack-title-row"><strong>Escalation focus</strong><span className="scenario-type-tag">{selectedEscalationScenario ? formatCodeLabel(selectedEscalationScenario.approvalStage || 'pending_approval') : 'Clear'}</span></div>
            {selectedEscalationScenario ? (
              <div className="signal-list">
                <div className="signal-list-item">
                  <strong>{selectedEscalationScenario.title}</strong>
                  <p>{selectedEscalationScenario.summary}</p>
                  <p className="muted-text">Escalated to {selectedEscalationScenario.slaEscalatedTo || 'Monitoring'}{selectedEscalationScenario.slaEscalatedAt ? ` | ${formatTimestamp(selectedEscalationScenario.slaEscalatedAt)}` : ''}</p>
                  <p className="muted-text">Review owner {selectedEscalationScenario.reviewOwner || 'Unassigned'}{selectedEscalationScenario.finalApprovalOwner ? ` | Final approver ${selectedEscalationScenario.finalApprovalOwner}` : ''}</p>
                  <p className={`muted-text${selectedEscalationScenario.overdue ? ' overdue-text' : ''}`}>Due {formatTimestamp(selectedEscalationScenario.approvalDueAt)}{selectedEscalationScenario.slaAcknowledged ? ` | Acknowledged by ${selectedEscalationScenario.slaAcknowledgedBy}` : ' | Waiting on ownership'}</p>
                  {selectedEscalationScenario.slaAcknowledgementNote ? <p className="muted-text">Acknowledgement note: {selectedEscalationScenario.slaAcknowledgementNote}</p> : null}
                </div>
              </div>
            ) : <EmptyState>No approval lanes are currently in escalation. This page is ready when SLA pressure starts forming.</EmptyState>}
          </article>
          <ScenarioDecisionConsole
            scenario={selectedEscalationScenario}
            title="Escalation action console"
            emptyMessage="Select an escalated plan to acknowledge it, reject it, or move it toward final approval."
            context={scenarioDecisionContext}
          />
        </div>
      </Panel>
    </section>
  )
}
