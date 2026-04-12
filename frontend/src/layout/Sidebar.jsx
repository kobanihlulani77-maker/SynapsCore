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
  return (
    <>
      <button className="brand-lockup brand-button workspace-brand" onClick={() => navigateToPage('dashboard')} type="button">
        <span className="brand-mark">S</span>
        <span><strong>SynapseCore</strong><small>{signedInSession?.tenantName || signedInSession?.tenantCode || 'Operational workspace'}</small></span>
      </button>
      <div className="workspace-switcher">
        <span className="workspace-switcher-label">Workspace</span>
        <strong>{signedInSession?.tenantName || signedInSession?.tenantCode || 'Signed out'}</strong>
        <p>{signedInSession ? `${signedInSession.displayName} | ${signedInSession.actorName}` : 'Use the sign-in page to open a tenant workspace.'}</p>
      </div>
      <nav className="workspace-nav">
        {navGroups.map((group) => (
          <div key={group.label} className="workspace-nav-group">
            <p>{group.label}</p>
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
                    <span>{page.label}</span>
                    <strong>{pageBadgeMap[page.key] || 0}</strong>
                  </button>
                )
              })}
            </div>
          </div>
        ))}
      </nav>
      <div className="workspace-sidebar-footer">
        <div className="workspace-sidebar-status">
          <span className={`live-dot status-${connectionState}`} />
          <span>{connectionState === 'live' ? 'Realtime live' : `Realtime ${formatCodeLabel(connectionState)}`}</span>
        </div>
        <button className="ghost-button" onClick={() => navigateToPage('profile')} type="button">Profile & Session</button>
      </div>
    </>
  )
}
