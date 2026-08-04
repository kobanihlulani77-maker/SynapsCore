import { useState } from 'react'

const setupSteps = [
  { key: 'company', label: 'Company information' },
  { key: 'workspace', label: 'Workspace setup' },
  { key: 'admin', label: 'First admin operator' },
  { key: 'profile', label: 'Operations profile' },
  { key: 'next', label: 'Guided next steps' },
]

const setupBoundaries = [
  'This frontend prepares a workspace setup brief.',
  'Live tenant creation uses supported backend provisioning paths.',
  'SynapseCore supports operations beside existing ERP, WMS, ecommerce, and source systems.',
]

const industryOptions = [
  'Logistics provider',
  'Warehouse operation',
  'Ecommerce fulfillment',
  'Retail chain',
  'Distributor',
  'Manufacturer',
  'Operations center',
]

const operationsProfiles = [
  'Warehouse and fulfillment',
  'Retail replenishment and distribution',
  'Procurement-heavy operations',
  'Connector reliability and recovery',
  'Cross-site operational control',
]

const scaleOptions = [
  'Single site pilot',
  'Multi-site rollout',
  'Regional operating network',
  'Enterprise portfolio',
]

const onboardingLanes = [
  {
    title: 'Catalog and product model',
    body: 'Add the first SKUs so orders, inventory, and scenarios all speak the same operational language.',
    target: 'catalog',
  },
  {
    title: 'Inventory and location posture',
    body: 'Bring the first warehouse or operating site online so stock, backlog, and pressure can be measured clearly.',
    target: 'inventory',
  },
  {
    title: 'Operators and roles',
    body: 'Invite the first planners, operators, and approvers into the company workspace with the right scope.',
    target: 'users',
  },
  {
    title: 'Integrations and recovery',
    body: 'Connect inbound systems and keep replay/recovery ready before exceptions start landing under pressure.',
    target: 'integrations',
  },
]

const defaultWorkspaceDraft = {
  companyName: '',
  workspaceCode: '',
  industry: industryOptions[0],
  operationsProfile: operationsProfiles[0],
  scaleProfile: scaleOptions[0],
  adminName: '',
  adminEmail: '',
  username: '',
  password: '',
  teamShape: 'Operations planners, warehouse leads, and tenant admins',
  launchPriority: 'Bring catalog, inventory, and user access online first',
}

function slugifyWorkspaceCode(value) {
  return value
    .toUpperCase()
    .replace(/[^A-Z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
    .slice(0, 20)
}

function getFieldError(stepKey, draft) {
  switch (stepKey) {
    case 'company':
      if (!draft.companyName.trim()) return 'Company name is required.'
      return ''
    case 'workspace':
      if (!draft.workspaceCode.trim()) return 'Workspace code is required.'
      return ''
    case 'admin':
      if (!draft.adminName.trim()) return 'First admin name is required.'
      if (!draft.adminEmail.trim()) return 'Admin email is required.'
      if (!draft.username.trim()) return 'Admin username is required.'
      if (draft.password.trim().length < 10) return 'Choose a password with at least 10 characters.'
      return ''
    case 'profile':
      if (!draft.teamShape.trim()) return 'Describe the initial operator team.'
      if (!draft.launchPriority.trim()) return 'Capture the first operational priority.'
      return ''
    default:
      return ''
  }
}

export default function CreateWorkspacePage({ context }) {
  const {
    effectivePageMeta,
    navigateToPage,
    publicPages,
    setAuthSessionState,
  } = context

  const [draft, setDraft] = useState(defaultWorkspaceDraft)
  const [currentStepIndex, setCurrentStepIndex] = useState(0)
  const [manualWorkspaceCode, setManualWorkspaceCode] = useState(false)
  const [showPassword, setShowPassword] = useState(false)
  const [workspacePrepared, setWorkspacePrepared] = useState(false)

  const activeStep = setupSteps[currentStepIndex]
  const fieldError = getFieldError(activeStep.key, draft)
  const canAdvance = !fieldError

  const updateDraft = (field, value) => {
    setDraft((current) => {
      const nextDraft = { ...current, [field]: value }
      if (field === 'companyName' && !manualWorkspaceCode) {
        nextDraft.workspaceCode = slugifyWorkspaceCode(value)
      }
      return nextDraft
    })
  }

  const moveToNextStep = () => {
    if (!canAdvance) return
    setCurrentStepIndex((current) => Math.min(current + 1, setupSteps.length - 1))
  }

  const moveToPreviousStep = () => {
    setCurrentStepIndex((current) => Math.max(current - 1, 0))
  }

  const prepareWorkspaceBrief = () => {
    setWorkspacePrepared(true)
    setCurrentStepIndex(setupSteps.length - 1)
  }

  const continueToSignIn = () => {
    setAuthSessionState((current) => ({
      ...current,
      tenantCode: draft.workspaceCode.trim().toUpperCase(),
      username: draft.username.trim(),
      password: '',
      error: '',
    }))
    navigateToPage('sign-in')
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

      <section className="workspace-wizard-shell">
        <aside className="workspace-wizard-sidebar">
          <p className="eyebrow">Company workspace setup</p>
          <h1>{effectivePageMeta.title}</h1>
          <p>{effectivePageMeta.description}</p>
          <div className="workspace-guidance-block workspace-entry-boundary">
            <strong>Controlled pilot setup</strong>
            <p>Prepare company context, first administrator, and rollout lane before operators sign in. Provisioning is confirmed only by supported backend flows.</p>
          </div>
          <div className="workspace-wizard-steps">
            {setupSteps.map((step, index) => {
              const state = index < currentStepIndex ? 'complete' : index === currentStepIndex ? 'active' : 'upcoming'
              return (
                <button
                  key={step.key}
                  className={`workspace-step-row workspace-step-${state}`}
                  onClick={() => setCurrentStepIndex(index)}
                  type="button"
                >
                  <span>{String(index + 1).padStart(2, '0')}</span>
                  <strong>{step.label}</strong>
                </button>
              )
            })}
          </div>
          <div className="workspace-guidance-block">
            <strong>How this works</strong>
            <p>Existing operators sign into a company workspace with workspace code, username, and password. The first admin sets up the company environment and guides catalog, users, inventory, and integrations afterward.</p>
          </div>
        </aside>

        <section className="workspace-wizard-panel">
          <div className="workspace-wizard-header">
            <div>
              <p className="panel-kicker">Step {currentStepIndex + 1}</p>
              <h2>{activeStep.label}</h2>
            </div>
            <span className="enterprise-status-pill status-live">Guided setup</span>
          </div>

          {activeStep.key === 'company' ? (
            <div className="workspace-wizard-grid">
              <label className="field">
                <span>Company name</span>
                <input
                  type="text"
                  value={draft.companyName}
                  onChange={(event) => updateDraft('companyName', event.target.value)}
                  placeholder="Acme Distribution Group"
                />
                <span className="field-hint">This is the company or operating group that will own the workspace.</span>
              </label>
              <label className="field">
                <span>Industry / operating environment</span>
                <select value={draft.industry} onChange={(event) => updateDraft('industry', event.target.value)}>
                  {industryOptions.map((option) => <option key={option} value={option}>{option}</option>)}
                </select>
                <span className="field-hint">Choose the closest environment so SynapseCore can guide the right rollout lane.</span>
              </label>
            </div>
          ) : null}

          {activeStep.key === 'workspace' ? (
            <div className="workspace-wizard-grid">
              <label className="field">
                <span>Workspace code</span>
                <div className="field-control">
                  <input
                    type="text"
                    value={draft.workspaceCode}
                    onChange={(event) => {
                      setManualWorkspaceCode(true)
                      updateDraft('workspaceCode', slugifyWorkspaceCode(event.target.value))
                    }}
                    placeholder="ACME-OPS"
                  />
                  <button
                    className="field-inline-button"
                    onClick={() => {
                      setManualWorkspaceCode(true)
                      updateDraft('workspaceCode', slugifyWorkspaceCode(draft.companyName))
                    }}
                    type="button"
                  >
                    Generate
                  </button>
                </div>
                <span className="field-hint">Operators use this workspace code to enter the correct company environment.</span>
              </label>
              <label className="field">
                <span>Operations profile</span>
                <select value={draft.operationsProfile} onChange={(event) => updateDraft('operationsProfile', event.target.value)}>
                  {operationsProfiles.map((option) => <option key={option} value={option}>{option}</option>)}
                </select>
              </label>
              <label className="field">
                <span>Scale profile</span>
                <select value={draft.scaleProfile} onChange={(event) => updateDraft('scaleProfile', event.target.value)}>
                  {scaleOptions.map((option) => <option key={option} value={option}>{option}</option>)}
                </select>
                <span className="field-hint">This helps frame the first rollout sequence and the amount of operational complexity expected at launch.</span>
              </label>
            </div>
          ) : null}

          {activeStep.key === 'admin' ? (
            <div className="workspace-wizard-grid">
              <label className="field">
                <span>First admin operator</span>
                <input
                  type="text"
                  value={draft.adminName}
                  onChange={(event) => updateDraft('adminName', event.target.value)}
                  placeholder="Amina Dlamini"
                />
              </label>
              <label className="field">
                <span>Admin email</span>
                <input
                  type="email"
                  value={draft.adminEmail}
                  onChange={(event) => updateDraft('adminEmail', event.target.value)}
                  placeholder="amina@acmeops.com"
                />
              </label>
              <label className="field">
                <span>Admin username</span>
                <input
                  type="text"
                  value={draft.username}
                  onChange={(event) => updateDraft('username', event.target.value)}
                  placeholder="amina.admin"
                />
                <span className="field-hint">This is the first company operator identity that will enter the workspace.</span>
              </label>
              <label className="field">
                <span>Initial password</span>
                <div className="field-control">
                  <input
                    type={showPassword ? 'text' : 'password'}
                    value={draft.password}
                    onChange={(event) => updateDraft('password', event.target.value)}
                    placeholder="Choose a strong setup password"
                  />
                  <button className="field-inline-button" onClick={() => setShowPassword((current) => !current)} type="button">
                    {showPassword ? 'Hide' : 'Show'}
                  </button>
                </div>
                <span className="field-hint">The first admin can later rotate credentials and invite more operators after sign-in.</span>
              </label>
            </div>
          ) : null}

          {activeStep.key === 'profile' ? (
            <div className="workspace-wizard-grid">
              <label className="field">
                <span>Initial team shape</span>
                <input
                  type="text"
                  value={draft.teamShape}
                  onChange={(event) => updateDraft('teamShape', event.target.value)}
                  placeholder="Operations planners, warehouse leads, and tenant admins"
                />
              </label>
              <label className="field">
                <span>First rollout priority</span>
                <textarea
                  value={draft.launchPriority}
                  onChange={(event) => updateDraft('launchPriority', event.target.value)}
                  placeholder="Catalog, users, inventory, or integration stability"
                  rows={4}
                />
                <span className="field-hint">Call out the first thing SynapseCore should help the company stabilize after sign-in.</span>
              </label>
            </div>
          ) : null}

          {activeStep.key === 'next' ? (
            <div className="workspace-wizard-review">
              <article className="workspace-review-card">
                <p className="panel-kicker">Workspace summary</p>
                <h3>{draft.companyName || 'Company workspace'}</h3>
                <div className="workspace-review-grid">
                  <div><span>Workspace code</span><strong>{draft.workspaceCode || 'Not set'}</strong></div>
                  <div><span>Industry</span><strong>{draft.industry}</strong></div>
                  <div><span>Operations profile</span><strong>{draft.operationsProfile}</strong></div>
                  <div><span>First admin</span><strong>{draft.username || 'Pending'}</strong></div>
                </div>
                <p className="muted-text">
                  {workspacePrepared
                    ? 'The setup brief is prepared. This frontend flow can hand off to live workspace provisioning when onboarding APIs are connected, without changing the experience.'
                    : 'Review the company setup brief, then continue into sign-in or pilot planning. Live provisioning can connect to this exact experience later.'}
                </p>
              </article>

              <article className="workspace-review-card">
                <p className="panel-kicker">What happens next</p>
                <div className="workspace-next-list">
                  {onboardingLanes.map((lane) => (
                    <div key={lane.title} className="workspace-next-row">
                      <strong>{lane.title}</strong>
                      <p>{lane.body}</p>
                    </div>
                  ))}
                </div>
              </article>

              <article className="workspace-review-card workspace-setup-boundaries">
                <p className="panel-kicker">Boundaries</p>
                <h3>What this setup does and does not do</h3>
                <div className="workspace-boundary-list">
                  {setupBoundaries.map((boundary) => (
                    <div key={boundary} className="workspace-boundary-row">
                      <strong>{boundary}</strong>
                    </div>
                  ))}
                </div>
              </article>
            </div>
          ) : null}

          {fieldError && activeStep.key !== 'next' ? <p className="error-text">{fieldError}</p> : null}

          <div className="workspace-wizard-actions">
            <button className="ghost-button" disabled={currentStepIndex === 0} onClick={moveToPreviousStep} type="button">Back</button>
            {activeStep.key !== 'next' ? (
              <>
                {currentStepIndex === setupSteps.length - 2 ? (
                  <button className="primary-button" disabled={!canAdvance} onClick={prepareWorkspaceBrief} type="button">Prepare Workspace Brief</button>
                ) : (
                  <button className="primary-button" disabled={!canAdvance} onClick={moveToNextStep} type="button">Continue</button>
                )}
              </>
            ) : (
              <>
                <button className="secondary-button" onClick={() => navigateToPage('product')} type="button">Review Product Surface</button>
                <button className="primary-button" onClick={continueToSignIn} type="button">Continue to Sign In</button>
              </>
            )}
          </div>
        </section>
      </section>
    </main>
  )
}
