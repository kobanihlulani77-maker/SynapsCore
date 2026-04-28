import AppShell from '../layout/AppShell'
import Sidebar from '../layout/Sidebar'
import Topbar from '../layout/Topbar'
import WorkspaceUtilityRail from '../layout/WorkspaceUtilityRail'
import WorkspacePageHeader from '../layout/WorkspacePageHeader'
import AppRoutes from './AppRoutes'
import WorkspaceNotices from './WorkspaceNotices'

export default function WorkspaceAuthenticatedApp({
  currentPage,
  effectivePageMeta,
  navGroups,
  pageLookup,
  pageBadgeMap,
  connectionState,
  formatCodeLabel,
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
  globalNotificationCount,
  topbarQuickActions,
  pageState,
  fetchSnapshot,
  actionState,
  systemRuntimeState,
  fetchSystemRuntime,
  signedInSession,
  signOutOperator,
  authSessionState,
  utilityRailContext,
  showDashboardHero,
  controlHighlights,
  pendingReviewCount,
  pendingReplayCount,
  systemIncidents,
  pageStatusMap,
  appPageCount,
  enabledConnectorCount,
  snapshot,
  dashboardContext,
  alertsContext,
  recommendationsContext,
  ordersContext,
  inventoryContext,
  catalogContext,
  locationsContext,
  fulfillmentContext,
  scenarioControlContext,
  scenarioPlannerContext,
  scenarioHistoryContext,
  approvalsContext,
  escalationsContext,
  integrationsContext,
  replayContext,
  runtimeContext,
  auditContext,
  usersContext,
  settingsContext,
  profileContext,
  platformAdminContext,
  tenantsContext,
  systemConfigContext,
  releasesContext,
}) {
  return (
    <AppShell
      currentPage={currentPage}
      pageGroup={effectivePageMeta.group || 'workspace'}
      sidebar={(
        <Sidebar
          signedInSession={signedInSession}
          navigateToPage={navigateToPage}
          navGroups={navGroups}
          pageLookup={pageLookup}
          currentPage={currentPage}
          pageBadgeMap={pageBadgeMap}
          connectionState={connectionState}
          formatCodeLabel={formatCodeLabel}
        />
      )}
      topbar={(
        <Topbar
          effectivePageMeta={effectivePageMeta}
          workspaceSearch={workspaceSearch}
          setWorkspaceSearch={setWorkspaceSearch}
          searchInputRef={searchInputRef}
          firstWorkspaceSearchResult={firstWorkspaceSearchResult}
          navigateToPage={navigateToPage}
          hasWorkspaceSearch={hasWorkspaceSearch}
          workspaceSearchMatchCount={workspaceSearchMatchCount}
          workspaceSearchSections={workspaceSearchSections}
          pageSectionActions={pageSectionActions}
          jumpToPageSection={jumpToPageSection}
          liveClockLabel={liveClockLabel}
          connectionState={connectionState}
          formatCodeLabel={formatCodeLabel}
          globalNotificationCount={globalNotificationCount}
          topbarQuickActions={topbarQuickActions}
          pageState={{ ...pageState, onRefresh: fetchSnapshot }}
          actionState={actionState}
          systemRuntimeState={{ ...systemRuntimeState, onRefresh: fetchSystemRuntime }}
          signedInSession={signedInSession}
          signOutOperator={signOutOperator}
          authSessionState={authSessionState}
        />
      )}
      utilityRail={<WorkspaceUtilityRail context={utilityRailContext} />}
    >
      <WorkspacePageHeader
        showDashboardHero={showDashboardHero}
        controlHighlights={controlHighlights}
        pendingReviewCount={pendingReviewCount}
        pendingReplayCount={pendingReplayCount}
        systemIncidents={systemIncidents}
        pageStatusMap={pageStatusMap}
        appPageCount={appPageCount}
        enabledConnectorCount={enabledConnectorCount}
        snapshot={snapshot}
        effectivePageMeta={effectivePageMeta}
        currentPage={currentPage}
      />
      <WorkspaceNotices pageError={pageState.error} actionError={actionState.error} authError={authSessionState.error} />
      <AppRoutes
        dashboardContext={dashboardContext}
        alertsContext={alertsContext}
        recommendationsContext={recommendationsContext}
        ordersContext={ordersContext}
        inventoryContext={inventoryContext}
        catalogContext={catalogContext}
        locationsContext={locationsContext}
        fulfillmentContext={fulfillmentContext}
        scenarioControlContext={scenarioControlContext}
        scenarioPlannerContext={scenarioPlannerContext}
        scenarioHistoryContext={scenarioHistoryContext}
        approvalsContext={approvalsContext}
        escalationsContext={escalationsContext}
        integrationsContext={integrationsContext}
        replayContext={replayContext}
        runtimeContext={runtimeContext}
        auditContext={auditContext}
        usersContext={usersContext}
        settingsContext={settingsContext}
        profileContext={profileContext}
        platformAdminContext={platformAdminContext}
        tenantsContext={tenantsContext}
        systemConfigContext={systemConfigContext}
        releasesContext={releasesContext}
      />
    </AppShell>
  )
}
