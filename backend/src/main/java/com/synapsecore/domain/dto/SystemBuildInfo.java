package com.synapsecore.domain.dto;

public record SystemBuildInfo(
    String version,
    String commit,
    String builtAt
) {
}
