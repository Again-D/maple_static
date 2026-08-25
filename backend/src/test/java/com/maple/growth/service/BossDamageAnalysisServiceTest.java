package com.maple.growth.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maple.growth.entity.DailySnapshotEntity;
import org.junit.jupiter.api.Test;

class BossDamageAnalysisServiceTest {
    private final BossDamageAnalysisService service = new BossDamageAnalysisService(new ObjectMapper());

    @Test
    void calculatesEffectiveMultiplierFromFinalStats() throws Exception {
        DailySnapshotEntity snapshot = new DailySnapshotEntity();
        snapshot.setRawStatJson(new ObjectMapper().readTree("""
                {"final_stat":[
                  {"stat_name":"보스 몬스터 데미지","stat_value":"300"},
                  {"stat_name":"방어율 무시","stat_value":"90"}
                ]}
                """));

        var result = service.analyze(snapshot);

        assertThat(result.available()).isTrue();
        assertThat(result.catalogVersion()).isEqualTo("2026.08.25-assumption-1");
        assertThat(result.catalogReviewedAt()).isEqualTo("2026-08-25");
        assertThat(result.catalogSource()).contains("assumptions");
        assertThat(result.bossDamagePercent()).isEqualTo(300);
        assertThat(result.ignoreDefensePercent()).isEqualTo(90);
        assertThat(result.bosses()).isNotEmpty();
        assertThat(result.bosses().stream().allMatch(boss -> boss.effectiveDamageMultiplier() != null)).isTrue();
    }

    @Test
    void marksAnalysisUnavailableWhenStatsAreMissing() {
        var result = service.analyze(null);

        assertThat(result.available()).isFalse();
        assertThat(result.bosses()).allMatch(boss -> !boss.available() && boss.effectiveDamageMultiplier() == null);
    }

    @Test
    void clampsDefenseFactorWhenIgnoreDefenseExceedsTheAssumption() throws Exception {
        DailySnapshotEntity snapshot = new DailySnapshotEntity();
        snapshot.setRawStatJson(new ObjectMapper().readTree("""
                {"final_stat":[
                  {"stat_name":"보스 몬스터 데미지","stat_value":"100"},
                  {"stat_name":"방어율 무시","stat_value":"100"}
                ]}
                """));

        var result = service.analyze(snapshot);

        assertThat(result.bosses()).allSatisfy(boss -> assertThat(boss.effectiveDamageMultiplier()).isEqualByComparingTo("2.000"));
    }

    @Test
    void rejectsMalformedCatalogEntries() throws Exception {
        var objectMapper = new ObjectMapper();
        var root = objectMapper.readTree("{\"version\":\"v1\",\"bosses\":[{\"id\":\"broken\",\"name\":\"Broken\",\"difficulty\":\"Hard\",\"defenseRate\":-1}]}" );

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> BossDamageAnalysisService.validateCatalog(root, root.path("bosses")))
                .isInstanceOf(IllegalStateException.class);
    }
}
