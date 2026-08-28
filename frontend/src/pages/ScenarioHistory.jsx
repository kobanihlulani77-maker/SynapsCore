import { MetricCard } from '../components/Card'
import Panel from '../components/Panel'
import EmptyState from '../components/EmptyState'
import ScenarioDecisionConsole from '../components/ScenarioDecisionConsole'
import OperationalGuidance from '../components/OperationalGuidance'

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
    pageLoading,
    pageError,
  } = context

  if (!isAuthenticated || !isScenarioHistoryPage) return null

  const approvedDecisionScenarios = scenarioHistoryItems.filter((scenario) => scenario.approvalStatus === 'APPROVED').slice(0, 4)
  const revisionScenarios = scenarioHistoryItems.filter((scenario) => scenario.revisionNumber).slice(0, 4)
  const savedPlans = scenarioHistoryItems.filter((scenario) => scenario.type === 'SAVED_PLAN').length
  const comparisons = scenarioHistoryItems.filter((scenario) => scenario.type === 'COMPARISON').length
  const revisions = scenarioHistoryItems.filter((scenario) => scenario.revisionNumber).length
  const approvedDecisionCount = scenarioHistoryItems.filter((scenario) => scenario.approvalStatus === 'APPROVED').length

  return (
    <section className="content-grid">
      <Panel wide id="scenario-history-evidence">
        <div className="panel-header">
          <div>
            <p className="panel-kicker">Scenario history</p>
            <h2>Saved plans, revisions, and compare posture</h2>
          </div>
          <span className="panel-badge scenario-badge">{scenarioHistoryItems.length}</span>
        </div>

        <div className="workflow-decision-hero scenario-history-hero">
          <div className="workflow-decision-copy">
            <p className="panel-kicker">Decision memory</p>
            <div className="runtime-decision-title">
              <span className={`runtime-decision-badge ${approvedDecisionCount ? 'status-partial' : 'status-success'}`}>{approvedDecisionCount ? 'GOVERNED' : 'TRACEABLE'}</span>
              <h2>{selectedHistoryScenario ? selectedHistoryScenario.title : 'Select a scenario to inspect evidence.'}</h2>
            </div>
            <p>{selectedHistoryScenario ? selectedHistoryScenario.summary : 'Saved plans, revisions, approvals, and external handoff posture remain traceable here.'}</p>
            <div className="ops-pill-row">
              <span className="workspace-meta-pill">Saved {savedPlans}</span>
              <span className="workspace-meta-pill">Revisions {revisions}</span>
              <span className="workspace-meta-pill">Approved {approvedDecisionCount}</span>
            </div>
          </div>
          <div className="workflow-action-console">
            <div className="workflow-action-card">
              <span>Current scenario state</span>
              <strong>{selectedHistoryScenario ? formatCodeLabel(selectedHistoryScenario.approvalStatus || selectedHistoryScenario.type) : 'Waiting'}</strong>
            <p>{selectedHistoryScenario?.approvalStatus === 'APPROVED' ? 'Governance is complete. The approved decision is ready for external operational follow-through.' : 'History remains evidence-first unless a current governance action is available.'}</p>
            </div>
            <button className="secondary-button" onClick={() => approvedDecisionScenarios[0] && setSelectedScenarioId(approvedDecisionScenarios[0].id)} disabled={!approvedDecisionScenarios[0]} type="button">
              Focus approved decision
            </button>
          </div>
        </div>

        <OperationalGuidance
          stateLabel={pageLoading ? 'Loading history' : pageError ? 'Unavailable' : approvedDecisionCount ? 'Governed evidence' : 'Historical evidence'}
          stateTone={pageError ? 'status-failure' : pageLoading ? 'status-partial' : approvedDecisionCount ? 'status-partial' : 'status-success'}
          stateDetail={pageLoading ? 'Scenario history is still loading.' : pageError ? 'The history read is unavailable; do not interpret the visible list as empty.' : `${scenarioHistoryItems.length} scenario record${scenarioHistoryItems.length === 1 ? '' : 's'} are available for traceability.`}
          attention={approvedDecisionCount ? `${approvedDecisionCount} approved decision${approvedDecisionCount === 1 ? '' : 's'} are ready for external operational follow-through.` : 'Historical records are evidence until a current governance action is available.'}
          nextAction={selectedHistoryScenario ? 'Inspect the selected record, then use the current scenario or approval surface for any governed action.' : 'Select a saved plan or revision to inspect its evidence trail.'}
          evidence="History preserves type, timing, warehouse, approval stage, ownership, revision, and external handoff posture when the backend provides them."
          role="Historical visibility does not grant approval authority or external operational control."
          limitation="Do not treat a historical row as a current action control; PREVIEW records remain analysis-only."
        />

        <div className="summary-grid compact-summary-grid">
          <MetricCard label="Saved plans" value={savedPlans} accent="blue" note="Scenario plans currently retained for comparison and approval." />
          <MetricCard label="Comparisons" value={comparisons} accent="teal" note="Alternative scenario comparisons generated for operator review." />
          <MetricCard label="Revisions" value={revisions} accent="amber" note="Revision memory showing how teams refined the plan before go-live." />
          <MetricCard label="Approved decisions" value={approvedDecisionCount} accent="orange" note="Plans with governance complete and no internal execution path." />
        </div>

        <div className="approval-board scenario-history-board">
          {scenarioHistoryItems.slice(0, 6).map((scenario) => (
            <button
              key={scenario.id}
              className={`stack-card selectable-card ${selectedHistoryScenario?.id === scenario.id ? 'is-selected' : ''}`}
              onClick={() => setSelectedScenarioId(scenario.id)}
              type="button"
            >
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

        <div className="experience-grid scenario-history-workbench">
          <article className="stack-card section-card workflow-selected-panel">
            <div className="stack-title-row">
              <strong>Selected scenario memory</strong>
              <span className="scenario-type-tag">{selectedHistoryScenario ? formatCodeLabel(selectedHistoryScenario.type) : 'Waiting'}</span>
            </div>
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
            emptyMessage="Choose a scenario run to load it into the planner, approve it, reject it, or inspect its external handoff posture."
            context={scenarioDecisionContext}
          />
        </div>

        <div className="experience-grid experience-grid-split">
          <article className="stack-card section-card">
            <div className="stack-title-row">
              <strong>Approved decisions</strong>
              <span className="scenario-type-tag">{approvedDecisionScenarios.length}</span>
            </div>
            <div className="signal-list">
              {approvedDecisionScenarios.length ? approvedDecisionScenarios.map((scenario) => (
                <div key={scenario.id} className="signal-list-item">
                  <strong>{scenario.title}</strong>
                  <p>{scenario.summary}</p>
                  <p className="muted-text">{scenario.reviewOwner || 'No review owner'} | {scenario.approvedBy || 'Governance complete'}</p>
                </div>
              )) : <EmptyState>Approved decisions appear here with their governance history; operational execution remains external.</EmptyState>}
            </div>
          </article>

          <article className="stack-card section-card">
            <div className="stack-title-row">
              <strong>Revision memory</strong>
              <span className="scenario-type-tag">{revisionScenarios.length}</span>
            </div>
            <div className="signal-list">
              {revisionScenarios.length ? revisionScenarios.map((scenario) => (
                <div key={scenario.id} className="signal-list-item">
                  <strong>{scenario.title}</strong>
                  <p>{scenario.recommendedOption || 'Decision path recorded in scenario history.'}</p>
                  <p className="muted-text">Rev {scenario.revisionNumber} | {formatTimestamp(scenario.createdAt)}</p>
                </div>
              )) : <EmptyState>Revisions show how operators refined plans before external operational follow-through.</EmptyState>}
            </div>
          </article>
        </div>
      </Panel>
    </section>
  )
}
