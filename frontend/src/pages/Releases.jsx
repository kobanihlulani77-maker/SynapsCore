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

  return (
    <section className="content-grid">
      <Panel wide>
        <div className="panel-header">
          <div>
            <p className="panel-kicker">Release and environment</p>
            <h2>Deployment fingerprint, uptime posture, and environment trust</h2>
          </div>
          <span className={`panel-badge ${runtime ? getRuntimeStatusClassName(runtime.overallStatus) : 'audit-badge'}`}>{runtime ? runtime.overallStatus : 'Loading'}</span>
        </div>

        <div className="ops-command-hero">
          <div className="ops-command-copy">
            <strong>Release trust surface</strong>
            <p>
              This page should make deployment identity feel trustworthy and readable: what is running, when it was built,
              what endpoints the frontend expects, and whether the current runtime posture still looks safe.
            </p>
            <div className="ops-pill-row">
              <span className="workspace-meta-pill">Build fingerprint</span>
              <span className="workspace-meta-pill">Endpoint aware</span>
              <span className="workspace-meta-pill">Runtime linked</span>
            </div>
          </div>
        </div>

        <div className="summary-grid compact-summary-grid">
          <MetricCard label="Backend version" value={formatBuildValue(runtime?.build?.version)} accent="blue" note="Version currently served by the live backend runtime." />
          <MetricCard label="Frontend version" value={formatBuildValue(frontendBuildVersion)} accent="teal" note="Version currently loaded by the workspace frontend." />
          <MetricCard label="Commit" value={formatBuildValue(runtime?.build?.commit).slice(0, 7)} accent="amber" note="Short backend commit fingerprint for release verification." />
          <MetricCard label="Profiles" value={runtime?.activeProfiles?.join(', ') || '...'} accent="rose" note="Runtime profile posture currently active in the environment." />
        </div>

        <div className="experience-grid experience-grid-split">
          <article className="stack-card section-card" id="releases-builds">
            <div className="stack-title-row"><strong>Backend build</strong><span className="status-tag status-success">{formatBuildValue(runtime?.build?.version)}</span></div>
            <div className="signal-list">
              <div className="signal-list-item">
                <strong>Release identity</strong>
                <p>Commit {formatBuildValue(runtime?.build?.commit)} | Built {formatBuildValue(runtime?.build?.builtAt)}</p>
                <p className="muted-text">Observed {formatTimestamp(runtime?.observedAt)}</p>
              </div>
              <div className="signal-list-item">
                <strong>Runtime posture</strong>
                <p>{runtime?.overallStatus || 'Loading'}</p>
                <p className="muted-text">Release trust is not only versioning; it includes the live runtime posture now serving operators.</p>
              </div>
            </div>
          </article>

          <article className="stack-card section-card">
            <div className="stack-title-row"><strong>Frontend build</strong><span className="status-tag status-success">{formatBuildValue(frontendBuildVersion)}</span></div>
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
          <article className="stack-card section-card" id="releases-checklist">
            <div className="stack-title-row"><strong>Environment checklist</strong><span className="scenario-type-tag">{runtime?.activeProfiles?.join(', ') || 'Loading'}</span></div>
            <div className="signal-list">
              <div className="signal-list-item">
                <strong>Runtime readiness</strong>
                <p>{runtime ? formatCodeLabel(runtime.readinessState) : 'Loading'}</p>
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
                <p>Pending {runtime?.backbone?.pendingDispatchCount ?? 0} | Failed {runtime?.backbone?.failedDispatchCount ?? 0}</p>
                <p className="muted-text">Release health includes live operational pressure, not just versioning.</p>
              </div>
            </div>
          </article>
        </div>
      </Panel>
    </section>
  )
}
