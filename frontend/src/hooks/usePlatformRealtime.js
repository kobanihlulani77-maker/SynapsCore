import { useEffect, useRef } from 'react'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

export const PLATFORM_ACTIVITY_CHANGED_TOPIC = '/topic/platform/activity.changed'
export const PLATFORM_LIVE_RECONCILIATION_INTERVAL_MS = 60_000
export const PLATFORM_DEGRADED_RECONCILIATION_INTERVAL_MS = 15_000

export default function usePlatformRealtime({
  signedIn,
  websocketBrokerUrl,
  sockJsUrl,
  fetchActivity,
  fetchOverview,
  onStateChange,
}) {
  const callbacksRef = useRef({ fetchActivity, fetchOverview, onStateChange })
  callbacksRef.current = { fetchActivity, fetchOverview, onStateChange }

  useEffect(() => {
    let active = true
    let connectionState = 'connecting'
    let activeClient = null
    let liveIntervalId = null
    let degradedIntervalId = null
    let refreshTimeoutId = null
    let refreshInFlight = false
    let refreshQueued = false

    const setState = (nextState) => {
      connectionState = nextState
      callbacksRef.current.onStateChange?.(nextState)
    }

    const refresh = async () => {
      if (!active || !signedIn || refreshInFlight) {
        if (active && signedIn && refreshInFlight) refreshQueued = true
        return
      }
      refreshInFlight = true
      try {
        await Promise.all([
          callbacksRef.current.fetchActivity?.(),
          callbacksRef.current.fetchOverview?.(),
        ])
      } catch {
        // The platform pages own the visible request error state.
      } finally {
        refreshInFlight = false
        if (refreshQueued) {
          refreshQueued = false
          scheduleRefresh()
        }
      }
    }

    const scheduleRefresh = (delayMs = 750) => {
      if (!active || !signedIn) return
      if (refreshTimeoutId !== null) return
      refreshTimeoutId = globalThis.setTimeout(() => {
        refreshTimeoutId = null
        void refresh()
      }, delayMs)
    }

    const stopIntervals = () => {
      if (liveIntervalId !== null) globalThis.clearInterval(liveIntervalId)
      if (degradedIntervalId !== null) globalThis.clearInterval(degradedIntervalId)
      liveIntervalId = null
      degradedIntervalId = null
    }

    const startLiveReconciliation = () => {
      if (liveIntervalId === null) {
        liveIntervalId = globalThis.setInterval(() => void refresh(), PLATFORM_LIVE_RECONCILIATION_INTERVAL_MS)
      }
    }

    const startDegradedReconciliation = () => {
      if (degradedIntervalId === null) {
        degradedIntervalId = globalThis.setInterval(() => void refresh(), PLATFORM_DEGRADED_RECONCILIATION_INTERVAL_MS)
      }
    }

    const enterDegraded = (nextState) => {
      if (!active || !signedIn) return
      stopIntervals()
      setState(nextState)
      startDegradedReconciliation()
      void refresh()
    }

    if (!signedIn) {
      setState('signed-out')
      return () => { active = false }
    }

    setState('connecting')
    if (!sockJsUrl && !websocketBrokerUrl) {
      enterDegraded('degraded')
      return () => {
        active = false
        stopIntervals()
      }
    }

    const client = new Client({
      reconnectDelay: 5_000,
      connectionTimeout: 20_000,
      heartbeatIncoming: 10_000,
      heartbeatOutgoing: 10_000,
      brokerURL: websocketBrokerUrl || undefined,
      webSocketFactory: !websocketBrokerUrl && sockJsUrl
        ? () => new SockJS(sockJsUrl, undefined, {
          transports: ['websocket', 'xhr-streaming', 'xhr-polling'],
          timeout: 20_000,
        })
        : undefined,
      onConnect: () => {
        if (!active || activeClient !== client) return
        stopIntervals()
        setState('live')
        startLiveReconciliation()
        client.subscribe(PLATFORM_ACTIVITY_CHANGED_TOPIC, (message) => {
          try {
            const payload = JSON.parse(message?.body || '')
            if (!payload || payload.type !== 'PLATFORM_ACTIVITY_CHANGED') return
            scheduleRefresh()
          } catch {
            // Ignore malformed notifications; REST reconciliation remains authoritative.
          }
        })
        void refresh()
      },
      onStompError: () => enterDegraded('degraded'),
      onWebSocketError: () => enterDegraded('degraded'),
      onWebSocketClose: () => enterDegraded('reconnecting'),
    })

    activeClient = client
    client.activate()

    return () => {
      active = false
      stopIntervals()
      if (refreshTimeoutId !== null) globalThis.clearTimeout(refreshTimeoutId)
      refreshTimeoutId = null
      activeClient = null
      client.deactivate().catch(() => {})
    }
  }, [signedIn, websocketBrokerUrl, sockJsUrl])
}
