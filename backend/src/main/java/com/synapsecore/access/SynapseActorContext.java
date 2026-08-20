package com.synapsecore.access;

import java.util.List;
import java.util.Set;

public record SynapseActorContext(
    String actorName,
    Set<SynapseAccessRole> roles,
    List<String> warehouseScopes
) {
    public boolean canAccessWarehouse(String warehouseCode) {
        return warehouseCode == null || warehouseCode.isBlank()
            || warehouseScopes == null || warehouseScopes.isEmpty()
            || warehouseScopes.stream().anyMatch(scope -> scope.equalsIgnoreCase(warehouseCode));
    }
}
