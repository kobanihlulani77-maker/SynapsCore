export const createLatestRequestGate = () => {
  let latestRequestId = 0

  return {
    begin() {
      latestRequestId += 1
      return latestRequestId
    },
    isCurrent(requestId) {
      return requestId === latestRequestId
    },
    invalidate() {
      latestRequestId += 1
      return latestRequestId
    },
  }
}
