package com.maple.growth.scheduler;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import com.maple.growth.config.AppProperties;
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
    private final AppProperties appProperties;
    private final ReentrantLock runGuard = new ReentrantLock();

    @Scheduled(cron = "${app.snapshot-cron:0 0 4 * * *}", zone = "${app.timezone:Asia/Seoul}")
    public void collectDailySnapshots() {
        if (runGuard.tryLock()) {
            try {
                collectSnapshotsForTrackedCharacters();
            } finally {
                runGuard.unlock();
            }
            return;
        }

        try {
            if (runGuard.tryLock(appProperties.schedulerDuplicateWaitSeconds(), TimeUnit.SECONDS)) {
                runGuard.unlock();
                log.info("Daily snapshot collection skipped: active run completed while waiting");
                return;
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.warn("Daily snapshot collection skipped: interrupted while waiting for an active run");
            return;
        }
        log.warn("Daily snapshot collection skipped: active run exceeded wait limit of {} seconds", appProperties.schedulerDuplicateWaitSeconds());
    }

    private void collectSnapshotsForTrackedCharacters() {
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
