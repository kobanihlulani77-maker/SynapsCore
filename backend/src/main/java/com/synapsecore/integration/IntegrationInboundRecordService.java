package com.synapsecore.integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapsecore.audit.RequestTraceContext;
import com.synapsecore.domain.entity.IntegrationConnectorType;
import com.synapsecore.domain.entity.IntegrationInboundRecord;
import com.synapsecore.domain.entity.IntegrationInboundStatus;
import com.synapsecore.domain.repository.IntegrationInboundRecordRepository;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class IntegrationInboundRecordService {

    private final IntegrationInboundRecordRepository integrationInboundRecordRepository;
    private final RequestTraceContext requestTraceContext;
    private final ObjectMapper objectMapper;

    @Transactional
    public IntegrationInboundRecord recordReceived(String tenantCode,
                                                  String sourceSystem,
                                                  IntegrationConnectorType connectorType,
                                                  String fileName,
                                                  String externalOrderId,
                                                  String warehouseCode,
                                                  Object requestPayload) {
        return integrationInboundRecordRepository.save(IntegrationInboundRecord.builder()
            .tenantCode(tenantCode)
            .sourceSystem(sourceSystem)
            .connectorType(connectorType)
            .fileName(limit(fileName, 160))
            .externalOrderId(limit(externalOrderId, 80))
            .warehouseCode(limit(warehouseCode, 40))
            .requestId(requestTraceContext.getRequiredRequestId())
            .status(IntegrationInboundStatus.RECEIVED)
            .requestPayload(serializePayload(requestPayload))
            .build());
    }

    @Transactional
    public void markAccepted(Long inboundRecordId, String ingestionSource) {
        update(inboundRecordId, record -> {
            record.setStatus(IntegrationInboundStatus.ACCEPTED);
            record.setIngestionSource(limit(ingestionSource, 120));
            record.setFailureMessage(null);
            record.setFailureCode(null);
        });
    }

    @Transactional
    public void markRejected(Long inboundRecordId, com.synapsecore.integration.IntegrationFailureCode failureCode, String failureMessage) {
        update(inboundRecordId, record -> {
            record.setStatus(IntegrationInboundStatus.REJECTED);
            record.setFailureMessage(limit(failureMessage, 320));
            record.setFailureCode(failureCode);
        });
    }

    @Transactional
    public void markReplayQueued(Long inboundRecordId,
                                 Long replayRecordId,
                                 com.synapsecore.integration.IntegrationFailureCode failureCode,
                                 String failureMessage) {
        update(inboundRecordId, record -> {
            record.setStatus(IntegrationInboundStatus.REPLAY_QUEUED);
            record.setReplayRecordId(replayRecordId);
            record.setFailureMessage(limit(failureMessage, 320));
            record.setFailureCode(failureCode);
        });
    }

    @Transactional
    public void markReplayed(Long inboundRecordId, String replayedOrderExternalId) {
        update(inboundRecordId, record -> {
            record.setStatus(IntegrationInboundStatus.REPLAYED);
            record.setReplayedOrderExternalId(limit(replayedOrderExternalId, 80));
            record.setFailureMessage(null);
            record.setFailureCode(null);
        });
    }

    private void update(Long inboundRecordId, java.util.function.Consumer<IntegrationInboundRecord> updater) {
        if (inboundRecordId == null) {
            return;
        }
        IntegrationInboundRecord record = integrationInboundRecordRepository.findById(inboundRecordId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Integration inbound record not found: " + inboundRecordId));
        updater.accept(record);
        integrationInboundRecordRepository.save(record);
    }

    private String serializePayload(Object requestPayload) {
        try {
            if (requestPayload instanceof String rawPayload) {
                return rawPayload;
            }
            return objectMapper.writeValueAsString(requestPayload instanceof Map<?, ?> map ? map : requestPayload);
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Integration inbound payload could not be stored.", exception);
        }
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, Math.max(0, maxLength - 3)) + "...";
    }
}
