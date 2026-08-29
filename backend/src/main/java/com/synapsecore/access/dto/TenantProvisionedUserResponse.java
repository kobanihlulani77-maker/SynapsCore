package com.synapsecore.access.dto;

import com.synapsecore.access.SynapseAccessRole;
import java.util.List;

public record TenantProvisionedUserResponse(
    String username,
    String operatorActorName,
    List<SynapseAccessRole> roles,
    List<String> warehouseScopes,
    boolean temporaryCredentialIssued,
    String temporaryCredential
) {
}
