import assert from 'node:assert/strict'
import { chromium } from '@playwright/test'
import { createServer } from 'vite'

const server = await createServer({
  configFile: false,
  server: { host: '127.0.0.1', port: 0 },
})
let browser

try {
  await server.listen()
  const address = server.httpServer.address()
  const origin = `http://127.0.0.1:${address.port}`
  browser = await chromium.launch({ headless: true })
  const page = await browser.newPage()
  await page.route(origin + '/', (route) => route.fulfill({
    contentType: 'text/html',
    body: '<!doctype html><html><body><div id="root"></div></body></html>',
  }))
  await page.goto(origin)
  await page.evaluate(async () => {
    const reactModule = await import('/node_modules/.vite/deps/react.js')
    const reactDomModule = await import('/node_modules/.vite/deps/react-dom_client.js')
    const React = reactModule.default || reactModule
    const { createRoot } = reactDomModule.default || reactDomModule
    const { default: ReplayPage } = await import('/src/pages/Replay.jsx')
    const root = createRoot(document.getElementById('root'))
    const sourceSystem = 'proof_connector'
    const connectorType = 'CSV_ORDER_IMPORT'
    const record = {
      id: 1,
      sourceSystem,
      connectorType,
      externalOrderId: 'PROOF-1',
      warehouseCode: 'WH-NORTH',
      status: 'PENDING',
      replayAttemptCount: 0,
    }
    let snapshotConnector = { sourceSystem, type: connectorType, enabled: false, version: 0 }
    let callCount = 0
    const render = () => {
      root.render(React.createElement(ReplayPage, {
        context: {
          isAuthenticated: true,
          isReplayPage: true,
          snapshot: {
            integrationReplayQueue: [record],
            integrationConnectors: [snapshotConnector],
          },
          selectedReplayRecordId: 1,
          setSelectedReplayRecordId: () => {},
          pendingReplayCount: 1,
          integrationReplayState: { loadingId: null },
          replayFailedIntegration: () => {},
          signedInSession: { tenantCode: 'PROOF', username: 'operator', authenticatedAt: 'now' },
          signedInRoles: ['INTEGRATION_ADMIN'],
          signedInWarehouseScopes: [],
          hasWarehouseScope: () => true,
          navigateToPage: () => {},
          formatCodeLabel: (value) => value,
          formatTimestamp: (value) => value || '',
          getReplayStatusClassName: () => 'status-partial',
          fetchJson: async () => {
            callCount += 1
            await new Promise((resolve) => setTimeout(resolve, 200))
            return [{ sourceSystem, type: connectorType, enabled: callCount > 1, version: callCount > 1 ? 1 : 0 }]
          },
        },
      }))
    }
    render()
    const interval = setInterval(render, 50)
    window.__replayProof = {
      calls: () => callCount,
      stop: () => clearInterval(interval),
      setSnapshotConnector: (connector) => {
        snapshotConnector = connector
        render()
      },
    }
  })

  const replayButton = page.getByRole('button', { name: 'Replay Into Live Flow' })
  await replayButton.waitFor({ state: 'visible' })
  await page.waitForFunction(() => {
    const button = [...document.querySelectorAll('button')]
      .find((item) => item.textContent?.trim() === 'Replay Into Live Flow')
    return window.__replayProof.calls() >= 2 && button && !button.disabled
  }, null, { timeout: 6_000 })
  assert.equal(await replayButton.isEnabled(), true)

  await page.evaluate(() => {
    window.__replayProof.stop()
    window.__replayProof.setSnapshotConnector({
      sourceSystem: 'proof_connector',
      type: 'CSV_ORDER_IMPORT',
      enabled: false,
      version: 2,
    })
  })
  await page.waitForFunction(() => {
    const button = [...document.querySelectorAll('button')]
      .find((item) => item.textContent?.trim() === 'Replay Into Live Flow')
    return button?.disabled === true
  })
  assert.equal(await replayButton.isDisabled(), true)
  console.log('Replay connector poll lifecycle browser check passed.')
} finally {
  await browser?.close()
  await server.close()
}
