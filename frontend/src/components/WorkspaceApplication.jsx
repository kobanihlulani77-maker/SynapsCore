import WorkspaceRouteSwitch from './WorkspaceRouteSwitch'
import useWorkspacePageContexts from '../hooks/useWorkspacePageContexts'

export default function WorkspaceApplication({
  workspaceState,
  authContext,
  apiContext,
  navigation,
  bootstrapContext,
  sessionActions,
  catalogActions,
  integrationActions,
  workspaceAdminActions,
  scenarioActions,
}) {
  const {
    routeState,
    authenticatedAppProps,
  } = useWorkspacePageContexts({
    workspaceState,
    authContext,
    apiContext,
    navigation,
    bootstrapContext,
    sessionActions,
    catalogActions,
    integrationActions,
    workspaceAdminActions,
    scenarioActions,
  })

  return (
    <WorkspaceRouteSwitch
      routeState={routeState}
      authenticatedAppProps={authenticatedAppProps}
    />
  )
}
