import { SummaryCard } from '../components/Card'
import Panel from '../components/Panel'
import EmptyState from '../components/EmptyState'

export default function PlatformAdminPage({ context }) {
  const {
    isAuthenticated,
    isPlatformPage,
    runtime,
    tenantDirectoryState,
    systemIncidents,
    pendingReplayCount,
    selectedTenantPortfolio,
    setSelectedTenantPortfolioCode,
    signedInSession,
    formatBuildValue,
    formatCodeLabel,
    formatTimestamp,
    getIncidentStatusClassName,
    getRuntimeStatusClassName,
    navigateToPage,
  } = context

  if (!isAuthenticated || !isPlatformPage) return null

  return (
    <section className="content-grid">
      <Panel wide>
        <div className="panel-header">
          <div><p className="panel-kicker">Platform admin</p><h2>Cross-tenant overview and release trust</h2></div>
          <span className={`panel-badge ${runtime ? getRuntimeStatusClassName(runtime.overallStatus) : 'audit-badge'}`}>{runtime ? runtime.overallStatus : 'Loading'}</span>
        </div>
        <div className="summary-grid compact-summary-grid">
          <SummaryCard label="Tenants" value={tenantDirectoryState.items.length} accent="blue" />
          <SummaryCard label="Incidents" value={systemIncidents.length} accent="rose" />
          <SummaryCard label="Pending dispatch" value={runtime?.backbone?.pendingDispatchCount ?? 0} accent="amber" />
          <SummaryCard label="Replay pressure" value={pendingReplayCount} accent="teal" />
        </div>
        <div className="experience-grid experience-grid-three">
          <article className="stack-card section-card">
            <div className="stack-title-row"><strong>Tenant portfolio</strong><span className="scenario-type-tag">{tenantDirectoryState.items.length}</span></div>
            <div className="signal-list">
              {tenantDirectoryState.items.length ? tenantDirectoryState.items.map((tenant) => (
                <button key={tenant.code} className={`signal-list-item selectable-card ${selectedTenantPortfolio?.code === tenant.code ? 'is-selected' : ''}`} onClick={() => setSelectedTenantPortfolioCode(tenant.code)} type="button">
                  <div className="stack-title-row">
                    <strong>{tenant.name}</strong>
                    <span className={`status-tag ${signedInSession?.tenantCode === tenant.code ? 'status-success' : 'status-partial'}`}>{tenant.code}</span>
                  </div>
                  <p>{tenant.description || 'Operational workspace ready for rollout.'}</p>
                  <p className="muted-text">Cross-tenant posture can be managed from the global admin lane.</p>
                </button>
              )) : <EmptyState>Tenant portfolio visibility will appear here as workspaces are created.</EmptyState>}
            </div>
          </article>
          <article className="stack-card section-card">
            <div className="stack-title-row"><strong>Platform incidents</strong><span className="scenario-type-tag">{systemIncidents.length}</span></div>
            <div className="signal-list">
              {systemIncidents.length ? systemIncidents.slice(0, 4).map((incident) => (
                <div key={incident.incidentKey} className="signal-list-item">
                  <div className="stack-title-row">
                    <strong>{incident.title}</strong>
                    <span className={`status-tag ${getIncidentStatusClassName(incident.severity)}`}>{formatCodeLabel(incident.severity)}</span>
                  </div>
                  <p>{incident.detail}</p>
                  <p className="muted-text">{formatTimestamp(incident.createdAt)}</p>
                </div>
              )) : <EmptyState>No active cross-tenant incidents are visible right now.</EmptyState>}
            </div>
          </article>
          <article className="stack-card section-card">
            <div className="stack-title-row"><strong>Release trust</strong><span className={`status-tag ${runtime?.secureSessionCookies ? 'status-success' : 'status-partial'}`}>{runtime?.secureSessionCookies ? 'Secure cookies' : 'Local HTTP'}</span></div>
            <div className="signal-list">
              <div className="signal-list-item">
                <strong>Backend build</strong>
                <p>{formatBuildValue(runtime?.build?.version)}</p>
                <p className="muted-text">Commit {formatBuildValue(runtime?.build?.commit)} | Observed {formatTimestamp(runtime?.observedAt)}</p>
              </div>
              <div className="signal-list-item">
                <strong>Dispatch posture</strong>
                <p>Pending {runtime?.backbone?.pendingDispatchCount ?? 0} | Failed {runtime?.backbone?.failedDispatchCount ?? 0}</p>
                <p className="muted-text">Use this lane to watch backbone pressure before it turns into tenant-facing impact.</p>
              </div>
            </div>
          </article>
        </div>
        <div className="experience-grid experience-grid-split">
          <article className="stack-card section-card">
            <div className="stack-title-row"><strong>Tenant focus</strong><span className="scenario-type-tag">{selectedTenantPortfolio?.code || 'Waiting'}</span></div>
            {selectedTenantPortfolio ? (
              <div className="signal-list">
                <div className="signal-list-item">
                  <strong>{selectedTenantPortfolio.name}</strong>
                  <p>{selectedTenantPortfolio.description || 'Operational workspace ready for rollout.'}</p>
                  <p className="muted-text">Tenant code {selectedTenantPortfolio.code}</p>
                  <p className="muted-text">{signedInSession?.tenantCode === selectedTenantPortfolio.code ? 'Current signed-in tenant workspace.' : 'Cross-tenant portfolio visibility only.'}</p>
                </div>
              </div>
            ) : <EmptyState>Select a tenant workspace to inspect its rollout posture and cross-tenant context.</EmptyState>}
          </article>
          <article className="stack-card section-card">
            <div className="stack-title-row"><strong>Platform response posture</strong><span className="scenario-type-tag">{runtime?.overallStatus || 'Loading'}</span></div>
            <div className="utility-metric-grid">
              <div><span>Tenants</span><strong>{tenantDirectoryState.items.length}</strong></div>
              <div><span>Incidents</span><strong>{systemIncidents.length}</strong></div>
              <div><span>Queued</span><strong>{runtime?.backbone?.pendingDispatchCount ?? 0}</strong></div>
              <div><span>Replay</span><strong>{pendingReplayCount}</strong></div>
            </div>
            <div className="history-action-row">
              <button className="ghost-button" onClick={() => navigateToPage('releases')} type="button">Open Releases</button>
              <button className="ghost-button" onClick={() => navigateToPage('runtime')} type="button">Open Runtime</button>
            </div>
            <p className="muted-text">This lane should help platform owners judge tenant rollout health against incidents, queue pressure, and release trust in one view.</p>
          </article>
        </div>
      </Panel>
    </section>
  )
}
