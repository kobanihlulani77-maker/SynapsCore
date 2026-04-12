export const fetchCurrentSession = (fetchJson) => fetchJson('/api/auth/session')

export const signInOperatorRequest = (fetchJson, credentials) => fetchJson('/api/auth/session/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify(credentials),
}, { ignoreUnauthorized: true })

export const signOutOperatorRequest = (fetchJson) => fetchJson('/api/auth/session/logout', { method: 'POST' })

export const changePasswordRequest = (fetchJson, payload) => fetchJson('/api/auth/session/password', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify(payload),
})
