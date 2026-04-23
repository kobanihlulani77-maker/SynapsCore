import { useEffect } from 'react'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

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
        if (active) {
          setPageState({ loading: false, error: error.message })
        }
      }
    }

    loadSnapshot()

    if (!signedInTenantCode) {
      setConnectionState('signed-out')
      return () => {
        active = false
      }
    }

    if (!sockJsUrl && !websocketBrokerUrl) {
      setConnectionState('degraded')
      return () => {
        active = false
      }
    }

    const topicPrefix = buildTenantTopicPrefix(activeTenantCode)
    setConnectionState('connecting')

    const client = new Client({
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      brokerURL: /^wss?:/i.test(websocketBrokerUrl) ? websocketBrokerUrl : undefined,
      webSocketFactory: /^wss?:/i.test(websocketBrokerUrl) ? undefined : () => new SockJS(sockJsUrl),
      onConnect: () => {
        setConnectionState('live')
        client.subscribe(`${topicPrefix}/dashboard.summary`, (message) => mergeSnapshot({ summary: JSON.parse(message.body) }))
        client.subscribe(`${topicPrefix}/alerts`, (message) => mergeSnapshot({ alerts: JSON.parse(message.body) }))
        client.subscribe(`${topicPrefix}/recommendations`, (message) => mergeSnapshot({ recommendations: JSON.parse(message.body) }))
        client.subscribe(`${topicPrefix}/inventory`, (message) => mergeSnapshot({ inventory: JSON.parse(message.body) }))
        client.subscribe(`${topicPrefix}/fulfillment.overview`, (message) => mergeSnapshot({ fulfillment: JSON.parse(message.body) }))
        client.subscribe(`${topicPrefix}/orders.recent`, (message) => mergeSnapshot({ recentOrders: JSON.parse(message.body) }))
        client.subscribe(`${topicPrefix}/events.recent`, (message) => mergeSnapshot({ recentEvents: JSON.parse(message.body) }))
        client.subscribe(`${topicPrefix}/audit.recent`, (message) => mergeSnapshot({ auditLogs: JSON.parse(message.body) }))
        client.subscribe(`${topicPrefix}/system.incidents`, (message) => mergeSnapshot({ systemIncidents: JSON.parse(message.body) }))
        client.subscribe(`${topicPrefix}/integrations.connectors`, (message) => mergeSnapshot({ integrationConnectors: JSON.parse(message.body) }))
        client.subscribe(`${topicPrefix}/integrations.imports`, (message) => mergeSnapshot({ integrationImportRuns: JSON.parse(message.body) }))
        client.subscribe(`${topicPrefix}/integrations.replay`, (message) => mergeSnapshot({ integrationReplayQueue: JSON.parse(message.body) }))
        client.subscribe(`${topicPrefix}/scenarios.notifications`, (message) => mergeSnapshot({ scenarioNotifications: JSON.parse(message.body) }))
        client.subscribe(`${topicPrefix}/scenarios.escalated`, (message) => mergeSnapshot({ slaEscalations: JSON.parse(message.body) }))
      },
      onStompError: () => setConnectionState('degraded'),
      onWebSocketError: () => setConnectionState('degraded'),
      onWebSocketClose: () => setConnectionState('reconnecting'),
    })

    client.activate()

    return () => {
      active = false
      client.deactivate()
    }
  }, [activeTenantCode, signedInTenantCode, websocketBrokerUrl, sockJsUrl])
}
