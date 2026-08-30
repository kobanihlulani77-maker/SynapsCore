package com.synapsecore.access.dto;

import java.util.List;

public record TenantWorkspaceReadiness(
    boolean ready,
    List<String> reasons
) {
}
