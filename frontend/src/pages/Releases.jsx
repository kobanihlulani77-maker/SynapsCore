import { SummaryCard } from '../components/Card'
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
    getRuntimeStatusClassName,
  } = context

  if (!isAuthenticated || !isReleasesPage) return null

  return (
    <section className="content-grid">
      <Panel wide>
        <div className="panel-header">
          <div><p className="panel-kicker">Release and environment</p><h2>Deployment fingerprint, uptime posture, and environment trust</h2></div>
          <span className={`panel-badge ${runtime ? getRuntimeStatusClassName(runtime.overallStatus) : 'audit-badge'}`}>{runtime ? runtime.overallStatus : 'Loading'}</span>
        </div>
        <div className="summary-grid compact-summary-grid">
          <SummaryCard label="Backend version" value={formatBuildValue(runtime?.build?.version)} accent="blue" />
          <SummaryCard label="Frontend version" value={formatBuildValue(frontendBuildVersion)} accent="teal" />
          <SummaryCard label="Commit" value={formatBuildValue(runtime?.build?.commit).slice(0, 7)} accent="amber" />
          <SummaryCard label="Profile" value={runtime?.activeProfiles?.join(', ') || '...'} accent="rose" />
        </div>
        <div className="approval-board">
          <div className="stack-card">
            <div className="stack-title-row"><strong>Backend build</strong><span className="status-tag status-success">{formatBuildValue(runtime?.build?.version)}</span></div>
            <p>Commit {formatBuildValue(runtime?.build?.commit)} | Built {formatBuildValue(runtime?.build?.builtAt)}</p>
            <p className="muted-text">Observed {formatTimestamp(runtime?.observedAt)}</p>
          </div>
          <div className="stack-card">
            <div className="stack-title-row"><strong>Frontend build</strong><span className="status-tag status-success">{formatBuildValue(frontendBuildVersion)}</span></div>
            <p>Commit {formatBuildValue(frontendBuildCommit)} | Built {formatBuildValue(frontendBuildTime)}</p>
            <p className="muted-text">API {apiUrl} | WS {wsUrl}</p>
          </div>
        </div>
        <div className="experience-grid experience-grid-three">
          <article className="stack-card section-card">
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
                <p className="muted-text">This must align with the frontend runtime config for live updates and incident lanes.</p>
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
            <div className="stack-title-row"><strong>Runtime posture</strong><span className="scenario-type-tag">{runtime?.overallStatus || 'Loading'}</span></div>
            <div className="signal-list">
              <div className="signal-list-item">
                <strong>Observed</strong>
                <p>{formatTimestamp(runtime?.observedAt)}</p>
                <p className="muted-text">Use this page as the release trust surface for deployment verification.</p>
              </div>
              <div className="signal-list-item">
                <strong>Queue pressure</strong>
                <p>Pending {runtime?.backbone?.pendingDispatchCount ?? 0} | Failed {runtime?.backbone?.failedDispatchCount ?? 0}</p>
                <p className="muted-text">Release health is not only versioning; it includes live operational pressure.</p>
              </div>
            </div>
          </article>
        </div>
      </Panel>
    </section>
  )
}
