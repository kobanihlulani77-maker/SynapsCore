import EmptyState from './EmptyState'

export default function ActivityFeed({ title, kicker, items = [], emptyMessage = 'No activity yet.', action }) {
  return (
    <article className="stack-card section-card activity-feed">
      <div className="stack-title-row">
        <div>
          {kicker ? <p className="panel-kicker">{kicker}</p> : null}
          <strong>{title}</strong>
        </div>
        {action || null}
      </div>
      <div className="signal-list">
        {items.length ? items.map((item) => (
          <div key={item.id || item.title} className="signal-list-item">
            <strong>{item.title}</strong>
            <p>{item.body}</p>
            {item.meta ? <p className="muted-text">{item.meta}</p> : null}
          </div>
        )) : <EmptyState>{emptyMessage}</EmptyState>}
      </div>
    </article>
  )
}
