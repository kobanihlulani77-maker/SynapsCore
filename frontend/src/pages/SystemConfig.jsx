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
          <SummaryCard label="Average request latency" value={runtime ? `${formatMetricValue(runtime.metrics.averageHttpRequestLatencyMs)} ms` : '...'} accent="amber" />
          <SummaryCard label="Allowed origins" value={runtime?.allowedOrigins?.length ?? 0} accent="rose" />
          <SummaryCard label="Auth failures" value={formatMetricValue(runtime?.metrics?.authFailures)} accent="rose" />
          <SummaryCard label="Rate-limit rejections" value={formatMetricValue(runtime?.metrics?.rateLimitRejections)} accent="amber" />
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
            <div className="stack-title-row"><strong>Request and access posture</strong><span className="scenario-type-tag">{runtime?.activeProfiles?.join(', ') || 'Loading'}</span></div>
            <div className="signal-list">
              <div className="signal-list-item">
                <strong>Average request latency</strong>
                <p>{runtime ? `${formatMetricValue(runtime.metrics.averageHttpRequestLatencyMs)} ms` : '...'}</p>
                <p className="muted-text">Tracks the average response time across live workspace traffic for this tenant scope.</p>
              </div>
              <div className="signal-list-item">
                <strong>Access pressure</strong>
                <p>{formatMetricValue(runtime?.metrics?.authFailures)} auth failures | {formatMetricValue(runtime?.metrics?.rateLimitRejections)} rate-limit rejections</p>
                <p className="muted-text">This surface helps operators distinguish normal traffic from sign-in abuse or bootstrap endpoint pressure.</p>
              </div>
              <div className="signal-list-item">
                <strong>Tenant resolution</strong>
                <p>{runtime?.headerFallbackEnabled ? 'Enabled' : 'Disabled'}</p>
                <p className="muted-text">Session-only tenant resolution is the secure production target.</p>
              </div>
              <div className="signal-list-item">
                <strong>Production posture</strong>
                <p>Live-only</p>
                <p className="muted-text">Development-only reseed helpers are not part of the live operational platform path.</p>
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
              <div className="signal-list-item">
                <strong>Realtime and contention</strong>
                <p>{formatMetricValue(runtime?.metrics?.realtimePublishes)} realtime publishes | {formatMetricValue(runtime?.metrics?.inventoryLockConflicts)} inventory lock conflicts</p>
                <p className="muted-text">Distributed fanout volume and inventory contention are visible here before they turn into user-facing instability.</p>
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
