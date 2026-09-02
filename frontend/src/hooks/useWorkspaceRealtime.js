import { useEffect } from 'react'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

const isSessionBootstrapError = (message = '') => /signed-in user session|session is missing or expired|sign in again/i.test(String(message))
const realtimeDebugKey = '__SYNAPSE_REALTIME_DEBUG__'
const degradedRefreshIntervalMs = 15_000
const liveReconciliationIntervalMs = 60_000
const hostedSockJsTransportCandidates = ['websocket', 'xhr-streaming', 'xhr-polling']
const enableRealtimeConsoleLogs = Boolean(import.meta.env.DEV)

export default function useWorkspaceRealtime({
  activeTenantCode,
  signedInTenantCode,
  signedInRoles = [],
  signedInWarehouseScopes = [],
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
  const signedInRoleKey = [...signedInRoles].sort().join('|')
  const signedInWarehouseScopeKey = [...signedInWarehouseScopes].sort().join('|')

  useEffect(() => {
    let active = true
    let connectionMode = 'connecting'
    let degradedRefreshIntervalId = null
    let liveReconciliationIntervalId = null
    let activeClient = null
    let authoritativeSnapshotRefreshTimeoutId = null
    let authoritativeSnapshotRefreshInFlight = false
    let authoritativeSnapshotRefreshQueued = false

    const publishRealtimeDebug = (partial) => {
      const nextDebugState = {
        ...(globalThis[realtimeDebugKey] || {}),
        activeTenantCode: activeTenantCode || '',
        signedInTenantCode: signedInTenantCode || '',
        configuredTransport: websocketBrokerUrl ? 'native-stomp' : (sockJsUrl ? 'sockjs-stomp' : 'none'),
        websocketBrokerUrl: websocketBrokerUrl || '',
        sockJsUrl: sockJsUrl || '',
        updatedAt: new Date().toISOString(),
        ...partial,
      }
      globalThis[realtimeDebugKey] = nextDebugState
      return nextDebugState
    }

    const logRealtime = (level, message, details = null) => {
      if (!enableRealtimeConsoleLogs) {
        return
      }
      const logger = console?.[level] || console.log
      if (details) {
        logger(`[synapsecore:realtime] ${message}`, details)
      } else {
        logger(`[synapsecore:realtime] ${message}`)
      }
    }

    async function loadSnapshot() {
      if (!signedInTenantCode) {
        if (active) {
          setSnapshot(emptySnapshot)
          setPageState({ loading: false, error: '' })
          setCatalogState({ loading: false, error: '', success: '', products: [], importResult: null })
        }
        return true
      }

      try {
        await Promise.all([fetchSnapshot(), fetchCatalogProducts({ quiet: true })])
        publishRealtimeDebug({
          lastSnapshotSyncAt: new Date().toISOString(),
          lastSnapshotSyncStatus: 'ok',
        })
        return true
      } catch (error) {
        publishRealtimeDebug({
          lastSnapshotSyncAt: new Date().toISOString(),
          lastSnapshotSyncStatus: 'error',
          lastSnapshotSyncError: error?.message || String(error),
        })
        if (active && !isSessionBootstrapError(error?.message)) {
          setPageState((current) => ({
            ...current,
            loading: false,
            error: error.message,
            freshness: current.lastSuccessfulAt ? 'stale' : 'unknown',
            degradedSources: current.degradedSources?.length ? current.degradedSources : ['Dashboard snapshot'],
          }))
        }
        return false
      }
    }

    function updateConnectionState(nextState) {
      connectionMode = nextState
      setConnectionState(nextState)
      publishRealtimeDebug({
        connectionState: nextState,
      })
    }

    function startDegradedRefreshLoop() {
      if (degradedRefreshIntervalId !== null) {
        return
      }

      degradedRefreshIntervalId = globalThis.setInterval(() => {
        void refreshWhileDegraded()
      }, degradedRefreshIntervalMs)
    }

    function stopDegradedRefreshLoop() {
      if (degradedRefreshIntervalId !== null) {
        globalThis.clearInterval(degradedRefreshIntervalId)
        degradedRefreshIntervalId = null
      }
    }

    function startLiveReconciliationLoop() {
      if (liveReconciliationIntervalId !== null) {
        return
      }

      liveReconciliationIntervalId = globalThis.setInterval(() => {
        void refreshWhileLive()
      }, liveReconciliationIntervalMs)
    }

    function stopLiveReconciliationLoop() {
      if (liveReconciliationIntervalId !== null) {
        globalThis.clearInterval(liveReconciliationIntervalId)
        liveReconciliationIntervalId = null
      }
    }

    async function refreshWhileLive() {
      if (!active || !signedInTenantCode || connectionMode !== 'live' || !fetchSnapshot) {
        return
      }

      try {
        await fetchSnapshot()
        publishRealtimeDebug({
          lastLiveReconciliationAt: new Date().toISOString(),
          lastLiveReconciliationStatus: 'ok',
        })
      } catch (error) {
        publishRealtimeDebug({
          lastLiveReconciliationAt: new Date().toISOString(),
          lastLiveReconciliationStatus: 'error',
          lastLiveReconciliationError: error?.message || String(error),
        })
      }
    }

    async function refreshWhileDegraded() {
      if (!active || !signedInTenantCode || connectionMode === 'live' || connectionMode === 'connecting') {
        return
      }

      try {
        await loadSnapshot()
      } catch {
        // Keep the current degraded connection state visible; snapshot errors are already surfaced by loadSnapshot.
      }
    }

    function stopAuthoritativeSnapshotRefreshTimer() {
      if (authoritativeSnapshotRefreshTimeoutId !== null) {
        globalThis.clearTimeout(authoritativeSnapshotRefreshTimeoutId)
        authoritativeSnapshotRefreshTimeoutId = null
      }
    }

    async function refreshAuthoritativeSnapshot(reason = 'realtime-snapshot-refresh') {
      if (!active || !signedInTenantCode) {
        return
      }
      if (authoritativeSnapshotRefreshInFlight) {
        authoritativeSnapshotRefreshQueued = true
        return
      }

      authoritativeSnapshotRefreshInFlight = true
      try {
        const synchronized = await loadSnapshot()
        publishRealtimeDebug({
          lastAuthoritativeSnapshotRefreshAt: new Date().toISOString(),
          lastAuthoritativeSnapshotRefreshReason: reason,
          lastAuthoritativeSnapshotRefreshStatus: synchronized ? 'ok' : 'error',
        })
      } finally {
        authoritativeSnapshotRefreshInFlight = false
        if (authoritativeSnapshotRefreshQueued) {
          authoritativeSnapshotRefreshQueued = false
          void refreshAuthoritativeSnapshot(`${reason}:queued`)
        }
      }
    }

    function scheduleAuthoritativeSnapshotRefresh(reason, delayMs = 750) {
      if (!active || !signedInTenantCode) {
        return
      }
      stopAuthoritativeSnapshotRefreshTimer()
      authoritativeSnapshotRefreshTimeoutId = globalThis.setTimeout(() => {
        authoritativeSnapshotRefreshTimeoutId = null
        void refreshAuthoritativeSnapshot(reason)
      }, delayMs)
    }

    loadSnapshot()

    if (!signedInTenantCode) {
      updateConnectionState('signed-out')
      stopDegradedRefreshLoop()
      publishRealtimeDebug({
        connectionState: 'signed-out',
      })
      return () => {
        active = false
      }
    }

    if (!sockJsUrl && !websocketBrokerUrl) {
      updateConnectionState('degraded')
      startDegradedRefreshLoop()
      publishRealtimeDebug({
        connectionState: 'degraded',
        lastTransportError: 'Realtime transport is not configured.',
      })
      return () => {
        active = false
      }
    }

    const topicPrefix = buildTenantTopicPrefix(activeTenantCode)
    const roleSet = new Set(signedInRoleKey.split('|').filter(Boolean))
    const hasIntegrationAccess = roleSet.has('INTEGRATION_ADMIN') || roleSet.has('INTEGRATION_OPERATOR')
    const hasTenantWideWarehouseAccess = !signedInWarehouseScopeKey
    updateConnectionState('connecting')
    publishRealtimeDebug({
      topicPrefix,
      selectedTransport: websocketBrokerUrl ? 'native-stomp' : 'sockjs-stomp',
      connectionState: 'connecting',
      lastConnectAttemptAt: new Date().toISOString(),
    })

    const readSockJsTransportDetails = (socket) => ({
      transportName: socket?._transport?.transportName || '',
      transportUrl: socket?._transport?.url || '',
      readyState: typeof socket?.readyState === 'number' ? socket.readyState : null,
    })

    function attachSockJsDiagnostics(socket) {
      if (!socket?.addEventListener) {
        return
      }

      socket.addEventListener('open', () => {
        publishRealtimeDebug({
          lastSockJsOpenAt: new Date().toISOString(),
          sockJsTransportDetails: readSockJsTransportDetails(socket),
        })
        logRealtime('info', 'sockjs transport opened', readSockJsTransportDetails(socket))
      })

      socket.addEventListener('close', (event) => {
        publishRealtimeDebug({
          lastSockJsCloseAt: new Date().toISOString(),
          sockJsTransportDetails: readSockJsTransportDetails(socket),
          lastSockJsCloseCode: event?.code ?? null,
          lastSockJsCloseReason: event?.reason || '',
          lastSockJsCloseWasClean: typeof event?.wasClean === 'boolean' ? event.wasClean : null,
        })
        logRealtime('warn', 'sockjs transport closed', {
          ...readSockJsTransportDetails(socket),
          code: event?.code ?? null,
          reason: event?.reason || '',
          wasClean: typeof event?.wasClean === 'boolean' ? event.wasClean : null,
        })
      })

      socket.addEventListener('error', (event) => {
        publishRealtimeDebug({
          lastSockJsErrorAt: new Date().toISOString(),
          sockJsTransportDetails: readSockJsTransportDetails(socket),
          lastSockJsError: event?.message || 'SockJS transport error.',
        })
        logRealtime('warn', 'sockjs transport error', {
          ...readSockJsTransportDetails(socket),
          message: event?.message || 'SockJS transport error.',
        })
      })
    }

    function handleTransportFailure(sourceClient, nextState, details = null) {
      if (!active || activeClient !== sourceClient) {
        return
      }

      publishRealtimeDebug({
        connectionState: nextState,
        lastTransportErrorAt: new Date().toISOString(),
        lastTransportError: details?.message || nextState,
        lastTransportEvent: details || null,
      })
      logRealtime('warn', `transport moved to ${nextState}`, details)
      updateConnectionState(nextState)
      stopLiveReconciliationLoop()
      startDegradedRefreshLoop()
      void refreshWhileDegraded()
    }

    function parseRealtimeBody(message, topicName) {
      try {
        return JSON.parse(message?.body || '')
      } catch (error) {
        publishRealtimeDebug({
          lastMalformedMessageAt: new Date().toISOString(),
          lastMalformedMessageTopic: topicName,
          lastMalformedMessageSize: typeof message?.body === 'string' ? message.body.length : 0,
          lastMalformedMessageError: error?.message || String(error),
        })
        logRealtime('warn', 'malformed realtime message ignored', {
          topic: topicName,
          size: typeof message?.body === 'string' ? message.body.length : 0,
        })
        scheduleAuthoritativeSnapshotRefresh(`malformed-${topicName}`)
        return null
      }
    }

    function startClient(nextTransportMode) {
      const nextClient = new Client({
        reconnectDelay: 5000,
        connectionTimeout: 20_000,
        heartbeatIncoming: 10000,
        heartbeatOutgoing: 10000,
        brokerURL: nextTransportMode === 'native' && /^wss?:/i.test(websocketBrokerUrl) ? websocketBrokerUrl : undefined,
        webSocketFactory: nextTransportMode === 'sockjs' && sockJsUrl
          ? () => {
            const socket = new SockJS(sockJsUrl, undefined, {
              transports: hostedSockJsTransportCandidates,
              timeout: 20_000,
            })
            publishRealtimeDebug({
              lastSockJsConstructedAt: new Date().toISOString(),
              sockJsTransportCandidates: hostedSockJsTransportCandidates,
              sockJsTransportDetails: readSockJsTransportDetails(socket),
            })
            attachSockJsDiagnostics(socket)
            return socket
          }
          : undefined,
        beforeConnect: async () => {
          publishRealtimeDebug({
            lastStompBeforeConnectAt: new Date().toISOString(),
            selectedTransport: nextTransportMode === 'native' ? 'native-stomp' : 'sockjs-stomp',
          })
        },
        debug: (message) => {
          publishRealtimeDebug({
            lastStompDebugAt: new Date().toISOString(),
            lastStompDebugMessage: message,
          })
        },
        onChangeState: (state) => {
          publishRealtimeDebug({
            lastStompActivationStateAt: new Date().toISOString(),
            lastStompActivationState: state,
          })
        },
        onConnect: () => {
          if (!active || activeClient !== nextClient) {
            return
          }

          stopDegradedRefreshLoop()
          startLiveReconciliationLoop()
          publishRealtimeDebug({
            connectionState: 'live',
            selectedTransport: nextTransportMode === 'native' ? 'native-stomp' : 'sockjs-stomp',
            lastConnectSuccessAt: new Date().toISOString(),
            lastTransportError: '',
            lastTransportEvent: null,
          })
          logRealtime('info', 'connection entered live state', {
            transport: nextTransportMode,
            topicPrefix,
          })
          updateConnectionState('live')
          nextClient.subscribe(`${topicPrefix}/dashboard.summary`, (message) => {
            const payload = parseRealtimeBody(message, 'dashboard.summary')
            if (!payload) return
            mergeSnapshot({ summary: payload })
            scheduleAuthoritativeSnapshotRefresh('dashboard-summary-topic')
          })
          if (hasTenantWideWarehouseAccess) {
            nextClient.subscribe(`${topicPrefix}/alerts`, (message) => {
              const payload = parseRealtimeBody(message, 'alerts')
              if (!payload) return
              scheduleAuthoritativeSnapshotRefresh('alerts-topic')
            })
          } else {
            nextClient.subscribe(`${topicPrefix}/alerts.changed`, () => {
              scheduleAuthoritativeSnapshotRefresh('alerts-changed-topic')
            })
          }
          if (hasTenantWideWarehouseAccess) {
            nextClient.subscribe(`${topicPrefix}/recommendations`, (message) => {
              const payload = parseRealtimeBody(message, 'recommendations')
              if (!payload) return
              scheduleAuthoritativeSnapshotRefresh('recommendations-topic')
            })
          } else {
            nextClient.subscribe(`${topicPrefix}/recommendations.changed`, () => {
              scheduleAuthoritativeSnapshotRefresh('recommendations-changed-topic')
            })
          }
          if (hasTenantWideWarehouseAccess) {
            nextClient.subscribe(`${topicPrefix}/inventory`, (message) => {
              const payload = parseRealtimeBody(message, 'inventory')
              if (!payload) return
              scheduleAuthoritativeSnapshotRefresh('inventory-topic')
            })
            nextClient.subscribe(`${topicPrefix}/fulfillment.overview`, (message) => {
              const payload = parseRealtimeBody(message, 'fulfillment.overview')
              if (payload) scheduleAuthoritativeSnapshotRefresh('fulfillment-topic')
            })
            nextClient.subscribe(`${topicPrefix}/orders.recent`, (message) => {
              const payload = parseRealtimeBody(message, 'orders.recent')
              if (payload) scheduleAuthoritativeSnapshotRefresh('orders-topic')
            })
          }
          if (hasTenantWideWarehouseAccess) {
            nextClient.subscribe(`${topicPrefix}/events.recent`, (message) => {
              const payload = parseRealtimeBody(message, 'events.recent')
              if (!payload) return
              scheduleAuthoritativeSnapshotRefresh('events-recent-topic')
            })
            nextClient.subscribe(`${topicPrefix}/audit.recent`, (message) => {
              const payload = parseRealtimeBody(message, 'audit.recent')
              if (payload) scheduleAuthoritativeSnapshotRefresh('audit-topic')
            })
            nextClient.subscribe(`${topicPrefix}/system.incidents`, (message) => {
              const payload = parseRealtimeBody(message, 'system.incidents')
              if (payload) scheduleAuthoritativeSnapshotRefresh('incidents-topic')
            })
          }
          if (hasIntegrationAccess) {
            nextClient.subscribe(`${topicPrefix}/integrations.changed`, () => scheduleAuthoritativeSnapshotRefresh('integrations-changed-topic'))
            if (hasTenantWideWarehouseAccess) {
              nextClient.subscribe(`${topicPrefix}/integrations.connectors`, (message) => {
                const payload = parseRealtimeBody(message, 'integrations.connectors')
                if (payload) scheduleAuthoritativeSnapshotRefresh('integrations-connectors-topic')
              })
              nextClient.subscribe(`${topicPrefix}/integrations.imports`, (message) => {
                const payload = parseRealtimeBody(message, 'integrations.imports')
                if (payload) scheduleAuthoritativeSnapshotRefresh('integrations-imports-topic')
              })
              nextClient.subscribe(`${topicPrefix}/integrations.replay`, (message) => {
                const payload = parseRealtimeBody(message, 'integrations.replay')
                if (payload) scheduleAuthoritativeSnapshotRefresh('integrations-replay-topic')
              })
            }
          }
          if (hasTenantWideWarehouseAccess) {
            nextClient.subscribe(`${topicPrefix}/scenarios.notifications`, (message) => {
              const payload = parseRealtimeBody(message, 'scenarios.notifications')
              if (payload) scheduleAuthoritativeSnapshotRefresh('scenario-notifications-topic')
            })
            nextClient.subscribe(`${topicPrefix}/scenarios.escalated`, (message) => {
              const payload = parseRealtimeBody(message, 'scenarios.escalated')
              if (payload) scheduleAuthoritativeSnapshotRefresh('scenario-escalated-topic')
            })
          }
          void loadSnapshot()
        },
        onStompError: (frame) => {
          handleTransportFailure(nextClient, 'degraded', {
            stage: 'stomp',
            message: frame?.headers?.message || frame?.body || 'STOMP broker rejected the connection.',
            headers: frame?.headers || null,
            body: frame?.body || '',
          })
        },
        onWebSocketError: (event) => {
          handleTransportFailure(nextClient, 'degraded', {
            stage: 'websocket-error',
            message: event?.message || 'WebSocket transport error.',
          })
        },
        onWebSocketClose: (event) => {
          handleTransportFailure(nextClient, 'reconnecting', {
            stage: 'websocket-close',
            code: event?.code ?? null,
            reason: event?.reason || '',
            wasClean: typeof event?.wasClean === 'boolean' ? event.wasClean : null,
            message: `WebSocket closed${event?.code ? ` with code ${event.code}` : ''}${event?.reason ? ` (${event.reason})` : ''}.`,
          })
        },
      })

      activeClient = nextClient
      updateConnectionState('connecting')
      nextClient.activate()
    }

    startClient(websocketBrokerUrl ? 'native' : 'sockjs')

    return () => {
      active = false
      stopDegradedRefreshLoop()
      stopLiveReconciliationLoop()
      stopAuthoritativeSnapshotRefreshTimer()
      publishRealtimeDebug({
        connectionState: 'disposed',
        disposedAt: new Date().toISOString(),
      })
      activeClient?.deactivate().catch(() => {})
    }
  }, [activeTenantCode, signedInTenantCode, signedInRoleKey, signedInWarehouseScopeKey, websocketBrokerUrl, sockJsUrl])
}
