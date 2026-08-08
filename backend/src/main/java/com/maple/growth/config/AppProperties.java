package com.maple.growth.config;

import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String timezone,
        String snapshotCron,
        String corsAllowedOrigins,
        String nexonBaseUrl,
        int nexonTimeoutSeconds,
        Integer schedulerDuplicateWaitSeconds
) {

    public AppProperties {
        timezone = defaultIfBlank(timezone, "Asia/Seoul");
        snapshotCron = defaultIfBlank(snapshotCron, "0 0 4 * * *");
        corsAllowedOrigins = defaultIfBlank(corsAllowedOrigins, "http://localhost:3000");
        nexonBaseUrl = defaultIfBlank(nexonBaseUrl, "https://open.api.nexon.com");
        nexonTimeoutSeconds = nexonTimeoutSeconds > 0 ? nexonTimeoutSeconds : 10;
        if (schedulerDuplicateWaitSeconds == null) {
            schedulerDuplicateWaitSeconds = 300;
        } else if (schedulerDuplicateWaitSeconds <= 0) {
            throw new IllegalArgumentException("schedulerDuplicateWaitSeconds must be greater than zero");
        }
    }

    public ZoneId zoneId() {
        return ZoneId.of(timezone);
    }

    public List<String> allowedOrigins() {
        return Arrays.stream(corsAllowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toList();
    }

    private static String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
