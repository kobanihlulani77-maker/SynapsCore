export default function Panel({ wide = false, className = '', kicker, title, badge, actions, children, ...props }) {
  return (
    <article className={`panel ${wide ? 'panel-wide' : ''} ${className}`.trim()} {...props}>
      {kicker || title || badge || actions ? (
        <div className="panel-header">
          <div>
            {kicker ? <p className="panel-kicker">{kicker}</p> : null}
            {title ? <h2>{title}</h2> : null}
          </div>
          <div className="panel-header-actions">
            {actions || null}
            {badge || null}
          </div>
        </div>
      ) : null}
      {children}
    </article>
  )
}
