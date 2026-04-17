package com.synapsecore.integration;

import com.synapsecore.access.AccessControlService;
import com.synapsecore.integration.dto.IntegrationConnectorRequest;
import com.synapsecore.integration.dto.IntegrationConnectorResponse;
import com.synapsecore.integration.dto.IntegrationImportRunResponse;
import com.synapsecore.integration.dto.IntegrationReplayRecordResponse;
import com.synapsecore.integration.dto.IntegrationReplayResultResponse;
import com.synapsecore.integration.dto.ExternalOrderWebhookRequest;
import com.synapsecore.integration.dto.ExternalOrderWebhookResponse;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.synapsecore.integration.dto.ExternalOrderCsvImportResponse;

@RestController
@RequestMapping("/api/integrations/orders")
@RequiredArgsConstructor
public class ExternalOrderWebhookController {

    private final ExternalOrderWebhookService externalOrderWebhookService;
    private final ExternalOrderCsvImportService externalOrderCsvImportService;
    private final IntegrationConnectorService integrationConnectorService;
    private final IntegrationImportRunService integrationImportRunService;
    private final IntegrationReplayService integrationReplayService;
    private final AccessControlService accessControlService;
    private final IntegrationInboundAccessService integrationInboundAccessService;

    @PostMapping("/webhook")
    @ResponseStatus(HttpStatus.CREATED)
    public ExternalOrderWebhookResponse ingestOrderWebhook(@Valid @RequestBody ExternalOrderWebhookRequest request,
                                                           HttpServletRequest httpRequest) {
        var authenticatedConnector = integrationInboundAccessService.authenticateOptional(
            httpRequest,
            request.sourceSystem(),
            com.synapsecore.domain.entity.IntegrationConnectorType.WEBHOOK_ORDER,
            "accept webhook orders"
        );
        if (authenticatedConnector.isEmpty()) {
            accessControlService.requireWorkspaceAccess("ingest webhook orders");
        }
        return externalOrderWebhookService.ingest(request, authenticatedConnector.orElse(null));
    }

    @PostMapping(value = "/csv-import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public ExternalOrderCsvImportResponse importOrdersFromCsv(@RequestParam("file") MultipartFile file,
                                                              @RequestParam(required = false) String sourceSystem,
                                                              HttpServletRequest httpRequest) {
        if (integrationInboundAccessService.hasConnectorToken(httpRequest)
            && (sourceSystem == null || sourceSystem.isBlank())) {
            throw IntegrationFailureCodes.badRequest(
                IntegrationFailureCode.INVALID_SOURCE_SYSTEM,
                "sourceSystem request parameter is required for connector-authenticated CSV imports."
            );
        }
        var authenticatedConnector = integrationInboundAccessService.authenticateOptional(
            httpRequest,
            sourceSystem,
            com.synapsecore.domain.entity.IntegrationConnectorType.CSV_ORDER_IMPORT,
            "accept CSV imports"
        );
        if (authenticatedConnector.isEmpty()) {
            accessControlService.requireWorkspaceAccess("import CSV orders");
        }
        return externalOrderCsvImportService.ingest(file, sourceSystem, authenticatedConnector.orElse(null));
    }

    @GetMapping("/connectors")
    public List<IntegrationConnectorResponse> getConnectors() {
        accessControlService.requireWorkspaceAccess("view integration connectors");
        return integrationConnectorService.getConnectors();
    }

    @PostMapping("/connectors")
    @ResponseStatus(HttpStatus.OK)
    public IntegrationConnectorResponse saveConnector(@Valid @RequestBody IntegrationConnectorRequest request) {
        return integrationConnectorService.upsertConnector(
            request,
            accessControlService.requireIntegrationAdmin("manage integration connectors").actorName()
        );
    }

    @GetMapping("/imports/recent")
    public List<IntegrationImportRunResponse> getRecentImportRuns() {
        accessControlService.requireWorkspaceAccess("view recent integration imports");
        return integrationImportRunService.getRecentRuns();
    }

    @GetMapping("/replay-queue")
    public List<IntegrationReplayRecordResponse> getReplayQueue() {
        accessControlService.requireWorkspaceAccess("view integration replay queue");
        return integrationReplayService.getReplayQueue();
    }

    @PostMapping("/replay/{replayRecordId}")
    @ResponseStatus(HttpStatus.OK)
    public IntegrationReplayResultResponse replayFailedOrder(@org.springframework.web.bind.annotation.PathVariable Long replayRecordId) {
        return integrationReplayService.replay(
            replayRecordId,
            accessControlService.requireIntegrationOperator("replay failed inbound orders").actorName()
        );
    }
}
