import { useEffect } from 'react'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

const isSessionBootstrapError = (message = '') => /signed-in user session|session is missing or expired|sign in again/i.test(String(message))

export default function useWorkspaceRealtime({
  activeTenantCode,
  signedInTenantCode,
  websocketBrokerUrl,
  sockJsUrl,
  buildTenantTopicPrefix,
  fetchSnapshot,
  fetchCatalogProducts,
  mergeSnapshot,
  setSnapshot,
  setPageState,
  setCatalogState,
  setConnectionState,
  emptySnapshot,
}) {
  useEffect(() => {
    let active = true
    let connectionMode = 'connecting'
    let fallbackIntervalId = null
    let connectionWatchdogId = null
    let activeClient = null
    let transportMode = websocketBrokerUrl ? 'native' : (sockJsUrl ? 'sockjs' : 'none')
    let fallbackTransportAttempted = false
    let hasConnectedLive = false

    async function loadSnapshot() {
      if (!signedInTenantCode) {
        if (active) {
          setSnapshot(emptySnapshot)
          setPageState({ loading: false, error: '' })
          setCatalogState({ loading: false, error: '', success: '', products: [], importResult: null })
        }
        return
      }

      try {
        await Promise.all([fetchSnapshot(), fetchCatalogProducts({ quiet: true })])
      } catch (error) {
        if (active && !isSessionBootstrapError(error?.message)) {
          setPageState({ loading: false, error: error.message })
        }
      }
    }

    function updateConnectionState(nextState) {
      connectionMode = nextState
      setConnectionState(nextState)
    }

    function clearConnectionWatchdog() {
      if (connectionWatchdogId !== null) {
        globalThis.clearTimeout(connectionWatchdogId)
        connectionWatchdogId = null
      }
    }

    function scheduleSockJsFallback(sourceClient, delayMs) {
      if (!sockJsUrl || transportMode !== 'native' || fallbackTransportAttempted) {
        return
      }

      clearConnectionWatchdog()
      connectionWatchdogId = globalThis.setTimeout(() => {
        if (!active || activeClient !== sourceClient || hasConnectedLive || fallbackTransportAttempted) {
          return
        }
        fallbackTransportAttempted = true
        startClient('sockjs')
      }, delayMs)
    }

    async function refreshWhileDegraded() {
      if (!active || connectionMode === 'live' || !signedInTenantCode) {
        return
      }

      try {
        await loadSnapshot()
      } catch {
        // Keep the current degraded connection state visible; snapshot errors are already surfaced by loadSnapshot.
      }
    }

    loadSnapshot()

    if (!signedInTenantCode) {
      updateConnectionState('signed-out')
      return () => {
        active = false
      }
    }

    if (!sockJsUrl && !websocketBrokerUrl) {
      updateConnectionState('degraded')
      return () => {
        active = false
      }
    }

    const topicPrefix = buildTenantTopicPrefix(activeTenantCode)
    updateConnectionState('connecting')
    fallbackIntervalId = globalThis.setInterval(() => {
      void refreshWhileDegraded()
    }, 5000)

    function handleTransportFailure(sourceClient, nextState) {
      if (!active || activeClient !== sourceClient) {
        return
      }

      if (transportMode === 'native' && sockJsUrl && !fallbackTransportAttempted) {
        scheduleSockJsFallback(sourceClient, 12_000)
      }

      updateConnectionState(nextState)
      void refreshWhileDegraded()
    }

    function startClient(nextTransportMode) {
      const previousClient = activeClient
      transportMode = nextTransportMode
      hasConnectedLive = false

      const nextClient = new Client({
        reconnectDelay: 5000,
        heartbeatIncoming: 10000,
        heartbeatOutgoing: 10000,
        brokerURL: nextTransportMode === 'native' && /^wss?:/i.test(websocketBrokerUrl) ? websocketBrokerUrl : undefined,
        webSocketFactory: nextTransportMode === 'sockjs' && sockJsUrl ? () => new SockJS(sockJsUrl) : undefined,
        onConnect: () => {
          if (!active || activeClient !== nextClient) {
            return
          }

          hasConnectedLive = true
          clearConnectionWatchdog()
          updateConnectionState('live')
          nextClient.subscribe(`${topicPrefix}/dashboard.summary`, (message) => mergeSnapshot({ summary: JSON.parse(message.body) }))
          nextClient.subscribe(`${topicPrefix}/alerts`, (message) => mergeSnapshot({ alerts: JSON.parse(message.body) }))
          nextClient.subscribe(`${topicPrefix}/recommendations`, (message) => mergeSnapshot({ recommendations: JSON.parse(message.body) }))
          nextClient.subscribe(`${topicPrefix}/inventory`, (message) => mergeSnapshot({ inventory: JSON.parse(message.body) }))
          nextClient.subscribe(`${topicPrefix}/fulfillment.overview`, (message) => mergeSnapshot({ fulfillment: JSON.parse(message.body) }))
          nextClient.subscribe(`${topicPrefix}/orders.recent`, (message) => mergeSnapshot({ recentOrders: JSON.parse(message.body) }))
          nextClient.subscribe(`${topicPrefix}/events.recent`, (message) => mergeSnapshot({ recentEvents: JSON.parse(message.body) }))
          nextClient.subscribe(`${topicPrefix}/audit.recent`, (message) => mergeSnapshot({ auditLogs: JSON.parse(message.body) }))
          nextClient.subscribe(`${topicPrefix}/system.incidents`, (message) => mergeSnapshot({ systemIncidents: JSON.parse(message.body) }))
          nextClient.subscribe(`${topicPrefix}/integrations.connectors`, (message) => mergeSnapshot({ integrationConnectors: JSON.parse(message.body) }))
          nextClient.subscribe(`${topicPrefix}/integrations.imports`, (message) => mergeSnapshot({ integrationImportRuns: JSON.parse(message.body) }))
          nextClient.subscribe(`${topicPrefix}/integrations.replay`, (message) => mergeSnapshot({ integrationReplayQueue: JSON.parse(message.body) }))
          nextClient.subscribe(`${topicPrefix}/scenarios.notifications`, (message) => mergeSnapshot({ scenarioNotifications: JSON.parse(message.body) }))
          nextClient.subscribe(`${topicPrefix}/scenarios.escalated`, (message) => mergeSnapshot({ slaEscalations: JSON.parse(message.body) }))
        },
        onStompError: () => {
          handleTransportFailure(nextClient, 'degraded')
        },
        onWebSocketError: () => {
          handleTransportFailure(nextClient, 'degraded')
        },
        onWebSocketClose: () => {
          handleTransportFailure(nextClient, 'reconnecting')
        },
      })

      activeClient = nextClient
      clearConnectionWatchdog()
      if (previousClient && previousClient !== nextClient) {
        previousClient.deactivate().catch(() => {})
      }
      updateConnectionState(previousClient ? 'reconnecting' : 'connecting')
      if (nextTransportMode === 'native' && sockJsUrl) {
        scheduleSockJsFallback(nextClient, 15_000)
      }
      nextClient.activate()
    }

    startClient(transportMode)

    return () => {
      active = false
      clearConnectionWatchdog()
      if (fallbackIntervalId !== null) {
        globalThis.clearInterval(fallbackIntervalId)
      }
      activeClient?.deactivate().catch(() => {})
    }
  }, [activeTenantCode, signedInTenantCode, websocketBrokerUrl, sockJsUrl])
}
