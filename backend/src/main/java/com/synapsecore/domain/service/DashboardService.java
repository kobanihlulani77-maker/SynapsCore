package com.synapsecore.domain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapsecore.domain.dto.DashboardSummaryResponse;
import com.synapsecore.domain.entity.AlertStatus;
import com.synapsecore.domain.repository.AlertRepository;
import com.synapsecore.domain.repository.CustomerOrderRepository;
import com.synapsecore.domain.repository.InventoryRepository;
import com.synapsecore.domain.repository.ProductRepository;
import com.synapsecore.domain.repository.RecommendationRepository;
import com.synapsecore.domain.repository.WarehouseRepository;
import com.synapsecore.fulfillment.FulfillmentService;
import com.synapsecore.simulation.SimulationStateService;
import com.synapsecore.tenant.TenantContextService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final CustomerOrderRepository customerOrderRepository;
    private final AlertRepository alertRepository;
    private final InventoryRepository inventoryRepository;
    private final RecommendationRepository recommendationRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final FulfillmentService fulfillmentService;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final SimulationStateService simulationStateService;
    private final TenantContextService tenantContextService;

    @Value("${synapsecore.dashboard.cache-enabled:true}")
    private boolean cacheEnabled;

    @Value("${synapsecore.dashboard.summary-cache-key}")
    private String cacheKey;

    public DashboardSummaryResponse getSummary() {
        String tenantCode = tenantContextService.getCurrentTenantCodeOrDefault();
        if (!cacheEnabled) {
            return refreshSummary();
        }
        try {
            String cached = redisTemplate.opsForValue().get(cacheKey + ":" + tenantCode);
            if (cached != null) {
                return objectMapper.readValue(cached, DashboardSummaryResponse.class);
            }
        } catch (Exception ignored) {
        }
        return refreshSummary();
    }

    public DashboardSummaryResponse refreshSummary() {
        String tenantCode = tenantContextService.getCurrentTenantCodeOrDefault();
        Instant now = Instant.now();
        Instant recentWindow = now.minus(24, ChronoUnit.HOURS);
        var fulfillmentOverview = fulfillmentService.getOverview();
        DashboardSummaryResponse summary = new DashboardSummaryResponse(
            customerOrderRepository.countByTenant_CodeIgnoreCase(tenantCode),
            alertRepository.countByTenant_CodeIgnoreCaseAndStatus(tenantCode, AlertStatus.ACTIVE),
            inventoryRepository.countLowStockItemsByTenantCode(tenantCode),
            recommendationRepository.countByTenant_CodeIgnoreCaseAndCreatedAtAfter(tenantCode, recentWindow),
            fulfillmentOverview.backlogCount(),
            fulfillmentOverview.delayedShipmentCount(),
            fulfillmentOverview.atRiskCount(),
            productRepository.count(),
            warehouseRepository.countByTenant_CodeIgnoreCase(tenantCode),
            customerOrderRepository.countByTenant_CodeIgnoreCaseAndCreatedAtAfter(tenantCode, recentWindow),
            inventoryRepository.countByTenantCode(tenantCode),
            simulationStateService.getStatus().active(),
            now
        );
        if (!cacheEnabled) {
            return summary;
        }
        try {
            redisTemplate.opsForValue().set(cacheKey + ":" + tenantCode, objectMapper.writeValueAsString(summary));
        } catch (Exception ignored) {
        }
        return summary;
    }
}
