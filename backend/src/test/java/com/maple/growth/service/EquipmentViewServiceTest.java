package com.maple.growth.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maple.growth.entity.DailySnapshotEntity;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

class EquipmentViewServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final EquipmentViewService service = new EquipmentViewService();

    @Test
    void normalizesActiveItemsAndOnlyExposesPopulatedDetails() throws Exception {
        DailySnapshotEntity snapshot = snapshot(objectMapper.readTree("""
                {
                  "item_equipment": [
                    {
                      "item_equipment_part": "무기",
                      "item_equipment_slot": "무기",
                      "item_name": "에테르넬 스태프",
                      "item_icon": "https://open.api.nexon.com/static/maplestory/item/icon/staff.png",
                      "item_starforce": "22",
                      "item_potential_option_grade": "레전드리",
                      "item_base_option": {"int": 100, "magic_power": 250},
                      "item_potential_option_1": "마력 12%",
                      "item_potential_option_2": null
                    }
                  ],
                  "item_equipment_preset_1": [{"item_name": "프리셋 무기"}]
                }
                """));

        var result = service.fromSnapshot(snapshot);

        assertThat(result.available()).isTrue();
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).name()).isEqualTo("에테르넬 스태프");
        assertThat(result.items().get(0).starforce()).isEqualTo("22");
        assertThat(result.items().get(0).baseOptions()).containsEntry("magic_power", "250");
        assertThat(result.items().get(0).potentialOptions()).containsExactly("마력 12%");
        assertThat(result.items().get(0).additionalOptions()).isEmpty();
    }

    @Test
    void skipsMalformedRowsAndReturnsUnavailableWhenNoActiveItemsExist() throws Exception {
        DailySnapshotEntity snapshot = snapshot(objectMapper.readTree("""
                {"item_equipment": [
                  {"item_equipment_part": "모자", "item_name": "슬롯 없음"},
                  {"item_equipment_slot": "신발", "item_name": "파트 없음"}
                ]}
                """));

        var result = service.fromSnapshot(snapshot);

        assertThat(result.items()).isEmpty();
        assertThat(result.available()).isFalse();
        assertThat(result.snapshotDate()).isEqualTo("2026-08-16");
    }

    @Test
    void emitsCurrentStateCandidateWithAnObservedReason() throws Exception {
        DailySnapshotEntity snapshot = snapshot(objectMapper.readTree("""
                {"item_equipment": [
                  {"item_equipment_part":"무기","item_equipment_slot":"무기","item_name":"검","item_starforce":"0"}
                ]}
                """));

        var result = service.fromSnapshot(snapshot);

        assertThat(result.upgradeCandidates()).singleElement()
                .extracting(candidate -> candidate.category(), candidate -> candidate.reason())
                .containsExactly("스타포스", "현재 스타포스가 0이라 우선 검토 후보입니다.");
    }

    @Test
    void keepsTheFirstRowWhenAnActiveSlotIsDuplicated() throws Exception {
        DailySnapshotEntity snapshot = snapshot(objectMapper.readTree("""
                {"item_equipment": [
                  {"item_equipment_part": "무기", "item_equipment_slot": "무기", "item_name": "첫 무기"},
                  {"item_equipment_part": "무기", "item_equipment_slot": "무기", "item_name": "중복 무기"}
                ]}
                """));

        var result = service.fromSnapshot(snapshot);

        assertThat(result.items()).singleElement().extracting(item -> item.name()).isEqualTo("첫 무기");
    }

    private DailySnapshotEntity snapshot(com.fasterxml.jackson.databind.JsonNode equipment) {
        DailySnapshotEntity snapshot = new DailySnapshotEntity();
        snapshot.setSnapshotDate(LocalDate.of(2026, 8, 16));
        snapshot.setCapturedAt(OffsetDateTime.parse("2026-08-16T04:00:00+09:00"));
        snapshot.setRawEquipmentJson(equipment);
        return snapshot;
    }
}
