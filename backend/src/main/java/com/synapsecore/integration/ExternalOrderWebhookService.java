package com.synapsecore.integration;

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

    public ExternalOrderWebhookResponse ingest(ExternalOrderWebhookRequest request) {
        String sourceSystem = request.sourceSystem().trim().toLowerCase(Locale.ROOT);
        OrderCreateRequest mappedRequest = null;

        try {
            var connector = integrationConnectorService.requireEnabledConnector(
                sourceSystem,
                IntegrationConnectorType.WEBHOOK_ORDER,
                "accept webhook orders");
            sourceSystem = connector.getSourceSystem();
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
            integrationImportRunService.recordRun(
                sourceSystem,
                IntegrationConnectorType.WEBHOOK_ORDER,
                null,
                1,
                0,
                1,
                "Rejected webhook order " + (mappedRequest == null ? request.externalOrderId().trim() : mappedRequest.externalOrderId()) + " from " + sourceSystem
                    + ". Reason: " + exception.getReason()
            );
            integrationReplayService.recordFailure(
                sourceSystem,
                IntegrationConnectorType.WEBHOOK_ORDER,
                mappedRequest == null
                    ? new OrderCreateRequest(
                        request.externalOrderId().trim(),
                        request.warehouseCode() == null ? "" : request.warehouseCode().trim(),
                        mapItems(request)
                    )
                    : mappedRequest,
                exception.getReason()
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
