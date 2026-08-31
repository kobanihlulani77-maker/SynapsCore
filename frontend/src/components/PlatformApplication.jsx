import { useEffect, useState } from 'react'
import AppShell from '../layout/AppShell'
import { pageLookup, platformPages } from '../config/pageRegistry'
import {
  formatBuildValue,
  formatCodeLabel,
  formatMetricValue,
  formatTimestamp,
  getIncidentStatusClassName,
  getRuntimeStatusClassName,
} from '../config/workspaceModel'
import { apiUrl, extractResponseErrorMessage, readResponsePayload } from '../services/api'
import LoadingState from './LoadingState'
import Panel from './Panel'
import PlatformActivityPage from '../pages/PlatformActivity'
import PlatformAdminPage from '../pages/PlatformAdmin'
import ReleasesPage from '../pages/Releases'
import SystemConfigPage from '../pages/SystemConfig'
import TenantsPage from '../pages/Tenants'
import usePlatformRealtime from '../hooks/usePlatformRealtime'

const emptySession = { signedIn: false }
const createBlankWarehouse = () => ({ code: '', name: '', location: '' })
const createBlankUser = () => ({
  username: '',
  fullName: '',
  operatorActorName: '',
  operatorDisplayName: '',
  operatorDescription: '',
  roles: [],
  warehouseScopes: [],
  tenantWide: false,
  initialPassword: '',
})
const createEmptyTenantForm = () => ({
  tenantCode: '',
  tenantName: '',
  description: '',
  warehouses: [createBlankWarehouse()],
  users: [createBlankUser()],
  requiredRoles: [],
  requiredRolesConfigured: false,
})

const platformFetch = async (path, init = {}) => {
  let response
  try {
    response = await fetch(`${apiUrl}${path}`, {
      credentials: 'include',
      ...init,
      headers: { 'Content-Type': 'application/json', ...(init.headers || {}) },
    })
  } catch {
    const method = String(init.method || 'GET').toUpperCase()
    throw new Error(`${method} ${apiUrl}${path} could not be completed. The browser did not receive an HTTP response; check backend availability or proxy health.`)
  }
  const payload = await readResponsePayload(response)
  if (!response.ok) {
    throw new Error(extractResponseErrorMessage(response, payload, `Request to ${path} failed.`))
  }
  return payload
}

function PlatformSignIn({ form, setForm, busy, error, onSubmit }) {
  return (
    <main className="public-shell public-page-sign-in">
      <header className="public-topbar">
        <a className="brand-lockup brand-button" href="/">
          <span className="brand-mark">S</span>
          <span><strong>SynapseCore</strong><small>Platform control plane</small></span>
        </a>
        <a className="ghost-button" href="/sign-in">Customer workspace sign in</a>
      </header>
      <section className="public-signin-shell enterprise-signin-shell">
        <article className="public-signin-story enterprise-signin-story">
          <p className="eyebrow">Separate platform authority</p>
          <h1>Access the SynapseCore control plane.</h1>
          <p>This surface is for the SynapseCore platform owner. Tenant administrators and customer roles use the company workspace sign-in instead.</p>
          <div className="public-positioning-card signin-positioning-card">
            <strong>Metadata-first administration</strong>
            <p>The control plane shows platform health, tenant support summaries, release identity, and operational conditions. It does not browse raw customer business payloads.</p>
          </div>
        </article>
        <article className="public-signin-card enterprise-signin-form">
          <p className="panel-kicker">Platform owner sign in</p>
          <h2>Open the control plane</h2>
          <form className="signin-form-shell" onSubmit={onSubmit}>
            <label className="field"><span>Platform username</span><input autoComplete="username" value={form.username} onChange={(event) => setForm((current) => ({ ...current, username: event.target.value }))} /></label>
            <label className="field"><span>Password</span><input type="password" autoComplete="current-password" value={form.password} onChange={(event) => setForm((current) => ({ ...current, password: event.target.value }))} /></label>
            {error ? <div className="notice notice-error" role="alert"><strong>Platform sign-in needs attention</strong><p>{error}</p></div> : null}
            <button className="primary-button" disabled={busy || !form.username.trim() || !form.password} type="submit">{busy ? 'Verifying authority...' : 'Enter Platform Control Plane'}</button>
          </form>
        </article>
      </section>
    </main>
  )
}

function PlatformDataState({ loading, error, onRetry }) {
  if (loading) return <section className="content-grid"><Panel wide><LoadingState label="Loading platform control-plane evidence..." /></Panel></section>
  if (!error) return null
  return <section className="content-grid"><Panel wide kicker="Platform evidence unavailable" title="The control plane could not load current evidence"><div className="notice notice-error" role="alert"><strong>Do not interpret this as an empty or healthy platform.</strong><p>{error}</p><div className="history-action-row"><button className="secondary-button" onClick={onRetry} type="button">Retry platform evidence</button></div></div></Panel></section>
}

function PlatformApplicationContent({ page, context, activityState, navigate, platformDataLoading, platformDataError, platformRealtimeState }) {
  if (page === 'platform-activity') return <PlatformActivityPage activity={activityState.items} loading={activityState.loading || platformDataLoading} error={activityState.error || platformDataError} navigateToPage={navigate} realtimeState={platformRealtimeState} />
  if (page === 'platform') return <PlatformAdminPage context={context.platformAdminContext} />
  if (page === 'tenants') return <TenantsPage context={context.tenantsContext} />
  if (page === 'system-config') return <SystemConfigPage context={context.systemConfigContext} />
  return <ReleasesPage context={context.releasesContext} />
}

export default function PlatformApplication({ initialPage }) {
  const [page, setPage] = useState(initialPage)
  const [session, setSession] = useState(emptySession)
  const [sessionLoading, setSessionLoading] = useState(true)
  const [error, setError] = useState('')
  const [dataLoading, setDataLoading] = useState(false)
  const [dataError, setDataError] = useState('')
  const [form, setForm] = useState({ username: '', password: '' })
  const [overview, setOverview] = useState(null)
  const [activityState, setActivityState] = useState({ items: [], loading: false, error: '' })
  const [tenantForm, setTenantForm] = useState(createEmptyTenantForm)
  const [tenantOnboardingState, setTenantOnboardingState] = useState({ loading: false, success: '', error: '', result: null })
  const [selectedTenantPortfolioCode, setSelectedTenantPortfolioCode] = useState('')
  const [authSessionState, setAuthSessionState] = useState({ loading: false, action: '', username: '', tenantCode: '' })

  const navigate = (nextPage) => {
    const target = platformPages.find((item) => item.key === nextPage) || platformPages[1]
    globalThis.history.pushState({}, '', target.path)
    setPage(target.key)
  }

  const loadOverview = async () => {
    setDataLoading(true)
    setDataError('')
    try {
      const payload = await platformFetch('/api/platform/overview')
      setOverview(payload)
      return payload
    } catch (requestError) {
      setDataError(requestError.message)
      throw requestError
    } finally {
      setDataLoading(false)
    }
  }

  const loadActivity = async () => {
    setActivityState((current) => ({ ...current, loading: true, error: '' }))
    try {
      const items = await platformFetch('/api/platform/activity')
      setActivityState({ items: Array.isArray(items) ? items : [], loading: false, error: '' })
    } catch (requestError) {
      setActivityState((current) => ({ ...current, loading: false, error: requestError.message }))
    }
  }

  useEffect(() => {
    let active = true
    platformFetch('/api/platform/session')
      .then(async (payload) => {
        if (!active) return
        setSession(payload)
        if (payload.signedIn) {
          await loadOverview()
          if (initialPage === 'platform-activity') await loadActivity()
        }
      })
      .catch((requestError) => active && setError(requestError.message))
      .finally(() => active && setSessionLoading(false))
    return () => { active = false }
  }, [])

  useEffect(() => {
    if (session.signedIn && page === 'platform-activity' && !activityState.items.length && !activityState.loading && !activityState.error) loadActivity()
  }, [page, session.signedIn])

  const signIn = async (event) => {
    event.preventDefault()
    setSessionLoading(true)
    setError('')
    try {
      const payload = await platformFetch('/api/platform/session/login', { method: 'POST', body: JSON.stringify(form) })
      setSession(payload)
      setForm((current) => ({ ...current, password: '' }))
      await loadOverview()
      navigate('platform')
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setSessionLoading(false)
    }
  }

  const signOut = async () => {
    try {
      await platformFetch('/api/platform/session/logout', { method: 'POST' })
    } finally {
      setSession(emptySession)
      setOverview(null)
      setActivityState({ items: [], loading: false, error: '' })
      navigate('platform-sign-in')
    }
  }

  const [platformRealtimeState, setPlatformRealtimeState] = useState('connecting')

  usePlatformRealtime({
    signedIn: session.signedIn,
    websocketBrokerUrl: '',
    sockJsUrl: `${apiUrl}/ws`,
    fetchActivity: loadActivity,
    fetchOverview: loadOverview,
    onStateChange: setPlatformRealtimeState,
  })

  const createTenant = async (event) => {
    event.preventDefault()
    setTenantOnboardingState({ loading: true, success: '', error: '', result: null })
    try {
      const tenantCode = tenantForm.tenantCode.trim().toUpperCase()
      const tenantName = tenantForm.tenantName.trim()
      const warehouses = tenantForm.warehouses.map((warehouse) => ({
        code: warehouse.code.trim().toUpperCase(),
        name: warehouse.name.trim(),
        location: warehouse.location.trim(),
      }))
      const users = tenantForm.users.map((user) => ({
        username: user.username.trim().toLowerCase(),
        fullName: user.fullName.trim(),
        operatorActorName: user.operatorActorName.trim(),
        operatorDisplayName: user.operatorDisplayName.trim() || null,
        operatorDescription: user.operatorDescription.trim() || null,
        roles: user.roles,
        warehouseScopes: user.tenantWide ? [] : user.warehouseScopes,
        initialPassword: user.initialPassword.trim() || null,
      }))
      const adminUser = users.find((user) => user.roles.includes('TENANT_ADMIN'))
      const result = await platformFetch('/api/access/tenants', { method: 'POST', body: JSON.stringify({
        tenantCode,
        tenantName,
        description: tenantForm.description.trim() || null,
        adminFullName: adminUser.fullName,
        adminUsername: adminUser.username,
        adminPassword: adminUser.initialPassword,
        primaryLocation: warehouses[0].location,
        secondaryLocation: warehouses[1]?.location || null,
        warehouses,
        users,
        requiredRoles: tenantForm.requiredRoles,
      }) })
      await loadOverview()
      setTenantForm(createEmptyTenantForm())
      setTenantOnboardingState({ loading: false, success: 'Provisioning complete. The explicit configuration was accepted; this does not imply operational data readiness.', error: '', result })
    } catch (requestError) {
      setTenantOnboardingState({ loading: false, success: '', error: requestError.message, result: null })
    }
  }

  if (sessionLoading && !session.signedIn) return <PlatformSignIn form={form} setForm={setForm} busy error={error} onSubmit={signIn} />
  if (!session.signedIn) return <PlatformSignIn form={form} setForm={setForm} busy={sessionLoading} error={error} onSubmit={signIn} />

  const activePage = page === 'platform-sign-in' ? 'platform' : page
  const visiblePages = platformPages.filter((item) => item.key !== 'platform-sign-in')
  const tenantItems = overview?.tenants || []
  const runtime = overview?.runtime || null
  const selectedTenantPortfolio = tenantItems.find((tenant) => tenant.code === selectedTenantPortfolioCode) || null
  const platformSession = { ...session, tenantCode: null, roles: ['PLATFORM_OWNER'] }
  const commonContext = {
    isAuthenticated: true,
    runtime,
    tenantDirectoryState: { items: tenantItems, loading: dataLoading },
    signedInSession: platformSession,
    formatBuildValue,
    formatCodeLabel,
    formatTimestamp,
    getIncidentStatusClassName,
    getRuntimeStatusClassName,
    navigateToPage: navigate,
  }
  const context = {
    platformAdminContext: {
      ...commonContext,
      isPlatformPage: activePage === 'platform',
      systemIncidents: [],
      pendingReplayCount: tenantItems.reduce((total, tenant) => total + tenant.replayAttentionCount, 0),
      selectedTenantPortfolio,
      setSelectedTenantPortfolioCode,
    },
    tenantsContext: {
      ...commonContext,
      isTenantsPage: activePage === 'tenants',
      signedInRoles: ['PLATFORM_OWNER'],
      tenantOnboardingState,
      tenantOnboardingForm: tenantForm,
      setTenantOnboardingForm: setTenantForm,
      onboardTenant: createTenant,
      signInOperator: () => { globalThis.location.href = '/sign-in' },
      authSessionState,
      setAuthSessionState,
    },
    systemConfigContext: { ...commonContext, isSystemConfigPage: activePage === 'system-config', formatMetricValue },
    releasesContext: {
      ...commonContext,
      isReleasesPage: activePage === 'releases',
      frontendBuildVersion: null,
      frontendBuildCommit: null,
      frontendBuildTime: null,
      apiUrl,
      wsUrl: `${apiUrl}/ws`,
      realtimeTransportLabel: 'SockJS / STOMP',
    },
  }

  return <AppShell
    currentPage={activePage}
    pageGroup="platform"
    sidebar={<><div className="workspace-sidebar-header"><button className="brand-lockup brand-button workspace-brand" onClick={() => navigate('platform')} type="button"><span className="brand-mark">S</span><span><strong>SynapseCore</strong><small>Platform control plane</small></span></button><div className="workspace-switcher"><span className="workspace-switcher-label">Platform owner</span><strong>{session.displayName}</strong><p>Separate from customer tenant authority</p></div></div><nav className="workspace-nav" aria-label="Platform control plane navigation"><div className="workspace-nav-group"><div className="workspace-nav-group-header"><p>Control plane</p><span>{visiblePages.length}</span></div><div className="workspace-nav-links">{visiblePages.map((item) => <button className={`workspace-nav-link ${activePage === item.key ? 'workspace-nav-link-active' : ''}`} key={item.key} onClick={() => navigate(item.key)} type="button"><div className="workspace-nav-link-copy"><span>{item.label}</span><small>Platform metadata</small></div></button>)}</div></div></nav><div className="workspace-sidebar-footer"><button className="ghost-button" onClick={signOut} type="button">Sign out platform owner</button></div></>}
    topbar={<header className="workspace-topbar"><div><p className="panel-kicker">Platform authority</p><strong>{pageLookup[activePage]?.label || 'Platform Overview'}</strong></div><span className="workspace-status-pill status-live">Metadata-first control plane</span></header>}
    utilityRail={null}
  >
    {dataError && activePage !== 'platform-activity' ? <PlatformDataState loading={dataLoading} error={dataError} onRetry={loadOverview} /> : dataLoading && !overview && activePage !== 'platform-activity' ? <PlatformDataState loading error="" /> : <PlatformApplicationContent page={activePage} context={context} activityState={activityState} navigate={navigate} platformDataLoading={dataLoading && !overview} platformDataError={dataError} platformRealtimeState={platformRealtimeState} />}
  </AppShell>
}
