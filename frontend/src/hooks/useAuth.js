import { useState } from 'react'

export default function useAuth({
  defaultTenantCode,
  defaultUsername,
  rememberedWorkspacePreference,
  createDefaultPasswordChangeForm,
}) {
  const [rememberWorkspace, setRememberWorkspace] = useState(() => rememberedWorkspacePreference?.remember ?? true)
  const [authSessionState, setAuthSessionState] = useState({
    loading: true,
    error: '',
    session: null,
    tenantCode: defaultTenantCode,
    username: defaultUsername,
    password: '',
    action: '',
  })
  const [passwordChangeState, setPasswordChangeState] = useState({
    loading: false,
    error: '',
    success: '',
    form: createDefaultPasswordChangeForm(),
  })

  return {
    rememberWorkspace,
    setRememberWorkspace,
    authSessionState,
    setAuthSessionState,
    passwordChangeState,
    setPasswordChangeState,
  }
}
