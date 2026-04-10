package com.synapsecore.config;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "synapsecore.cors")
public class SynapseCorsProperties {

    private List<String> allowedOrigins = List.of();

    public List<String> getAllowedOrigins() {
        return allowedOrigins.stream()
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .collect(Collectors.toUnmodifiableList());
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins == null ? List.of() : allowedOrigins;
    }
}
