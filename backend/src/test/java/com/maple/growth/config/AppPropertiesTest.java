package com.maple.growth.config;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AppPropertiesTest {

    @Test
    void defaultsUseSeoulTimezoneAndFourAmCron() {
        AppProperties properties = new AppProperties(null, null, null, null, 0);

        assertThat(properties.timezone()).isEqualTo("Asia/Seoul");
        assertThat(properties.snapshotCron()).isEqualTo("0 0 4 * * *");
        assertThat(properties.zoneId().getId()).isEqualTo("Asia/Seoul");
    }

    @Test
    void allowedOriginsAreTrimmedAndFiltered() {
        AppProperties properties = new AppProperties("Asia/Seoul", "0 0 4 * * *", " http://localhost:3000 , ,https://example.com ", "https://open.api.nexon.com", 10);

        assertThat(properties.allowedOrigins()).isEqualTo(List.of("http://localhost:3000", "https://example.com"));
    }
}
