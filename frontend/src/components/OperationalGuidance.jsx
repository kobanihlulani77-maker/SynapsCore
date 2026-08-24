export default function OperationalGuidance({
  stateLabel,
  stateTone = 'status-success',
  stateDetail,
  attention,
  nextAction,
  evidence,
  role,
  limitation,
}) {
  return (
    <div className="workspace-guidance-block" role="note" aria-label="Operational guidance">
      <div className="stack-title-row">
        <strong>Operator readout</strong>
        <span className={`status-tag ${stateTone}`}>{stateLabel}</span>
      </div>
      <div className="workspace-next-list">
        <div className="workspace-next-row">
          <strong>Current state</strong>
          <p>{stateDetail}</p>
        </div>
        <div className="workspace-next-row">
          <strong>Attention</strong>
          <p>{attention}</p>
        </div>
        <div className="workspace-next-row">
          <strong>Next action</strong>
          <p>{nextAction}</p>
        </div>
        <div className="workspace-next-row">
          <strong>Evidence and authority</strong>
          <p>{evidence}{role ? ` ${role}` : ''}</p>
        </div>
        {limitation ? (
          <div className="workspace-next-row">
            <strong>Boundary</strong>
            <p>{limitation}</p>
          </div>
        ) : null}
      </div>
    </div>
  )
}
