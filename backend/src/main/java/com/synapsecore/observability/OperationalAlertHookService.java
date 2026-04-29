package com.synapsecore.observability;

import com.synapsecore.audit.RequestTraceContext;
import com.synapsecore.config.SynapseObservabilityProperties;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
@Slf4j
public class OperationalAlertHookService {

    private final RestClient.Builder restClientBuilder;
    private final SynapseObservabilityProperties observabilityProperties;
    private final RequestTraceContext requestTraceContext;
    private final OperationalMetricsService operationalMetricsService;

    public boolean isConfigured() {
        return observabilityProperties.getAlertHook().isConfigured();
    }

    public void emit(String alertType, String severity, String summary, String detail) {
        String tenantCode = requestTraceContext.getCurrentTenantOrDefault();
        if (!isConfigured()) {
            return;
        }
        try {
            restClientBuilder.build()
                .post()
                .uri(observabilityProperties.getAlertHook().getWebhookUrl().trim())
                .contentType(MediaType.APPLICATION_JSON)
                .body(new AlertHookPayload(
                    alertType,
                    severity,
                    summary,
                    detail,
                    requestTraceContext.getRequiredRequestId(),
                    requestTraceContext.getCurrentActorOrAnonymous(),
                    tenantCode,
                    Instant.now()
                ))
                .retrieve()
                .toBodilessEntity();
            operationalMetricsService.recordAlertHookDelivery(tenantCode, true);
        } catch (RuntimeException exception) {
            operationalMetricsService.recordAlertHookDelivery(tenantCode, false);
            log.warn("Operational alert hook delivery failed for {} [{}]: {}", alertType, requestTraceContext.getRequiredRequestId(), exception.getMessage());
        }
    }

    private record AlertHookPayload(
        String alertType,
        String severity,
        String summary,
        String detail,
        String requestId,
        String actor,
        String tenantCode,
        Instant observedAt
    ) {
    }
}
