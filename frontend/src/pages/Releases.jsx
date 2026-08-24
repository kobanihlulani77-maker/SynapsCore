import { MetricCard } from '../components/Card'
import Panel from '../components/Panel'

export default function ReleasesPage({ context }) {
  const {
    isAuthenticated,
    isReleasesPage,
    runtime,
    formatBuildValue,
    formatCodeLabel,
    formatTimestamp,
    frontendBuildVersion,
    frontendBuildCommit,
    frontendBuildTime,
    apiUrl,
    wsUrl,
    realtimeTransportLabel,
    getRuntimeStatusClassName,
  } = context

  if (!isAuthenticated || !isReleasesPage) return null
  const runtimeStatus = runtime?.overallStatus || 'Not reported'
  const backendVersion = formatBuildValue(runtime?.build?.version)
  const backendCommit = formatBuildValue(runtime?.build?.commit)
  const releaseEvidenceLabel = runtime?.readinessState ? formatCodeLabel(runtime.readinessState) : 'Unavailable'

  return (
    <section className="content-grid">
      <Panel wide>
        <div className="panel-header">
          <div>
            <p className="panel-kicker">Release and environment</p>
            <h2>Release, deployment, and environment</h2>
          </div>
          <span className={`panel-badge ${runtime ? getRuntimeStatusClassName(runtime.overallStatus) : 'audit-badge'}`}>{runtime ? runtime.overallStatus : 'Loading'}</span>
        </div>

        <div className="workflow-decision-hero release-trust-hero">
          <div className="workflow-decision-copy">
            <strong>Release trust surface</strong>
            <p>
              Confirm what version is running, which commit it reports, which endpoints the frontend is using, and what
              live runtime evidence is available before treating this environment as trusted.
            </p>
            <div className="ops-pill-row">
              <span className="workspace-meta-pill">{runtimeStatus}</span>
              <span className="workspace-meta-pill">Evidence {releaseEvidenceLabel}</span>
              <span className="workspace-meta-pill">{backendCommit === 'untracked' ? 'Commit not reported' : backendCommit.slice(0, 7)}</span>
            </div>
          </div>
          <div className="workflow-action-console">
            <div className="workflow-action-card">
              <span>Current backend</span>
              <strong>{backendVersion}</strong>
              <p>Commit {backendCommit}. Built {formatBuildValue(runtime?.build?.builtAt)}.</p>
            </div>
            <div className="workflow-action-card">
              <span>Current frontend</span>
              <strong>{formatBuildValue(frontendBuildVersion)}</strong>
              <p>Commit {formatBuildValue(frontendBuildCommit)}. Built {formatBuildValue(frontendBuildTime)}.</p>
            </div>
          </div>
        </div>

        <div className="summary-grid compact-summary-grid">
          <MetricCard label="Backend version" value={backendVersion} accent="blue" note="Version currently served by the live backend runtime." />
          <MetricCard label="Frontend version" value={formatBuildValue(frontendBuildVersion)} accent="teal" note="Version currently loaded by the workspace frontend." />
          <MetricCard label="Readiness" value={releaseEvidenceLabel} accent="amber" note="Runtime readiness evidence reported by the backend." />
          <MetricCard label="Profiles" value={runtime?.activeProfiles?.join(', ') || 'Not reported'} accent="rose" note="Runtime profile posture currently active in the environment." />
        </div>

        <div className="experience-grid experience-grid-split">
          <article className="stack-card section-card workflow-selected-panel admin-focus-panel" id="releases-builds">
            <div className="stack-title-row"><strong>Backend build</strong><span className={`status-tag ${runtime ? getRuntimeStatusClassName(runtime.overallStatus) : 'status-partial'}`}>{backendVersion}</span></div>
            <div className="signal-list">
              <div className="signal-list-item">
                <strong>Release identity</strong>
                <p>Commit {backendCommit} | Built {formatBuildValue(runtime?.build?.builtAt)}</p>
                <p className="muted-text">Observed {formatTimestamp(runtime?.observedAt)}</p>
              </div>
              <div className="signal-list-item">
                <strong>Runtime posture</strong>
                <p>{runtime?.overallStatus || 'Loading'}</p>
                <p className="muted-text">Release trust is not only versioning; it includes the live runtime posture now serving operators.</p>
              </div>
            </div>
          </article>

          <article className="stack-card section-card admin-form-panel">
            <div className="stack-title-row"><strong>Frontend build</strong><span className="status-tag status-partial">{formatBuildValue(frontendBuildVersion)}</span></div>
            <div className="signal-list">
              <div className="signal-list-item">
                <strong>Release identity</strong>
                <p>Commit {formatBuildValue(frontendBuildCommit)} | Built {formatBuildValue(frontendBuildTime)}</p>
              </div>
              <div className="signal-list-item">
                <strong>Expected endpoints</strong>
                <p>API {apiUrl}</p>
                <p className="muted-text">Realtime {wsUrl}</p>
                <p className="muted-text">{realtimeTransportLabel}</p>
              </div>
            </div>
          </article>
        </div>

        <div className="experience-grid experience-grid-three">
          <article className="stack-card section-card admin-risk-panel" id="releases-checklist">
            <div className="stack-title-row"><strong>Environment checklist</strong><span className="scenario-type-tag">{runtime?.activeProfiles?.join(', ') || 'Loading'}</span></div>
            <div className="signal-list">
              <div className="signal-list-item">
                <strong>Runtime readiness</strong>
                <p>{runtime ? formatCodeLabel(runtime.readinessState) : 'Unavailable'}</p>
                <p className="muted-text">The environment should report UP before teams start using the control center live.</p>
              </div>
              <div className="signal-list-item">
                <strong>Realtime endpoint</strong>
                <p>{wsUrl}</p>
                <p className="muted-text">{realtimeTransportLabel}. This must align with frontend runtime config for live updates and incident lanes.</p>
              </div>
            </div>
          </article>

          <article className="stack-card section-card">
            <div className="stack-title-row"><strong>Build fingerprint</strong><span className="scenario-type-tag">Trusted surface</span></div>
            <div className="signal-list">
              <div className="signal-list-item">
                <strong>Backend</strong>
                <p>{formatBuildValue(runtime?.build?.version)}</p>
                <p className="muted-text">Commit {formatBuildValue(runtime?.build?.commit)}</p>
              </div>
              <div className="signal-list-item">
                <strong>Frontend</strong>
                <p>{formatBuildValue(frontendBuildVersion)}</p>
                <p className="muted-text">Commit {formatBuildValue(frontendBuildCommit)}</p>
              </div>
            </div>
          </article>

          <article className="stack-card section-card">
            <div className="stack-title-row"><strong>Operational trust</strong><span className="scenario-type-tag">{runtime?.overallStatus || 'Loading'}</span></div>
            <div className="signal-list">
              <div className="signal-list-item">
                <strong>Observed</strong>
                <p>{formatTimestamp(runtime?.observedAt)}</p>
                <p className="muted-text">Use this page as the release trust surface for deployment verification.</p>
              </div>
              <div className="signal-list-item">
                <strong>Queue pressure</strong>
                <p>Pending {runtime?.pendingDispatchCount ?? 0} | Failed {runtime?.failedDispatchCount ?? 0}</p>
                <p className="muted-text">Release health includes live operational pressure, not just versioning.</p>
              </div>
            </div>
          </article>
        </div>
      </Panel>
    </section>
  )
}
