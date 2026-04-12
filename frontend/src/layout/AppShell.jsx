export default function AppShell({ currentPage, pageGroup, sidebar, topbar, utilityRail, children }) {
  return (
    <div className={`workspace-shell page-group-${pageGroup || 'workspace'} page-${currentPage}`}>
      <aside className="workspace-sidebar">
        {sidebar}
      </aside>
      <div className="workspace-frame">
        {topbar}
        <div className="workspace-body">
          <main className="workspace-content">
            {children}
          </main>
          {utilityRail}
        </div>
      </div>
    </div>
  )
}
