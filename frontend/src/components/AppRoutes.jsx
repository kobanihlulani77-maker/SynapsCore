import AlertsPage from '../pages/Alerts'
import ApprovalsPage from '../pages/Approvals'
import AuditPage from '../pages/Audit'
import CatalogPage from '../pages/Catalog'
import DashboardPage from '../pages/Dashboard'
import EscalationsPage from '../pages/Escalations'
import FulfillmentPage from '../pages/Fulfillment'
import IntegrationsPage from '../pages/Integrations'
import InventoryPage from '../pages/Inventory'
import LocationsPage from '../pages/Locations'
import OrdersPage from '../pages/Orders'
import PlatformAdminPage from '../pages/PlatformAdmin'
import ProfilePage from '../pages/Profile'
import RecommendationsPage from '../pages/Recommendations'
import ReleasesPage from '../pages/Releases'
import ReplayPage from '../pages/Replay'
import RuntimePage from '../pages/Runtime'
import ScenarioControlPage from '../pages/ScenarioControl'
import ScenarioHistoryPage from '../pages/ScenarioHistory'
import ScenarioPlannerPage from '../pages/ScenarioPlanner'
import SettingsPage from '../pages/Settings'
import SystemConfigPage from '../pages/SystemConfig'
import TenantsPage from '../pages/Tenants'
import UsersPage from '../pages/Users'

export default function AppRoutes({
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
    <>
      <DashboardPage context={dashboardContext} />
      <AlertsPage context={alertsContext} />
      <RecommendationsPage context={recommendationsContext} />
      <OrdersPage context={ordersContext} />
      <InventoryPage context={inventoryContext} />
      <CatalogPage context={catalogContext} />
      <LocationsPage context={locationsContext} />
      <FulfillmentPage context={fulfillmentContext} />
      <ScenarioControlPage context={scenarioControlContext} />
      <ScenarioPlannerPage context={scenarioPlannerContext} />
      <ScenarioHistoryPage context={scenarioHistoryContext} />
      <ApprovalsPage context={approvalsContext} />
      <EscalationsPage context={escalationsContext} />
      <IntegrationsPage context={integrationsContext} />
      <ReplayPage context={replayContext} />
      <RuntimePage context={runtimeContext} />
      <AuditPage context={auditContext} />
      <UsersPage context={usersContext} />
      <SettingsPage context={settingsContext} />
      <ProfilePage context={profileContext} />
      <PlatformAdminPage context={platformAdminContext} />
      <TenantsPage context={tenantsContext} />
      <SystemConfigPage context={systemConfigContext} />
      <ReleasesPage context={releasesContext} />
    </>
  )
}
