package com.synapsecore.alert;

import com.synapsecore.access.AccessDirectoryService;
import com.synapsecore.domain.entity.AccessOperator;
import com.synapsecore.domain.entity.Alert;
import com.synapsecore.domain.entity.AlertStatus;
import com.synapsecore.domain.repository.AlertRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AlertScopeService {

    private final AlertRepository alertRepository;
    private final AccessDirectoryService accessDirectoryService;

    @Transactional(readOnly = true)
    public List<Alert> visibleActiveAlerts(String tenantCode) {
        return visibleAlerts(alertRepository.findAllByTenant_CodeIgnoreCaseAndStatusOrderByCreatedAtDesc(
            tenantCode,
            AlertStatus.ACTIVE));
    }

    @Transactional(readOnly = true)
    public List<Alert> visibleRecentAlerts(String tenantCode) {
        return visibleAlerts(alertRepository.findAllByTenant_CodeIgnoreCaseOrderByUpdatedAtDesc(tenantCode));
    }

    public long countVisibleActiveAlerts(String tenantCode) {
        return visibleActiveAlerts(tenantCode).size();
    }

    public boolean isCurrentOperatorWarehouseScoped() {
        return currentOperator().map(operator -> !accessDirectoryService.getWarehouseScopes(operator).isEmpty())
            .orElse(false);
    }

    private List<Alert> visibleAlerts(List<Alert> alerts) {
        Optional<AccessOperator> operator = currentOperator();
        if (operator.isEmpty()) {
            return alerts;
        }
        return alerts.stream()
            .filter(alert -> alert.getWarehouse() != null
                && accessDirectoryService.hasWarehouseAccess(operator.get(), alert.getWarehouse().getCode()))
            .toList();
    }

    private Optional<AccessOperator> currentOperator() {
        return accessDirectoryService.getCurrentOperator();
    }
}
