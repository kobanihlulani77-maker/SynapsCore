import { SummaryCard } from '../components/Card'
import Panel from '../components/Panel'
import EmptyState from '../components/EmptyState'
import ScenarioDecisionConsole from '../components/ScenarioDecisionConsole'

export default function ScenarioHistoryPage({ context }) {
  const {
    isAuthenticated,
    isScenarioHistoryPage,
    scenarioHistoryItems,
    selectedHistoryScenario,
    setSelectedScenarioId,
    formatCodeLabel,
    formatTimestamp,
    scenarioDecisionContext,
  } = context

  if (!isAuthenticated || !isScenarioHistoryPage) return null

  const executableScenarios = scenarioHistoryItems.filter((scenario) => scenario.executable).slice(0, 4)
  const revisionScenarios = scenarioHistoryItems.filter((scenario) => scenario.revisionNumber).slice(0, 4)

  return (
    <section className="content-grid">
      <Panel wide>
        <div className="panel-header">
          <div><p className="panel-kicker">Scenario history</p><h2>Saved plans, revisions, and compare posture</h2></div>
          <span className="panel-badge scenario-badge">{scenarioHistoryItems.length}</span>
        </div>
        <div className="summary-grid compact-summary-grid">
          <SummaryCard label="Saved plans" value={scenarioHistoryItems.filter((scenario) => scenario.type === 'SAVED_PLAN').length} accent="blue" />
          <SummaryCard label="Comparisons" value={scenarioHistoryItems.filter((scenario) => scenario.type === 'COMPARISON').length} accent="teal" />
          <SummaryCard label="Revisions" value={scenarioHistoryItems.filter((scenario) => scenario.revisionNumber).length} accent="amber" />
          <SummaryCard label="Executable" value={scenarioHistoryItems.filter((scenario) => scenario.executable).length} accent="orange" />
        </div>
        <div className="approval-board">
          {scenarioHistoryItems.slice(0, 6).map((scenario) => (
            <button key={scenario.id} className={`stack-card selectable-card ${selectedHistoryScenario?.id === scenario.id ? 'is-selected' : ''}`} onClick={() => setSelectedScenarioId(scenario.id)} type="button">
              <div className="stack-title-row">
                <strong>{scenario.title}</strong>
                <div className="stack-tag-row">
                  <span className="scenario-type-tag">{formatCodeLabel(scenario.type)}</span>
                  {scenario.revisionNumber ? <span className="status-tag status-partial">Rev {scenario.revisionNumber}</span> : null}
                </div>
              </div>
              <p>{scenario.summary}</p>
              <p className="muted-text">{scenario.recommendedOption || 'No recommended option'} | {formatTimestamp(scenario.createdAt)}</p>
            </button>
          ))}
          {!scenarioHistoryItems.length ? <EmptyState>Scenario history will fill up after planners start previewing and saving alternative operating paths.</EmptyState> : null}
        </div>
        <div className="experience-grid experience-grid-split">
          <article className="stack-card section-card">
            <div className="stack-title-row"><strong>Selected scenario memory</strong><span className="scenario-type-tag">{selectedHistoryScenario ? formatCodeLabel(selectedHistoryScenario.type) : 'Waiting'}</span></div>
            {selectedHistoryScenario ? (
              <div className="signal-list">
                <div className="signal-list-item">
                  <strong>{selectedHistoryScenario.title}</strong>
                  <p>{selectedHistoryScenario.summary}</p>
                  <p className="muted-text">{selectedHistoryScenario.warehouseCode ? `${selectedHistoryScenario.warehouseCode} | ` : ''}{selectedHistoryScenario.recommendedOption || 'No recommended option'}</p>
                  <p className="muted-text">Approval {formatCodeLabel(selectedHistoryScenario.approvalStatus)}{selectedHistoryScenario.approvalPolicy ? ` | Policy ${formatCodeLabel(selectedHistoryScenario.approvalPolicy)}` : ''}{selectedHistoryScenario.approvalStage ? ` | Stage ${formatCodeLabel(selectedHistoryScenario.approvalStage)}` : ''}</p>
                  <p className="muted-text">{selectedHistoryScenario.requestedBy ? `Requested by ${selectedHistoryScenario.requestedBy}` : 'Requester pending'}{selectedHistoryScenario.reviewOwner ? ` | Review owner ${selectedHistoryScenario.reviewOwner}` : ''}{selectedHistoryScenario.finalApprovalOwner ? ` | Final approver ${selectedHistoryScenario.finalApprovalOwner}` : ''}</p>
                  {selectedHistoryScenario.approvalDueAt ? <p className={`muted-text${selectedHistoryScenario.overdue ? ' overdue-text' : ''}`}>Due {formatTimestamp(selectedHistoryScenario.approvalDueAt)}</p> : null}
                  {selectedHistoryScenario.revisionNumber ? <p className="muted-text">Revision {selectedHistoryScenario.revisionNumber}{selectedHistoryScenario.revisionOfScenarioRunId ? ` | Based on ${selectedHistoryScenario.revisionOfScenarioRunId}` : ''}</p> : null}
                  {selectedHistoryScenario.approvalNote ? <p className="muted-text">Approval note: {selectedHistoryScenario.approvalNote}</p> : null}
                  {selectedHistoryScenario.rejectionReason ? <p className="muted-text">Review note: {selectedHistoryScenario.rejectionReason}</p> : null}
                </div>
              </div>
            ) : <EmptyState>Select a saved plan or revision to inspect its decision memory and next action posture.</EmptyState>}
          </article>
          <ScenarioDecisionConsole
            scenario={selectedHistoryScenario}
            title="Scenario action console"
            emptyMessage="Choose a scenario run to load it into the planner, approve it, reject it, or push it into execution."
            context={scenarioDecisionContext}
          />
        </div>
        <div className="experience-grid experience-grid-split">
          <article className="stack-card section-card">
            <div className="stack-title-row"><strong>Execution-ready plans</strong><span className="scenario-type-tag">{executableScenarios.length}</span></div>
            <div className="signal-list">
              {executableScenarios.length ? executableScenarios.map((scenario) => (
                <div key={scenario.id} className="signal-list-item">
                  <strong>{scenario.title}</strong>
                  <p>{scenario.summary}</p>
                  <p className="muted-text">{scenario.reviewOwner || 'No review owner'} | {scenario.approvedBy || 'Pending execution'}</p>
                </div>
              )) : <EmptyState>Executable plans appear here once approved scenarios are ready to be pushed into the live flow.</EmptyState>}
            </div>
          </article>
          <article className="stack-card section-card">
            <div className="stack-title-row"><strong>Revision memory</strong><span className="scenario-type-tag">{revisionScenarios.length}</span></div>
            <div className="signal-list">
              {revisionScenarios.length ? revisionScenarios.map((scenario) => (
                <div key={scenario.id} className="signal-list-item">
                  <strong>{scenario.title}</strong>
                  <p>{scenario.recommendedOption || 'Decision path recorded in scenario history.'}</p>
                  <p className="muted-text">Rev {scenario.revisionNumber} | {formatTimestamp(scenario.createdAt)}</p>
                </div>
              )) : <EmptyState>Revisions show how operators refined plans before final execution.</EmptyState>}
            </div>
          </article>
        </div>
      </Panel>
    </section>
  )
}
