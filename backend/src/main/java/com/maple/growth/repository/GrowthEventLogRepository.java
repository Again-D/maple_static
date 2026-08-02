package com.maple.growth.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.maple.growth.entity.CharacterEntity;
import com.maple.growth.entity.DailySnapshotEntity;
import com.maple.growth.entity.GrowthEventLogEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GrowthEventLogRepository extends JpaRepository<GrowthEventLogEntity, Long> {
    List<GrowthEventLogEntity> findByCharacterOrderByEventDateDescIdDesc(CharacterEntity character, Pageable pageable);

    List<GrowthEventLogEntity> findByCharacterOrderByEventDateDescIdDesc(CharacterEntity character);

    void deleteBySnapshot(DailySnapshotEntity snapshot);

    void deleteBySnapshotAndEventTypeIn(DailySnapshotEntity snapshot, Collection<String> eventTypes);

    List<GrowthEventLogEntity> findByCharacterIdAndEventDateBetweenOrderByEventDateDescIdDesc(
            UUID characterId,
            LocalDate startDate,
            LocalDate endDate
    );
}
