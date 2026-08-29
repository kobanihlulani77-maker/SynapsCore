import EmptyState from '../components/EmptyState'
import Panel from '../components/Panel'
import { MetricCard } from '../components/Card'
import OperationalGuidance from '../components/OperationalGuidance'

export default function RecommendationsPage({ context }) {
  const {
    isAuthenticated,
    isRecommendationsPage,
    snapshot,
    recommendationNow,
    recommendationSoon,
    recommendationWatch,
    selectedRecommendationId,
    setSelectedRecommendationId,
    formatTimestamp,
    pageLoading,
    pageError,
  } = context

  if (!isAuthenticated || !isRecommendationsPage) {
    return null
  }

  const recommendationCandidates = [...recommendationNow, ...recommendationSoon, ...recommendationWatch]
  const columns = [
    { title: 'Urgent now', items: recommendationNow, tone: 'priority-high', description: 'Immediate operator decisions that should shape the next response.' },
    { title: 'Important soon', items: recommendationSoon, tone: 'priority-medium', description: 'Work to schedule before it becomes active operational risk.' },
    { title: 'Watch', items: recommendationWatch, tone: 'priority-low', description: 'Signals worth monitoring before teams commit new work.' },
  ]
  const selectedRecommendation = recommendationCandidates.find((recommendation) => recommendation.id === selectedRecommendationId) || recommendationCandidates[0]
  const warehouseCoverage = new Set(snapshot.recommendations.map((item) => item.warehouseCode).filter(Boolean)).size
  const evidenceBackedCount = snapshot.recommendations.filter((recommendation) => Boolean(recommendation.policyExplanation)).length
  const recommendationScopeLabel = (recommendation) => recommendation.type === 'TRANSFER_STOCK'
    ? `${recommendation.sourceWarehouseCode || 'Source'} -> ${recommendation.destinationWarehouseCode || recommendation.warehouseCode || 'Destination'}`
    : recommendation.warehouseCode || 'Tenant-wide'
  const recommendationIdentityLabel = (recommendation) => recommendation.productSku
    ? `${recommendation.productSku} | ${recommendationScopeLabel(recommendation)}`
    : recommendationScopeLabel(recommendation)
  const nextDecisionLabel = recommendationNow.length
    ? 'Decide now'
    : recommendationSoon.length
      ? 'Schedule'
      : recommendationWatch.length
        ? 'Monitor'
        : 'Clear'

  return (
    <section className="content-grid">
      <Panel wide id="recommendations-lanes">
        <div className="panel-header">
          <div>
            <p className="panel-kicker">Recommendations center</p>
            <h2>Ranked action queue for operators</h2>
          </div>
          <span className="panel-badge recommendation-badge">{snapshot.recommendations.length}</span>
        </div>

        <div className="workflow-decision-hero recommendation-decision-hero">
          <div className="workflow-decision-copy">
            <strong>Decision intelligence surface</strong>
            <p>
              Recommendations turn live platform intelligence into clear human decisions. The page should show what is
              suggested, why it matters, and what action the operator still owns.
            </p>
            <div className="ops-pill-row">
              <span className="workspace-meta-pill">{nextDecisionLabel}</span>
              <span className="workspace-meta-pill">{evidenceBackedCount} evidence backed</span>
              <span className="workspace-meta-pill">Human decision required</span>
            </div>
          </div>
          <div className="workflow-action-console">
            <div className="workflow-action-card">
              <span>Primary recommendation</span>
              <strong>{selectedRecommendation ? selectedRecommendation.title : 'No action waiting'}</strong>
              <p>{selectedRecommendation ? selectedRecommendation.description : 'The recommendation queue stays quiet until the workspace has useful guidance.'}</p>
            </div>
            <div className="workflow-action-card">
              <span>Operator responsibility</span>
              <strong>Review before action</strong>
              <p>Guidance supports the decision; it does not hide the human approval, scheduling, or execution choice.</p>
            </div>
            <div className="ops-command-actions">
              <button className="secondary-button" onClick={() => recommendationNow[0] && setSelectedRecommendationId(recommendationNow[0].id)} disabled={!recommendationNow[0]} type="button">
                Focus urgent recommendation
              </button>
            </div>
          </div>
        </div>

        <OperationalGuidance
          stateLabel={pageLoading ? 'Loading guidance' : pageError ? 'Unavailable' : 'Decision support'}
          stateTone={pageError ? 'status-failure' : pageLoading ? 'status-partial' : 'status-success'}
          stateDetail={pageLoading ? 'Recommendation signals are still loading.' : pageError ? 'The recommendation read is unavailable; do not treat the visible lanes as empty.' : `${snapshot.recommendations.length} recommendation${snapshot.recommendations.length === 1 ? '' : 's'} are available for human review.`}
          attention={recommendationNow.length ? `${recommendationNow.length} urgent recommendation${recommendationNow.length === 1 ? '' : 's'} need the fastest review.` : recommendationSoon.length ? `${recommendationSoon.length} recommendation${recommendationSoon.length === 1 ? '' : 's'} should be scheduled before they become active pressure.` : 'No urgent recommendation pressure is currently returned.'}
          nextAction={selectedRecommendation ? 'Inspect the supporting signal, decide whether a scenario is needed, and use Approvals before any governed consequence.' : 'Monitor the workspace signals; no automatic action is created by this page.'}
          evidence="Rationale is shown only when the backend provides policy context; otherwise the item remains an operator-review signal."
          role="Recommendations are not decisions and never execute work automatically."
          limitation="Priority is an operating queue signal, not proof that a recommendation is correct or urgent in every source system."
        />

        <div className="summary-grid compact-summary-grid">
          <MetricCard label="Urgent now" value={recommendationNow.length} accent="rose" note="Recommendations that should shape the next operator action immediately." />
          <MetricCard label="Important soon" value={recommendationSoon.length} accent="amber" note="Work that should be queued before it turns into active risk." />
          <MetricCard label="Watch" value={recommendationWatch.length} accent="blue" note="Signals worth monitoring while current operations stay stable." />
          <MetricCard label="Action lanes" value={warehouseCoverage || 'All'} accent="teal" note="Warehouse scope currently touched by recommendation logic." />
        </div>

        <div className="recommendation-board">
          {columns.map((column) => (
            <article key={column.title} className="recommendation-column">
              <div className="stack-title-row">
                <strong>{column.title}</strong>
                <span className={`status-tag ${column.tone}`}>{column.items.length}</span>
              </div>
              <p className="muted-text">{column.description}</p>
              <div className="stack-list compact-stack-list">
                {column.items.length ? column.items.map((recommendation) => (
                  <button
                    key={recommendation.id}
                    className={`stack-card selectable-card recommendation-decision-card ${selectedRecommendation?.id === recommendation.id ? 'is-selected' : ''}`}
                    onClick={() => setSelectedRecommendationId(recommendation.id)}
                    type="button"
                  >
                    <div className="stack-title-row">
                      <strong>{recommendation.title}</strong>
                      <span className={`priority-tag priority-${recommendation.priority.toLowerCase()}`}>{recommendation.priority}</span>
                    </div>
                    <p>{recommendation.description}</p>
                    <p className="muted-text">{recommendationIdentityLabel(recommendation)} | {recommendation.status || 'CURRENT'} | {formatTimestamp(recommendation.updatedAt || recommendation.createdAt)}</p>
                    {recommendation.policyExplanation ? <p className="muted-text recommendation-evidence-line">{recommendation.policyExplanation}</p> : null}
                  </button>
                )) : <EmptyState>No items in this action lane.</EmptyState>}
              </div>
            </article>
          ))}
        </div>

        <div className="experience-grid experience-grid-split">
          <article className="stack-card section-card workflow-selected-panel recommendation-focus-panel" id="recommendations-focus">
            <div className="stack-title-row">
              <strong>Selected recommendation</strong>
              <span className="scenario-type-tag">{selectedRecommendation ? selectedRecommendation.priority : 'Clear'}</span>
            </div>
            {selectedRecommendation ? (
              <div className="signal-list">
                <div className="signal-list-item">
                  <strong>{selectedRecommendation.title}</strong>
                  <p>{selectedRecommendation.description}</p>
                  <p className="muted-text">Created {formatTimestamp(selectedRecommendation.createdAt)}</p>
                  <p className="muted-text">{recommendationIdentityLabel(selectedRecommendation)} | {selectedRecommendation.status || 'CURRENT'} | Priority {selectedRecommendation.priority}</p>
                  {selectedRecommendation.policyExplanation ? <p className="muted-text">Evidence: {selectedRecommendation.policyExplanation}</p> : null}
                </div>
                <div className="utility-metric-grid">
                  <div><span>Decision</span><strong>{selectedRecommendation.priority === 'HIGH' ? 'Act' : selectedRecommendation.priority === 'MEDIUM' ? 'Plan' : 'Watch'}</strong></div>
                  <div><span>Evidence</span><strong>{selectedRecommendation.policyExplanation ? 'Present' : 'Basic'}</strong></div>
                  <div><span>Owner</span><strong>Operator</strong></div>
              <div><span>Automation</span><strong>None</strong></div>
                </div>
              </div>
            ) : (
              <EmptyState>
                When the platform has action guidance, the leading recommendation appears here with its operating context.
              </EmptyState>
            )}
          </article>

          <article className="stack-card section-card">
            <div className="stack-title-row">
              <strong>Recommendation posture</strong>
              <span className="scenario-type-tag">{snapshot.recommendations.length}</span>
            </div>
            <div className="utility-metric-grid">
              <div><span>Urgent now</span><strong>{recommendationNow.length}</strong></div>
              <div><span>Important soon</span><strong>{recommendationSoon.length}</strong></div>
              <div><span>Watch</span><strong>{recommendationWatch.length}</strong></div>
              <div><span>Action lanes</span><strong>{warehouseCoverage || 'All'}</strong></div>
              <div><span>Evidence</span><strong>{evidenceBackedCount}</strong></div>
            </div>
            <p className="muted-text">The best items should feel immediately executable. This page exists to help teams act faster, not just review more data.</p>
          </article>
        </div>
      </Panel>
    </section>
  )
}
