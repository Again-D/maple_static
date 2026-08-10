package com.maple.growth.scheduler;

import java.util.List;
import java.util.UUID;

import com.maple.growth.config.CollectionProperties;
import com.maple.growth.service.CollectionRetryQueueService;
import com.maple.growth.service.CollectionRunService;
import com.maple.growth.service.NexonApiException;
import com.maple.growth.service.SnapshotSyncService;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CollectionRetrySchedulerTest {

    private final CollectionProperties properties = new CollectionProperties("0 */15 * * * *", 20, 3, 60, 900);

    @Test
    void successfulRetryCompletesJobAndRun() {
        CollectionRetryQueueService queue = mock(CollectionRetryQueueService.class);
        CollectionRunService runs = mock(CollectionRunService.class);
        SnapshotSyncService sync = mock(SnapshotSyncService.class);
        UUID claimToken = UUID.randomUUID();
        CollectionRetryQueueService.ClaimedRetryJob job = new CollectionRetryQueueService.ClaimedRetryJob(10L, "Aries92", claimToken);
        UUID runId = UUID.randomUUID();
        when(queue.claimDueJobs()).thenReturn(List.of(job));
        when(runs.start(any(), anyInt())).thenReturn(runId);

        new CollectionRetryScheduler(queue, runs, sync, properties).processRetries();

        verify(sync).refresh("Aries92");
        verify(queue).markSucceeded(10L, claimToken);
        verify(runs).recordSuccess(runId);
        verify(runs).finish(runId);
    }

    @Test
    void retryableFailureRecordsARequeuedAttempt() {
        CollectionRetryQueueService queue = mock(CollectionRetryQueueService.class);
        CollectionRunService runs = mock(CollectionRunService.class);
        SnapshotSyncService sync = mock(SnapshotSyncService.class);
        UUID claimToken = UUID.randomUUID();
        CollectionRetryQueueService.ClaimedRetryJob job = new CollectionRetryQueueService.ClaimedRetryJob(10L, "Aries92", claimToken);
        UUID runId = UUID.randomUUID();
        NexonApiException failure = new NexonApiException(com.maple.growth.dto.api.ApiErrorCode.RATE_LIMITED, "too many", true);
        when(queue.claimDueJobs()).thenReturn(List.of(job));
        when(runs.start(any(), anyInt())).thenReturn(runId);
        when(sync.refresh("Aries92")).thenThrow(failure);
        when(queue.markFailed(10L, claimToken, failure)).thenReturn(true);

        new CollectionRetryScheduler(queue, runs, sync, properties).processRetries();

        verify(queue).markFailed(10L, claimToken, failure);
        verify(runs).recordFailure(runId, true);
        verify(runs).finish(runId);
    }
}
