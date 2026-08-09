package com.maple.growth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.collection")
public record CollectionProperties(
        String retryCron,
        Integer retryBatchSize,
        Integer retryMaxAttempts,
        Integer retryInitialBackoffSeconds,
        Integer retryLeaseSeconds
) {

    public CollectionProperties {
        retryCron = defaultIfBlank(retryCron, "0 */15 * * * *");
        if (retryBatchSize == null) {
            retryBatchSize = 20;
        } else if (retryBatchSize <= 0) {
            throw new IllegalArgumentException("retryBatchSize must be greater than zero");
        }
        if (retryMaxAttempts == null) {
            retryMaxAttempts = 3;
        } else if (retryMaxAttempts <= 0) {
            throw new IllegalArgumentException("retryMaxAttempts must be greater than zero");
        }
        if (retryInitialBackoffSeconds == null) {
            retryInitialBackoffSeconds = 300;
        } else if (retryInitialBackoffSeconds <= 0) {
            throw new IllegalArgumentException("retryInitialBackoffSeconds must be greater than zero");
        }
        if (retryLeaseSeconds == null) {
            retryLeaseSeconds = 900;
        } else if (retryLeaseSeconds <= 0) {
            throw new IllegalArgumentException("retryLeaseSeconds must be greater than zero");
        }
    }

    private static String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
