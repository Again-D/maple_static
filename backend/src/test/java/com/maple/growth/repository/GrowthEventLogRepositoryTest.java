package com.maple.growth.repository;

import java.time.LocalDate;

import com.maple.growth.entity.CharacterEntity;
import com.maple.growth.entity.DailySnapshotEntity;
import com.maple.growth.entity.GrowthEventLogEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:growth_event_repo;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class GrowthEventLogRepositoryTest {

    @Autowired
    CharacterRepository characterRepository;

    @Autowired
    DailySnapshotRepository dailySnapshotRepository;

    @Autowired
    GrowthEventLogRepository growthEventLogRepository;

    @Test
    void eventKeyUniquenessPreventsDuplicateGeneratedEvents() {
        CharacterEntity character = characterRepository.save(new CharacterEntity("ocid-3", "Aries94", "루나", "히어로", "male", "img"));
        DailySnapshotEntity snapshot = new DailySnapshotEntity(character, LocalDate.of(2026, 8, 2));
        snapshot.setLevel(200);
        snapshot.setExp(1000);
        snapshot.setCapturedAt(java.time.OffsetDateTime.parse("2026-08-02T04:00:00+09:00"));
        DailySnapshotEntity savedSnapshot = dailySnapshotRepository.saveAndFlush(snapshot);

        GrowthEventLogEntity first = new GrowthEventLogEntity(character, savedSnapshot, savedSnapshot.getSnapshotDate(), "LEVEL_UP", "level_up:199->200");
        first.setTitle("Lv.199 -> Lv.200 레벨업");
        first.setImportanceLevel(3);
        growthEventLogRepository.saveAndFlush(first);

        GrowthEventLogEntity duplicate = new GrowthEventLogEntity(character, savedSnapshot, savedSnapshot.getSnapshotDate(), "LEVEL_UP", "level_up:199->200");
        duplicate.setTitle("Lv.199 -> Lv.200 레벨업");
        duplicate.setImportanceLevel(3);

        assertThatThrownBy(() -> growthEventLogRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
