import { MetricCard } from '../components/Card'
import Panel from '../components/Panel'
import LoadingState from '../components/LoadingState'
import StatusBadge from '../components/StatusBadge'

const statusTone = (value) => {
  const normalized = String(value || '').toUpperCase()
  if (['UP', 'CORRECT', 'ACCEPTING_TRAFFIC'].includes(normalized)) return 'healthy'
  if (['DEGRADED', 'UNKNOWN', ''].includes(normalized)) return 'warning'
  return 'critical'
}

const formatValue = (value, fallback = 'Not reported') => value || fallback

export default function SystemConfigPage({ context }) {
  const { isAuthenticated, isSystemConfigPage, runtime } = context

  if (!isAuthenticated || !isSystemConfigPage) return null
  if (!runtime) return <section className="content-grid"><Panel wide><LoadingState label="Loading platform runtime evidence..." /></Panel></section>

  const readiness = formatValue(runtime.readinessState, 'UNKNOWN')
  const liveness = formatValue(runtime.livenessState, 'UNKNOWN')
  const overallStatus = formatValue(runtime.overallStatus, 'UNKNOWN')
  const brokerMode = formatValue(runtime.realtimeBrokerMode, 'UNKNOWN')
  const runtimeHealthy = overallStatus === 'UP' && readiness === 'ACCEPTING_TRAFFIC' && liveness === 'CORRECT'
  const runtimePosture = runtimeHealthy ? 'Healthy' : overallStatus === 'DEGRADED' ? 'Degraded' : 'Review'
  const queueNeedsReview = runtime.failedDispatchCount > 0 || runtime.pendingDispatchCount > 0

  return (
    <section className="content-grid">
      <Panel wide>
        <div className="panel-header">
          <div><p className="panel-kicker">System configuration</p><h2>System configuration and operational defaults</h2></div>
          <StatusBadge tone={statusTone(overallStatus)}>{overallStatus}</StatusBadge>
        </div>

        <div className="workflow-decision-hero config-trust-hero">
          <div className="workflow-decision-copy">
            <strong>Platform runtime trust</strong>
            <p>Use the runtime facts reported by the platform control-plane API to decide whether the service is accepting traffic, degraded, or requires investigation. This page does not infer causes that the backend did not report.</p>
            <div className="ops-pill-row">
              <span className="workspace-meta-pill">Posture {runtimePosture}</span>
              <span className="workspace-meta-pill">Liveness {liveness}</span>
              <span className="workspace-meta-pill">Readiness {readiness}</span>
            </div>
          </div>
          <div className="workflow-action-console">
            <div className="workflow-action-card"><span>Operational meaning</span><strong>{runtimeHealthy ? 'Accepting traffic' : `${runtimePosture} runtime`}</strong><p>Liveness and readiness are shown separately so accepting traffic is not treated as the entire trust decision.</p></div>
            <div className="workflow-action-card"><span>Next diagnostic</span><strong>{queueNeedsReview ? 'Review dispatch pressure' : runtimeHealthy ? 'Continue monitoring evidence' : 'Confirm runtime health'}</strong><p>Use Release Trust for build identity and Activity for metadata-only evidence.</p></div>
          </div>
        </div>

        <div className="summary-grid compact-summary-grid">
          <MetricCard label="Overall" value={overallStatus} accent="blue" note="Combined liveness and readiness posture reported by the backend." />
          <MetricCard label="Liveness" value={liveness} accent="teal" note="Whether the application process reports itself alive." />
          <MetricCard label="Readiness" value={readiness} accent="amber" note="Whether the application reports that it can accept traffic." />
          <MetricCard label="Realtime" value={brokerMode} accent="rose" note="Broker mode reported for platform realtime delivery." />
        </div>

        <div className="experience-grid experience-grid-split">
          <article className="stack-card section-card workflow-selected-panel admin-focus-panel" id="platform-runtime-trust">
            <div className="stack-title-row"><strong>Trust dimensions</strong><StatusBadge tone={statusTone(runtimePosture)}>{runtimePosture}</StatusBadge></div>
            <div className="signal-list">
              <div className="signal-list-item"><strong>Liveness</strong><p>{liveness}</p><p className="muted-text">If liveness is not CORRECT, treat the application as unavailable rather than merely waiting for readiness.</p></div>
              <div className="signal-list-item"><strong>Readiness</strong><p>{readiness}</p><p className="muted-text">If readiness is not ACCEPTING_TRAFFIC, pause operational use and proof until the dependency posture is understood.</p></div>
              <div className="signal-list-item"><strong>Session security</strong><p>{runtime.secureSessionCookies ? 'Secure cookies enabled' : 'Secure cookies not reported as enabled'}</p><p className="muted-text">This is a display-safe security posture; credentials and secrets are never shown.</p></div>
            </div>
          </article>

          <article className="stack-card section-card admin-risk-panel" id="platform-runtime-realtime">
            <div className="stack-title-row"><strong>Realtime and dispatch</strong><StatusBadge tone={queueNeedsReview ? 'warning' : 'healthy'}>{queueNeedsReview ? 'Review' : 'Reported clear'}</StatusBadge></div>
            <div className="signal-list">
              <div className="signal-list-item"><strong>Broker</strong><p>{brokerMode}</p><p className="muted-text">Distributed mode {runtime.realtimeDistributedMode ? 'enabled' : 'not enabled'}; Redis pub/sub {runtime.realtimeRedisPubSubConfigured ? 'configured' : 'not reported'}; STOMP relay {runtime.realtimeStompRelayConfigured ? 'configured' : 'not reported'}.</p></div>
              <div className="signal-list-item"><strong>Dispatch queue</strong><p>Pending {runtime.pendingDispatchCount} | Failed {runtime.failedDispatchCount}</p><p className="muted-text">These are platform-level queue counts. They do not identify a cause or expose tenant payloads.</p></div>
              <div className="signal-list-item"><strong>Alert hook</strong><p>{runtime.alertHookConfigured ? 'Configured' : 'Not reported as configured'}</p><p className="muted-text">Use the reported value as an integration signal, not as proof that every alert was delivered.</p></div>
            </div>
          </article>
        </div>

        <div className="experience-grid experience-grid-three">
          <article className="stack-card section-card admin-form-panel"><div className="stack-title-row"><strong>Release context</strong><span className="scenario-type-tag">{runtime.activeProfiles?.join(', ') || 'Unknown'}</span></div><div className="signal-list"><div className="signal-list-item"><strong>Build</strong><p>{formatValue(runtime.build?.version)}</p><p className="muted-text">Commit {formatValue(runtime.build?.commit)} | Built {formatValue(runtime.build?.builtAt)}</p></div></div></article>
          <article className="stack-card section-card admin-form-panel"><div className="stack-title-row"><strong>Deployment context</strong><span className="scenario-type-tag">{formatValue(runtime.build?.runtime, 'Unknown')}</span></div><div className="signal-list"><div className="signal-list-item"><strong>Observed</strong><p>{formatValue(runtime.observedAt)}</p><p className="muted-text">Use this timestamp when correlating platform evidence with an incident or release check.</p></div></div></article>
          <article className="stack-card section-card admin-risk-panel"><div className="stack-title-row"><strong>Next action</strong><span className="scenario-type-tag">Operator guidance</span></div><div className="signal-list"><div className="signal-list-item"><strong>{runtimeHealthy && !queueNeedsReview ? 'Monitor' : 'Investigate'}</strong><p>{runtimeHealthy && !queueNeedsReview ? 'Continue monitoring runtime evidence.' : 'Confirm the failing or degraded dimension before operating or proving.'}</p><p className="muted-text">Platform Activity and Release Trust provide the next evidence surfaces.</p></div></div></article>
        </div>
      </Panel>
    </section>
  )
}
