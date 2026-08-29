package com.synapsecore.domain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapsecore.alert.AlertScopeService;
import com.synapsecore.domain.dto.DashboardSummaryResponse;
import com.synapsecore.domain.repository.CustomerOrderRepository;
import com.synapsecore.domain.repository.InventoryRepository;
import com.synapsecore.domain.repository.RecommendationRepository;
import com.synapsecore.domain.repository.WarehouseRepository;
import com.synapsecore.decision.RecommendationScopeService;
import com.synapsecore.domain.entity.RecommendationStatus;
import com.synapsecore.fulfillment.FulfillmentService;
import com.synapsecore.tenant.TenantContextService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final CustomerOrderRepository customerOrderRepository;
    private final InventoryRepository inventoryRepository;
    private final RecommendationRepository recommendationRepository;
    private final WarehouseRepository warehouseRepository;
    private final FulfillmentService fulfillmentService;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final TenantContextService tenantContextService;
    private final AlertScopeService alertScopeService;
    @Autowired
    private RecommendationScopeService recommendationScopeService;

    @Value("${synapsecore.dashboard.cache-enabled:true}")
    private boolean cacheEnabled;

    @Value("${synapsecore.dashboard.summary-cache-key}")
    private String cacheKey;

    public DashboardSummaryResponse getSummary() {
        String tenantCode = tenantContextService.getCurrentTenantCodeOrDefault();
        if (!cacheEnabled || alertScopeService.isCurrentOperatorWarehouseScoped()) {
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
        boolean warehouseScoped = alertScopeService.isCurrentOperatorWarehouseScoped();
        Instant now = Instant.now();
        Instant recentWindow = now.minus(24, ChronoUnit.HOURS);
        var fulfillmentOverview = fulfillmentService.getOverview();
        DashboardSummaryResponse summary = new DashboardSummaryResponse(
            customerOrderRepository.countByTenant_CodeIgnoreCase(tenantCode),
            alertScopeService.countVisibleActiveAlerts(tenantCode),
            inventoryRepository.countLowStockItemsByTenantCode(tenantCode),
            recommendationScopeService.visible(recommendationRepository.findAllByTenant_CodeIgnoreCaseAndStatusOrderByUpdatedAtDesc(
                tenantCode, RecommendationStatus.CURRENT)).size(),
            fulfillmentOverview.backlogCount(),
            fulfillmentOverview.delayedShipmentCount(),
            fulfillmentOverview.atRiskCount(),
            inventoryRepository.countDistinctProductsByTenantCode(tenantCode),
            warehouseRepository.countByTenant_CodeIgnoreCase(tenantCode),
            customerOrderRepository.countByTenant_CodeIgnoreCaseAndCreatedAtAfter(tenantCode, recentWindow),
            inventoryRepository.countByTenantCode(tenantCode),
            now
        );
        if (!cacheEnabled || warehouseScoped) {
            return summary;
        }
        try {
            redisTemplate.opsForValue().set(cacheKey + ":" + tenantCode, objectMapper.writeValueAsString(summary));
        } catch (Exception ignored) {
        }
        return summary;
    }
}
