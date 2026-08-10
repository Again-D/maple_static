package com.maple.growth.scheduler;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.UUID;

import com.maple.growth.config.AppProperties;
import com.maple.growth.domain.CollectionRunTrigger;
import com.maple.growth.entity.CharacterEntity;
import com.maple.growth.repository.CharacterRepository;
import com.maple.growth.service.CollectionRunService;
import com.maple.growth.service.SnapshotSyncService;
import com.maple.growth.service.NexonApiException;
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
    private final CollectionRunService collectionRunService;
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
                collectionRunService.skip("active_run_completed_while_waiting");
                log.info("Daily snapshot collection skipped: active run completed while waiting");
                return;
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            collectionRunService.skip("interrupted_while_waiting");
            log.warn("Daily snapshot collection skipped: interrupted while waiting for an active run");
            return;
        }
        collectionRunService.skip("active_run_wait_limit_exceeded");
        log.warn("Daily snapshot collection skipped: active run exceeded wait limit of {} seconds", appProperties.schedulerDuplicateWaitSeconds());
    }

    private void collectSnapshotsForTrackedCharacters() {
        UUID runId = collectionRunService.start(CollectionRunTrigger.SCHEDULED, 0);
        try {
            var characters = characterRepository.findAll().stream().filter(CharacterEntity::isAutoTrack).toList();
            collectionRunService.setTargetCount(runId, characters.size());
            log.info("Daily snapshot collection started: runId={}, targetCount={}", runId, characters.size());
            for (CharacterEntity character : characters) {
                try {
                    snapshotSyncService.refresh(character.getCharacterName());
                    collectionRunService.recordSuccess(runId);
                } catch (NexonApiException exception) {
                    collectionRunService.recordFailure(runId, character, exception);
                    log.warn("Daily snapshot failed: runId={}, characterName={}, errorCode={}, retryable={}",
                            runId, character.getCharacterName(), exception.getErrorCode(), exception.isRetryable());
                } catch (Exception exception) {
                    collectionRunService.recordFailure(runId);
                    log.warn("Daily snapshot failed: runId={}, characterName={}, error={}",
                            runId, character.getCharacterName(), exception.getMessage());
                }
            }
        } catch (Exception exception) {
            collectionRunService.recordFailure(runId);
            log.warn("Daily snapshot collection failed before character processing: runId={}, error={}", runId, exception.getMessage());
        } finally {
            collectionRunService.finish(runId);
            log.info("Daily snapshot collection finished: runId={}", runId);
        }
    }
}
