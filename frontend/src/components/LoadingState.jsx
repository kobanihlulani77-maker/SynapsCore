export default function LoadingState({ label = 'Loading operational surface...' }) {
  return (
    <div className="loading-state" aria-live="polite">
      <div className="loading-skeleton loading-skeleton-wide" />
      <div className="loading-skeleton" />
      <div className="loading-skeleton loading-skeleton-short" />
      <p>{label}</p>
    </div>
  )
}
