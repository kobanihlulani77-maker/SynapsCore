export function Card({ className = '', children, ...props }) {
  return (
    <article className={`stack-card ${className}`.trim()} {...props}>
      {children}
    </article>
  )
}

export function SummaryCard({ label, value, accent }) {
  return (
    <article className="summary-card">
      <span className="summary-label">{label}</span>
      <strong className={`summary-value accent-${accent}`}>{value}</strong>
    </article>
  )
}

export function MetricCard({ label, value, note, accent = 'blue', className = '' }) {
  return (
    <article className={`summary-card metric-card metric-card-${accent} ${className}`.trim()}>
      <span className="summary-label">{label}</span>
      <strong className={`summary-value accent-${accent}`}>{value}</strong>
      {note ? <p className="metric-card-note">{note}</p> : null}
    </article>
  )
}

export function AlertCard({ title, body, tone = 'partial', meta, action, className = '' }) {
  return (
    <article className={`stack-card alert-card alert-card-${tone} ${className}`.trim()}>
      <div className="stack-title-row">
        <strong>{title}</strong>
        {action || null}
      </div>
      <p>{body}</p>
      {meta ? <p className="muted-text">{meta}</p> : null}
    </article>
  )
}

export default Card
