package com.maple.growth.scheduler;

import com.maple.growth.entity.CharacterEntity;
import com.maple.growth.repository.CharacterRepository;
import com.maple.growth.service.SnapshotSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DailySnapshotScheduler {

    private final CharacterRepository characterRepository;
    private final SnapshotSyncService snapshotSyncService;

    @Scheduled(cron = "${app.snapshot-cron:0 0 4 * * *}", zone = "${app.timezone:Asia/Seoul}")
    public void collectDailySnapshots() {
        var characters = characterRepository.findAll().stream().filter(CharacterEntity::isAutoTrack).toList();
        int successCount = 0;
        int failureCount = 0;
        log.info("Daily snapshot collection started: targetCount={}", characters.size());
        for (CharacterEntity character : characters) {
            try {
                snapshotSyncService.refresh(character.getCharacterName());
                successCount++;
            } catch (Exception exception) {
                failureCount++;
                log.warn("Daily snapshot failed: characterName={}, error={}", character.getCharacterName(), exception.getMessage());
            }
        }
        log.info("Daily snapshot collection finished: targetCount={}, successCount={}, failureCount={}", characters.size(), successCount, failureCount);
    }
}
