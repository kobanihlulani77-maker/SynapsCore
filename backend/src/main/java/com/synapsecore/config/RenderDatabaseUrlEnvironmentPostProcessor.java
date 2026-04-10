package com.synapsecore.config;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

public class RenderDatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String DATA_SOURCE_URL = "spring.datasource.url";
    private static final String DATA_SOURCE_USERNAME = "spring.datasource.username";
    private static final String DATA_SOURCE_PASSWORD = "spring.datasource.password";
    private static final String DATABASE_URL = "DATABASE_URL";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (environment.containsProperty(DATA_SOURCE_URL)) {
            return;
        }

        String rawUrl = environment.getProperty(DATABASE_URL);
        if (rawUrl == null || rawUrl.isBlank()) {
            return;
        }

        String normalized = normalizeDatabaseUrl(rawUrl.trim());
        if (normalized == null) {
            return;
        }

        Map<String, Object> values = new LinkedHashMap<>();
        values.put(DATA_SOURCE_URL, normalized);

        if (!environment.containsProperty(DATA_SOURCE_USERNAME) || !environment.containsProperty(DATA_SOURCE_PASSWORD)) {
            applyCredentialsFromUrl(rawUrl, values, environment);
        }

        environment.getPropertySources().addFirst(new MapPropertySource("renderDatabaseUrl", values));
    }

    private String normalizeDatabaseUrl(String rawUrl) {
        String lower = rawUrl.toLowerCase(Locale.ROOT);
        if (lower.startsWith("jdbc:")) {
            return rawUrl;
        }
        if (lower.startsWith("postgres://")) {
            return "jdbc:postgresql://" + rawUrl.substring("postgres://".length());
        }
        if (lower.startsWith("postgresql://")) {
            return "jdbc:postgresql://" + rawUrl.substring("postgresql://".length());
        }
        return null;
    }

    private void applyCredentialsFromUrl(String rawUrl, Map<String, Object> values, ConfigurableEnvironment environment) {
        URI uri;
        try {
            uri = new URI(rawUrl);
        } catch (URISyntaxException ex) {
            return;
        }

        String userInfo = uri.getUserInfo();
        if (userInfo == null || userInfo.isBlank()) {
            return;
        }

        String[] parts = userInfo.split(":", 2);
        if (parts.length > 0 && !environment.containsProperty(DATA_SOURCE_USERNAME)) {
            values.put(DATA_SOURCE_USERNAME, parts[0]);
        }
        if (parts.length == 2 && !environment.containsProperty(DATA_SOURCE_PASSWORD)) {
            values.put(DATA_SOURCE_PASSWORD, parts[1]);
        }
    }
}
