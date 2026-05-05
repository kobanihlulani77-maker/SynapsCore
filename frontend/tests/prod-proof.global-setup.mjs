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
const authenticatedWarmupTimeoutMs = Number.parseInt(process.env.PLAYWRIGHT_AUTHENTICATED_WARMUP_TIMEOUT_MS || '180000', 10)

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
  const startedAt = Date.now()
  let attempt = 0

  while (Date.now() - startedAt < authenticatedWarmupTimeoutMs) {
    attempt += 1
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

      const remainingWarmupMs = Math.max(15_000, authenticatedWarmupTimeoutMs - (Date.now() - startedAt))
      const waitWithinRemainingBudget = async (description, probe, predicate, maxRequestTimeoutMs = 30_000) => {
        const phaseRemainingMs = Math.max(15_000, authenticatedWarmupTimeoutMs - (Date.now() - startedAt))
        const requestTimeoutMs = Math.max(
          15_000,
          Math.min(
            maxRequestTimeoutMs,
            phaseRemainingMs - 5_000,
          ),
        )

        await waitForProbe(
          description,
          () => probe(requestTimeoutMs),
          predicate,
          phaseRemainingMs,
        )
      }

      await waitWithinRemainingBudget(
        'authenticated session',
        async (requestTimeoutMs) => {
          const sessionResponse = await proofApi.get('/api/auth/session', { timeout: requestTimeoutMs })
          const sessionPayload = await sessionResponse.json().catch(() => null)
          return {
            detail: `session=${sessionResponse.status()} signedIn=${sessionPayload?.signedIn}`,
            status: sessionResponse.status(),
            payload: sessionPayload,
          }
        },
        (result) => result.status === 200 && result.payload?.signedIn === true,
        20_000,
      )

      await waitWithinRemainingBudget(
        'authenticated dashboard summary',
        async (requestTimeoutMs) => {
          const summaryResponse = await proofApi.get('/api/dashboard/summary', { timeout: requestTimeoutMs })
          const summaryPayload = await summaryResponse.json().catch(() => null)
          return {
            detail: `summary=${summaryResponse.status()} totalOrders=${summaryPayload?.totalOrders}`,
            status: summaryResponse.status(),
            payload: summaryPayload,
          }
        },
        (result) => (
          result.status === 200
          && typeof result.payload?.totalOrders === 'number'
        ),
        30_000,
      )

      await waitWithinRemainingBudget(
        'authenticated runtime',
        async (requestTimeoutMs) => {
          const runtimeResponse = await proofApi.get('/api/system/runtime', { timeout: requestTimeoutMs })
          const runtimePayload = await runtimeResponse.json().catch(() => null)
          return {
            detail: `runtime=${runtimeResponse.status()} readiness=${runtimePayload?.readinessState}`,
            status: runtimeResponse.status(),
            payload: runtimePayload,
          }
        },
        (result) => (
          result.status === 200
          && typeof result.payload?.readinessState === 'string'
        ),
        30_000,
      )

      await waitForProbe(
        'authenticated dashboard snapshot',
        async () => {
          const phaseRemainingMs = Math.max(15_000, authenticatedWarmupTimeoutMs - (Date.now() - startedAt))
          const requestTimeoutMs = Math.max(20_000, Math.min(60_000, phaseRemainingMs - 5_000))
          const snapshotResponse = await proofApi.get('/api/dashboard/snapshot', { timeout: requestTimeoutMs })
          const snapshotPayload = await snapshotResponse.json().catch(() => null)

          return {
            detail: `snapshot=${snapshotResponse.status()} inventory=${Array.isArray(snapshotPayload?.inventory)} generatedAt=${snapshotPayload?.generatedAt || ''}`.trim(),
            snapshotStatus: snapshotResponse.status(),
            snapshotPayload,
          }
        },
        (result) => (
          result.snapshotStatus === 200
          && Array.isArray(result.snapshotPayload?.inventory)
        ),
        remainingWarmupMs,
      )

      await proofApi.post('/api/auth/session/logout').catch(() => null)
      await proofApi.dispose()
      return
    } catch (error) {
      lastError = error instanceof Error ? error : new Error(String(error))
      await proofApi.dispose()
      const remainingMs = authenticatedWarmupTimeoutMs - (Date.now() - startedAt)
      if (remainingMs <= 0) {
        break
      }

      console.log(`[hosted-proof] authenticated warm-up attempt ${attempt} did not settle yet; retrying shortly`)
      await delay(Math.min(10_000, remainingMs))
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
          detail: `HTTP ${response.status()} websocket=${payload?.websocket} cookie_needed=${payload?.cookie_needed}`,
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
