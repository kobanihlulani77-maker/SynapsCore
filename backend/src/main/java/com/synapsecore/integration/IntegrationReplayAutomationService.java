package com.synapsecore.integration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class IntegrationReplayAutomationService {

    private final IntegrationReplayService integrationReplayService;

    @Value("${synapsecore.integration.replay.automation.enabled:true}")
    private boolean automationEnabled;

    @Value("${synapsecore.integration.replay.automation.batch-size:10}")
    private int automationBatchSize;

    @Scheduled(fixedDelayString = "${synapsecore.integration.replay.automation.interval-ms:30000}")
    public void processAutomatedReplay() {
        if (!automationEnabled) {
            return;
        }

        int processed = integrationReplayService.processAutomatedReplayBatch(automationBatchSize);
        if (processed > 0) {
            log.info("Processed {} automated integration replay attempt(s).", processed);
        }
    }
}
