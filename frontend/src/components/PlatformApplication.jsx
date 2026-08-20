import { useEffect, useState } from 'react'
import AppShell from '../layout/AppShell'
import Panel from './Panel'
import { platformPages } from '../config/pageRegistry'
import { apiUrl, extractResponseErrorMessage, readResponsePayload } from '../services/api'

const emptySession = { signedIn: false }

const platformFetch = async (path, init = {}) => {
  const response = await fetch(`${apiUrl}${path}`, {
    credentials: 'include',
    ...init,
    headers: { 'Content-Type': 'application/json', ...(init.headers || {}) },
  })
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
            <p>The normal control plane shows platform health, tenant support summaries, release identity, and operational conditions. It does not browse raw customer business payloads.</p>
          </div>
        </article>
        <article className="public-signin-card enterprise-signin-form">
          <p className="panel-kicker">Platform owner sign in</p>
          <h2>Open the control plane</h2>
          <form className="signin-form-shell" onSubmit={onSubmit}>
            <label className="field">
              <span>Platform username</span>
              <input autoComplete="username" value={form.username} onChange={(event) => setForm((current) => ({ ...current, username: event.target.value }))} />
            </label>
            <label className="field">
              <span>Password</span>
              <input type="password" autoComplete="current-password" value={form.password} onChange={(event) => setForm((current) => ({ ...current, password: event.target.value }))} />
            </label>
            {error ? <div className="notice notice-error" role="alert"><strong>Platform sign-in needs attention</strong><p>{error}</p></div> : null}
            <button className="primary-button" disabled={busy || !form.username.trim() || !form.password} type="submit">
              {busy ? 'Verifying authority...' : 'Enter Platform Control Plane'}
            </button>
          </form>
        </article>
      </section>
    </main>
  )
}

function TenantDirectory({ tenants, form, setForm, onCreate, busy, message }) {
  return (
    <div className="content-grid">
      <Panel wide>
        <div className="panel-header"><div><p className="panel-kicker">Tenant metadata</p><h2>Workspace support directory</h2></div><span className="panel-badge">{tenants.length}</span></div>
        <div className="data-grid"><table className="data-grid-table"><thead><tr><th>Tenant</th><th>State</th><th>Users</th><th>Connectors</th><th>Inbound</th><th>Replay</th><th>Alerts</th></tr></thead><tbody>
          {tenants.map((tenant) => <tr key={tenant.code}><td><strong>{tenant.name}</strong><span>{tenant.code}</span></td><td>{tenant.supportState}</td><td>{tenant.activeUserCount}</td><td>{tenant.connectorCount} ({tenant.disabledConnectorCount} disabled)</td><td>{tenant.failedInboundCount}</td><td>{tenant.replayAttentionCount}</td><td>{tenant.activeAlertCount}</td></tr>)}
        </tbody></table></div>
      </Panel>
      <Panel>
        <div className="panel-header"><div><p className="panel-kicker">Controlled onboarding</p><h2>Create a tenant workspace</h2></div></div>
        <form className="admin-form-grid" onSubmit={onCreate}>
          <label className="field"><span>Tenant code</span><input value={form.tenantCode} onChange={(event) => setForm((current) => ({ ...current, tenantCode: event.target.value.toUpperCase() }))} /></label>
          <label className="field"><span>Tenant name</span><input value={form.tenantName} onChange={(event) => setForm((current) => ({ ...current, tenantName: event.target.value }))} /></label>
          <label className="field"><span>Admin full name</span><input value={form.adminFullName} onChange={(event) => setForm((current) => ({ ...current, adminFullName: event.target.value }))} /></label>
          <label className="field"><span>Admin username</span><input autoComplete="off" value={form.adminUsername} onChange={(event) => setForm((current) => ({ ...current, adminUsername: event.target.value }))} /></label>
          <label className="field"><span>Temporary admin password</span><input type="password" autoComplete="new-password" value={form.adminPassword} onChange={(event) => setForm((current) => ({ ...current, adminPassword: event.target.value }))} /></label>
          <label className="field"><span>Primary location</span><input value={form.primaryLocation} onChange={(event) => setForm((current) => ({ ...current, primaryLocation: event.target.value }))} /></label>
          <button className="primary-button" disabled={busy} type="submit">{busy ? 'Creating workspace...' : 'Create Tenant Workspace'}</button>
        </form>
        {message ? <p className="muted-text">{message}</p> : null}
      </Panel>
    </div>
  )
}

function PlatformContent({ page, overview, tenantForm, setTenantForm, createTenant, createBusy, createMessage }) {
  const runtime = overview?.runtime
  if (page === 'tenants') {
    return <TenantDirectory tenants={overview?.tenants || []} form={tenantForm} setForm={setTenantForm} onCreate={createTenant} busy={createBusy} message={createMessage} />
  }

  if (page === 'system-config' || page === 'releases') {
    return <section className="content-grid"><Panel wide><div className="panel-header"><div><p className="panel-kicker">Platform runtime</p><h2>{page === 'releases' ? 'Release identity and deployment trust' : 'Infrastructure health and realtime posture'}</h2></div><span className="panel-badge">{runtime?.overallStatus || 'Unknown'}</span></div><div className="summary-grid compact-summary-grid"><article className="metric-card"><span>Readiness</span><strong>{runtime?.readinessState || 'Unknown'}</strong></article><article className="metric-card"><span>Liveness</span><strong>{runtime?.livenessState || 'Unknown'}</strong></article><article className="metric-card"><span>Realtime</span><strong>{runtime?.realtimeBrokerMode || 'Unknown'}</strong></article><article className="metric-card"><span>Release</span><strong>{runtime?.build?.version || 'Untracked'}</strong></article></div><div className="signal-list"><div className="signal-list-item"><strong>Build commit</strong><p>{runtime?.build?.commit || 'Untracked'}</p></div><div className="signal-list-item"><strong>Dispatch support state</strong><p>Pending {runtime?.pendingDispatchCount ?? 0} | Failed {runtime?.failedDispatchCount ?? 0}</p></div></div></Panel></section>
  }

  return <section className="content-grid"><Panel wide><div className="panel-header"><div><p className="panel-kicker">Platform control plane</p><h2>Health, tenant posture, and support signals</h2></div><span className="panel-badge">{runtime?.overallStatus || 'Unknown'}</span></div><div className="summary-grid compact-summary-grid"><article className="metric-card"><span>Tenants</span><strong>{overview?.tenants?.length || 0}</strong></article><article className="metric-card"><span>Need attention</span><strong>{overview?.tenants?.filter((tenant) => tenant.supportState === 'ATTENTION').length || 0}</strong></article><article className="metric-card"><span>Readiness</span><strong>{runtime?.readinessState || 'Unknown'}</strong></article><article className="metric-card"><span>Activity signals</span><strong>{overview?.activity?.length || 0}</strong></article></div><div className="experience-grid experience-grid-split"><article className="stack-card section-card"><div className="stack-title-row"><strong>Tenant support posture</strong><span className="scenario-type-tag">Metadata only</span></div><div className="signal-list">{overview?.tenants?.map((tenant) => <div className="signal-list-item" key={tenant.code}><strong>{tenant.name}</strong><p>{tenant.supportState} | Failed inbound {tenant.failedInboundCount} | Replay {tenant.replayAttentionCount} | Alerts {tenant.activeAlertCount}</p></div>)}</div></article><article className="stack-card section-card"><div className="stack-title-row"><strong>Platform activity</strong><span className="scenario-type-tag">No payloads</span></div><div className="signal-list">{overview?.activity?.map((item, index) => <div className="signal-list-item" key={`${item.observedAt}-${index}`}><strong>{item.tenantCode} | {item.condition}</strong><p>{item.category} | {item.status}</p></div>)}</div></article></div></Panel></section>
}

export default function PlatformApplication({ initialPage }) {
  const [page, setPage] = useState(initialPage)
  const [session, setSession] = useState(emptySession)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [form, setForm] = useState({ username: '', password: '' })
  const [overview, setOverview] = useState(null)
  const [tenantForm, setTenantForm] = useState({ tenantCode: '', tenantName: '', description: 'Controlled pilot workspace.', adminFullName: '', adminUsername: '', adminPassword: '', primaryLocation: '' })
  const [createState, setCreateState] = useState({ busy: false, message: '' })

  const navigate = (nextPage) => {
    const target = platformPages.find((item) => item.key === nextPage) || platformPages[1]
    globalThis.history.pushState({}, '', target.path)
    setPage(target.key)
  }

  const loadOverview = async () => {
    const payload = await platformFetch('/api/platform/overview')
    setOverview(payload)
  }

  useEffect(() => {
    let active = true
    platformFetch('/api/platform/session')
      .then(async (payload) => {
        if (!active) return
        setSession(payload)
        if (payload.signedIn) await loadOverview()
      })
      .catch((requestError) => active && setError(requestError.message))
      .finally(() => active && setLoading(false))
    return () => { active = false }
  }, [])

  const signIn = async (event) => {
    event.preventDefault()
    setLoading(true)
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
      setLoading(false)
    }
  }

  const signOut = async () => {
    await platformFetch('/api/platform/session/logout', { method: 'POST' })
    setSession(emptySession)
    setOverview(null)
    navigate('platform-sign-in')
  }

  const createTenant = async (event) => {
    event.preventDefault()
    setCreateState({ busy: true, message: '' })
    try {
      await platformFetch('/api/access/tenants', { method: 'POST', body: JSON.stringify(tenantForm) })
      await loadOverview()
      setTenantForm({ tenantCode: '', tenantName: '', description: 'Controlled pilot workspace.', adminFullName: '', adminUsername: '', adminPassword: '', primaryLocation: '' })
      setCreateState({ busy: false, message: 'Tenant workspace created through the platform-owner session.' })
    } catch (requestError) {
      setCreateState({ busy: false, message: requestError.message })
    }
  }

  if (!session.signedIn || page === 'platform-sign-in') {
    return <PlatformSignIn form={form} setForm={setForm} busy={loading} error={error} onSubmit={signIn} />
  }

  const visiblePages = platformPages.filter((item) => item.key !== 'platform-sign-in')
  return <AppShell currentPage={page} pageGroup="platform" sidebar={<><div className="workspace-sidebar-header"><button className="brand-lockup brand-button workspace-brand" onClick={() => navigate('platform')} type="button"><span className="brand-mark">S</span><span><strong>SynapseCore</strong><small>Platform control plane</small></span></button><div className="workspace-switcher"><span className="workspace-switcher-label">Platform owner</span><strong>{session.displayName}</strong><p>Separate from customer tenant authority</p></div></div><nav className="workspace-nav"><div className="workspace-nav-group"><div className="workspace-nav-group-header"><p>Control plane</p><span>{visiblePages.length}</span></div><div className="workspace-nav-links">{visiblePages.map((item) => <button className={`workspace-nav-link ${page === item.key ? 'workspace-nav-link-active' : ''}`} key={item.key} onClick={() => navigate(item.key)} type="button"><div className="workspace-nav-link-copy"><span>{item.label}</span><small>Platform metadata</small></div></button>)}</div></div></nav><div className="workspace-sidebar-footer"><button className="ghost-button" onClick={signOut} type="button">Sign out platform owner</button></div></>} topbar={<header className="workspace-topbar"><div><p className="panel-kicker">Platform authority</p><strong>{visiblePages.find((item) => item.key === page)?.label || 'Platform Overview'}</strong></div><span className="workspace-status-pill status-live">Metadata-first control plane</span></header>} utilityRail={null}><PlatformContent page={page} overview={overview} tenantForm={tenantForm} setTenantForm={setTenantForm} createTenant={createTenant} createBusy={createState.busy} createMessage={createState.message} /></AppShell>
}
