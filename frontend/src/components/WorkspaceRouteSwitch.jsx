import WorkspaceAuthenticatedApp from './WorkspaceAuthenticatedApp'
import SignInPage from '../pages/SignIn'
import PublicExperience from '../pages/PublicExperience'
import CreateWorkspacePage from '../pages/CreateWorkspace'

export default function WorkspaceRouteSwitch({
  routeState,
  authenticatedAppProps,
}) {
  const {
    isPublicPage,
    effectivePageMeta,
    signInPageContext,
    publicExperienceContext,
    createWorkspaceContext,
  } = routeState

  if (isPublicPage && effectivePageMeta.key === 'sign-in') {
    return <SignInPage context={signInPageContext} />
  }

  if (isPublicPage && effectivePageMeta.key === 'create-workspace') {
    return <CreateWorkspacePage context={createWorkspaceContext} />
  }

  if (isPublicPage) {
    return <PublicExperience context={publicExperienceContext} />
  }

  return <WorkspaceAuthenticatedApp {...authenticatedAppProps} />
}
