function buildNotice({ title, body, tone = 'warning', actionLabel = '', onAction = null }) {
  return { title, body, tone, actionLabel, onAction }
}

export default function WorkspaceNotices({
  pageError,
  actionError,
  authError,
  runtimeError,
  onRetry,
}) {
  const notices = []

  if (pageError || runtimeError) {
    notices.push(buildNotice({
      title: 'Command surface refresh needs attention',
      body: pageError || runtimeError,
      tone: 'warning',
      actionLabel: onRetry ? 'Retry workspace refresh' : '',
      onAction: onRetry,
    }))
  }

  if (actionError) {
    notices.push(buildNotice({
      title: 'Last protected action needs review',
      body: actionError,
      tone: 'danger',
    }))
  }

  if (authError) {
    notices.push(buildNotice({
      title: authError.toLowerCase().includes('expired') ? 'Session expired' : 'Workspace access needs attention',
      body: authError,
      tone: authError.toLowerCase().includes('expired') ? 'warning' : 'danger',
    }))
  }

  if (!notices.length) {
    return null
  }

  return (
    <div className="workspace-notices" role="status" aria-live="polite">
      {notices.map((notice) => (
        <article key={`${notice.title}-${notice.body}`} className={`workspace-notice-card tone-${notice.tone}`}>
          <div>
            <strong>{notice.title}</strong>
            <p>{notice.body}</p>
          </div>
          {notice.actionLabel && notice.onAction ? (
            <button className="ghost-button" onClick={notice.onAction} type="button">{notice.actionLabel}</button>
          ) : null}
        </article>
      ))}
    </div>
  )
}
