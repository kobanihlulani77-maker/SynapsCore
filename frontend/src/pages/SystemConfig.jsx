import { MetricCard } from '../components/Card'
import Panel from '../components/Panel'

export default function SystemConfigPage({ context }) {
  const {
    isAuthenticated,
    isSystemConfigPage,
    runtime,
    formatMetricValue,
  } = context

  if (!isAuthenticated || !isSystemConfigPage) return null
  const runtimeReady = runtime?.readinessState === 'UP' || runtime?.overallStatus === 'UP'
  const queueAttention = (runtime?.backbone?.failedDispatchCount ?? 0) > 0 || (runtime?.backbone?.oldestPendingAgeSeconds ?? 0) > 0
  const originAttention = runtime && !runtime.allowedOrigins?.length
  const configPosture = !runtime
    ? 'Unavailable'
    : runtimeReady && !queueAttention && !originAttention
      ? 'Safe'
      : queueAttention || originAttention
        ? 'Watch'
        : 'Incomplete'

  return (
    <section className="content-grid">
      <Panel wide>
        <div className="panel-header">
          <div>
            <p className="panel-kicker">System configuration</p>
            <h2>System configuration and operational defaults</h2>
          </div>
          <span className="panel-badge audit-badge">{runtime?.activeProfiles?.join(', ') || 'Loading'}</span>
        </div>

        <div className="workflow-decision-hero config-trust-hero">
          <div className="workflow-decision-copy">
            <strong>Platform operating envelope</strong>
            <p>
              Translate runtime configuration into operational trust: which environment is visible, which capability could
              be affected, and what a technical administrator should check next.
            </p>
            <div className="ops-pill-row">
              <span className="workspace-meta-pill">Posture {configPosture}</span>
              <span className="workspace-meta-pill">{runtime?.activeProfiles?.join(', ') || 'Environment not reported'}</span>
              <span className="workspace-meta-pill">{runtime?.allowedOrigins?.length ?? 0} allowed origins</span>
            </div>
          </div>
          <div className="workflow-action-console">
            <div className="workflow-action-card">
              <span>Primary capability</span>
              <strong>{queueAttention ? 'Realtime dispatch needs review' : 'Realtime dispatch configured'}</strong>
              <p>Queue cadence and dispatch pressure affect how operational updates fan out across the workspace.</p>
            </div>
            <div className="workflow-action-card">
              <span>Technical next check</span>
              <strong>{originAttention ? 'Review allowed origins' : runtimeReady ? 'Monitor runtime evidence' : 'Confirm readiness'}</strong>
              <p>Only display-safe configuration is shown here; secrets and credentials stay out of the browser.</p>
            </div>
          </div>
        </div>

        <div className="summary-grid compact-summary-grid">
          <MetricCard label="Dispatch interval" value={runtime ? `${runtime.backbone.dispatchIntervalMs} ms` : '...'} accent="blue" note="How frequently the internal dispatch queue drains pending operational work." />
          <MetricCard label="Batch size" value={runtime?.backbone?.batchSize ?? 0} accent="teal" note="How much queue work the platform processes per dispatch cycle." />
          <MetricCard label="Average latency" value={runtime ? `${formatMetricValue(runtime.metrics.averageHttpRequestLatencyMs)} ms` : '...'} accent="amber" note="Observed average request latency across the current runtime window." />
          <MetricCard label="Allowed origins" value={runtime?.allowedOrigins?.length ?? 0} accent="rose" note="Browser origins currently trusted to interact with the live platform." />
        </div>

        <div className="experience-grid experience-grid-split">
          <article className="stack-card section-card workflow-selected-panel admin-focus-panel">
            <div className="stack-title-row"><strong>Realtime and queue backbone</strong><span className={`status-tag ${queueAttention ? 'status-partial' : 'status-success'}`}>{queueAttention ? 'Watch' : 'Configured'}</span></div>
            <div className="signal-list">
              <div className="signal-list-item">
                <strong>Dispatch cadence</strong>
                <p>Dispatch queue drains every {runtime?.backbone?.dispatchIntervalMs ?? '...'} ms in batches of {runtime?.backbone?.batchSize ?? '...'}.</p>
                <p className="muted-text">Operational impact: realtime dashboard and incident updates depend on this fan-out path.</p>
                <p className="muted-text">Oldest queued work {runtime?.backbone?.oldestPendingAgeSeconds == null ? 'clear' : `${runtime.backbone.oldestPendingAgeSeconds}s`} | Failed dispatch {runtime?.backbone?.failedDispatchCount ?? 0}</p>
              </div>
              <div className="signal-list-item">
                <strong>Broker posture</strong>
                <p>Alert hook {runtime?.backbone?.alertHookConfigured ? 'configured' : 'not configured'} | Broker {runtime?.backbone?.realtimeBrokerMode || 'unknown'}</p>
                <p className="muted-text">Use this to understand how live operational state propagates across the platform.</p>
              </div>
            </div>
          </article>

          <article className="stack-card section-card admin-risk-panel">
            <div className="stack-title-row"><strong>Session and origin posture</strong><span className={`status-tag ${runtime?.secureSessionCookies ? 'status-success' : 'status-partial'}`}>{runtime?.secureSessionCookies ? 'Secure' : 'Local HTTP'}</span></div>
            <div className="signal-list">
              <div className="signal-list-item">
                <strong>Allowed origins</strong>
                <p>{runtime?.allowedOrigins?.join(', ') || 'Configuration value not reported'}</p>
                <p className="muted-text">Review this before rollout to ensure browser sessions, CORS, and realtime connect cleanly.</p>
              </div>
              <div className="signal-list-item">
                <strong>Tenant resolution</strong>
                <p>{runtime?.headerFallbackEnabled ? 'Header fallback enabled' : 'Session-only tenant resolution'}</p>
                <p className="muted-text">Session-only resolution is the secure production target for company workspaces.</p>
              </div>
            </div>
          </article>
        </div>

        <div className="experience-grid experience-grid-three">
          <article className="stack-card section-card admin-form-panel">
            <div className="stack-title-row"><strong>Request and access posture</strong><span className="scenario-type-tag">{runtime?.activeProfiles?.join(', ') || 'Loading'}</span></div>
            <div className="signal-list">
              <div className="signal-list-item">
                <strong>Average request latency</strong>
                <p>{runtime ? `${formatMetricValue(runtime.metrics.averageHttpRequestLatencyMs)} ms` : '...'}</p>
                <p className="muted-text">Tracks the average response time across live workspace traffic.</p>
              </div>
              <div className="signal-list-item">
                <strong>Access pressure</strong>
                <p>{formatMetricValue(runtime?.metrics?.authFailures)} auth failures | {formatMetricValue(runtime?.metrics?.rateLimitRejections)} rate-limit rejections</p>
                <p className="muted-text">Helps teams distinguish normal traffic from sign-in abuse or endpoint pressure.</p>
              </div>
            </div>
          </article>

          <article className="stack-card section-card admin-form-panel">
            <div className="stack-title-row"><strong>Dispatch envelope</strong><span className="scenario-type-tag">Queue-backed</span></div>
            <div className="signal-list">
              <div className="signal-list-item">
                <strong>Queued now</strong>
                <p>{runtime?.backbone?.pendingDispatchCount ?? 0}</p>
                <p className="muted-text">Tracks the internal dispatch queue used to fan out state changes safely.</p>
              </div>
              <div className="signal-list-item">
                <strong>Processed total</strong>
                <p>{formatMetricValue(runtime?.metrics?.dispatchProcessed)}</p>
                <p className="muted-text">Use together with failures and oldest age to decide when to intervene operationally.</p>
              </div>
            </div>
          </article>

          <article className="stack-card section-card admin-risk-panel">
            <div className="stack-title-row"><strong>Failure signals</strong><span className="scenario-type-tag">Operator readable</span></div>
            <div className="signal-list">
              <div className="signal-list-item">
                <strong>Integration pressure</strong>
                <p>{formatMetricValue(runtime?.metrics?.integrationFailures)} integration failures | {formatMetricValue(runtime?.metrics?.replayFailures)} replay failures</p>
                <p className="muted-text">Use this alongside replay backlog and connector diagnostics to decide when inbound lanes need intervention.</p>
              </div>
              <div className="signal-list-item">
                <strong>Production posture</strong>
                <p>Live-only</p>
                <p className="muted-text">Development-only reseed helpers are not part of the live operational path.</p>
              </div>
            </div>
          </article>
        </div>
      </Panel>
    </section>
  )
}
