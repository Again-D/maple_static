package com.maple.growth.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maple.growth.entity.CharacterEntity;
import com.maple.growth.entity.DailySnapshotEntity;
import com.maple.growth.entity.GrowthEventLogEntity;
import com.maple.growth.repository.GrowthEventLogRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GrowthEventServiceTest {

    @Test
    void levelCombatHexaAndUnionRulesCreateStableEvents() {
        GrowthEventLogRepository repository = mock(GrowthEventLogRepository.class);
        ObjectMapper objectMapper = new ObjectMapper();
        GrowthEventService service = new GrowthEventService(repository, objectMapper);

        CharacterEntity character = new CharacterEntity("ocid-1", "Aries92", "루나", "나이트로드", "male", "img");
        DailySnapshotEntity previous = snapshot(character, 100, 1_000_000L, new BigDecimal("10.0000"), 1_000L, 800, 5, 1, LocalDate.of(2026, 8, 1));
        DailySnapshotEntity current = snapshot(character, 102, 1_120_000L, new BigDecimal("20.0000"), 1_120L, 900, 7, 3, LocalDate.of(2026, 8, 2));

        List<GrowthEventLogEntity> events = service.buildEvents(current, previous);

        assertThat(events).extracting(GrowthEventLogEntity::getEventType)
                .contains("LEVEL_UP", "COMBAT_POWER_CHANGE", "HEXA_UPGRADED", "UNION_UPGRADED");
    }

    @Test
    void sameSnapshotRecomputeDoesNotDuplicateRows() {
        GrowthEventLogRepository repository = mock(GrowthEventLogRepository.class);
        ObjectMapper objectMapper = new ObjectMapper();
        GrowthEventService service = new GrowthEventService(repository, objectMapper);

        CharacterEntity character = new CharacterEntity("ocid-2", "Aries93", "루나", "비숍", "female", "img");
        DailySnapshotEntity previous = snapshot(character, 100, 1000L, new BigDecimal("10.0000"), 1_000L, 800, 5, 1, LocalDate.of(2026, 8, 1));
        DailySnapshotEntity current = snapshot(character, 101, 1200L, new BigDecimal("12.0000"), 1_150L, 800, 6, 1, LocalDate.of(2026, 8, 2));

        when(repository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        int count = service.recomputeEvents(current, previous);

        assertThat(count).isGreaterThan(0);
        verify(repository).deleteBySnapshot(current);
    }

    @Test
    void levelUpUsesDocumentedDetailKeysAndDescription() {
        GrowthEventLogRepository repository = mock(GrowthEventLogRepository.class);
        ObjectMapper objectMapper = new ObjectMapper();
        GrowthEventService service = new GrowthEventService(repository, objectMapper);

        CharacterEntity character = new CharacterEntity("ocid-5", "Aries96", "루나", "나이트로드", "male", "img");
        DailySnapshotEntity previous = snapshot(character, 100, 1_000_000L, new BigDecimal("10.0000"), 1_000L, 800, 5, 1, LocalDate.of(2026, 8, 1));
        DailySnapshotEntity current = snapshot(character, 101, 1_000_000L, new BigDecimal("10.0000"), 1_000L, 800, 5, 1, LocalDate.of(2026, 8, 2));

        GrowthEventLogEntity levelUp = service.buildEvents(current, previous).stream()
                .filter(event -> "LEVEL_UP".equals(event.getEventType()))
                .findFirst()
                .orElseThrow();

        assertThat(levelUp.getDescription()).isEqualTo("이전 대표 스냅샷 대비 레벨이 상승했습니다.");
        assertThat(levelUp.getDetailJson().get("fromLevel").asInt()).isEqualTo(100);
        assertThat(levelUp.getDetailJson().get("toLevel").asInt()).isEqualTo(101);
        assertThat(levelUp.getDetailJson().get("delta").asInt()).isEqualTo(1);
    }

    @Test
    void combatPowerBelowAbsoluteThresholdAndBelowRatioThresholdDoesNotEmitEvent() {
        GrowthEventLogRepository repository = mock(GrowthEventLogRepository.class);
        ObjectMapper objectMapper = new ObjectMapper();
        GrowthEventService service = new GrowthEventService(repository, objectMapper);

        CharacterEntity character = new CharacterEntity("ocid-3", "Aries94", "루나", "나이트로드", "male", "img");
        DailySnapshotEntity previous = snapshot(character, 100, 9_999_999L, new BigDecimal("10.0000"), 1_000L, 800, 5, 1, LocalDate.of(2026, 8, 1));
        DailySnapshotEntity current = snapshot(character, 100, 10_099_998L, new BigDecimal("10.0000"), 1_000L, 800, 5, 1, LocalDate.of(2026, 8, 2));

        assertThat(service.buildEvents(current, previous)).extracting(GrowthEventLogEntity::getEventType)
                .doesNotContain("COMBAT_POWER_CHANGE");
    }

    @Test
    void combatPowerAtAbsoluteThresholdEmitsEvent() {
        GrowthEventLogRepository repository = mock(GrowthEventLogRepository.class);
        ObjectMapper objectMapper = new ObjectMapper();
        GrowthEventService service = new GrowthEventService(repository, objectMapper);

        CharacterEntity character = new CharacterEntity("ocid-6", "Aries97", "루나", "나이트로드", "male", "img");
        DailySnapshotEntity previous = snapshot(character, 100, 1_000_000L, new BigDecimal("10.0000"), 1_000L, 800, 5, 1, LocalDate.of(2026, 8, 1));
        DailySnapshotEntity current = snapshot(character, 100, 1_100_000L, new BigDecimal("10.0000"), 1_000L, 800, 5, 1, LocalDate.of(2026, 8, 2));

        assertThat(service.buildEvents(current, previous)).extracting(GrowthEventLogEntity::getEventType)
                .contains("COMBAT_POWER_CHANGE");
    }

    @Test
    void combatPowerAtRatioThresholdEmitsEventEvenBelowAbsoluteThreshold() {
        GrowthEventLogRepository repository = mock(GrowthEventLogRepository.class);
        ObjectMapper objectMapper = new ObjectMapper();
        GrowthEventService service = new GrowthEventService(repository, objectMapper);

        CharacterEntity character = new CharacterEntity("ocid-7", "Aries98", "루나", "나이트로드", "male", "img");
        DailySnapshotEntity previous = snapshot(character, 100, 1_000_000L, new BigDecimal("10.0000"), 1_000L, 800, 5, 1, LocalDate.of(2026, 8, 1));
        DailySnapshotEntity current = snapshot(character, 100, 1_010_000L, new BigDecimal("10.0000"), 1_000L, 800, 5, 1, LocalDate.of(2026, 8, 2));

        assertThat(service.buildEvents(current, previous)).extracting(GrowthEventLogEntity::getEventType)
                .contains("COMBAT_POWER_CHANGE");
    }

    @Test
    void combatPowerAtOnePercentWhenPreviousIsZeroDoesNotEmitEvent() {
        GrowthEventLogRepository repository = mock(GrowthEventLogRepository.class);
        ObjectMapper objectMapper = new ObjectMapper();
        GrowthEventService service = new GrowthEventService(repository, objectMapper);

        CharacterEntity character = new CharacterEntity("ocid-8", "Aries99", "루나", "나이트로드", "male", "img");
        DailySnapshotEntity previous = snapshot(character, 100, 0L, new BigDecimal("10.0000"), 1_000L, 800, 5, 1, LocalDate.of(2026, 8, 1));
        DailySnapshotEntity current = snapshot(character, 100, 50_000L, new BigDecimal("10.0000"), 1_000L, 800, 5, 1, LocalDate.of(2026, 8, 2));

        assertThat(service.buildEvents(current, previous)).extracting(GrowthEventLogEntity::getEventType)
                .doesNotContain("COMBAT_POWER_CHANGE");
    }

    @Test
    void combatPowerBigChangeSetsImportanceTwo() {
        GrowthEventLogRepository repository = mock(GrowthEventLogRepository.class);
        ObjectMapper objectMapper = new ObjectMapper();
        GrowthEventService service = new GrowthEventService(repository, objectMapper);

        CharacterEntity character = new CharacterEntity("ocid-9", "Aries100", "루나", "나이트로드", "male", "img");
        DailySnapshotEntity previous = snapshot(character, 100, 1_000_000L, new BigDecimal("10.0000"), 1_000L, 800, 5, 1, LocalDate.of(2026, 8, 1));
        DailySnapshotEntity current = snapshot(character, 100, 2_050_000L, new BigDecimal("10.0000"), 1_000L, 800, 5, 1, LocalDate.of(2026, 8, 2));

        GrowthEventLogEntity event = service.buildEvents(current, previous).stream()
                .filter(item -> "COMBAT_POWER_CHANGE".equals(item.getEventType()))
                .findFirst()
                .orElseThrow();

        assertThat(event.getImportanceLevel()).isEqualTo(2);
    }

    @Test
    void missingOptionalMetricsSkipOnlyTheirOwnEvents() {
        GrowthEventLogRepository repository = mock(GrowthEventLogRepository.class);
        ObjectMapper objectMapper = new ObjectMapper();
        GrowthEventService service = new GrowthEventService(repository, objectMapper);

        CharacterEntity character = new CharacterEntity("ocid-10", "Aries101", "루나", "나이트로드", "male", "img");
        DailySnapshotEntity previous = snapshot(character, 100, 1_000_000L, new BigDecimal("10.0000"), 1_000L, 800, 5, 1, LocalDate.of(2026, 8, 1));
        previous.setHexaMatrixLevelSum(null);
        previous.setUnionLevel(null);
        previous.setUnionArtifactLevel(null);
        DailySnapshotEntity current = snapshot(character, 101, 1_150_000L, new BigDecimal("10.5000"), 1_000L, 800, 5, 1, LocalDate.of(2026, 8, 2));
        current.setHexaMatrixLevelSum(null);
        current.setUnionLevel(810);
        current.setUnionArtifactLevel(null);

        List<GrowthEventLogEntity> events = service.buildEvents(current, previous);

        assertThat(events).extracting(GrowthEventLogEntity::getEventType)
                .contains("LEVEL_UP", "COMBAT_POWER_CHANGE")
                .doesNotContain("HEXA_UPGRADED");
    }

    @Test
    void eventDescriptionsCarryBeforeAndAfterValues() {
        GrowthEventLogRepository repository = mock(GrowthEventLogRepository.class);
        ObjectMapper objectMapper = new ObjectMapper();
        GrowthEventService service = new GrowthEventService(repository, objectMapper);

        CharacterEntity character = new CharacterEntity("ocid-4", "Aries95", "루나", "나이트로드", "male", "img");
        DailySnapshotEntity previous = snapshot(character, 100, 1_000_000L, new BigDecimal("10.0000"), 1_000L, 800, 5, 1, LocalDate.of(2026, 8, 1));
        DailySnapshotEntity current = snapshot(character, 101, 1_120_000L, new BigDecimal("11.0000"), 1_100L, 820, 6, 2, LocalDate.of(2026, 8, 2));

        List<GrowthEventLogEntity> events = service.buildEvents(current, previous);

        assertThat(events.stream().filter(event -> !"LEVEL_UP".equals(event.getEventType())).toList())
                .allSatisfy(event -> assertThat(event.getDescription()).isNotEqualTo(event.getTitle()));
        assertThat(events).anySatisfy(event -> {
            if ("COMBAT_POWER_CHANGE".equals(event.getEventType())) {
                assertThat(event.getDescription()).isEqualTo("1,000,000 -> 1,120,000");
            }
        });
    }

    @Test
    void recomputeRemovesStaleGeneratedRowsBeforeSavingDesiredOnes() {
        GrowthEventLogRepository repository = mock(GrowthEventLogRepository.class);
        ObjectMapper objectMapper = new ObjectMapper();
        GrowthEventService service = new GrowthEventService(repository, objectMapper);

        CharacterEntity character = new CharacterEntity("ocid-11", "Aries102", "루나", "나이트로드", "male", "img");
        DailySnapshotEntity previous = snapshot(character, 100, 1_000_000L, new BigDecimal("10.0000"), 1_000L, 800, 5, 1, LocalDate.of(2026, 8, 1));
        DailySnapshotEntity current = snapshot(character, 102, 1_120_000L, new BigDecimal("12.0000"), 1_120L, 820, 6, 2, LocalDate.of(2026, 8, 2));

        when(repository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        int count = service.recomputeEvents(current, previous);

        assertThat(count).isGreaterThan(0);
        verify(repository).deleteBySnapshot(current);
        verify(repository).saveAll(any());
    }

    @Test
    void itemReplacedIsNotGenerated() {
        GrowthEventLogRepository repository = mock(GrowthEventLogRepository.class);
        ObjectMapper objectMapper = new ObjectMapper();
        GrowthEventService service = new GrowthEventService(repository, objectMapper);

        CharacterEntity character = new CharacterEntity("ocid-12", "Aries103", "루나", "나이트로드", "male", "img");
        DailySnapshotEntity previous = snapshot(character, 100, 1_000_000L, new BigDecimal("10.0000"), 1_000L, 800, 5, 1, LocalDate.of(2026, 8, 1));
        DailySnapshotEntity current = snapshot(character, 100, 1_000_000L, new BigDecimal("10.0000"), 1_000L, 800, 5, 1, LocalDate.of(2026, 8, 2));

        current.setRawEquipmentJson(objectMapper.createObjectNode().put("weapon", "new"));
        previous.setRawEquipmentJson(objectMapper.createObjectNode().put("weapon", "old"));

        assertThat(service.buildEvents(current, previous)).extracting(GrowthEventLogEntity::getEventType)
                .doesNotContain("ITEM_REPLACED");
    }

    private DailySnapshotEntity snapshot(CharacterEntity character, int level, long combatPower, BigDecimal expRate, Long exp, Integer unionLevel, Integer artifactLevel, Integer hexa, LocalDate date) {
        DailySnapshotEntity snapshot = new DailySnapshotEntity(character, date);
        snapshot.setLevel(level);
        snapshot.setCombatPower(combatPower);
        snapshot.setExpRate(expRate);
        snapshot.setExp(exp);
        snapshot.setUnionLevel(unionLevel);
        snapshot.setUnionArtifactLevel(artifactLevel);
        snapshot.setHexaMatrixLevelSum(hexa);
        snapshot.setCapturedAt(java.time.OffsetDateTime.parse("2026-08-02T04:00:00+09:00"));
        return snapshot;
    }
}
