export default function ActionPanel({ title, kicker, badge, actions = [], emptyMessage = 'No actions available yet.' }) {
  return (
    <article className="panel action-panel">
      <div className="panel-header">
        <div>
          <p className="panel-kicker">{kicker}</p>
          <h2>{title}</h2>
        </div>
        {badge || null}
      </div>
      <div className="act-now-grid">
        {actions.length ? actions.map((action) => (
          <button key={action.id || action.title} className="stack-card action-card" onClick={action.onClick} type="button">
            <div className="stack-title-row">
              <strong>{action.title}</strong>
              {action.tag ? <span className="scenario-type-tag">{action.tag}</span> : null}
            </div>
            <p>{action.note}</p>
          </button>
        )) : <div className="empty-state">{emptyMessage}</div>}
      </div>
    </article>
  )
}
