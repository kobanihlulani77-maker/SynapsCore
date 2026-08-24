import Panel from '../components/Panel'
import EmptyState from '../components/EmptyState'
import LoadingState from '../components/LoadingState'
import StatusBadge from '../components/StatusBadge'

const formatTimestamp = (value) => (value ? new Date(value).toLocaleString() : 'Time not reported')
const statusTone = (value) => ['RECORDED', 'SUCCESS', 'COMPLETED'].includes(String(value).toUpperCase())
  ? 'healthy'
  : ['FAILURE', 'FAILED', 'ERROR'].includes(String(value).toUpperCase()) ? 'critical' : 'warning'

export default function PlatformActivityPage({ activity = [], loading = false, error = '', navigateToPage }) {
  return (
    <section className="content-grid">
      <Panel wide kicker="Platform activity" title="Platform activity and evidence" badge={<span className="panel-badge">Metadata only</span>}>
        <div className="workflow-decision-hero">
          <div className="workflow-decision-copy">
            <strong>Evidence without customer payloads</strong>
            <p>
              Review the latest platform-level business-event and audit metadata. This surface supports support triage;
              it does not expose orders, inventory, inbound bodies, replay payloads, or credentials.
            </p>
            <div className="ops-pill-row">
              <span className="workspace-meta-pill">{loading ? 'Loading activity' : `${activity.length} signals reported`}</span>
              <span className="workspace-meta-pill">Platform Owner scope</span>
              <span className="workspace-meta-pill">No raw payloads</span>
            </div>
          </div>
          <div className="workflow-action-console">
            <div className="workflow-action-card">
              <span>Use this page when</span>
              <strong>Support evidence needs a timeline</strong>
              <p>Start with the tenant directory for counts, runtime for infrastructure posture, or releases for build identity.</p>
            </div>
            <div className="ops-command-actions">
              <button className="ghost-button" onClick={() => navigateToPage('platform')} type="button">Open Platform Overview</button>
              <button className="ghost-button" onClick={() => navigateToPage('system-config')} type="button">Open Platform Runtime</button>
            </div>
          </div>
        </div>

        <div className="experience-grid experience-grid-split">
          <article className="stack-card section-card" id="platform-activity-feed">
            <div className="stack-title-row"><strong>Recent platform signals</strong><span className="scenario-type-tag">{activity.length}</span></div>
            {loading ? <LoadingState label="Loading platform activity metadata..." /> : error ? <div className="notice notice-error" role="alert"><strong>Activity unavailable</strong><p>{error}</p><p>Do not interpret an unavailable feed as an empty or healthy feed.</p></div> : activity.length ? (
              <div className="signal-list">
                {activity.map((item, index) => (
                  <div className="signal-list-item" key={`${item.tenantCode}-${item.observedAt}-${index}`}>
                    <div className="stack-title-row"><strong>{item.tenantCode || 'PLATFORM'}</strong><StatusBadge tone={statusTone(item.status)}>{item.status || 'UNKNOWN'}</StatusBadge></div>
                    <p>{item.category || 'Activity'}: {item.condition || 'Condition not reported'}</p>
                    <p className="muted-text">Observed {formatTimestamp(item.observedAt)}</p>
                  </div>
                ))}
              </div>
            ) : <EmptyState>No platform activity metadata is currently reported. This is an empty feed, not a runtime health assertion.</EmptyState>}
          </article>

          <article className="stack-card section-card" id="platform-activity-boundary">
            <div className="stack-title-row"><strong>Privacy and authority boundary</strong><span className="status-tag status-success">Enforced by API</span></div>
            <div className="signal-list">
              <div className="signal-list-item"><strong>Visible</strong><p>Tenant code, activity category, condition, status, and observed time.</p></div>
              <div className="signal-list-item"><strong>Not visible</strong><p>Customer orders, products, inventory, inbound bodies, replay payloads, connector secrets, and credentials.</p></div>
              <div className="signal-list-item"><strong>Access rule</strong><p>Platform Owner session required. Tenant users must be denied by the backend even when this route is entered directly.</p></div>
            </div>
          </article>
        </div>

        <article className="stack-card section-card" id="platform-activity-next-checks">
          <div className="stack-title-row"><strong>Next operational check</strong><span className="scenario-type-tag">Guidance</span></div>
          <p className="muted-text">Use the signal as evidence, not as an invented explanation. Open the tenant directory for support counts, the runtime page for readiness and realtime state, or Release Trust for deployed identity.</p>
          <div className="history-action-row">
            <button className="ghost-button" onClick={() => navigateToPage('tenants')} type="button">Open Tenant Directory</button>
            <button className="ghost-button" onClick={() => navigateToPage('releases')} type="button">Open Release Trust</button>
          </div>
        </article>
      </Panel>
    </section>
  )
}
