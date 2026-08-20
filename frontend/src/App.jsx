import PlatformApplication from './components/PlatformApplication'
import WorkspaceApplication from './components/WorkspaceApplication'
import useWorkspaceAppModel from './hooks/useWorkspaceAppModel'
import { pageLookup, resolvePageFromPath } from './config/pageRegistry'

function TenantApplication() {
  const workspaceAppModel = useWorkspaceAppModel()
  return <WorkspaceApplication {...workspaceAppModel} />
}

export default function App() {
  const initialPage = resolvePageFromPath()
  if (pageLookup[initialPage]?.audience === 'platform') {
    return <PlatformApplication initialPage={initialPage} />
  }

  return <TenantApplication />
}
