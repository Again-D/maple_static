package com.maple.growth;

import java.time.Clock;
import com.maple.growth.config.AppProperties;
import com.maple.growth.config.CollectionProperties;
import com.maple.growth.config.NexonApiProperties;
import com.maple.growth.config.OperationsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan(basePackageClasses = {
        AppProperties.class,
        CollectionProperties.class,
        NexonApiProperties.class,
        OperationsProperties.class
})
public class MapleGrowthApplication {

    public static void main(String[] args) {
        SpringApplication.run(MapleGrowthApplication.class, args);
    }

    @Bean
    Clock appClock() {
        return Clock.systemUTC();
    }
}
