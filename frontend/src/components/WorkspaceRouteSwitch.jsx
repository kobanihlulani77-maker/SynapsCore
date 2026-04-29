import WorkspaceAuthenticatedApp from './WorkspaceAuthenticatedApp'
import SignInPage from '../pages/SignIn'
import PublicExperience from '../pages/PublicExperience'

export default function WorkspaceRouteSwitch({
  routeState,
  authenticatedAppProps,
}) {
  const {
    isPublicPage,
    effectivePageMeta,
    signInPageContext,
    publicExperienceContext,
  } = routeState

  if (isPublicPage && effectivePageMeta.key === 'sign-in') {
    return <SignInPage context={signInPageContext} />
  }

  if (isPublicPage) {
    return <PublicExperience context={publicExperienceContext} />
  }

  return <WorkspaceAuthenticatedApp {...authenticatedAppProps} />
}
