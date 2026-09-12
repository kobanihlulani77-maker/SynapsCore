package com.synapsecore;

import static org.assertj.core.api.Assertions.assertThat;

import com.synapsecore.audit.RequestTraceContext;
import com.synapsecore.domain.entity.CustomerOrder;
import com.synapsecore.domain.entity.FulfillmentStatus;
import com.synapsecore.domain.entity.FulfillmentTask;
import com.synapsecore.domain.entity.OrderStatus;
import com.synapsecore.domain.repository.CustomerOrderRepository;
import com.synapsecore.domain.repository.FulfillmentTaskRepository;
import com.synapsecore.domain.repository.WarehouseRepository;
import com.synapsecore.fulfillment.FulfillmentService;
import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class OrderFulfillmentConnectionHoldIntegrationTest {

    private static final int UNRELATED_TASK_COUNT = 24;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private CustomerOrderRepository customerOrderRepository;

    @Autowired
    private FulfillmentTaskRepository fulfillmentTaskRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private FulfillmentService fulfillmentService;

    @Autowired
    private RequestTraceContext requestTraceContext;

    @Test
    void fulfillmentInitializationDoesNotHydrateActiveWorkFromAnotherWarehouse() {
        String suffix = String.valueOf(System.nanoTime());
        long baselineEntityLoads = initializeAndCountLoads("ORDER-HOLD-BASE-" + suffix);

        prepareUnrelatedFulfillment(suffix);
        long loadedEntityLoads = initializeAndCountLoads("ORDER-HOLD-LOADED-" + suffix);

        long unrelatedLoadGrowth = loadedEntityLoads - baselineEntityLoads;
        assertThat(unrelatedLoadGrowth)
            .as("adding %s active WH-COAST tasks must not scale WH-NORTH order hydration; baseline=%s loaded=%s",
                UNRELATED_TASK_COUNT, baselineEntityLoads, loadedEntityLoads)
            .isLessThan(10L);
    }

    private long initializeAndCountLoads(String externalOrderId) {
        requestTraceContext.setCurrentRequestId(externalOrderId);
        requestTraceContext.setCurrentActor("Integration Lead");
        requestTraceContext.setCurrentTenant("STARTER-OPS");
        try {
            return transactionTemplate.execute(status -> {
                var north = warehouseRepository.findByCode("WH-NORTH").orElseThrow();
                CustomerOrder order = customerOrderRepository.save(CustomerOrder.builder()
                    .tenant(north.getTenant())
                    .externalOrderId(externalOrderId)
                    .status(OrderStatus.RECEIVED)
                    .statusReason("Order hold proof.")
                    .totalAmount(BigDecimal.TEN)
                    .warehouse(north)
                    .build());
                Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
                statistics.setStatisticsEnabled(true);
                statistics.clear();
                fulfillmentService.initializeForOrder(order, "order-hold-proof");
                return statistics.getEntityLoadCount();
            });
        } finally {
            requestTraceContext.clear();
        }
    }

    private void prepareUnrelatedFulfillment(String suffix) {
        transactionTemplate.executeWithoutResult(status -> {
            var coast = warehouseRepository.findByCode("WH-COAST").orElseThrow();
            Instant now = Instant.now();
            for (int index = 0; index < UNRELATED_TASK_COUNT; index++) {
                CustomerOrder order = customerOrderRepository.save(CustomerOrder.builder()
                    .tenant(coast.getTenant())
                    .externalOrderId("UNRELATED-COAST-" + suffix + "-" + index)
                    .status(OrderStatus.RECEIVED)
                    .statusReason("Unrelated fulfillment scope proof.")
                    .totalAmount(BigDecimal.TEN)
                    .warehouse(coast)
                    .build());
                fulfillmentTaskRepository.save(FulfillmentTask.builder()
                    .tenant(coast.getTenant())
                    .customerOrder(order)
                    .warehouse(coast)
                    .status(FulfillmentStatus.QUEUED)
                    .queuedAt(now)
                    .totalUnits(1)
                    .fulfilledUnits(0)
                    .promisedDispatchAt(now.plusSeconds(7200))
                    .expectedDeliveryAt(now.plusSeconds(86400))
                    .exceptionCount(0)
                    .note("Unrelated fulfillment scope proof.")
                    .build());
            }
        });
    }

}
