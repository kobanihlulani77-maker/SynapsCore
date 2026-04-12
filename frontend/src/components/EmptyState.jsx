export default function EmptyState({ children, className = '' }) {
  return <div className={`empty-state ${className}`.trim()}>{children}</div>
}
