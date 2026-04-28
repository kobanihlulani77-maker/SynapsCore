export default function useWorkspaceSessionActions({
  authSessionState,
  setAuthSessionState,
  passwordChangeState,
  setPasswordChangeState,
  tenantOnboardingForm,
  setTenantOnboardingForm,
  setTenantOnboardingState,
  setTenantDirectoryState,
  rememberWorkspace,
  fetchJson,
  navigateToPage,
  resetSignedInWorkspace,
  defaultTenantOnboardingForm,
  createDefaultPasswordChangeForm,
  workspacePreferenceStorageKey,
  writeStoredJson,
  removeStoredValue,
  readPendingPostAuthPage,
  clearPendingPostAuthPage,
}) {
  function handleSignInSubmit(event) {
    event.preventDefault()
    const signInBusy = authSessionState.loading && authSessionState.action === 'signin'
    if (!signInBusy && authSessionState.tenantCode.trim() && authSessionState.username.trim() && authSessionState.password.trim()) {
      signInOperator()
    }
  }

  async function signInOperator() {
    setAuthSessionState((current) => ({ ...current, loading: true, error: '', action: 'signin' }))
    setPasswordChangeState((current) => ({ ...current, error: '', success: '' }))
    try {
      const payload = await fetchJson('/api/auth/session/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          tenantCode: authSessionState.tenantCode.trim(),
          username: authSessionState.username.trim(),
          password: authSessionState.password,
        }),
      }, { ignoreUnauthorized: true })

      if (rememberWorkspace) {
        writeStoredJson(globalThis.localStorage, workspacePreferenceStorageKey, {
          remember: true,
          tenantCode: payload.tenantCode || authSessionState.tenantCode.trim(),
          username: payload.username || authSessionState.username.trim(),
        })
      } else {
        removeStoredValue(globalThis.localStorage, workspacePreferenceStorageKey)
      }

      setAuthSessionState((current) => ({
        ...current,
        loading: false,
        error: '',
        action: '',
        session: payload,
        tenantCode: payload.tenantCode || current.tenantCode,
        username: payload.username,
        password: '',
      }))
      setPasswordChangeState({ loading: false, error: '', success: '', form: createDefaultPasswordChangeForm() })
      const nextPage = readPendingPostAuthPage() || 'dashboard'
      clearPendingPostAuthPage()
      navigateToPage(nextPage)
    } catch (error) {
      setAuthSessionState((current) => ({ ...current, loading: false, error: error.message, action: '' }))
    }
  }

  async function signOutOperator() {
    setAuthSessionState((current) => ({ ...current, loading: true, error: '', action: 'signout' }))
    setPasswordChangeState((current) => ({ ...current, loading: false, error: '', success: '', form: createDefaultPasswordChangeForm() }))
    try {
      const payload = await fetchJson('/api/auth/session/logout', { method: 'POST' })
      setAuthSessionState((current) => ({
        ...current,
        loading: false,
        error: '',
        action: '',
        session: payload.signedIn ? payload : null,
        tenantCode: payload.signedIn ? payload.tenantCode || current.tenantCode : '',
        username: payload.signedIn ? current.username : '',
        password: '',
      }))
      resetSignedInWorkspace()
      clearPendingPostAuthPage()
      navigateToPage('sign-in')
    } catch (error) {
      setAuthSessionState((current) => ({ ...current, loading: false, error: error.message, action: '' }))
    }
  }

  async function changeSignedInPassword() {
    setPasswordChangeState((current) => ({ ...current, loading: true, error: '', success: '' }))
    try {
      const payload = await fetchJson('/api/auth/session/password', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          currentPassword: passwordChangeState.form.currentPassword,
          newPassword: passwordChangeState.form.newPassword,
        }),
      })

      setAuthSessionState((current) => ({ ...current, session: payload, password: '' }))
      setPasswordChangeState({
        loading: false,
        error: '',
        success: `Password updated for ${payload.username}.`,
        form: createDefaultPasswordChangeForm(),
      })
    } catch (error) {
      setPasswordChangeState((current) => ({ ...current, loading: false, error: error.message, success: '' }))
    }
  }

  async function onboardTenant() {
    setTenantOnboardingState({ loading: true, error: '', success: '', result: null })
    try {
      const payload = await fetchJson('/api/access/tenants', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          tenantCode: tenantOnboardingForm.tenantCode.trim(),
          tenantName: tenantOnboardingForm.tenantName.trim(),
          description: tenantOnboardingForm.description.trim(),
          adminFullName: tenantOnboardingForm.adminFullName.trim(),
          adminUsername: tenantOnboardingForm.adminUsername.trim(),
          adminPassword: tenantOnboardingForm.adminPassword,
          primaryLocation: tenantOnboardingForm.primaryLocation.trim(),
          secondaryLocation: tenantOnboardingForm.secondaryLocation.trim(),
        }),
      })

      const tenants = await fetchJson('/api/access/tenants')
      setTenantDirectoryState({ loading: false, error: '', items: tenants })
      setTenantOnboardingState({
        loading: false,
        error: '',
        success: `${payload.tenantName} is ready. Bootstrap admin ${payload.adminUsername} and executive approver ${payload.executiveUsername} were created. Reset the executive password from Users before first use.`,
        result: payload,
      })
      setAuthSessionState((current) => ({
        ...current,
        tenantCode: payload.tenantCode,
        username: payload.adminUsername,
        password: tenantOnboardingForm.adminPassword,
      }))
      setTenantOnboardingForm(defaultTenantOnboardingForm)
    } catch (error) {
      setTenantOnboardingState({ loading: false, error: error.message, success: '', result: null })
    }
  }

  return {
    handleSignInSubmit,
    signInOperator,
    signOutOperator,
    changeSignedInPassword,
    onboardTenant,
  }
}
