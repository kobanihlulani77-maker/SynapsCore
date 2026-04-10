package com.synapsecore.scenario;

import com.synapsecore.domain.dto.OrderCreateRequest;
import com.synapsecore.domain.dto.OrderResponse;
import com.synapsecore.domain.entity.BusinessEventType;
import com.synapsecore.domain.entity.ScenarioRun;
import com.synapsecore.event.BusinessEventService;
import com.synapsecore.domain.service.OrderService;
import com.synapsecore.scenario.dto.ScenarioExecutionResponse;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ScenarioExecutionService {

    private final ScenarioHistoryService scenarioHistoryService;
    private final OrderService orderService;
    private final BusinessEventService businessEventService;

    @Transactional
    public ScenarioExecutionResponse execute(long scenarioRunId) {
        ScenarioRun scenarioRun = scenarioHistoryService.getScenarioRun(scenarioRunId);
        OrderCreateRequest request = scenarioHistoryService.getExecutableOrderRequest(scenarioRun);
        OrderResponse order = orderService.createOrder(request, "scenario-execution:" + scenarioRunId);
        scenarioHistoryService.recordExecution(scenarioRun, order);
        businessEventService.record(
            BusinessEventType.SCENARIO_EXECUTED,
            "scenario-planner",
            "Executed scenario " + scenarioRun.getId() + " into live order " + order.externalOrderId()
                + " for warehouse " + order.warehouseCode() + "."
        );

        return new ScenarioExecutionResponse(
            scenarioRun.getId(),
            scenarioRun.getTitle(),
            order,
            Instant.now()
        );
    }
}
