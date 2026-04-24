import { randomUUID } from 'node:crypto'
import { expect, request as playwrightRequest, test } from '@playwright/test'

const backendUrl = process.env.PLAYWRIGHT_API_BASE_URL
  || process.env.PLAYWRIGHT_BACKEND_URL
  || 'https://synapscore-3.onrender.com'
const requiredEnv = (...names) => {
  for (const name of names) {
    const value = process.env[name]
    if (value && value.trim()) {
      return value.trim()
    }
  }
  throw new Error(`Missing required environment variable. Set one of: ${names.join(', ')} for live production proof.`)
}

const proofTenantCode = requiredEnv('PLAYWRIGHT_TENANT_CODE').toUpperCase()
const deriveDefaultProofProductSku = (tenantCode) => {
  const normalizedTenant = tenantCode.replace(/[^A-Z0-9._-]/g, '-')
  const candidate = `SKU-${normalizedTenant}-PROOF`
  return candidate.length <= 64
    ? candidate
    : `SKU-${normalizedTenant.slice(0, Math.min(normalizedTenant.length, 50))}-PRF`
}
const defaultProofProductSku = deriveDefaultProofProductSku(proofTenantCode)
const proofProductSku = (process.env.PLAYWRIGHT_PROOF_PRODUCT_SKU || defaultProofProductSku).trim().toUpperCase()

const users = {
  operationsLead: {
    tenantCode: proofTenantCode,
    username: requiredEnv('PLAYWRIGHT_TENANT_ADMIN_USERNAME', 'PLAYWRIGHT_OPERATIONS_LEAD_USERNAME'),
    password: requiredEnv('PLAYWRIGHT_TENANT_ADMIN_PASSWORD', 'PLAYWRIGHT_OPERATIONS_LEAD_PASSWORD'),
  },
  operationsPlanner: {
    tenantCode: proofTenantCode,
    username: requiredEnv('PLAYWRIGHT_PLANNER_USERNAME', 'PLAYWRIGHT_OPERATIONS_PLANNER_USERNAME'),
    password: requiredEnv('PLAYWRIGHT_PLANNER_PASSWORD', 'PLAYWRIGHT_OPERATIONS_PLANNER_PASSWORD'),
  },
  integrationLead: {
    tenantCode: proofTenantCode,
    username: requiredEnv('PLAYWRIGHT_INTEGRATION_ADMIN_USERNAME', 'PLAYWRIGHT_INTEGRATION_LEAD_USERNAME'),
    password: requiredEnv('PLAYWRIGHT_INTEGRATION_ADMIN_PASSWORD', 'PLAYWRIGHT_INTEGRATION_LEAD_PASSWORD'),
  },
}

const appPages = [
  ['/dashboard', 'Live operational command center'],
  ['/alerts', 'Operational warning center'],
  ['/recommendations', 'Action queue for the operating team'],
  ['/orders', 'Live order operations'],
  ['/inventory', 'Inventory intelligence'],
  ['/catalog', 'Tenant product catalog'],
  ['/locations', 'Warehouse and site health'],
  ['/fulfillment', 'Fulfillment and logistics pressure'],
  ['/scenarios', 'Decision lab and scenario planning'],
  ['/scenario-history', 'Scenario history and compare'],
  ['/approvals', 'Approvals center'],
  ['/escalations', 'Operational escalation inbox'],
  ['/integrations', 'Connector management and telemetry'],
  ['/replay-queue', 'Failed inbound recovery'],
  ['/runtime', 'Runtime, incidents, and observability'],
  ['/audit-events', 'Audit trail and business events'],
  ['/users', 'Users and access control'],
  ['/company-settings', 'Tenant and workspace settings'],
  ['/profile', 'Personal profile and session controls'],
  ['/platform-admin', 'Platform overview and cross-tenant trust'],
  ['/tenant-management', 'Tenant onboarding and workspace rollout'],
  ['/system-config', 'System configuration and operational defaults'],
  ['/releases', 'Release, deployment, and environment'],
]

test.describe.configure({ mode: 'serial' })

async function createApiContext(credentials) {
  const api = await playwrightRequest.newContext({
    baseURL: backendUrl,
    extraHTTPHeaders: {
      'X-Synapse-Tenant': credentials.tenantCode,
    },
  })

  const loginResponse = await api.post('/api/auth/session/login', {
    data: credentials,
  })

  expect(loginResponse.ok()).toBeTruthy()
  return api
}

async function readJson(response) {
  const payload = await response.json()
  if (!response.ok()) {
    throw new Error(payload.message || `Request failed with status ${response.status()}.`)
  }
  return payload
}

async function loginViaUi(page, credentials) {
  await page.goto('/sign-in')
  await expect(page.getByRole('heading', { name: 'Access your operational workspace.' })).toBeVisible()
  const signInCard = page.locator('.public-signin-card')
  await waitForSignInReady(signInCard)
  await fillSignInForm(signInCard, credentials, credentials.password)
  await signInCard.getByRole('button', { name: 'Enter Platform' }).click()
  await expect(page).toHaveURL(/\/dashboard$/)
  await expect(page.getByRole('heading', { level: 1, name: 'Live operational command center' })).toBeVisible()
}

async function signOutViaUi(page) {
  const signOutButton = page.getByRole('button', { name: 'Sign Out' }).first()
  if (await signOutButton.isVisible()) {
    await signOutButton.click()
    await expect(page.getByRole('heading', { name: 'Access your operational workspace.' })).toBeVisible()
  }
}

async function fillSignInForm(signInCard, credentials, password) {
  const tenantField = signInCard.getByRole('combobox', { name: 'Tenant workspace', exact: true })
  const usernameField = signInCard.getByRole('textbox', { name: 'Username', exact: true })
  const passwordField = signInCard.getByLabel('Password', { exact: true })
  const submitButton = signInCard.getByRole('button', { name: 'Enter Platform' })

  let lastError = null
  for (let attempt = 0; attempt < 3; attempt += 1) {
    try {
      await tenantField.fill(credentials.tenantCode)
      await expect(tenantField).toHaveValue(credentials.tenantCode)

      await usernameField.fill(credentials.username)
      await expect(usernameField).toHaveValue(credentials.username)

      await passwordField.fill(password)
      await expect(passwordField).toHaveValue(password)
      await expect(submitButton).toBeEnabled()
      return
    } catch (error) {
      lastError = error
    }
  }

  throw lastError
}

async function waitForSignInReady(signInCard) {
  const loadingWorkspaceDirectory = signInCard.getByText('Loading available workspaces...')
  if (await loadingWorkspaceDirectory.isVisible().catch(() => false)) {
    await expect(loadingWorkspaceDirectory).toBeHidden({ timeout: 60_000 })
  }

  await expect(signInCard.getByRole('combobox', { name: 'Tenant workspace', exact: true })).toBeEnabled()
  await expect(signInCard.getByRole('textbox', { name: 'Username', exact: true })).toBeEnabled()
  await expect(signInCard.getByLabel('Password', { exact: true })).toBeEnabled()
}

async function navigateWithinApp(page, route) {
  await page.evaluate((nextRoute) => {
    window.history.pushState({}, '', nextRoute)
    window.dispatchEvent(new PopStateEvent('popstate'))
  }, route)
}

async function expectNoFatalUiErrors(page) {
  const fatalErrors = page.locator('.error-text:visible').filter({
    hasText: /Snapshot load issue:|Invalid operator credentials\.|Request failed|Failed to|Unable to|Unexpected|Forbidden|Access denied/i,
  })
  await expect(fatalErrors).toHaveCount(0)
}

async function summaryCardValue(page, label) {
  const card = page.locator('.summary-card').filter({ hasText: label }).first()
  await expect(card).toBeVisible()
  const value = await card.locator('.summary-value').textContent()
  return Number.parseInt((value || '').trim(), 10)
}

async function waitForNumericSummaryCard(page, label) {
  let numericValue = Number.NaN
  await expect.poll(async () => {
    numericValue = await summaryCardValue(page, label)
    return Number.isFinite(numericValue)
  }, {
    timeout: 30_000,
    message: `Expected the ${label} summary card to resolve to a numeric value.`,
  }).toBeTruthy()
  return numericValue
}

async function createReplayFixture() {
  const inventoryAdmin = await createApiContext(users.operationsLead)
  const api = await createApiContext(users.integrationLead)
  const suffix = randomUUID().slice(0, 8).toUpperCase()
  const sourceSystem = `ui_replay_${suffix}`.toLowerCase()
  const externalOrderId = `UI-RPL-${suffix}`

  try {
    await readJson(await inventoryAdmin.post('/api/inventory/update', {
      data: {
        productSku: proofProductSku,
        warehouseCode: 'WH-NORTH',
        quantityAvailable: 50,
        reorderThreshold: 12,
      },
    }))

    await readJson(await api.post('/api/integrations/orders/connectors', {
      data: {
        sourceSystem,
        type: 'CSV_ORDER_IMPORT',
        displayName: `UI Replay ${suffix}`,
        enabled: false,
        syncMode: 'BATCH_FILE_DROP',
        validationPolicy: 'RELAXED',
        transformationPolicy: 'NORMALIZE_CODES',
        allowDefaultWarehouseFallback: false,
        notes: 'Disposable replay verification connector.',
      },
    }))

    const csvImportResponse = await api.post('/api/integrations/orders/csv-import', {
      multipart: {
        file: {
          name: 'orders.csv',
          mimeType: 'text/csv',
          buffer: Buffer.from(
            `externalOrderId,warehouseCode,productSku,quantity,unitPrice\n${externalOrderId},WH-NORTH,${proofProductSku},2,88.00\n`,
            'utf8',
          ),
        },
        sourceSystem,
      },
    })
    const csvImportPayload = await readJson(csvImportResponse)
    expect(csvImportPayload.ordersFailed).toBe(1)

    await readJson(await api.post('/api/integrations/orders/connectors', {
      data: {
        sourceSystem,
        type: 'CSV_ORDER_IMPORT',
        displayName: `UI Replay ${suffix}`,
        enabled: true,
        syncMode: 'BATCH_FILE_DROP',
        validationPolicy: 'RELAXED',
        transformationPolicy: 'NORMALIZE_CODES',
        allowDefaultWarehouseFallback: false,
        notes: 'Enabled for replay verification.',
      },
    }))

    await expect.poll(async () => {
      const connectors = await readJson(await api.get('/api/integrations/orders/connectors'))
      return connectors.find((connector) => connector.sourceSystem === sourceSystem && connector.type === 'CSV_ORDER_IMPORT')?.enabled ?? false
    }, {
      timeout: 30_000,
      message: `Expected replay verification connector ${sourceSystem} to become enabled before UI replay proof.`,
    }).toBe(true)

    await expect.poll(async () => {
      const replayQueue = await readJson(await api.get('/api/integrations/orders/replay-queue'))
      const replayRecord = replayQueue.find((record) => record.externalOrderId === externalOrderId)
      if (!replayRecord) {
        return 'missing'
      }
      if (replayRecord.nextEligibleAt && Date.parse(replayRecord.nextEligibleAt) > Date.now()) {
        return 'waiting'
      }
      return replayRecord.status
    }, {
      timeout: 30_000,
      message: `Expected replay verification record ${externalOrderId} to be present and eligible before UI replay proof.`,
    }).toBe('PENDING')

    return { api, sourceSystem, externalOrderId }
  } catch (error) {
    await api.dispose()
    throw error
  } finally {
    await inventoryAdmin.dispose()
  }
}

async function createScenarioFixture() {
  const api = await createApiContext(users.operationsLead)
  const suffix = randomUUID().slice(0, 8).toUpperCase()
  const title = `UI Scenario ${suffix}`

  try {
    await readJson(await api.post('/api/scenarios/save', {
      data: {
        title,
        requestedBy: 'Operations Lead',
        request: {
          warehouseCode: 'WH-NORTH',
          items: [
            {
              productSku: proofProductSku,
              quantity: 1,
              unitPrice: 95,
            },
          ],
        },
      },
    }))

    return { api, title }
  } catch (error) {
    await api.dispose()
    throw error
  }
}

test('auth flow and the full authenticated page system render cleanly in a browser', async ({ page }) => {
  await page.goto('/dashboard')
  await expect(page.getByRole('heading', { name: 'Access your operational workspace.' })).toBeVisible()
  const signInCard = page.locator('.public-signin-card')
  await waitForSignInReady(signInCard)

  await fillSignInForm(signInCard, users.operationsLead, 'wrong-code')
  await signInCard.getByRole('button', { name: 'Enter Platform' }).click()
  await expect(signInCard.getByRole('button', { name: 'Enter Platform' })).toBeEnabled({ timeout: 60_000 })
  await expect(signInCard.getByText('Invalid operator credentials.')).toBeVisible({ timeout: 15_000 })

  await waitForSignInReady(signInCard)
  await fillSignInForm(signInCard, users.operationsLead, users.operationsLead.password)
  await signInCard.getByRole('button', { name: 'Enter Platform' }).click()
  await expect(page).toHaveURL(/\/dashboard$/)

  for (const [route, title] of appPages) {
    await navigateWithinApp(page, route)
    await expect(page.getByRole('heading', { level: 1, name: title })).toBeVisible()
    await expect(page.locator('.workspace-topbar')).toBeVisible()
    await expectNoFatalUiErrors(page)
  }

  await signOutViaUi(page)
})

test('product catalog onboarding works through tenant-scoped API and browser surface', async ({ page }) => {
  const api = await createApiContext(users.operationsLead)
  const suffix = randomUUID().slice(0, 8).toUpperCase()
  const primarySku = `SKU-UI-${suffix}`
  const importSku = `SKU-IMP-${suffix}`

  try {
    const createdProduct = await readJson(await api.post('/api/products', {
      data: {
        sku: primarySku,
        name: `UI Catalog ${suffix}`,
        category: 'Verification',
      },
    }))
    expect(createdProduct.sku).toBe(primarySku)
    expect(createdProduct.tenantCode).toBe(users.operationsLead.tenantCode)

    const updatedProduct = await readJson(await api.put(`/api/products/${createdProduct.id}`, {
      data: {
        sku: primarySku,
        name: `UI Catalog ${suffix} Updated`,
        category: 'Verification',
      },
    }))
    expect(updatedProduct.name).toContain('Updated')

    const importResult = await readJson(await api.post('/api/products/import', {
      multipart: {
        file: {
          name: 'products.csv',
          mimeType: 'text/csv',
          buffer: Buffer.from(
            `sku,name,category\n${importSku},Imported Product ${suffix},Verification\n${primarySku},Imported Update ${suffix},Verification\n${importSku},Duplicate Product ${suffix},Verification\n`,
            'utf8',
          ),
        },
      },
    }))
    expect(importResult.created).toBe(1)
    expect(importResult.updated).toBe(1)
    expect(importResult.failed).toBe(1)

    const products = await readJson(await api.get('/api/products'))
    expect(products.some((product) => product.sku === primarySku && product.tenantCode === users.operationsLead.tenantCode)).toBeTruthy()
    expect(products.some((product) => product.sku === importSku && product.tenantCode === users.operationsLead.tenantCode)).toBeTruthy()

    await loginViaUi(page, users.operationsLead)
    await navigateWithinApp(page, '/catalog')
    await expect(page.getByRole('heading', { level: 1, name: 'Tenant product catalog' })).toBeVisible()
    await expect(page.getByText(primarySku).first()).toBeVisible()
    await expect(page.getByText(importSku).first()).toBeVisible()
    await expectNoFatalUiErrors(page)
  } finally {
    await api.dispose()
  }
})

test('@realtime dashboard summary updates live without a browser refresh', async ({ page }) => {
  await loginViaUi(page, users.operationsLead)
  await expect(page.getByRole('heading', { level: 1, name: 'Live operational command center' })).toBeVisible()
  await expect(page.getByText('Realtime state')).toBeVisible()

  const api = await createApiContext(users.operationsLead)
  let candidate = null
  let revertQuantity = null
  let revertThreshold = null

  try {
    const inventory = await readJson(await api.get('/api/inventory'))
    candidate = inventory.find((item) => item.productSku === proofProductSku && item.warehouseCode === 'WH-NORTH')
      || inventory.find((item) => Number.isFinite(item.quantityAvailable) && Number.isFinite(item.reorderThreshold))
    expect(candidate).toBeTruthy()

    revertQuantity = candidate.quantityAvailable
    revertThreshold = candidate.reorderThreshold

    const beforeRisk = await waitForNumericSummaryCard(page, 'Risk')
    const threshold = Number.isFinite(candidate.reorderThreshold) ? candidate.reorderThreshold : 5
    const forceLowQuantity = Math.max(0, threshold - 1)
    const safeQuantity = threshold + 5

    await readJson(await api.post('/api/inventory/update', {
      data: {
        productSku: candidate.productSku,
        warehouseCode: candidate.warehouseCode,
        quantityAvailable: candidate.lowStock ? safeQuantity : forceLowQuantity,
        reorderThreshold: threshold,
      },
    }))

    await expect.poll(async () => summaryCardValue(page, 'Risk'), {
      timeout: 30_000,
      message: 'Expected the dashboard low-stock summary to change through the live websocket path.',
    })[candidate.lowStock ? 'toBeLessThan' : 'toBeGreaterThan'](beforeRisk)
  } finally {
    if (candidate && revertQuantity != null && revertThreshold != null) {
      await readJson(await api.post('/api/inventory/update', {
        data: {
          productSku: candidate.productSku,
          warehouseCode: candidate.warehouseCode,
          quantityAvailable: revertQuantity,
          reorderThreshold: revertThreshold,
        },
      }))
    }
    await api.dispose()
  }
})

test('replay recovery, scenario approval, execution, and browser role gating work through the UI', async ({ page }) => {
  const replayFixture = await createReplayFixture()

  try {
    await loginViaUi(page, users.integrationLead)
    await navigateWithinApp(page, '/replay-queue')
    await expect(page.getByRole('heading', { level: 1, name: 'Failed inbound recovery' })).toBeVisible()

    const replayPanel = page.locator('article.panel').filter({ hasText: replayFixture.externalOrderId }).first()
    const replayButton = replayPanel.getByRole('button', { name: 'Replay Into Live Flow' }).first()
    await expect(replayButton).toBeVisible()
    await expect(replayButton).toBeEnabled()
    await replayButton.click()
    await expect(page.locator('.success-text, .muted-text').filter({ hasText: `Replayed ${replayFixture.externalOrderId} into the live order flow.` }).first()).toBeVisible()
  } finally {
    await replayFixture.api.dispose()
  }

  await signOutViaUi(page)

  const scenarioFixture = await createScenarioFixture()

  try {
    await loginViaUi(page, users.operationsLead)
    await navigateWithinApp(page, '/scenario-history')
    await expect(page.getByRole('heading', { level: 1, name: 'Scenario history and compare' })).toBeVisible()

    const scenarioApprovalConsole = page.locator('.stack-card').filter({
      hasText: scenarioFixture.title,
      has: page.getByRole('button', { name: 'Approve Plan' }),
    }).first()
    await expect(scenarioApprovalConsole).toBeVisible()
    await scenarioApprovalConsole.getByRole('button', { name: 'Approve Plan' }).click()
    await expect(page.locator('.success-text').filter({ hasText: `Approved ${scenarioFixture.title} for execution under Standard approval.` }).first()).toBeVisible()

    const scenarioExecutionConsole = page.locator('.stack-card').filter({
      hasText: scenarioFixture.title,
      has: page.getByRole('button', { name: 'Execute Scenario' }),
    }).first()
    await expect(scenarioExecutionConsole.getByRole('button', { name: 'Execute Scenario' })).toBeVisible()
    await scenarioExecutionConsole.getByRole('button', { name: 'Execute Scenario' }).click()
    await expect(page.locator('.success-text').filter({ hasText: new RegExp(`^Executed ${scenarioFixture.title} as live order `, 'i') }).first()).toBeVisible()
  } finally {
    await scenarioFixture.api.dispose()
  }

  await signOutViaUi(page)

  await loginViaUi(page, users.operationsPlanner)
  await navigateWithinApp(page, '/users')
  await expect(page.getByRole('heading', { level: 1, name: 'Users and access control' })).toBeVisible()
  await expect(page.getByText('Tenant admin access required')).toBeVisible()
  await expect(page.getByText('Operators', { exact: true }).first()).toBeVisible()
})
