package com.maple.growth.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import com.maple.growth.config.AppProperties;
import org.springframework.stereotype.Component;

@Component
public class KstClock {
    private final Clock clock;
    private final ZoneId zoneId;

    public KstClock(Clock clock, AppProperties appProperties) {
        this.clock = clock;
        this.zoneId = appProperties.zoneId();
    }

    public ZoneId zoneId() {
        return zoneId;
    }

    public OffsetDateTime now() {
        return OffsetDateTime.ofInstant(clock.instant(), zoneId);
    }

    public LocalDate today() {
        return now().toLocalDate();
    }
}
