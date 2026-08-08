package com.maple.growth.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import com.maple.growth.config.AppProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KstClockTest {

    @Test
    void todayUsesSeoulTimezoneEvenWhenSystemClockIsUtc() {
        Clock fixedUtc = Clock.fixed(Instant.parse("2026-08-01T18:30:00Z"), ZoneOffset.UTC);
        KstClock kstClock = new KstClock(fixedUtc, new AppProperties("Asia/Seoul", "0 0 4 * * *", "http://localhost:3000", "https://open.api.nexon.com", 10, 300));

        assertThat(kstClock.today().toString()).isEqualTo("2026-08-02");
        assertThat(kstClock.now().getOffset().toString()).isEqualTo("+09:00");
    }
}
