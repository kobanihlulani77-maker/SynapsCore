package com.synapsecore.domain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapsecore.access.AccessDirectoryService;
import com.synapsecore.alert.AlertScopeService;
import com.synapsecore.domain.dto.DashboardSummaryResponse;
import com.synapsecore.domain.dto.FulfillmentOverviewResponse;
import com.synapsecore.domain.repository.CustomerOrderRepository;
import com.synapsecore.domain.repository.InventoryRepository;
import com.synapsecore.domain.repository.RecommendationRepository;
import com.synapsecore.domain.repository.WarehouseRepository;
import com.synapsecore.decision.RecommendationScopeService;
import com.synapsecore.domain.entity.RecommendationStatus;
import com.synapsecore.fulfillment.FulfillmentService;
import com.synapsecore.tenant.TenantContextService;
import java.time.Duration;
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
    private final AccessDirectoryService accessDirectoryService;
    @Autowired
    private RecommendationScopeService recommendationScopeService;

    @Value("${synapsecore.dashboard.cache-enabled:true}")
    private boolean cacheEnabled;

    @Value("${synapsecore.dashboard.summary-cache-key}")
    private String cacheKey;

    @Value("${synapsecore.dashboard.summary-cache-ttl-seconds:30}")
    private long summaryCacheTtlSeconds;

    public DashboardSummaryResponse getSummary() {
        return getSummary(null, null, null);
    }

    DashboardSummaryResponse getSummary(FulfillmentOverviewResponse fulfillmentSnapshot) {
        return getSummary(fulfillmentSnapshot, null, null);
    }

    DashboardSummaryResponse getSummary(FulfillmentOverviewResponse fulfillmentSnapshot,
                                        Long currentRecommendationCount) {
        return getSummary(fulfillmentSnapshot, currentRecommendationCount, null);
    }

    DashboardSummaryResponse getSummary(FulfillmentOverviewResponse fulfillmentSnapshot,
                                        Long currentRecommendationCount,
                                        Long activeAlertCount) {
        String tenantCode = tenantContextService.getCurrentTenantCodeOrDefault();
        if (!cacheEnabled || alertScopeService.isCurrentOperatorWarehouseScoped()) {
            return refreshSummary(fulfillmentSnapshot, currentRecommendationCount, activeAlertCount);
        }
        try {
            String cached = redisTemplate.opsForValue().get(cacheKey + ":" + tenantCode);
            if (cached != null) {
                return objectMapper.readValue(cached, DashboardSummaryResponse.class);
            }
        } catch (Exception ignored) {
        }
        return refreshSummary(fulfillmentSnapshot, currentRecommendationCount, activeAlertCount);
    }

    public DashboardSummaryResponse refreshSummary() {
        return refreshSummary(null, null, null);
    }

    private DashboardSummaryResponse refreshSummary(FulfillmentOverviewResponse fulfillmentSnapshot,
                                                    Long currentRecommendationCount,
                                                    Long activeAlertCount) {
        String tenantCode = tenantContextService.getCurrentTenantCodeOrDefault();
        boolean warehouseScoped = alertScopeService.isCurrentOperatorWarehouseScoped();
        var warehouseScopes = accessDirectoryService.getCurrentOperator()
            .map(accessDirectoryService::getWarehouseScopes)
            .orElse(java.util.List.of());
        Instant now = Instant.now();
        Instant recentWindow = now.minus(24, ChronoUnit.HOURS);
        var fulfillmentOverview = fulfillmentSnapshot != null
            ? fulfillmentSnapshot
            : fulfillmentService.getOverview();
        DashboardSummaryResponse summary = new DashboardSummaryResponse(
            warehouseScoped
                ? customerOrderRepository.countByTenantCodeAndWarehouseCodes(tenantCode, warehouseScopes)
                : customerOrderRepository.countByTenant_CodeIgnoreCase(tenantCode),
            activeAlertCount != null
                ? activeAlertCount
                : alertScopeService.countVisibleActiveAlerts(tenantCode),
            warehouseScoped
                ? inventoryRepository.countLowStockItemsByTenantCodeAndWarehouseCodes(tenantCode, warehouseScopes)
                : inventoryRepository.countLowStockItemsByTenantCode(tenantCode),
            currentRecommendationCount != null
                ? currentRecommendationCount
                : recommendationScopeService.visible(
                    recommendationRepository.findAllByTenant_CodeIgnoreCaseAndStatusOrderByUpdatedAtDesc(
                        tenantCode, RecommendationStatus.CURRENT)
                ).size(),
            fulfillmentOverview.backlogCount(),
            fulfillmentOverview.delayedShipmentCount(),
            fulfillmentOverview.atRiskCount(),
            inventoryRepository.countDistinctProductsByTenantCode(tenantCode),
            warehouseRepository.countByTenant_CodeIgnoreCase(tenantCode),
            warehouseScoped
                ? customerOrderRepository.countByTenantCodeAndWarehouseCodesAndCreatedAtAfter(tenantCode, warehouseScopes, recentWindow)
                : customerOrderRepository.countByTenant_CodeIgnoreCaseAndCreatedAtAfter(tenantCode, recentWindow),
            warehouseScoped
                ? inventoryRepository.countByTenantCodeAndWarehouseCodes(tenantCode, warehouseScopes)
                : inventoryRepository.countByTenantCode(tenantCode),
            now
        );
        if (!cacheEnabled || warehouseScoped) {
            return summary;
        }
        try {
            redisTemplate.opsForValue().set(
                cacheKey + ":" + tenantCode,
                objectMapper.writeValueAsString(summary),
                Duration.ofSeconds(Math.max(summaryCacheTtlSeconds, 1))
            );
        } catch (Exception ignored) {
        }
        return summary;
    }
}
