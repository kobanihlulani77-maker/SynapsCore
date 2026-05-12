import EmptyState from '../components/EmptyState'
import Panel from '../components/Panel'
import { MetricCard } from '../components/Card'

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
  } = context

  if (!isAuthenticated || !isRecommendationsPage) {
    return null
  }

  const recommendationCandidates = [...recommendationNow, ...recommendationSoon, ...recommendationWatch]
  const columns = [
    { title: 'Urgent now', items: recommendationNow, tone: 'priority-high', description: 'Immediate operational action lanes.' },
    { title: 'Important soon', items: recommendationSoon, tone: 'priority-medium', description: 'Items that should be scheduled before they become urgent.' },
    { title: 'Watch', items: recommendationWatch, tone: 'priority-low', description: 'Signals worth watching before teams commit work.' },
  ]
  const selectedRecommendation = recommendationCandidates.find((recommendation) => recommendation.id === selectedRecommendationId) || recommendationCandidates[0]
  const warehouseCoverage = new Set(snapshot.recommendations.map((item) => item.warehouseCode).filter(Boolean)).size

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

        <div className="ops-command-hero">
          <div className="ops-command-copy">
            <strong>Decision intelligence surface</strong>
            <p>
              Recommendations should turn live platform intelligence into clear next actions, not just another list of observations.
              Priority, impact, and timing need to feel readable at a glance.
            </p>
            <div className="ops-pill-row">
              <span className="workspace-meta-pill">Priority ranked</span>
              <span className="workspace-meta-pill">Action oriented</span>
              <span className="workspace-meta-pill">Warehouse aware</span>
            </div>
          </div>
          <div className="ops-command-actions">
            <button className="secondary-button" onClick={() => recommendationNow[0] && setSelectedRecommendationId(recommendationNow[0].id)} disabled={!recommendationNow[0]} type="button">
              Focus urgent recommendation
            </button>
          </div>
        </div>

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
                    className={`stack-card selectable-card ${selectedRecommendation?.id === recommendation.id ? 'is-selected' : ''}`}
                    onClick={() => setSelectedRecommendationId(recommendation.id)}
                    type="button"
                  >
                    <div className="stack-title-row">
                      <strong>{recommendation.title}</strong>
                      <span className={`priority-tag priority-${recommendation.priority.toLowerCase()}`}>{recommendation.priority}</span>
                    </div>
                    <p>{recommendation.description}</p>
                    <p className="muted-text">{recommendation.warehouseCode || 'Tenant-wide'} | {formatTimestamp(recommendation.createdAt)}</p>
                  </button>
                )) : <EmptyState>No items in this action lane.</EmptyState>}
              </div>
            </article>
          ))}
        </div>

        <div className="experience-grid experience-grid-split">
          <article className="stack-card section-card" id="recommendations-focus">
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
                  <p className="muted-text">{selectedRecommendation.warehouseCode || 'Tenant-wide lane'} | Priority {selectedRecommendation.priority}</p>
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
            </div>
            <p className="muted-text">The best items should feel immediately executable. This page exists to help teams act faster, not just review more data.</p>
          </article>
        </div>
      </Panel>
    </section>
  )
}
