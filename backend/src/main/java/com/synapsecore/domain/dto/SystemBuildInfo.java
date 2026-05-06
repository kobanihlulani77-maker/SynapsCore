package com.synapsecore.domain.dto;

public record SystemBuildInfo(
    String version,
    String commit,
    String builtAt,
    String branch,
    String platform,
    String serviceName,
    String serviceId,
    String instanceId
) {
}
