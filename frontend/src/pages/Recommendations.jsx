import EmptyState from '../components/EmptyState'
import Panel from '../components/Panel'

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
    { title: 'Urgent now', items: recommendationNow, tone: 'priority-high' },
    { title: 'Important soon', items: recommendationSoon, tone: 'priority-medium' },
    { title: 'Watch', items: recommendationWatch, tone: 'priority-low' },
  ]
  const selectedRecommendation = recommendationCandidates.find((recommendation) => recommendation.id === selectedRecommendationId) || recommendationCandidates[0]

  return (
    <section className="content-grid">
      <Panel wide id="recommendations-lanes">
        <div className="panel-header">
          <div><p className="panel-kicker">Recommendations center</p><h2>Ranked action queue for operators</h2></div>
          <span className="panel-badge recommendation-badge">{snapshot.recommendations.length}</span>
        </div>
        <div className="recommendation-board">
          {columns.map((column) => (
            <article key={column.title} className="recommendation-column">
              <div className="stack-title-row">
                <strong>{column.title}</strong>
                <span className={`status-tag ${column.tone}`}>{column.items.length}</span>
              </div>
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
                    <p className="muted-text">{formatTimestamp(recommendation.createdAt)}</p>
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
                </div>
              </div>
            ) : <EmptyState>When the system has action guidance, the leading recommendation appears here with its operating context.</EmptyState>}
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
              <div><span>Action lanes</span><strong>{new Set(snapshot.recommendations.map((item) => item.warehouseCode).filter(Boolean)).size || 'All'}</strong></div>
            </div>
            <p className="muted-text">This page exists to help the team act faster, not just review data. The best items should feel immediately executable.</p>
          </article>
        </div>
      </Panel>
    </section>
  )
}
