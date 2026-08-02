package com.maple.growth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nexon")
public record NexonApiProperties(String apiKey) {

    public NexonApiProperties {
        apiKey = apiKey == null ? "" : apiKey;
    }
}
