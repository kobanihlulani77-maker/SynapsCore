export default function WorkspacePageHeader({ showDashboardHero, controlHighlights, pendingReviewCount, pendingReplayCount, systemIncidents, pageStatusMap, appPageCount, enabledConnectorCount, snapshot, effectivePageMeta, currentPage }) {
  if (showDashboardHero) {
    return (
      <section className="command-hero">
        <div className="command-hero-copy">
          <p className="eyebrow">Premium operational command center</p>
          <h2>Operate live business pressure with calm, visible control.</h2>
          <p className="hero-copy">One workspace for orders, stock, fulfillment, connectors, approvals, incidents, and access control. SynapseCore keeps the operating picture live, ranked, and actionable.</p>
          <div className="hero-highlight-grid">
            {controlHighlights.map((highlight) => (
              <article key={highlight.label} className={`hero-highlight tone-${highlight.tone}`}>
                <span className="hero-highlight-label">{highlight.label}</span>
                <strong>{highlight.value}</strong>
                <p>{highlight.note}</p>
              </article>
            ))}
          </div>
        </div>
        <div className="command-hero-aside">
          <article className="command-hero-panel">
            <p className="panel-kicker">Workspace pulse</p>
            <h3>{pendingReviewCount || pendingReplayCount || systemIncidents.length ? 'Attention needed' : 'Operational posture stable'}</h3>
            <p>{pageStatusMap.dashboard}</p>
            <div className="command-hero-metrics">
              <div><span>Pages</span><strong>{appPageCount}</strong></div>
              <div><span>Connectors</span><strong>{enabledConnectorCount}/{snapshot.integrationConnectors.length || 0}</strong></div>
              <div><span>Replay</span><strong>{pendingReplayCount}</strong></div>
              <div><span>Approvals</span><strong>{pendingReviewCount}</strong></div>
            </div>
          </article>
        </div>
      </section>
    )
  }

  return (
    <section className="page-intro-panel">
      <div>
        <p className="panel-kicker">Current page</p>
        <h2>{effectivePageMeta.label}</h2>
        <p>{pageStatusMap[currentPage]}</p>
      </div>
      <div className="page-intro-tags">
        {effectivePageMeta.focus.map((focusItem) => <span key={focusItem} className="scenario-type-tag">{focusItem}</span>)}
      </div>
    </section>
  )
}
