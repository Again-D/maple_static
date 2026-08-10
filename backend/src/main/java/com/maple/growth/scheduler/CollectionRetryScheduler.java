package com.maple.growth.scheduler;

import java.util.List;
import java.util.UUID;

import com.maple.growth.config.CollectionProperties;
import com.maple.growth.domain.CollectionRunTrigger;
import com.maple.growth.service.CollectionRetryQueueService;
import com.maple.growth.service.CollectionRunService;
import com.maple.growth.service.NexonApiException;
import com.maple.growth.service.SnapshotSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CollectionRetryScheduler {

    private final CollectionRetryQueueService retryQueueService;
    private final CollectionRunService collectionRunService;
    private final SnapshotSyncService snapshotSyncService;
    private final CollectionProperties collectionProperties;

    @Scheduled(cron = "${app.collection.retry-cron:0 */15 * * * *}", zone = "${app.timezone:Asia/Seoul}")
    public void processRetries() {
        int reclaimed = retryQueueService.reclaimExpiredClaims();
        List<CollectionRetryQueueService.ClaimedRetryJob> jobs = retryQueueService.claimDueJobs();
        if (jobs.isEmpty()) {
            if (reclaimed > 0) {
                log.info("Collection retry poll reclaimed expired jobs: count={}", reclaimed);
            }
            return;
        }

        UUID runId = collectionRunService.start(CollectionRunTrigger.RETRY, jobs.size());
        log.info("Collection retry run started: runId={}, targetCount={}, reclaimedCount={}, maxAttempts={}",
                runId, jobs.size(), reclaimed, collectionProperties.retryMaxAttempts());
        try {
            for (CollectionRetryQueueService.ClaimedRetryJob job : jobs) {
                try {
                    snapshotSyncService.refresh(job.characterName());
                    retryQueueService.markSucceeded(job.id(), job.claimToken());
                    collectionRunService.recordSuccess(runId);
                } catch (NexonApiException exception) {
                    boolean queued = retryQueueService.markFailed(job.id(), job.claimToken(), exception);
                    collectionRunService.recordFailure(runId, queued);
                    log.warn("Collection retry failed: runId={}, jobId={}, characterName={}, errorCode={}, retryable={}, requeued={}",
                            runId, job.id(), job.characterName(), exception.getErrorCode(), exception.isRetryable(), queued);
                } catch (Exception exception) {
                    retryQueueService.markUnexpectedFailure(job.id(), job.claimToken(), exception);
                    collectionRunService.recordFailure(runId, false);
                    log.warn("Collection retry failed unexpectedly: runId={}, jobId={}, characterName={}, error={}",
                            runId, job.id(), job.characterName(), exception.getMessage());
                }
            }
        } finally {
            collectionRunService.finish(runId);
        }
        log.info("Collection retry run finished: runId={}", runId);
    }
}
