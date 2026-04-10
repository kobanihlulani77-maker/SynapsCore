package com.synapsecore.audit;

import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class RequestTraceContext {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String ANONYMOUS_ACTOR = "anonymous";
    public static final String DEFAULT_TENANT = "unscoped";

    private final ThreadLocal<String> currentRequestId = new ThreadLocal<>();
    private final ThreadLocal<String> currentActor = new ThreadLocal<>();
    private final ThreadLocal<String> currentTenant = new ThreadLocal<>();

    public void setCurrentRequestId(String requestId) {
        currentRequestId.set(requestId);
    }

    public void setCurrentActor(String actorName) {
        currentActor.set(actorName);
    }

    public void setCurrentTenant(String tenantCode) {
        currentTenant.set(tenantCode);
    }

    public Optional<String> getCurrentRequestId() {
        return Optional.ofNullable(currentRequestId.get());
    }

    public Optional<String> getCurrentActor() {
        return Optional.ofNullable(currentActor.get());
    }

    public Optional<String> getCurrentTenant() {
        return Optional.ofNullable(currentTenant.get());
    }

    public String getRequiredRequestId() {
        return getCurrentRequestId().orElse("system-no-request");
    }

    public String getCurrentActorOrAnonymous() {
        return getCurrentActor().orElse(ANONYMOUS_ACTOR);
    }

    public String getCurrentTenantOrDefault() {
        return getCurrentTenant().orElse(DEFAULT_TENANT);
    }

    public void clear() {
        currentRequestId.remove();
        currentActor.remove();
        currentTenant.remove();
    }
}
