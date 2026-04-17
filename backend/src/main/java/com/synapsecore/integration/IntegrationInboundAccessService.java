package com.synapsecore.integration;

import com.synapsecore.audit.RequestTraceContext;
import com.synapsecore.domain.entity.IntegrationConnector;
import com.synapsecore.domain.entity.IntegrationConnectorType;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class IntegrationInboundAccessService {

    public static final String CONNECTOR_TOKEN_HEADER = "X-Synapse-Connector-Token";

    private static final String ACTOR_MDC_KEY = "actor";
    private static final String TENANT_MDC_KEY = "tenant";

    private final IntegrationConnectorService integrationConnectorService;
    private final RequestTraceContext requestTraceContext;

    public boolean hasConnectorToken(HttpServletRequest request) {
        String rawToken = request.getHeader(CONNECTOR_TOKEN_HEADER);
        return rawToken != null && !rawToken.isBlank();
    }

    public Optional<IntegrationConnector> authenticateOptional(HttpServletRequest request,
                                                               String sourceSystem,
                                                               IntegrationConnectorType connectorType,
                                                               String actionDescription) {
        String rawToken = request.getHeader(CONNECTOR_TOKEN_HEADER);
        if (rawToken == null) {
            return Optional.empty();
        }
        if (rawToken.isBlank()) {
            throw IntegrationFailureCodes.status(HttpStatus.UNAUTHORIZED,
                IntegrationFailureCode.INVALID_CONNECTOR_TOKEN,
                "A non-empty connector token is required for connector-authenticated ingestion.");
        }

        IntegrationConnector connector = integrationConnectorService.requireEnabledConnectorByInboundToken(
            sourceSystem,
            connectorType,
            rawToken.trim(),
            actionDescription
        );
        applyConnectorTrace(connector);
        return Optional.of(connector);
    }

    private void applyConnectorTrace(IntegrationConnector connector) {
        String tenantCode = connector.getTenant() == null ? null : connector.getTenant().getCode();
        if (tenantCode == null || tenantCode.isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Connector " + connector.getSourceSystem() + " is missing tenant ownership.");
        }
        String actorName = "connector:" + connector.getSourceSystem();
        requestTraceContext.setCurrentTenant(tenantCode);
        requestTraceContext.setCurrentActor(actorName);
        MDC.put(TENANT_MDC_KEY, tenantCode);
        MDC.put(ACTOR_MDC_KEY, actorName);
    }
}
