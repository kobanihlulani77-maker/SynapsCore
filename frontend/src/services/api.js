const runtimeConfig = globalThis.__SYNAPSE_RUNTIME_CONFIG__ || {}

export const workspacePreferenceStorageKey = 'synapsecore.workspacePreference'
export const postAuthRedirectStorageKey = 'synapsecore.postAuthRedirect'

export const trimTrailingSlash = (value = '') => value.replace(/\/+$/, '')
export const isLocalHostname = (hostname = '') => ['localhost', '127.0.0.1', '0.0.0.0'].includes((hostname || '').toLowerCase())

export const normalizeAbsoluteUrl = (value) => {
  if (!value || !String(value).trim()) return ''
  try {
    return trimTrailingSlash(new URL(String(value).trim(), globalThis.location?.origin || undefined).toString())
  } catch {
    return trimTrailingSlash(String(value).trim())
  }
}

export const resolveApiBaseUrl = () => {
  const configuredUrl = runtimeConfig.apiUrl || import.meta.env.VITE_API_URL
  if (configuredUrl) return normalizeAbsoluteUrl(configuredUrl)
  const browserOrigin = globalThis.location?.origin || ''
  return browserOrigin && !isLocalHostname(globalThis.location?.hostname || '') ? trimTrailingSlash(browserOrigin) : 'http://localhost:8080'
}

export const resolveRealtimeUrl = (configuredUrl, apiBaseUrl, preferredProtocol) => {
  const rawValue = configuredUrl || (apiBaseUrl ? `${apiBaseUrl}/ws` : '')
  if (!rawValue || !String(rawValue).trim()) return ''

  try {
    const normalizedUrl = new URL(rawValue, globalThis.location?.origin || undefined)
    if (preferredProtocol === 'ws') {
      if (normalizedUrl.protocol === 'http:') normalizedUrl.protocol = 'ws:'
      if (normalizedUrl.protocol === 'https:') normalizedUrl.protocol = 'wss:'
    } else {
      if (normalizedUrl.protocol === 'ws:') normalizedUrl.protocol = 'http:'
      if (normalizedUrl.protocol === 'wss:') normalizedUrl.protocol = 'https:'
    }
    return trimTrailingSlash(normalizedUrl.toString())
  } catch {
    const nextValue = trimTrailingSlash(String(rawValue).trim())
    if (preferredProtocol === 'ws') {
      return nextValue
        .replace(/^http:/i, 'ws:')
        .replace(/^https:/i, 'wss:')
    }
    return nextValue
      .replace(/^ws:/i, 'http:')
      .replace(/^wss:/i, 'https:')
  }
}

export const readStoredJson = (storage, key, fallbackValue) => {
  try {
    const rawValue = storage?.getItem?.(key)
    return rawValue ? JSON.parse(rawValue) : fallbackValue
  } catch {
    return fallbackValue
  }
}

export const writeStoredJson = (storage, key, value) => {
  try {
    storage?.setItem?.(key, JSON.stringify(value))
  } catch {
    // Storage can be unavailable in embedded browsers.
  }
}

export const removeStoredValue = (storage, key) => {
  try {
    storage?.removeItem?.(key)
  } catch {
    // Ignore cleanup issues so auth flow remains usable.
  }
}

export const rememberedWorkspacePreference = readStoredJson(globalThis.localStorage, workspacePreferenceStorageKey, null)
export const configuredRealtimeUrl = runtimeConfig.wsUrl || import.meta.env.VITE_WS_URL || ''
export const apiUrl = resolveApiBaseUrl()
export const websocketBrokerUrl = resolveRealtimeUrl(configuredRealtimeUrl, apiUrl, 'ws')
export const sockJsUrl = resolveRealtimeUrl(configuredRealtimeUrl, apiUrl, 'http')
export const wsUrl = websocketBrokerUrl || sockJsUrl
export const frontendBuildVersion = runtimeConfig.appBuildVersion || import.meta.env.VITE_APP_BUILD_VERSION || 'local-dev'
export const frontendBuildCommit = runtimeConfig.appBuildCommit || import.meta.env.VITE_APP_BUILD_COMMIT || 'local-dev'
export const frontendBuildTime = runtimeConfig.appBuildTime || import.meta.env.VITE_APP_BUILD_TIME || 'untracked'

export const readResponsePayload = async (response) => {
  const contentType = response.headers.get('content-type') || ''
  if (contentType.includes('application/json')) {
    try {
      return await response.json()
    } catch {
      return null
    }
  }

  try {
    const text = await response.text()
    return text ? { message: text } : null
  } catch {
    return null
  }
}

export const extractResponseErrorMessage = (response, payload, fallbackMessage) => {
  if (typeof payload === 'string' && payload.trim()) return payload.trim()
  if (payload && typeof payload === 'object') {
    if (typeof payload.message === 'string' && payload.message.trim()) return payload.message.trim()
    if (typeof payload.detail === 'string' && payload.detail.trim()) return payload.detail.trim()
    if (typeof payload.error === 'string' && payload.error.trim()) return payload.error.trim()
    if (typeof payload.title === 'string' && payload.title.trim()) return payload.title.trim()
  }

  if (response.status === 401) return 'Your session is missing or expired. Sign in again to reopen the workspace.'
  if (response.status === 403) return 'This operator does not have permission to perform that action.'
  return fallbackMessage
}

export const createApiRequestHelpers = ({ authSession, currentPage, onUnauthorized }) => {
  const fetchApi = (path, init = {}) => {
    const headers = new Headers(init.headers || {})
    if (authSession?.tenantCode) {
      headers.set('X-Synapse-Tenant', authSession.tenantCode)
    }
    return fetch(`${apiUrl}${path}`, { credentials: 'include', ...init, headers })
  }

  const fetchJson = async (path, init = {}, options = {}) => {
    let response
    try {
      response = await fetchApi(path, init)
    } catch {
      throw new Error(`Unable to reach the SynapseCore backend at ${apiUrl}. Check the live API URL, CORS policy, and backend availability.`)
    }

    const payload = await readResponsePayload(response)
    if (!response.ok) {
      const message = extractResponseErrorMessage(response, payload, `Request to ${path} failed.`)
      if (response.status === 401 && !options.ignoreUnauthorized) {
        onUnauthorized?.(message, currentPage)
      }
      throw new Error(message)
    }

    return payload
  }

  return { fetchApi, fetchJson }
}
