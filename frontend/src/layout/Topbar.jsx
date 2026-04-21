export default function Topbar({
  effectivePageMeta,
  workspaceSearch,
  setWorkspaceSearch,
  searchInputRef,
  firstWorkspaceSearchResult,
  navigateToPage,
  hasWorkspaceSearch,
  workspaceSearchMatchCount,
  workspaceSearchSections,
  pageSectionActions,
  jumpToPageSection,
  liveClockLabel,
  connectionState,
  formatCodeLabel,
  globalNotificationCount,
  topbarQuickActions,
  pageState,
  actionState,
  systemRuntimeState,
  signedInSession,
  signOutOperator,
  authSessionState,
}) {
  return (
    <header className="workspace-topbar">
      <div>
        <p className="eyebrow">{effectivePageMeta.group ? effectivePageMeta.group.toUpperCase() : 'WORKSPACE'}</p>
        <h1>{effectivePageMeta.title}</h1>
        <p className="workspace-topbar-copy">{effectivePageMeta.description}</p>
      </div>
      <div className="workspace-topbar-actions">
        <div className="workspace-topbar-search">
          <label className="field workspace-search-field">
            <span>Global search</span>
            <input
              ref={searchInputRef}
              type="text"
              value={workspaceSearch}
              onChange={(event) => setWorkspaceSearch(event.target.value)}
              onKeyDown={(event) => {
                if (event.key === 'Enter' && firstWorkspaceSearchResult) {
                  event.preventDefault()
                  navigateToPage(firstWorkspaceSearchResult.target)
                  setWorkspaceSearch('')
                }
              }}
              placeholder="Search pages, orders, or alerts"
            />
          </label>
          {hasWorkspaceSearch ? (
            <div className="workspace-search-results">
              <div className="workspace-search-results-header">
                <strong>{workspaceSearchMatchCount ? `${workspaceSearchMatchCount} match${workspaceSearchMatchCount === 1 ? '' : 'es'}` : 'No matches yet'}</strong>
                <div className="workspace-search-shortcuts">
                  <span className="workspace-shortcut-chip">/ focus</span>
                  <span className="workspace-shortcut-chip">Enter open</span>
                  <button className="ghost-button workspace-search-clear" onClick={() => setWorkspaceSearch('')} type="button">Clear</button>
                </div>
              </div>
              {workspaceSearchSections.length ? workspaceSearchSections.map((section) => (
                <section key={section.key} className="workspace-search-group">
                  <div className="workspace-search-group-title">
                    <span>{section.label}</span>
                    <small>{section.items.length}</small>
                  </div>
                  {section.items.map((item) => (
                    <button
                      key={item.id}
                      className="workspace-search-result"
                      onClick={() => {
                        navigateToPage(item.target)
                        setWorkspaceSearch('')
                      }}
                      type="button"
                    >
                      <strong>{item.title}</strong>
                      <span>{item.meta}</span>
                    </button>
                  ))}
                </section>
              )) : <div className="empty-state">No pages, orders, alerts, or incidents match the current search.</div>}
            </div>
          ) : null}
        </div>
        <div className="workspace-command-bar" aria-label="Page focus">
          {pageSectionActions.length
            ? pageSectionActions.map((action) => (
              <button
                key={action.targetId}
                className="hero-jump-link"
                onClick={() => jumpToPageSection(action.targetId)}
                type="button"
              >
                {action.label}
              </button>
            ))
            : effectivePageMeta.focus.map((focusItem) => <span key={focusItem} className="hero-jump-link">{focusItem}</span>)}
        </div>
        <div className="workspace-status-strip">
          <span className="workspace-status-pill">{liveClockLabel}</span>
          <span className={`workspace-status-pill status-${connectionState}`}>{connectionState === 'live' ? 'Live system' : formatCodeLabel(connectionState)}</span>
          <button className="workspace-status-pill workspace-status-button" onClick={() => navigateToPage('alerts')} type="button">
            Notifications {globalNotificationCount}
          </button>
        </div>
        <div className="workspace-topbar-controls">
          {topbarQuickActions.map((action) => (
            <button key={action.label} className="ghost-button" onClick={() => navigateToPage(action.target)} type="button">{action.label}</button>
          ))}
          <button className="ghost-button" onClick={async () => { await Promise.all([pageState.onRefresh(), systemRuntimeState.onRefresh()]) }} disabled={pageState.loading || actionState.loading || systemRuntimeState.loading} type="button">Refresh</button>
          <button className="ghost-button" onClick={() => navigateToPage('profile')} type="button">{signedInSession?.displayName || 'Profile'}</button>
          <button className="ghost-button" onClick={signOutOperator} disabled={authSessionState.loading} type="button">
            {authSessionState.action === 'signout' ? 'Signing Out...' : 'Sign Out'}
          </button>
        </div>
      </div>
    </header>
  )
}
