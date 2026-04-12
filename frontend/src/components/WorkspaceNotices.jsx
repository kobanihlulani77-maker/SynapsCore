export default function WorkspaceNotices({ pageError, actionError, authError }) {
  return (
    <>
      {pageError ? <p className="error-text">Snapshot load issue: {pageError}</p> : null}
      {actionError ? <p className="error-text">{actionError}</p> : null}
      {authError ? <p className="error-text">{authError}</p> : null}
    </>
  )
}
