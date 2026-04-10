package com.synapsecore.access;

import java.util.Set;

public record SynapseActorContext(
    String actorName,
    Set<SynapseAccessRole> roles
) {
}
