export default function Sidebar({
  signedInSession,
  navigateToPage,
  navGroups,
  pageLookup,
  currentPage,
  pageBadgeMap,
  connectionState,
  formatCodeLabel,
}) {
  const roleSummary = signedInSession?.roles?.length
    ? signedInSession.roles.map((role) => formatCodeLabel(role)).join(' / ')
    : 'Workspace operator'
  const scopeSummary = signedInSession?.warehouseScopes?.length
    ? signedInSession.warehouseScopes.join(', ')
    : 'Tenant-wide'

  return (
    <>
      <div className="workspace-sidebar-header">
        <button className="brand-lockup brand-button workspace-brand" onClick={() => navigateToPage('dashboard')} type="button">
          <span className="brand-mark">S</span>
          <span><strong>SynapseCore</strong><small>{signedInSession?.tenantName || signedInSession?.tenantCode || 'Operational workspace'}</small></span>
        </button>
        <div className="workspace-switcher">
          <span className="workspace-switcher-label">Company workspace</span>
          <strong>{signedInSession?.tenantName || signedInSession?.tenantCode || 'Signed out'}</strong>
          <p>{signedInSession ? `${signedInSession.tenantCode} | ${signedInSession.displayName}` : 'Use sign-in to open a company workspace.'}</p>
          <div className="workspace-switcher-meta">
            <span className="workspace-meta-pill">{roleSummary}</span>
            <span className="workspace-meta-pill">Scope {scopeSummary}</span>
          </div>
        </div>
      </div>
      <nav className="workspace-nav">
        {navGroups.map((group) => (
          <div key={group.label} className="workspace-nav-group">
            <div className="workspace-nav-group-header">
              <p>{group.label}</p>
              <span>{group.keys.length}</span>
            </div>
            <div className="workspace-nav-links">
              {group.keys.map((pageKey) => {
                const page = pageLookup[pageKey]
                return (
                  <button
                    key={page.key}
                    className={`workspace-nav-link ${currentPage === page.key ? 'workspace-nav-link-active' : ''}`}
                    onClick={() => navigateToPage(page.key)}
                    type="button"
                  >
                    <div className="workspace-nav-link-copy">
                      <span>{page.label}</span>
                      <small>{page.focus?.[0] || page.description}</small>
                    </div>
                    <strong>{pageBadgeMap[page.key] || 0}</strong>
                  </button>
                )
              })}
            </div>
          </div>
        ))}
      </nav>
      <div className="workspace-sidebar-footer">
        <div className="workspace-sidebar-status workspace-shell-health">
          <span className={`live-dot status-${connectionState}`} />
          <div>
            <strong>{connectionState === 'live' ? 'Live control signal' : `Realtime ${formatCodeLabel(connectionState)}`}</strong>
            <span>{signedInSession?.actorName || 'Workspace identity'} | {signedInSession?.tenantCode || 'No workspace'}</span>
          </div>
        </div>
        <div className="workspace-sidebar-quick-actions">
          <button className="ghost-button" onClick={() => navigateToPage('runtime')} type="button">Runtime</button>
          <button className="ghost-button" onClick={() => navigateToPage('profile')} type="button">Profile & Session</button>
        </div>
      </div>
    </>
  )
}
