export default function useIntegrationActions({
  fetchJson,
  refreshSnapshotQuietly,
  setIntegrationConnectorState,
  setIntegrationReplayState,
}) {
  async function toggleConnector(connector) {
    const loadingKey = `${connector.sourceSystem}:${connector.type}`
    setIntegrationConnectorState({ loadingKey, error: '', success: '' })
    try {
      const payload = await fetchJson('/api/integrations/orders/connectors', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          sourceSystem: connector.sourceSystem,
          type: connector.type,
          displayName: connector.displayName,
          enabled: !connector.enabled,
          syncMode: connector.syncMode,
          syncIntervalMinutes: connector.syncIntervalMinutes,
          pullEndpointUrl: connector.pullEndpointUrl,
          validationPolicy: connector.validationPolicy,
          transformationPolicy: connector.transformationPolicy,
          allowDefaultWarehouseFallback: connector.allowDefaultWarehouseFallback,
          defaultWarehouseCode: connector.defaultWarehouseCode,
          notes: connector.notes,
        }),
      })
      setIntegrationConnectorState({
        loadingKey: null,
        error: '',
        success: `${payload.displayName} ${payload.enabled ? 'enabled' : 'disabled'}.`,
      })
      await refreshSnapshotQuietly()
    } catch (error) {
      setIntegrationConnectorState({ loadingKey: null, error: error.message, success: '' })
    }
  }

  async function replayFailedIntegration(recordId) {
    setIntegrationReplayState({ loadingId: recordId, error: '', success: '' })
    try {
      const payload = await fetchJson(`/api/integrations/orders/replay/${recordId}`, { method: 'POST' })
      setIntegrationReplayState({
        loadingId: null,
        error: '',
        success: `Replayed ${payload.order.externalOrderId} into the live order flow.`,
      })
      await refreshSnapshotQuietly()
    } catch (error) {
      setIntegrationReplayState({ loadingId: null, error: error.message, success: '' })
    }
  }

  return {
    toggleConnector,
    replayFailedIntegration,
  }
}
