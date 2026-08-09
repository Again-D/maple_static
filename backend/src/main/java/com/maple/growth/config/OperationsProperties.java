package com.maple.growth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record OperationsProperties(String operationsApiToken) {

    public OperationsProperties {
        if (operationsApiToken == null || operationsApiToken.isBlank()) {
            throw new IllegalArgumentException("operationsApiToken must be configured");
        }
    }
}
