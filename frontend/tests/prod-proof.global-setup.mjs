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
  } finally {
    await Promise.allSettled([backend.dispose(), frontend.dispose()])
  }
}
