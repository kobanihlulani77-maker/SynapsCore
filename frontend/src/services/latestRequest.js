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

export const createSingleFlightRequest = () => {
  let inFlight = null

  return {
    run(key, factory) {
      if (inFlight?.key === key) {
        return inFlight.promise
      }

      const promise = Promise.resolve().then(factory)
      inFlight = { key, promise }
      promise.then(
        () => {
          if (inFlight?.promise === promise) {
            inFlight = null
          }
        },
        () => {
          if (inFlight?.promise === promise) {
            inFlight = null
          }
        },
      )
      return promise
    },
  }
}
