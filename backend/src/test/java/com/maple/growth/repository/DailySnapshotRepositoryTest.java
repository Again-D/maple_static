package com.maple.growth.repository;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.maple.growth.entity.CharacterEntity;
import com.maple.growth.entity.DailySnapshotEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:daily_snapshot_repo;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class DailySnapshotRepositoryTest {

    @Autowired
    CharacterRepository characterRepository;

    @Autowired
    DailySnapshotRepository dailySnapshotRepository;

    @Test
    void uniqueSnapshotPerCharacterAndDateIsEnforced() {
        CharacterEntity character = characterRepository.save(new CharacterEntity("ocid-1", "Aries92", "루나", "나이트로드", "male", "img"));
        LocalDate snapshotDate = LocalDate.of(2026, 8, 2);

        DailySnapshotEntity snapshot = new DailySnapshotEntity(character, snapshotDate);
        snapshot.setLevel(278);
        snapshot.setExp(123456789L);
        snapshot.setExpRate(new BigDecimal("42.1234"));
        snapshot.setCapturedAt(java.time.OffsetDateTime.parse("2026-08-02T04:00:00+09:00"));
        dailySnapshotRepository.saveAndFlush(snapshot);

        DailySnapshotEntity duplicate = new DailySnapshotEntity(character, snapshotDate);
        duplicate.setLevel(279);
        duplicate.setExp(223456789L);
        duplicate.setCapturedAt(java.time.OffsetDateTime.parse("2026-08-02T06:00:00+09:00"));

        assertThatThrownBy(() -> dailySnapshotRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void optionalMetricsCanRemainNull() throws Exception {
        CharacterEntity character = characterRepository.save(new CharacterEntity("ocid-2", "Aries93", "루나", "비숍", "female", "img"));
        DailySnapshotEntity snapshot = new DailySnapshotEntity(character, LocalDate.of(2026, 8, 2));
        snapshot.setLevel(100);
        snapshot.setExp(1_000L);
        snapshot.setExpRate(new BigDecimal("12.3400"));
        snapshot.setCapturedAt(java.time.OffsetDateTime.parse("2026-08-02T04:00:00+09:00"));

        DailySnapshotEntity saved = dailySnapshotRepository.saveAndFlush(snapshot);
        DailySnapshotEntity loaded = dailySnapshotRepository.findById(saved.getId()).orElseThrow();

        assertThat(loaded.getCombatPower()).isNull();
        assertThat(loaded.getUnionLevel()).isNull();
        assertThat(loaded.getUnionArtifactLevel()).isNull();
        assertThat(loaded.getHexaMatrixLevelSum()).isNull();
    }
}
