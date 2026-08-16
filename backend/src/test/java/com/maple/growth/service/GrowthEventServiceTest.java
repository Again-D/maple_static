package com.maple.growth.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.maple.growth.entity.CharacterEntity;
import com.maple.growth.entity.DailySnapshotEntity;
import com.maple.growth.entity.GrowthEventLogEntity;
import com.maple.growth.repository.GrowthEventLogRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
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
    void itemReplacedGeneratesSingleGroupedEventForMultipleSlotChanges() {
        GrowthEventLogRepository repository = mock(GrowthEventLogRepository.class);
        ObjectMapper objectMapper = new ObjectMapper();
        GrowthEventService service = new GrowthEventService(repository, objectMapper);

        CharacterEntity character = new CharacterEntity("ocid-12", "Aries103", "루나", "나이트로드", "male", "img");
        DailySnapshotEntity previous = snapshot(character, 100, 1_000_000L, new BigDecimal("10.0000"), 1_000L, 800, 5, 1, LocalDate.of(2026, 8, 1));
        DailySnapshotEntity current = snapshot(character, 100, 1_000_000L, new BigDecimal("10.0000"), 1_000L, 800, 5, 1, LocalDate.of(2026, 8, 2));

        previous.setRawEquipmentJson(rawEquipment(objectMapper,
                equipmentRow(objectMapper, "무기", "무기", "아케인 스태프"),
                equipmentRow(objectMapper, "장갑", "장갑", "아케인 장갑"),
                equipmentRow(objectMapper, "신발", "신발", "아케인 슈즈")
        ));
        current.setRawEquipmentJson(rawEquipment(objectMapper,
                equipmentRow(objectMapper, "무기", "무기", "에테르넬 스태프"),
                equipmentRow(objectMapper, "장갑", "장갑", "아케인 장갑"),
                equipmentRow(objectMapper, "신발", "신발", "에테르넬 슈즈")
        ));

        List<GrowthEventLogEntity> events = service.buildEvents(current, previous);
        List<GrowthEventLogEntity> itemReplacedEvents = events.stream()
                .filter(event -> "ITEM_REPLACED".equals(event.getEventType()))
                .toList();

        assertThat(itemReplacedEvents).hasSize(1);
        GrowthEventLogEntity event = itemReplacedEvents.get(0);
        assertThat(event.getImportanceLevel()).isEqualTo(2);
        assertThat(event.getEventKey()).startsWith("item_replaced:");

        JsonNode detailJson = event.getDetailJson();
        assertThat(detailJson.get("changeCount").asInt()).isEqualTo(2);
        assertThat(detailJson.get("changes")).isNotNull();
        assertThat(detailJson.get("changes").isArray()).isTrue();
        assertThat(detailJson.get("changes")).hasSize(2);
        assertThat(detailJson.get("changes").get(0).get("slot").asText()).isEqualTo("무기");
        assertThat(detailJson.get("changes").get(0).get("previousItemName").asText()).isEqualTo("아케인 스태프");
        assertThat(detailJson.get("changes").get(0).get("currentItemName").asText()).isEqualTo("에테르넬 스태프");
        assertThat(detailJson.get("changes").get(1).get("slot").asText()).isEqualTo("신발");
        assertThat(detailJson.get("changes").get(1).get("previousItemName").asText()).isEqualTo("아케인 슈즈");
        assertThat(detailJson.get("changes").get(1).get("currentItemName").asText()).isEqualTo("에테르넬 슈즈");
        assertThat(detailJson.get("combatPower").get("status").asText()).isEqualTo("unchanged");
        assertThat(detailJson.get("combatPower").get("message").asText()).isEqualTo("전투력 변화 없음");
    }

    @Test
    void itemReplacementAddsEstimatedContributionAndValuesOnlyComparison() {
        GrowthEventLogRepository repository = mock(GrowthEventLogRepository.class);
        ObjectMapper objectMapper = new ObjectMapper();
        GrowthEventService service = new GrowthEventService(repository, objectMapper);
        CharacterEntity character = new CharacterEntity("ocid-16", "Aries107", "루나", "나이트로드", "male", "img");
        DailySnapshotEntity previous = snapshot(character, 100, 1_000_000L, new BigDecimal("10.0000"), 1_000L, 800, 5, 1, LocalDate.of(2026, 8, 1));
        DailySnapshotEntity current = snapshot(character, 100, 1_200_000L, new BigDecimal("10.0000"), 1_000L, 800, 5, 1, LocalDate.of(2026, 8, 2));
        ObjectNode oldWeapon = equipmentRow(objectMapper, "무기", "무기", "이전 무기").put("item_starforce", "17").put("item_potential_option_grade", "에픽");
        ObjectNode newWeapon = equipmentRow(objectMapper, "무기", "무기", "현재 무기").put("item_starforce", "22").put("item_potential_option_grade", "레전드리");
        previous.setRawEquipmentJson(rawEquipment(objectMapper, oldWeapon));
        current.setRawEquipmentJson(rawEquipment(objectMapper, newWeapon));

        GrowthEventLogEntity event = service.buildEvents(current, previous).stream()
                .filter(item -> "ITEM_REPLACED".equals(item.getEventType()))
                .findFirst()
                .orElseThrow();

        assertThat(event.getDetailJson().get("combatPower").get("status").asText()).isEqualTo("estimated");
        assertThat(event.getDetailJson().get("combatPower").get("estimatedEquipmentContribution").asLong()).isEqualTo(200_000L);
        assertThat(event.getDetailJson().get("changes").get(0).get("previous").get("starforce").asText()).isEqualTo("17");
        assertThat(event.getDetailJson().get("changes").get(0).get("current").get("potentialGrade").asText()).isEqualTo("레전드리");
    }

    @Test
    void itemReplacedEventKeyIsDeterministicBoundedAndOrderIndependentForEquivalentDiffs() {
        GrowthEventLogRepository repository = mock(GrowthEventLogRepository.class);
        ObjectMapper objectMapper = new ObjectMapper();
        GrowthEventService service = new GrowthEventService(repository, objectMapper);
        CharacterEntity character = new CharacterEntity("ocid-13", "Aries104", "루나", "나이트로드", "male", "img");

        DailySnapshotEntity previousA = snapshot(character, 100, 1_000_000L, new BigDecimal("10.0000"), 1_000L, 800, 5, 1, LocalDate.of(2026, 8, 1));
        DailySnapshotEntity currentA = snapshot(character, 100, 1_000_000L, new BigDecimal("10.0000"), 1_000L, 800, 5, 1, LocalDate.of(2026, 8, 2));
        previousA.setRawEquipmentJson(rawEquipment(objectMapper,
                equipmentRow(objectMapper, "신발", "신발", "아케인 슈즈"),
                equipmentRow(objectMapper, "무기", "무기", "아케인 스태프")
        ));
        currentA.setRawEquipmentJson(rawEquipment(objectMapper,
                equipmentRow(objectMapper, "무기", "무기", "에테르넬 스태프"),
                equipmentRow(objectMapper, "신발", "신발", "에테르넬 슈즈")
        ));

        DailySnapshotEntity previousB = snapshot(character, 100, 1_000_000L, new BigDecimal("10.0000"), 1_000L, 800, 5, 1, LocalDate.of(2026, 8, 3));
        DailySnapshotEntity currentB = snapshot(character, 100, 1_000_000L, new BigDecimal("10.0000"), 1_000L, 800, 5, 1, LocalDate.of(2026, 8, 4));
        previousB.setRawEquipmentJson(rawEquipment(objectMapper,
                equipmentRow(objectMapper, "무기", "무기", "아케인 스태프"),
                equipmentRow(objectMapper, "신발", "신발", "아케인 슈즈")
        ));
        currentB.setRawEquipmentJson(rawEquipment(objectMapper,
                equipmentRow(objectMapper, "신발", "신발", "에테르넬 슈즈"),
                equipmentRow(objectMapper, "무기", "무기", "에테르넬 스태프")
        ));

        GrowthEventLogEntity eventA = service.buildEvents(currentA, previousA).stream()
                .filter(event -> "ITEM_REPLACED".equals(event.getEventType()))
                .findFirst()
                .orElseThrow();
        GrowthEventLogEntity eventB = service.buildEvents(currentB, previousB).stream()
                .filter(event -> "ITEM_REPLACED".equals(event.getEventType()))
                .findFirst()
                .orElseThrow();

        assertThat(eventA.getEventKey()).isEqualTo(eventB.getEventKey());
        assertThat(eventA.getEventKey().length()).isLessThanOrEqualTo(255);
    }

    @Test
    void recomputeWithGroupedItemReplacementStaysIdempotentAcrossSameDayRecompute() {
        GrowthEventLogRepository repository = mock(GrowthEventLogRepository.class);
        ObjectMapper objectMapper = new ObjectMapper();
        GrowthEventService service = new GrowthEventService(repository, objectMapper);

        CharacterEntity character = new CharacterEntity("ocid-14", "Aries105", "루나", "나이트로드", "male", "img");
        DailySnapshotEntity previous = snapshot(character, 100, 1_000_000L, new BigDecimal("10.0000"), 1_000L, 800, 5, 1, LocalDate.of(2026, 8, 1));
        DailySnapshotEntity current = snapshot(character, 100, 1_000_000L, new BigDecimal("10.0000"), 1_000L, 800, 5, 1, LocalDate.of(2026, 8, 2));

        previous.setRawEquipmentJson(rawEquipment(objectMapper,
                equipmentRow(objectMapper, "무기", "무기", "아케인 스태프"),
                equipmentRow(objectMapper, "신발", "신발", "아케인 슈즈")
        ));
        current.setRawEquipmentJson(rawEquipment(objectMapper,
                equipmentRow(objectMapper, "무기", "무기", "에테르넬 스태프"),
                equipmentRow(objectMapper, "신발", "신발", "에테르넬 슈즈")
        ));

        when(repository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        int firstCount = service.recomputeEvents(current, previous);
        int secondCount = service.recomputeEvents(current, previous);

        assertThat(firstCount).isGreaterThan(0);
        assertThat(secondCount).isEqualTo(firstCount);
        verify(repository, times(2)).deleteBySnapshot(current);

        ArgumentCaptor<List<GrowthEventLogEntity>> savedCaptor = ArgumentCaptor.forClass(List.class);
        verify(repository, times(2)).saveAll(savedCaptor.capture());
        savedCaptor.getAllValues().forEach(savedEvents -> {
            long groupedCount = savedEvents.stream()
                    .filter(event -> "ITEM_REPLACED".equals(event.getEventType()))
                    .count();
            assertThat(groupedCount).isEqualTo(1);
        });
    }

    @Test
    void itemReplacedIsNotGeneratedWhenComparableNormalizedDataMissingOnEitherSide() {
        GrowthEventLogRepository repository = mock(GrowthEventLogRepository.class);
        ObjectMapper objectMapper = new ObjectMapper();
        GrowthEventService service = new GrowthEventService(repository, objectMapper);

        CharacterEntity character = new CharacterEntity("ocid-15", "Aries106", "루나", "나이트로드", "male", "img");
        DailySnapshotEntity previous = snapshot(character, 100, 1_000_000L, new BigDecimal("10.0000"), 1_000L, 800, 5, 1, LocalDate.of(2026, 8, 1));
        DailySnapshotEntity current = snapshot(character, 100, 1_000_000L, new BigDecimal("10.0000"), 1_000L, 800, 5, 1, LocalDate.of(2026, 8, 2));

        previous.setRawEquipmentJson(objectMapper.createObjectNode().put("weapon", "old"));
        current.setRawEquipmentJson(rawEquipment(objectMapper, equipmentRow(objectMapper, "무기", "무기", "에테르넬 스태프")));

        assertThat(service.buildEvents(current, previous)).extracting(GrowthEventLogEntity::getEventType)
                .doesNotContain("ITEM_REPLACED");
    }

    @Test
    void normalizeActiveEquipmentIncludesOnlyCompleteItemEquipmentRowsInDeterministicOrder() {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode rawEquipment = objectMapper.createObjectNode();
        rawEquipment.putArray("item_equipment")
                .add(objectMapper.createObjectNode()
                        .put("item_equipment_part", "장갑")
                        .put("item_equipment_slot", "장갑")
                        .put("item_name", "아케인 장갑"))
                .add(objectMapper.createObjectNode()
                        .put("item_equipment_part", "무기")
                        .put("item_equipment_slot", "무기")
                        .put("item_name", "아케인 스태프"))
                .add(objectMapper.createObjectNode()
                        .put("item_equipment_part", "무기")
                        .put("item_equipment_slot", "무기")
                        .put("item_name", "에테르넬 스태프"))
                .add(objectMapper.createObjectNode()
                        .put("item_equipment_part", "모자")
                        .put("item_name", "누락 슬롯"))
                .add(objectMapper.createObjectNode()
                        .put("item_equipment_slot", "모자")
                        .put("item_name", "누락 파트"))
                .add(objectMapper.createObjectNode()
                        .put("item_equipment_part", "신발")
                        .put("item_equipment_slot", "신발"));
        rawEquipment.putArray("item_equipment_preset_1")
                .add(objectMapper.createObjectNode()
                        .put("item_equipment_part", "모자")
                        .put("item_equipment_slot", "모자")
                        .put("item_name", "프리셋 모자"));
        rawEquipment.putArray("dragon_equipment")
                .add(objectMapper.createObjectNode()
                        .put("item_equipment_part", "드래곤")
                        .put("item_equipment_slot", "드래곤")
                        .put("item_name", "드래곤 장비"));
        rawEquipment.putArray("mechanic_equipment")
                .add(objectMapper.createObjectNode()
                        .put("item_equipment_part", "메카닉")
                        .put("item_equipment_slot", "메카닉")
                        .put("item_name", "메카닉 장비"));

        Map<GrowthEventService.ActiveEquipmentSlotKey, GrowthEventService.ActiveEquipmentRecord> normalized =
                GrowthEventService.normalizeActiveEquipment(rawEquipment);

        assertThat(normalized.values())
                .extracting(GrowthEventService.ActiveEquipmentRecord::part, GrowthEventService.ActiveEquipmentRecord::slot, GrowthEventService.ActiveEquipmentRecord::itemName)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("무기", "무기", "에테르넬 스태프"),
                        org.assertj.core.groups.Tuple.tuple("장갑", "장갑", "아케인 장갑")
                );
    }

    @Test
    void buildComparablePairsSkipsIncompleteRowsAndMissingSlotsOnEitherSide() {
        ObjectMapper objectMapper = new ObjectMapper();

        ObjectNode previous = objectMapper.createObjectNode();
        previous.putArray("item_equipment")
                .add(objectMapper.createObjectNode()
                        .put("item_equipment_part", "무기")
                        .put("item_equipment_slot", "무기")
                        .put("item_name", "아케인 스태프"))
                .add(objectMapper.createObjectNode()
                        .put("item_equipment_part", "장갑")
                        .put("item_equipment_slot", "장갑")
                        .put("item_name", "아케인 장갑"))
                .add(objectMapper.createObjectNode()
                        .put("item_equipment_part", "모자")
                        .put("item_name", "누락 슬롯"));

        ObjectNode current = objectMapper.createObjectNode();
        current.putArray("item_equipment")
                .add(objectMapper.createObjectNode()
                        .put("item_equipment_part", "무기")
                        .put("item_equipment_slot", "무기")
                        .put("item_name", "에테르넬 스태프"))
                .add(objectMapper.createObjectNode()
                        .put("item_equipment_part", "신발")
                        .put("item_equipment_slot", "신발")
                        .put("item_name", "에테르넬 신발"));

        List<GrowthEventService.ComparableActiveEquipmentPair> pairs =
                GrowthEventService.buildComparableActiveEquipmentPairs(previous, current);

        assertThat(pairs)
                .extracting(
                        GrowthEventService.ComparableActiveEquipmentPair::part,
                        GrowthEventService.ComparableActiveEquipmentPair::slot,
                        GrowthEventService.ComparableActiveEquipmentPair::previousItemName,
                        GrowthEventService.ComparableActiveEquipmentPair::currentItemName
                )
                .containsExactly(org.assertj.core.groups.Tuple.tuple("무기", "무기", "아케인 스태프", "에테르넬 스태프"));
    }

    @Test
    void hasComparableActiveEquipmentIsFalseWhenOnlyPresetOrClassSpecificCollectionsExist() {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode rawEquipment = objectMapper.createObjectNode();
        rawEquipment.putArray("item_equipment_preset_1")
                .add(objectMapper.createObjectNode()
                        .put("item_equipment_part", "모자")
                        .put("item_equipment_slot", "모자")
                        .put("item_name", "프리셋 모자"));
        rawEquipment.putArray("dragon_equipment")
                .add(objectMapper.createObjectNode()
                        .put("item_equipment_part", "드래곤")
                        .put("item_equipment_slot", "드래곤")
                        .put("item_name", "드래곤 장비"));
        rawEquipment.putArray("mechanic_equipment")
                .add(objectMapper.createObjectNode()
                        .put("item_equipment_part", "메카닉")
                        .put("item_equipment_slot", "메카닉")
                        .put("item_name", "메카닉 장비"));

        assertThat(GrowthEventService.hasComparableActiveEquipment(rawEquipment)).isFalse();
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

    private ObjectNode equipmentRow(ObjectMapper objectMapper, String part, String slot, String itemName) {
        return objectMapper.createObjectNode()
                .put("item_equipment_part", part)
                .put("item_equipment_slot", slot)
                .put("item_name", itemName);
    }

    private ObjectNode rawEquipment(ObjectMapper objectMapper, ObjectNode... rows) {
        ObjectNode root = objectMapper.createObjectNode();
        var itemEquipment = root.putArray("item_equipment");
        for (ObjectNode row : rows) {
            itemEquipment.add(row);
        }
        return root;
    }
}
