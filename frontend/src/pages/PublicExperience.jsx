export default function PublicExperience({ context }) {
  const {
    effectivePageMeta,
    navigateToPage,
    publicPages,
  } = context

  const isLanding = effectivePageMeta.key === 'landing'
  const isProduct = effectivePageMeta.key === 'product'
  const isContact = effectivePageMeta.key === 'contact'

  const featureCards = [
    {
      title: 'Live operational visibility',
      body: 'Orders, inventory, fulfillment, incidents, connectors, and approvals aligned in one decision surface.',
    },
    {
      title: 'Decision intelligence',
      body: 'SynapseCore ranks what matters now, where risk is forming, and which action path protects the business fastest.',
    },
    {
      title: 'Control and trust',
      body: 'Run scenarios, replay failed inbound items, track audit evidence, and keep runtime posture visible for operators.',
    },
  ]

  const operatingLanes = [
    'Command dashboard',
    'Alerts and recommendations',
    'Inventory and locations',
    'Integrations and replay',
    'Approvals and auditability',
    'Runtime and release trust',
  ]

  return (
    <main className={`public-shell public-page-${effectivePageMeta.key}`}>
      <header className="public-topbar">
        <button className="brand-lockup brand-button" onClick={() => navigateToPage('landing')} type="button">
          <span className="brand-mark">S</span>
          <span><strong>SynapseCore</strong><small>Operational intelligence operating system</small></span>
        </button>
        <nav className="public-nav">
          {publicPages.filter((page) => page.key !== 'sign-in').map((page) => (
            <button key={page.key} className={`ghost-button ${page.key === effectivePageMeta.key ? 'page-step-active' : ''}`} onClick={() => navigateToPage(page.key)} type="button">
              {page.label}
            </button>
          ))}
          <button className="primary-button" onClick={() => navigateToPage('sign-in')} type="button">Enter Platform</button>
        </nav>
      </header>

      <section className="public-hero enterprise-public-hero">
        <article className="public-hero-copy">
          <p className="eyebrow">Enterprise operational command center</p>
          <h1>{isLanding ? 'Turn operating pressure into visible, controlled decisions.' : effectivePageMeta.title}</h1>
          <p>{effectivePageMeta.description}</p>
          <div className="history-action-row">
            <button className="primary-button" onClick={() => navigateToPage('sign-in')} type="button">Enter Platform</button>
            <button className="ghost-button" onClick={() => navigateToPage(isLanding ? 'product' : 'contact')} type="button">
              {isLanding ? 'Explore Product' : 'Talk to SynapseCore'}
            </button>
          </div>
        </article>
        <article className="public-hero-card public-hero-card-command">
          <div className="stack-title-row">
            <strong>What companies get</strong>
            <span className="scenario-type-tag">Realtime</span>
          </div>
          <div className="signal-list">
            {featureCards.map((feature) => (
              <div key={feature.title} className="signal-list-item">
                <strong>{feature.title}</strong>
                <p>{feature.body}</p>
              </div>
            ))}
          </div>
        </article>
      </section>

      <section className="content-grid public-command-grid">
        <article className="public-section-card">
          <p className="panel-kicker">{isContact ? 'Engagement lane' : 'Operating lanes'}</p>
          <h2>{isContact ? 'How to start with a company rollout' : 'One workspace for the full operational loop'}</h2>
          <div className="signal-list">
            {(isContact
              ? [
                'Connect the highest-pressure lane first.',
                'Stand up alerts, recommendations, and replay.',
                'Bring governance and runtime trust online next.',
                'Expand into broader planning and cross-site control.',
              ]
              : operatingLanes
            ).map((item) => (
              <div key={item} className="signal-list-item">
                <strong>{item}</strong>
                <p>{isContact ? 'Designed for phased enterprise rollout without losing control or visibility.' : 'Designed to reduce swivel-chair operations and speed up confident action.'}</p>
              </div>
            ))}
          </div>
        </article>

        <article className="public-section-card">
          <p className="panel-kicker">{isProduct ? 'Product posture' : 'Why it feels different'}</p>
          <h2>{isProduct ? 'Built to help operators act, not just watch.' : 'A calmer, stronger operating interface for serious teams.'}</h2>
          <div className="utility-metric-grid">
            <div><span>Visibility</span><strong>Live</strong></div>
            <div><span>Action model</span><strong>Guided</strong></div>
            <div><span>Governance</span><strong>Built in</strong></div>
            <div><span>Trust</span><strong>Auditable</strong></div>
          </div>
          <p className="muted-text">
            SynapseCore is not a generic admin panel. It is designed to sit above company systems, unify the operational picture, and help teams move with more confidence under pressure.
          </p>
        </article>
      </section>
    </main>
  )
}
