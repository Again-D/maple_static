package com.maple.growth.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    WebClient nexonWebClient(AppProperties appProperties, NexonApiProperties nexonApiProperties) {
        return WebClient.builder()
                .baseUrl(appProperties.nexonBaseUrl())
                .defaultHeader("x-nxopen-api-key", nexonApiProperties.apiKey())
                .filter(redactHeaders())
                .build();
    }

    private ExchangeFilterFunction redactHeaders() {
        return ExchangeFilterFunction.ofRequestProcessor(request -> {
            if (request.headers().containsKey(HttpHeaders.AUTHORIZATION)) {
                return reactor.core.publisher.Mono.just(request);
            }
            return reactor.core.publisher.Mono.just(request);
        });
    }

    @Bean
    Duration nexonTimeout(AppProperties appProperties) {
        return Duration.ofSeconds(appProperties.nexonTimeoutSeconds());
    }

    @Bean
    WebMvcConfigurer corsConfigurer(AppProperties appProperties) {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins(appProperties.allowedOrigins().toArray(String[]::new))
                        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                        .allowCredentials(false);
            }
        };
    }
}
