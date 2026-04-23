import { useEffect } from 'react'

export default function useWorkspaceShell({
  resolvePageFromPath,
  setCurrentPage,
  setClockTick,
  searchInputRef,
  workspaceSearch,
  setWorkspaceSearch,
}) {
  useEffect(() => {
    const handlePopState = () => setCurrentPage(resolvePageFromPath())
    globalThis.addEventListener?.('popstate', handlePopState)
    return () => globalThis.removeEventListener?.('popstate', handlePopState)
  }, [resolvePageFromPath, setCurrentPage])

  useEffect(() => {
    const timer = globalThis.setInterval?.(() => setClockTick(Date.now()), 30000)
    return () => globalThis.clearInterval?.(timer)
  }, [setClockTick])

  useEffect(() => {
    const handleCommandKeys = (event) => {
      const target = event.target
      const isTextInput = target instanceof HTMLElement && (
        target.tagName === 'INPUT'
        || target.tagName === 'TEXTAREA'
        || target.tagName === 'SELECT'
        || target.isContentEditable
      )

      if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k') {
        event.preventDefault()
        searchInputRef.current?.focus()
        searchInputRef.current?.select?.()
        return
      }

      if (event.key === '/' && !isTextInput) {
        event.preventDefault()
        searchInputRef.current?.focus()
        searchInputRef.current?.select?.()
        return
      }

      if (event.key === 'Escape') {
        if (workspaceSearch) {
          setWorkspaceSearch('')
        }
        if (target === searchInputRef.current) {
          searchInputRef.current?.blur()
        }
      }
    }

    globalThis.addEventListener?.('keydown', handleCommandKeys)
    return () => globalThis.removeEventListener?.('keydown', handleCommandKeys)
  }, [searchInputRef, workspaceSearch, setWorkspaceSearch])
}
