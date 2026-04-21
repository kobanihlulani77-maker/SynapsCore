package com.synapsecore.integration;

import com.synapsecore.audit.RequestTraceContext;
import com.synapsecore.domain.dto.OrderCreateRequest;
import com.synapsecore.domain.dto.OrderItemRequest;
import com.synapsecore.domain.dto.OrderResponse;
import com.synapsecore.domain.entity.IntegrationConnectorType;
import com.synapsecore.integration.IntegrationConnectorPolicyService.PreparedConnectorOrder;
import com.synapsecore.integration.dto.ExternalOrderWebhookRequest;
import com.synapsecore.integration.dto.ExternalOrderWebhookResponse;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ExternalOrderWebhookService {

    private final com.synapsecore.domain.service.OrderService orderService;
    private final IntegrationConnectorService integrationConnectorService;
    private final IntegrationConnectorPolicyService integrationConnectorPolicyService;
    private final IntegrationImportRunService integrationImportRunService;
    private final IntegrationReplayService integrationReplayService;
    private final IntegrationInboundRecordService integrationInboundRecordService;
    private final RequestTraceContext requestTraceContext;

    public ExternalOrderWebhookResponse ingest(ExternalOrderWebhookRequest request) {
        return ingest(request, null);
    }

    public ExternalOrderWebhookResponse ingest(ExternalOrderWebhookRequest request,
                                               com.synapsecore.domain.entity.IntegrationConnector authenticatedConnector) {
        String sourceSystem = request.sourceSystem().trim().toLowerCase(Locale.ROOT);
        OrderCreateRequest mappedRequest = null;
        Long inboundRecordId = null;
        String tenantCode = requestTraceContext.getCurrentTenant()
            .filter(currentTenant -> !RequestTraceContext.MISSING_TENANT_CONTEXT.equalsIgnoreCase(currentTenant))
            .orElse(authenticatedConnector != null ? authenticatedConnector.getTenant().getCode() : null);

        try {
            var connector = authenticatedConnector != null
                ? authenticatedConnector
                : integrationConnectorService.requireEnabledConnector(
                    sourceSystem,
                    IntegrationConnectorType.WEBHOOK_ORDER,
                    "accept webhook orders");
            tenantCode = connector.getTenant().getCode();
            sourceSystem = connector.getSourceSystem();
            inboundRecordId = integrationInboundRecordService.recordReceived(
                tenantCode,
                sourceSystem,
                IntegrationConnectorType.WEBHOOK_ORDER,
                null,
                request.externalOrderId() == null ? null : request.externalOrderId().trim(),
                request.warehouseCode() == null ? null : request.warehouseCode().trim(),
                request
            ).getId();
            PreparedConnectorOrder preparedOrder = integrationConnectorPolicyService.prepareOrder(
                connector,
                request.externalOrderId(),
                request.warehouseCode(),
                request.customerReference(),
                request.occurredAt(),
                mapItems(request)
            );
            mappedRequest = preparedOrder.orderRequest();
            String ingestionSource = "integration-webhook:" + sourceSystem.toLowerCase(Locale.ROOT);

            OrderResponse order = orderService.createOrder(mappedRequest, ingestionSource);
            integrationInboundRecordService.markAccepted(inboundRecordId, ingestionSource);
            integrationImportRunService.recordRun(
                sourceSystem,
                IntegrationConnectorType.WEBHOOK_ORDER,
                null,
                1,
                1,
                0,
                "Accepted webhook order " + order.externalOrderId() + " from " + sourceSystem + "."
            );

            return new ExternalOrderWebhookResponse(
                sourceSystem,
                ingestionSource,
                Instant.now(),
                order
            );
        } catch (org.springframework.web.server.ResponseStatusException exception) {
            var failure = IntegrationFailureCodes.extract(exception);
            integrationInboundRecordService.markRejected(inboundRecordId, failure.failureCode(), failure.failureMessage());
            integrationImportRunService.recordRun(
                sourceSystem,
                IntegrationConnectorType.WEBHOOK_ORDER,
                null,
                1,
                0,
                1,
                "Rejected webhook order " + (mappedRequest == null ? request.externalOrderId().trim() : mappedRequest.externalOrderId()) + " from " + sourceSystem
                    + ". Reason: " + failure.failureMessage()
            );
            integrationReplayService.recordFailure(
                tenantCode,
                sourceSystem,
                IntegrationConnectorType.WEBHOOK_ORDER,
                mappedRequest == null
                    ? new OrderCreateRequest(
                        request.externalOrderId().trim(),
                        request.warehouseCode() == null ? "" : request.warehouseCode().trim(),
                        mapItems(request)
                    )
                    : mappedRequest,
                failure.failureCode(),
                failure.failureMessage(),
                inboundRecordId
            );
            throw exception;
        }
    }

    private List<OrderItemRequest> mapItems(ExternalOrderWebhookRequest request) {
        return request.items().stream()
            .map(item -> new OrderItemRequest(
                item.productSku().trim(),
                item.quantity(),
                item.unitPrice()
            ))
            .toList();
    }
}
