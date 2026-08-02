package com.maple.growth.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.maple.growth.entity.CharacterEntity;
import com.maple.growth.entity.DailySnapshotEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailySnapshotRepository extends JpaRepository<DailySnapshotEntity, Long> {
    Optional<DailySnapshotEntity> findByCharacterAndSnapshotDate(CharacterEntity character, LocalDate snapshotDate);

    Optional<DailySnapshotEntity> findFirstByCharacterAndSnapshotDateLessThanOrderBySnapshotDateDescIdDesc(
            CharacterEntity character,
            LocalDate snapshotDate
    );

    Optional<DailySnapshotEntity> findFirstByCharacterOrderBySnapshotDateDescIdDesc(CharacterEntity character);

    List<DailySnapshotEntity> findByCharacterAndSnapshotDateBetweenOrderBySnapshotDateAsc(
            CharacterEntity character,
            LocalDate startDate,
            LocalDate endDate
    );

    List<DailySnapshotEntity> findTop7ByCharacterAndSnapshotDateLessThanEqualOrderBySnapshotDateDescIdDesc(
            CharacterEntity character,
            LocalDate endDate
    );

    List<DailySnapshotEntity> findByCharacterIdOrderBySnapshotDateDescIdDesc(UUID characterId);
}
