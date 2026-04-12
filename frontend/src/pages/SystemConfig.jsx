import { SummaryCard } from '../components/Card'
import Panel from '../components/Panel'

export default function SystemConfigPage({ context }) {
  const {
    isAuthenticated,
    isSystemConfigPage,
    runtime,
    formatMetricValue,
  } = context

  if (!isAuthenticated || !isSystemConfigPage) return null

  return (
    <section className="content-grid">
      <Panel wide>
        <div className="panel-header">
          <div><p className="panel-kicker">System configuration</p><h2>Runtime defaults, dispatch cadence, and control envelope</h2></div>
          <span className="panel-badge audit-badge">{runtime?.activeProfiles?.join(', ') || 'Loading'}</span>
        </div>
        <div className="summary-grid compact-summary-grid">
          <SummaryCard label="Dispatch interval" value={runtime ? `${runtime.backbone.dispatchIntervalMs} ms` : '...'} accent="blue" />
          <SummaryCard label="Batch size" value={runtime?.backbone?.batchSize ?? 0} accent="teal" />
          <SummaryCard label="Simulation interval" value={runtime ? `${runtime.simulationIntervalMs} ms` : '...'} accent="amber" />
          <SummaryCard label="Allowed origins" value={runtime?.allowedOrigins?.length ?? 0} accent="rose" />
        </div>
        <div className="approval-board">
          <div className="stack-card">
            <div className="stack-title-row"><strong>Realtime and queue backbone</strong><span className="status-tag status-success">Configured</span></div>
            <p>Dispatch queue drains every {runtime?.backbone?.dispatchIntervalMs ?? '...'} ms in batches of {runtime?.backbone?.batchSize ?? '...'}.</p>
            <p className="muted-text">Oldest queued work {runtime?.backbone?.oldestPendingAgeSeconds == null ? 'clear' : `${runtime.backbone.oldestPendingAgeSeconds}s`} | Failed dispatch {runtime?.backbone?.failedDispatchCount ?? 0}</p>
          </div>
          <div className="stack-card">
            <div className="stack-title-row"><strong>Session and origin posture</strong><span className={`status-tag ${runtime?.secureSessionCookies ? 'status-success' : 'status-partial'}`}>{runtime?.secureSessionCookies ? 'Secure' : 'Local HTTP'}</span></div>
            <p>Allowed origins {runtime?.allowedOrigins?.join(', ') || 'Loading'}.</p>
            <p className="muted-text">{runtime?.headerFallbackEnabled ? 'Header fallback remains available.' : 'Session-only tenant resolution is active.'}</p>
          </div>
        </div>
        <div className="experience-grid experience-grid-three">
          <article className="stack-card section-card">
            <div className="stack-title-row"><strong>Operational defaults</strong><span className="scenario-type-tag">{runtime?.activeProfiles?.join(', ') || 'Loading'}</span></div>
            <div className="signal-list">
              <div className="signal-list-item">
                <strong>Simulation cadence</strong>
                <p>{runtime ? `${runtime.simulationIntervalMs} ms` : '...'}</p>
                <p className="muted-text">Controls the local live-stream behavior used for product verification and scenario movement.</p>
              </div>
              <div className="signal-list-item">
                <strong>Header fallback</strong>
                <p>{runtime?.headerFallbackEnabled ? 'Enabled' : 'Disabled'}</p>
                <p className="muted-text">Session-only tenant resolution is the secure production target.</p>
              </div>
            </div>
          </article>
          <article className="stack-card section-card">
            <div className="stack-title-row"><strong>Dispatch envelope</strong><span className="scenario-type-tag">Queue-backed</span></div>
            <div className="signal-list">
              <div className="signal-list-item">
                <strong>Queued now</strong>
                <p>{runtime?.backbone?.pendingDispatchCount ?? 0}</p>
                <p className="muted-text">Tracks the internal operational dispatch queue used to fan out state changes safely.</p>
              </div>
              <div className="signal-list-item">
                <strong>Processed total</strong>
                <p>{formatMetricValue(runtime?.metrics?.dispatchProcessed)}</p>
                <p className="muted-text">Use together with failures and oldest age to decide when to intervene operationally.</p>
              </div>
            </div>
          </article>
          <article className="stack-card section-card">
            <div className="stack-title-row"><strong>Origin and cookie posture</strong><span className="scenario-type-tag">{runtime?.allowedOrigins?.length ?? 0} origins</span></div>
            <div className="signal-list">
              <div className="signal-list-item">
                <strong>Allowed origins</strong>
                <p>{runtime?.allowedOrigins?.join(', ') || 'Loading'}</p>
                <p className="muted-text">Review this before rollout to ensure browser sessions, CORS, and realtime connect cleanly.</p>
              </div>
              <div className="signal-list-item">
                <strong>Session cookies</strong>
                <p>{runtime?.secureSessionCookies ? 'Secure cookies enabled' : 'Local HTTP cookie posture'}</p>
                <p className="muted-text">Flip to secure cookies in public deployment so tenant sessions behave correctly over HTTPS.</p>
              </div>
            </div>
          </article>
        </div>
      </Panel>
    </section>
  )
}

