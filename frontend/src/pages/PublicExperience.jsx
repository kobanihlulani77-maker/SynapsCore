const audienceCards = [
  {
    title: 'Logistics companies',
    body: 'Coordinate inbound, outbound, route pressure, and partner handoffs from one live operational layer.',
  },
  {
    title: 'Warehouses',
    body: 'Unify stock posture, lane bottlenecks, fulfillment risk, and warehouse-specific operational actions.',
  },
  {
    title: 'Ecommerce fulfillment',
    body: 'Keep order promises, exception recovery, and inventory integrity visible without jumping between tools.',
  },
  {
    title: 'Retail chains',
    body: 'See location-level disruption, replenishment pressure, and connector failures before they become customer pain.',
  },
  {
    title: 'Distributors',
    body: 'Operate high-SKU, multi-site inventory and order flow with clearer operational priorities and traceability.',
  },
  {
    title: 'Manufacturers',
    body: 'Align material posture, dispatch dependencies, approvals, and supply pressure across complex operating environments.',
  },
  {
    title: 'Operations centers',
    body: 'Give control-room teams a calmer, tenant-scoped command surface for live decisions, escalation, and recovery.',
  },
]

const painCards = [
  'Fragmented systems hiding the true operating picture.',
  'Failed integrations that disappear into scripts and inboxes.',
  'Delayed approvals slowing down live operational decisions.',
  'Inventory and order mismatch across sites and channels.',
  'No replay recovery path when inbound work breaks.',
  'Weak visibility into risk, incidents, and runtime posture.',
  'Poor audit traceability when teams need to explain what happened.',
]

const capabilityCards = [
  {
    title: 'Live dashboard',
    body: 'A tenant-scoped command center for alerts, recommendations, orders, inventory posture, and fulfillment pressure.',
  },
  {
    title: 'Replay and recovery',
    body: 'Keep failed inbound work visible, actionable, and recoverable instead of buried inside one-off operator workarounds.',
  },
  {
    title: 'Tenant-scoped operations',
    body: 'Each company workspace has its own users, catalog, warehouses, integrations, runtime signals, and control envelope.',
  },
  {
    title: 'Alert and recommendation engine',
    body: 'Surface what is urgent now, what is likely next, and which action path protects the business fastest.',
  },
  {
    title: 'Approvals and escalations',
    body: 'Model changes, route approvals, and keep escalations visible before risky operational changes touch live flow.',
  },
  {
    title: 'Integration and runtime visibility',
    body: 'Watch connectors, imports, incidents, dispatch pressure, release fingerprints, and system trust posture from one platform.',
  },
]

const boundaryCards = [
  {
    title: 'Works beside source systems',
    body: 'SynapseCore improves visibility, coordination, recovery, and governed action without replacing ERP, WMS, ecommerce, or source-of-record systems during a pilot.',
  },
  {
    title: 'Best first step',
    body: 'Start with one controlled operational lane, prove the workflow, then expand based on real operator evidence.',
  },
]

const proofCards = [
  {
    title: 'Hosted proof flow exists',
    body: 'The product already exercises real operational paths across auth, workspace access, command flows, and recovery.',
    tone: 'info',
  },
  {
    title: 'Core lanes are being proven',
    body: 'Catalog, inventory, orders, realtime, replay, scenario approval, and runtime visibility are covered by the hosted proof discipline.',
    tone: 'success',
  },
  {
    title: 'Security and leakage testing continues',
    body: 'Hardening, role-gating, and tenant-isolation proofing are being worked through directly, not hidden behind fake claims.',
    tone: 'warning',
  },
]

const workspaceSteps = [
  {
    title: 'Existing operators sign in',
    body: 'Operators use company workspace code, username, and password to enter the correct SynapseCore environment.',
  },
  {
    title: 'First admin creates the workspace',
    body: 'A company admin stands up the initial workspace, identity boundary, and rollout path for the team.',
  },
  {
    title: 'Workspace code identifies the company',
    body: 'The workspace code tells SynapseCore which company environment, users, warehouses, and controls are in scope.',
  },
  {
    title: 'Onboarding expands in stages',
    body: 'Catalog, users, inventory, integrations, approvals, and runtime posture can be brought online through a guided rollout.',
  },
]

const commandSignals = [
  { label: 'Alert pressure', value: 'Live', tone: 'warning' },
  { label: 'Replay queue', value: 'Recoverable', tone: 'info' },
  { label: 'Approval lane', value: 'Governed', tone: 'success' },
]

const commandLanes = [
  {
    title: 'Orders and fulfillment',
    lines: ['Live order flow', 'Warehouse assignment', 'Backlog and SLA visibility'],
  },
  {
    title: 'Inventory and stock risk',
    lines: ['Low-stock detection', 'Velocity posture', 'Cross-site pressure'],
  },
  {
    title: 'Connectors and replay',
    lines: ['Inbound failure visibility', 'Replay controls', 'Connector health context'],
  },
  {
    title: 'Approvals and runtime',
    lines: ['Scenario decision path', 'Operational escalations', 'Incident and release trust'],
  },
]

export default function PublicExperience({ context }) {
  const {
    effectivePageMeta,
    navigateToPage,
    publicPages,
  } = context

  const isLanding = effectivePageMeta.key === 'landing'
  const isProduct = effectivePageMeta.key === 'product'
  const isContact = effectivePageMeta.key === 'contact'

  const heroTitle = isLanding
    ? 'Run operations from one live command center.'
    : effectivePageMeta.title

  const heroDescription = isLanding
    ? 'SynapseCore helps operational teams see live work, coordinate decisions, recover failed inbound flow, and keep runtime trust visible across one tenant-scoped command center.'
    : effectivePageMeta.description

  const heroEyebrow = isContact
    ? 'Workspace rollout and pilot path'
    : isProduct
      ? 'Enterprise operations platform'
      : 'Tenant-based live operations control'

  const heroMetrics = [
    { label: 'Operational model', value: 'Tenant-scoped control' },
    { label: 'Decision posture', value: 'Live, guided, auditable' },
    { label: 'Recovery posture', value: 'Replay-ready' },
  ]

  const finalCtaLabel = isContact ? 'Return Home' : 'Create Company Workspace'
  const finalCtaAction = () => {
    if (isContact) {
      navigateToPage('landing')
      return
    }
    navigateToPage('create-workspace')
  }

  return (
    <main className={`public-shell public-page-${effectivePageMeta.key}`}>
      <header className="public-topbar">
        <button className="brand-lockup brand-button" onClick={() => navigateToPage('landing')} type="button">
          <span className="brand-mark">S</span>
          <span><strong>SynapseCore</strong><small>Operational intelligence operating system</small></span>
        </button>
        <nav className="public-nav">
          {publicPages.filter((page) => page.key !== 'sign-in').map((page) => (
            <button
              key={page.key}
              className={`ghost-button ${page.key === effectivePageMeta.key ? 'page-step-active' : ''}`}
              onClick={() => navigateToPage(page.key)}
              type="button"
            >
              {page.label}
            </button>
          ))}
          <button className="primary-button" onClick={() => navigateToPage('sign-in')} type="button">Sign In to Workspace</button>
        </nav>
      </header>

      <section className="public-hero enterprise-public-hero">
        <article className="public-hero-copy">
          <p className="eyebrow">{heroEyebrow}</p>
          <h1>{heroTitle}</h1>
          <p>{heroDescription}</p>
          <div className="public-positioning-card">
            <strong>Built for controlled operational pilots.</strong>
            <p>Not an ERP or WMS replacement. SynapseCore sits above existing systems to improve visibility, recovery, approvals, and operational confidence.</p>
          </div>
          <div className="public-hero-metrics">
            {heroMetrics.map((metric) => (
              <div key={metric.label} className="public-hero-metric-card">
                <span>{metric.label}</span>
                <strong>{metric.value}</strong>
              </div>
            ))}
          </div>
          <div className="history-action-row">
            <button className="primary-button" onClick={() => navigateToPage('sign-in')} type="button">Sign In to Workspace</button>
            <button
              className="secondary-button"
              onClick={() => navigateToPage('create-workspace')}
              type="button"
            >
              Create Company Workspace
            </button>
          </div>
        </article>

        <article className="public-command-visual">
          <div className="public-command-header">
            <div>
              <p className="panel-kicker">Live command surface</p>
              <h2>One operating picture for control, recovery, and trust.</h2>
            </div>
            <span className="enterprise-status-pill status-live">Pilot-ready command surface</span>
          </div>
          <div className="public-command-signals">
            {commandSignals.map((signal) => (
              <div key={signal.label} className={`public-command-signal tone-${signal.tone}`}>
                <span>{signal.label}</span>
                <strong>{signal.value}</strong>
              </div>
            ))}
          </div>
          <div className="public-command-grid">
            {commandLanes.map((lane) => (
              <article key={lane.title} className="public-command-lane">
                <strong>{lane.title}</strong>
                <ul>
                  {lane.lines.map((line) => <li key={line}>{line}</li>)}
                </ul>
              </article>
            ))}
          </div>
        </article>
      </section>

      <section className="public-section-shell public-boundary-strip public-compact-section" aria-labelledby="pilot-boundaries">
        <div className="public-section-heading">
          <p className="panel-kicker">Pilot boundaries</p>
          <h2 id="pilot-boundaries">Clear value without pretending to replace the systems companies already rely on.</h2>
        </div>
        <div className="public-boundary-list">
          {boundaryCards.map((card) => (
            <article key={card.title} className="public-boundary-item">
              <strong>{card.title}</strong>
              <p>{card.body}</p>
            </article>
          ))}
        </div>
      </section>

      <section className="public-section-shell" aria-labelledby="who-it-is-for">
        <div className="public-section-heading">
          <p className="panel-kicker">Who it is for</p>
          <h2 id="who-it-is-for">Built for operational teams that cannot afford blind spots.</h2>
          <p>SynapseCore is designed for real operating environments where multiple systems, sites, and teams have to stay aligned under pressure.</p>
        </div>
        <div className="public-audience-list">
          {audienceCards.map((card) => (
            <article key={card.title} className="public-audience-row">
              <strong>{card.title}</strong>
              <p>{card.body}</p>
            </article>
          ))}
        </div>
      </section>

      <section className="public-section-shell public-section-shell-split public-operational-flow" aria-labelledby="operational-pain">
        <article className="public-flow-column">
          <p className="panel-kicker">Operational pain</p>
          <h2 id="operational-pain">Most operations teams are still working through fragmented pressure.</h2>
          <div className="public-pressure-list">
            {painCards.map((pain) => (
              <div key={pain} className="public-pressure-row">
                <strong>{pain}</strong>
              </div>
            ))}
          </div>
        </article>

        <article className="public-flow-column public-flow-column-primary">
          <p className="panel-kicker">How SynapseCore helps</p>
          <h2>Bring live signals, recovery, and governance into one product surface.</h2>
          <div className="public-capability-list">
            {capabilityCards.map((card, index) => (
              <article key={card.title} className={index === 0 ? 'public-capability-row public-capability-row-lead' : 'public-capability-row'}>
                <strong>{card.title}</strong>
                <p>{card.body}</p>
              </article>
            ))}
          </div>
        </article>
      </section>

      <section className="public-section-shell" aria-labelledby="proof-and-trust">
        <div className="public-section-heading">
          <p className="panel-kicker">Proof and trust</p>
          <h2 id="proof-and-trust">Grounded in real proof work, not inflated claims.</h2>
          <p>SynapseCore is being proved through live product flows and hardening work. The trust story is earned through system behavior, recoverability, and tenant-safe operations.</p>
        </div>
        <div className="public-proof-ledger">
          {proofCards.map((card) => (
            <article key={card.title} className={`public-proof-row tone-${card.tone}`}>
              <strong>{card.title}</strong>
              <p>{card.body}</p>
            </article>
          ))}
        </div>
      </section>

      <section className="public-section-shell" id="workspace-onboarding" aria-labelledby="workspace-onboarding-title">
        <div className="public-section-heading">
          <p className="panel-kicker">Create workspace path</p>
          <h2 id="workspace-onboarding-title">A company workspace gives each team its own operating environment.</h2>
          <p>SynapseCore uses workspace codes to separate companies cleanly. Existing operators sign in to the right environment, and first-time admins stand up the workspace for rollout.</p>
        </div>
        <div className="public-workspace-path">
          {workspaceSteps.map((step, index) => (
            <article key={step.title} className="public-workspace-step">
              <span className="public-step-marker">{String(index + 1).padStart(2, '0')}</span>
              <strong>{step.title}</strong>
              <p>{step.body}</p>
            </article>
          ))}
        </div>
      </section>

      <section className="public-final-cta">
        <article className="public-final-cta-card">
          <div>
            <p className="panel-kicker">{isContact ? 'Pilot planning' : 'Next step'}</p>
            <h2>{isContact ? 'Start with the company pressure you need to control first.' : 'See the platform, then stand up the right workspace path.'}</h2>
            <p>
              {isContact
                ? 'Prepare the first company workspace, define the rollout scope, and align catalog, inventory, integrations, and users in a deliberate onboarding motion.'
                : 'Operators can sign in today. Company admins can prepare the initial workspace and pilot path without changing backend contracts or waiting on the full onboarding flow.'}
            </p>
          </div>
          <div className="history-action-row">
            <button className="primary-button" onClick={() => navigateToPage('sign-in')} type="button">Sign In to Workspace</button>
            <button className="secondary-button" onClick={finalCtaAction} type="button">{finalCtaLabel}</button>
          </div>
        </article>
      </section>
    </main>
  )
}
