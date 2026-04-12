import {
  apiUrl,
  createApiRequestHelpers,
  frontendBuildCommit,
  frontendBuildTime,
  frontendBuildVersion,
  sockJsUrl,
  websocketBrokerUrl,
  wsUrl,
} from '../services/api'

export default function useApi({ authSession, currentPage, onUnauthorized }) {
  const helpers = createApiRequestHelpers({ authSession, currentPage, onUnauthorized })

  return {
    apiUrl,
    wsUrl,
    sockJsUrl,
    websocketBrokerUrl,
    frontendBuildVersion,
    frontendBuildCommit,
    frontendBuildTime,
    ...helpers,
  }
}
