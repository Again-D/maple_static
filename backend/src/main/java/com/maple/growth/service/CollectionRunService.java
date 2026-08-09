package com.maple.growth.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.maple.growth.domain.CollectionRunStatus;
import com.maple.growth.domain.CollectionRunTrigger;
import com.maple.growth.entity.CollectionRunEntity;
import com.maple.growth.entity.CharacterEntity;
import com.maple.growth.repository.CollectionRunRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CollectionRunService {

    private final CollectionRunRepository collectionRunRepository;
    private final CollectionRetryQueueService retryQueueService;
    private final KstClock kstClock;

    @Transactional
    public UUID start(CollectionRunTrigger trigger, int targetCount) {
        return collectionRunRepository.save(new CollectionRunEntity(trigger, targetCount, kstClock.now())).getId();
    }

    @Transactional
    public void setTargetCount(UUID runId, int targetCount) {
        CollectionRunEntity run = requireRun(runId);
        run.setTargetCount(targetCount);
    }

    @Transactional
    public void recordSuccess(UUID runId) {
        CollectionRunEntity run = requireRun(runId);
        run.setSuccessCount(run.getSuccessCount() + 1);
    }

    @Transactional
    public boolean recordFailure(UUID runId, CharacterEntity character, NexonApiException exception) {
        CollectionRunEntity run = requireRun(runId);
        run.setFailureCount(run.getFailureCount() + 1);
        boolean queued = exception.isRetryable() && character != null;
        if (queued) {
            retryQueueService.enqueue(character, run, exception);
            run.setRetryQueuedCount(run.getRetryQueuedCount() + 1);
        }
        return queued;
    }

    @Transactional
    public void recordFailure(UUID runId, boolean retryQueued) {
        CollectionRunEntity run = requireRun(runId);
        run.setFailureCount(run.getFailureCount() + 1);
        if (retryQueued) {
            run.setRetryQueuedCount(run.getRetryQueuedCount() + 1);
        }
    }

    @Transactional
    public void recordFailure(UUID runId) {
        CollectionRunEntity run = requireRun(runId);
        run.setFailureCount(run.getFailureCount() + 1);
    }

    @Transactional
    public void finish(UUID runId) {
        CollectionRunEntity run = requireRun(runId);
        if (run.getStatus() != CollectionRunStatus.RUNNING) {
            return;
        }
        if (run.getFailureCount() == 0) {
            run.setStatus(CollectionRunStatus.COMPLETED);
        } else if (run.getSuccessCount() > 0) {
            run.setStatus(CollectionRunStatus.PARTIALLY_FAILED);
        } else {
            run.setStatus(CollectionRunStatus.FAILED);
        }
        run.setCompletedAt(kstClock.now());
    }

    @Transactional
    public void skip(String reason) {
        CollectionRunEntity run = new CollectionRunEntity(CollectionRunTrigger.SCHEDULED, 0, kstClock.now());
        run.setStatus(CollectionRunStatus.SKIPPED);
        run.setSkipReason(reason);
        run.setCompletedAt(kstClock.now());
        collectionRunRepository.save(run);
    }

    @Transactional(readOnly = true)
    public List<CollectionRunEntity> recentRuns(int limit) {
        return collectionRunRepository.findByOrderByStartedAtDesc(
                PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "startedAt"))
        );
    }

    @Transactional(readOnly = true)
    public CollectionRunEntity require(UUID id) {
        return requireRun(id);
    }

    private CollectionRunEntity requireRun(UUID runId) {
        return collectionRunRepository.findById(runId)
                .orElseThrow(() -> new IllegalStateException("Collection run does not exist: " + runId));
    }
}
