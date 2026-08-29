package com.synapsecore.decision;

import com.synapsecore.access.AccessDirectoryService;
import com.synapsecore.domain.entity.AccessOperator;
import com.synapsecore.domain.entity.Recommendation;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RecommendationScopeService {

    private final AccessDirectoryService accessDirectoryService;

    public List<Recommendation> visible(List<Recommendation> recommendations) {
        Optional<AccessOperator> currentOperator = accessDirectoryService.getCurrentOperator();
        if (currentOperator.isEmpty()) {
            return recommendations;
        }
        AccessOperator operator = currentOperator.get();
        return recommendations.stream()
            .filter(recommendation -> accessDirectoryService.hasWarehouseAccess(operator, recommendation.getWarehouse().getCode()))
            .filter(recommendation -> recommendation.getType() != com.synapsecore.domain.entity.RecommendationType.TRANSFER_STOCK
                || (recommendation.getSourceWarehouse() != null
                    && recommendation.getDestinationWarehouse() != null
                    && accessDirectoryService.hasWarehouseAccess(operator, recommendation.getSourceWarehouse().getCode())
                    && accessDirectoryService.hasWarehouseAccess(operator, recommendation.getDestinationWarehouse().getCode())))
            .toList();
    }
}
