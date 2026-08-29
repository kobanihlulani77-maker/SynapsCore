import { useEffect } from 'react'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

const isSessionBootstrapError = (message = '') => /signed-in user session|session is missing or expired|sign in again/i.test(String(message))
const realtimeDebugKey = '__SYNAPSE_REALTIME_DEBUG__'
const degradedRefreshIntervalMs = 15_000
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
  fetchJson,
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
    let activeClient = null
    let decisionSurfaceRefreshTimeoutId = null
    let decisionSurfaceRefreshInFlight = false
    let decisionSurfaceRefreshQueued = false

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
        return
      }

      try {
        await Promise.all([fetchSnapshot(), fetchCatalogProducts({ quiet: true })])
        publishRealtimeDebug({
          lastSnapshotSyncAt: new Date().toISOString(),
          lastSnapshotSyncStatus: 'ok',
        })
      } catch (error) {
        publishRealtimeDebug({
          lastSnapshotSyncAt: new Date().toISOString(),
          lastSnapshotSyncStatus: 'error',
          lastSnapshotSyncError: error?.message || String(error),
        })
        if (active && !isSessionBootstrapError(error?.message)) {
          setPageState({ loading: false, error: error.message })
        }
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

    function stopDecisionSurfaceRefreshTimer() {
      if (decisionSurfaceRefreshTimeoutId !== null) {
        globalThis.clearTimeout(decisionSurfaceRefreshTimeoutId)
        decisionSurfaceRefreshTimeoutId = null
      }
    }

    async function refreshDecisionSurface(reason = 'realtime-surface-refresh') {
      if (!active || !signedInTenantCode || !fetchJson) {
        return
      }
      if (decisionSurfaceRefreshInFlight) {
        decisionSurfaceRefreshQueued = true
        return
      }

      decisionSurfaceRefreshInFlight = true
      try {
        const timeoutInit = (timeoutMs) => (
          globalThis.AbortSignal?.timeout ? { signal: globalThis.AbortSignal.timeout(timeoutMs) } : {}
        )

        const [summaryResult, inventoryResult, alertsResult, recommendationsResult] = await Promise.allSettled([
          fetchJson('/api/dashboard/summary', timeoutInit(8_000)),
          fetchJson('/api/inventory', timeoutInit(8_000)),
          fetchJson('/api/alerts', timeoutInit(8_000)),
          fetchJson('/api/recommendations', timeoutInit(8_000)),
        ])

        const nextPartial = {}
        if (summaryResult.status === 'fulfilled' && summaryResult.value) {
          nextPartial.summary = summaryResult.value
        }
        if (inventoryResult.status === 'fulfilled' && Array.isArray(inventoryResult.value)) {
          nextPartial.inventory = inventoryResult.value
        }
        if (alertsResult.status === 'fulfilled' && alertsResult.value) {
          nextPartial.alerts = alertsResult.value
        }
        if (recommendationsResult.status === 'fulfilled' && Array.isArray(recommendationsResult.value)) {
          nextPartial.recommendations = recommendationsResult.value
        }

        if (Object.keys(nextPartial).length) {
          mergeSnapshot(nextPartial)
          publishRealtimeDebug({
            lastDecisionSurfaceRefreshAt: new Date().toISOString(),
            lastDecisionSurfaceRefreshReason: reason,
            lastDecisionSurfaceRefreshStatus: 'ok',
            lastDecisionSurfaceRefreshKeys: Object.keys(nextPartial),
          })
        } else {
          publishRealtimeDebug({
            lastDecisionSurfaceRefreshAt: new Date().toISOString(),
            lastDecisionSurfaceRefreshReason: reason,
            lastDecisionSurfaceRefreshStatus: 'empty',
          })
        }
      } catch (error) {
        publishRealtimeDebug({
          lastDecisionSurfaceRefreshAt: new Date().toISOString(),
          lastDecisionSurfaceRefreshReason: reason,
          lastDecisionSurfaceRefreshStatus: 'error',
          lastDecisionSurfaceRefreshError: error?.message || String(error),
        })
      } finally {
        decisionSurfaceRefreshInFlight = false
        if (decisionSurfaceRefreshQueued) {
          decisionSurfaceRefreshQueued = false
          void refreshDecisionSurface(`${reason}:queued`)
        }
      }
    }

    function scheduleDecisionSurfaceRefresh(reason, delayMs = 750) {
      if (!active || !signedInTenantCode || !fetchJson) {
        return
      }
      stopDecisionSurfaceRefreshTimer()
      decisionSurfaceRefreshTimeoutId = globalThis.setTimeout(() => {
        decisionSurfaceRefreshTimeoutId = null
        void refreshDecisionSurface(reason)
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
      startDegradedRefreshLoop()
      void refreshWhileDegraded()
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
            mergeSnapshot({ summary: JSON.parse(message.body) })
            if (hasTenantWideWarehouseAccess) {
              scheduleDecisionSurfaceRefresh('dashboard-summary-topic')
            } else {
              void fetchSnapshot()
            }
          })
          if (hasTenantWideWarehouseAccess) {
            nextClient.subscribe(`${topicPrefix}/alerts`, (message) => {
              mergeSnapshot({ alerts: JSON.parse(message.body) })
              scheduleDecisionSurfaceRefresh('alerts-topic')
            })
          } else {
            nextClient.subscribe(`${topicPrefix}/alerts.changed`, () => {
              scheduleDecisionSurfaceRefresh('alerts-changed-topic')
            })
          }
          nextClient.subscribe(`${topicPrefix}/recommendations`, (message) => {
            mergeSnapshot({ recommendations: JSON.parse(message.body) })
            scheduleDecisionSurfaceRefresh('recommendations-topic')
          })
          if (hasTenantWideWarehouseAccess) {
            nextClient.subscribe(`${topicPrefix}/inventory`, (message) => {
              mergeSnapshot({ inventory: JSON.parse(message.body) })
              scheduleDecisionSurfaceRefresh('inventory-topic')
            })
            nextClient.subscribe(`${topicPrefix}/fulfillment.overview`, (message) => mergeSnapshot({ fulfillment: JSON.parse(message.body) }))
            nextClient.subscribe(`${topicPrefix}/orders.recent`, (message) => mergeSnapshot({ recentOrders: JSON.parse(message.body) }))
          }
          nextClient.subscribe(`${topicPrefix}/events.recent`, (message) => {
            mergeSnapshot({ recentEvents: JSON.parse(message.body) })
            scheduleDecisionSurfaceRefresh('events-recent-topic')
          })
          nextClient.subscribe(`${topicPrefix}/audit.recent`, (message) => mergeSnapshot({ auditLogs: JSON.parse(message.body) }))
          nextClient.subscribe(`${topicPrefix}/system.incidents`, (message) => mergeSnapshot({ systemIncidents: JSON.parse(message.body) }))
          if (hasIntegrationAccess) {
            nextClient.subscribe(`${topicPrefix}/integrations.changed`, () => void fetchSnapshot())
            if (hasTenantWideWarehouseAccess) {
              nextClient.subscribe(`${topicPrefix}/integrations.connectors`, (message) => mergeSnapshot({ integrationConnectors: JSON.parse(message.body) }))
              nextClient.subscribe(`${topicPrefix}/integrations.imports`, (message) => mergeSnapshot({ integrationImportRuns: JSON.parse(message.body) }))
              nextClient.subscribe(`${topicPrefix}/integrations.replay`, (message) => mergeSnapshot({ integrationReplayQueue: JSON.parse(message.body) }))
            }
          }
          if (hasTenantWideWarehouseAccess) {
            nextClient.subscribe(`${topicPrefix}/scenarios.notifications`, (message) => mergeSnapshot({ scenarioNotifications: JSON.parse(message.body) }))
            nextClient.subscribe(`${topicPrefix}/scenarios.escalated`, (message) => mergeSnapshot({ slaEscalations: JSON.parse(message.body) }))
          }
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
      stopDecisionSurfaceRefreshTimer()
      publishRealtimeDebug({
        connectionState: 'disposed',
        disposedAt: new Date().toISOString(),
      })
      activeClient?.deactivate().catch(() => {})
    }
  }, [activeTenantCode, signedInTenantCode, signedInRoleKey, signedInWarehouseScopeKey, websocketBrokerUrl, sockJsUrl])
}
