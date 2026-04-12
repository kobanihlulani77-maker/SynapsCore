const toneMap = {
  live: 'success',
  healthy: 'success',
  stable: 'success',
  degraded: 'partial',
  warning: 'partial',
  watch: 'partial',
  critical: 'failure',
  danger: 'failure',
  offline: 'failure',
}

export default function StatusBadge({ children, tone = 'partial', className = '' }) {
  const normalizedTone = toneMap[String(tone).toLowerCase()] || tone
  return (
    <span className={`status-tag status-${normalizedTone} ${className}`.trim()}>
      {children}
    </span>
  )
}
