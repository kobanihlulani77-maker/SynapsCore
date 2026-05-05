import fs from 'node:fs/promises'
import path from 'node:path'
import { request as playwrightRequest } from '@playwright/test'
import {
  authRateLimitCooldownBufferMs,
  authRateLimitWindowMs,
  hostedProofStatePath,
} from './prod-proof-state.mjs'

const frontendUrl = (
  process.env.PLAYWRIGHT_BASE_URL
  || process.env.PLAYWRIGHT_FRONTEND_URL
  || 'https://synapscore-frontend-3.onrender.com'
).replace(/\/+$/, '')

const backendUrl = (
  process.env.PLAYWRIGHT_API_BASE_URL
  || process.env.PLAYWRIGHT_BACKEND_URL
  || 'https://synapscore-3.onrender.com'
).replace(/\/+$/, '')

const warmupTimeoutMs = Number.parseInt(process.env.PLAYWRIGHT_WARMUP_TIMEOUT_MS || '240000', 10)
const authenticatedWarmupTimeoutMs = Number.parseInt(process.env.PLAYWRIGHT_AUTHENTICATED_WARMUP_TIMEOUT_MS || '120000', 10)
const authenticatedWarmupRetryLimit = 3

const optionalEnv = (...names) => {
  for (const name of names) {
    const value = process.env[name]
    if (value && value.trim()) {
      return value.trim()
    }
  }
  return ''
}

const proofWarmupCredentials = (() => {
  const tenantCode = optionalEnv('PLAYWRIGHT_TENANT_CODE').toUpperCase()
  const username = optionalEnv('PLAYWRIGHT_TENANT_ADMIN_USERNAME', 'PLAYWRIGHT_OPERATIONS_LEAD_USERNAME')
  const password = optionalEnv('PLAYWRIGHT_TENANT_ADMIN_PASSWORD', 'PLAYWRIGHT_OPERATIONS_LEAD_PASSWORD')

  if (!tenantCode || !username || !password) {
    return null
  }

  return { tenantCode, username, password }
})()

function delay(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

async function readProofState() {
  try {
    const raw = await fs.readFile(hostedProofStatePath, 'utf8')
    return JSON.parse(raw)
  } catch {
    return {}
  }
}

async function waitForRecordedAuthCooldown() {
  const state = await readProofState()
  const lastTriggeredAt = Number(state.authRateLimitTriggeredAt || 0)
  if (!Number.isFinite(lastTriggeredAt) || lastTriggeredAt <= 0) {
    return
  }

  const cooldownUntil = lastTriggeredAt + authRateLimitWindowMs + authRateLimitCooldownBufferMs
  const remainingMs = cooldownUntil - Date.now()
  if (remainingMs > 0) {
    console.log(`[hosted-proof] waiting ${Math.ceil(remainingMs / 1000)}s for prior auth rate-limit window to expire`)
    await delay(remainingMs)
  }
}

async function waitForProbe(description, probe, predicate, timeoutMs = warmupTimeoutMs) {
  const startedAt = Date.now()
  let lastDetail = 'no response yet'

  while (Date.now() - startedAt < timeoutMs) {
    try {
      const result = await probe()
      lastDetail = result.detail
      if (predicate(result)) {
        console.log(`[hosted-proof] ${description} ready`)
        return result
      }
    } catch (error) {
      lastDetail = error instanceof Error ? error.message : String(error)
    }

    await delay(5_000)
  }

  throw new Error(`[hosted-proof] ${description} did not become ready within ${Math.ceil(timeoutMs / 1000)}s. Last detail: ${lastDetail}`)
}

async function waitForAuthenticatedProofTraffic(credentials) {
  let lastError = new Error('[hosted-proof] authenticated proof warm-up was not attempted')

  for (let attempt = 1; attempt <= authenticatedWarmupRetryLimit; attempt += 1) {
    const proofApi = await playwrightRequest.newContext({
      baseURL: backendUrl,
      extraHTTPHeaders: {
        'X-Synapse-Tenant': credentials.tenantCode,
      },
    })

    try {
      const loginResponse = await proofApi.post('/api/auth/session/login', {
        data: credentials,
      })
      const loginPayload = await loginResponse.json().catch(() => null)
      if (loginResponse.status() !== 200 || loginPayload?.signedIn !== true) {
        throw new Error(`login HTTP ${loginResponse.status()} ${loginPayload?.message || ''}`.trim())
      }

      await waitForProbe(
        'authenticated dashboard session',
        async () => {
          const sessionResponse = await proofApi.get('/api/auth/session')
          const sessionPayload = await sessionResponse.json().catch(() => null)
          const snapshotResponse = await proofApi.get('/api/dashboard/snapshot')
          const snapshotPayload = await snapshotResponse.json().catch(() => null)
          const runtimeResponse = await proofApi.get('/api/system/runtime')
          const runtimePayload = await runtimeResponse.json().catch(() => null)

          return {
            detail: `session=${sessionResponse.status()} snapshot=${snapshotResponse.status()} runtime=${runtimeResponse.status()}`,
            sessionStatus: sessionResponse.status(),
            sessionPayload,
            snapshotStatus: snapshotResponse.status(),
            snapshotPayload,
            runtimeStatus: runtimeResponse.status(),
            runtimePayload,
          }
        },
        (result) => (
          result.sessionStatus === 200
          && result.sessionPayload?.signedIn === true
          && result.snapshotStatus === 200
          && Array.isArray(result.snapshotPayload?.inventory)
          && result.runtimeStatus === 200
          && typeof result.runtimePayload?.readinessState === 'string'
        ),
        authenticatedWarmupTimeoutMs,
      )

      await proofApi.post('/api/auth/session/logout').catch(() => null)
      await proofApi.dispose()
      return
    } catch (error) {
      lastError = error instanceof Error ? error : new Error(String(error))
      await proofApi.dispose()
      if (attempt < authenticatedWarmupRetryLimit) {
        console.log(`[hosted-proof] authenticated warm-up attempt ${attempt} did not settle yet; retrying shortly`)
        await delay(10_000)
      }
    }
  }

  throw new Error(`[hosted-proof] authenticated dashboard session did not become ready. Last detail: ${lastError.message}`)
}

export default async function globalSetup() {
  await fs.mkdir(path.dirname(hostedProofStatePath), { recursive: true })
  await waitForRecordedAuthCooldown()

  const backend = await playwrightRequest.newContext({ baseURL: backendUrl })
  const frontend = await playwrightRequest.newContext({ baseURL: frontendUrl })

  try {
    await waitForProbe(
      'backend readiness',
      async () => {
        const response = await backend.get('/actuator/health/readiness')
        const payload = await response.json().catch(() => null)
        return {
          detail: `HTTP ${response.status()} ${(payload && payload.status) || ''}`.trim(),
          status: response.status(),
          payload,
        }
      },
      (result) => result.status === 200 && result.payload?.status === 'UP',
    )

    await waitForProbe(
      'auth session endpoint',
      async () => {
        const response = await backend.get('/api/auth/session')
        const payload = await response.json().catch(() => null)
        return {
          detail: `HTTP ${response.status()} signedIn=${payload?.signedIn}`,
          status: response.status(),
          payload,
        }
      },
      (result) => result.status === 200 && typeof result.payload?.signedIn === 'boolean',
    )

    await waitForProbe(
      'realtime SockJS endpoint',
      async () => {
        const response = await backend.get(`/ws/info?t=${Date.now()}`)
        const payload = await response.json().catch(() => null)
        return {
          detail: `HTTP ${response.status()} websocket=${payload?.websocket}`,
          status: response.status(),
          payload,
        }
      },
      (result) => result.status === 200 && typeof result.payload?.websocket === 'boolean',
    )

    await waitForProbe(
      'frontend application shell',
      async () => {
        const response = await frontend.get('/sign-in')
        const body = await response.text().catch(() => '')
        return {
          detail: `HTTP ${response.status()}`,
          status: response.status(),
          body,
        }
      },
      (result) => result.status === 200 && typeof result.body === 'string' && result.body.includes('SynapseCore'),
    )

    if (proofWarmupCredentials) {
      await waitForAuthenticatedProofTraffic(proofWarmupCredentials)
    }
  } finally {
    await Promise.allSettled([backend.dispose(), frontend.dispose()])
  }
}
