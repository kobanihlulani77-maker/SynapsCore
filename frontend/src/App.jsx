import WorkspaceApplication from './components/WorkspaceApplication'
import useWorkspaceAppModel from './hooks/useWorkspaceAppModel'

export default function App() {
  const workspaceAppModel = useWorkspaceAppModel()

  return (
    <WorkspaceApplication {...workspaceAppModel} />
  )
}
