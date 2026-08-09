package com.maple.growth.service;

import java.util.List;

import com.maple.growth.domain.CollectionRetryJobStatus;
import com.maple.growth.dto.api.CollectionOperationsStatusDto;
import com.maple.growth.dto.api.CollectionRunSummaryDto;
import com.maple.growth.entity.CollectionRunEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CollectionOperationsService {

    private final CollectionRunService collectionRunService;
    private final CollectionRetryQueueService retryQueueService;

    @Transactional(readOnly = true)
    public CollectionOperationsStatusDto status(int limit) {
        List<CollectionRunSummaryDto> recentRuns = collectionRunService.recentRuns(limit).stream()
                .map(CollectionOperationsService::toSummary)
                .toList();
        return new CollectionOperationsStatusDto(
                recentRuns,
                retryQueueService.count(CollectionRetryJobStatus.PENDING),
                retryQueueService.count(CollectionRetryJobStatus.CLAIMED),
                retryQueueService.count(CollectionRetryJobStatus.SUCCEEDED),
                retryQueueService.count(CollectionRetryJobStatus.DEAD_LETTERED)
        );
    }

    private static CollectionRunSummaryDto toSummary(CollectionRunEntity run) {
        return new CollectionRunSummaryDto(
                run.getId(),
                run.getTriggerType().name(),
                run.getStatus().name(),
                run.getStartedAt(),
                run.getCompletedAt(),
                run.getTargetCount(),
                run.getSuccessCount(),
                run.getFailureCount(),
                run.getRetryQueuedCount(),
                run.getSkipReason()
        );
    }
}
