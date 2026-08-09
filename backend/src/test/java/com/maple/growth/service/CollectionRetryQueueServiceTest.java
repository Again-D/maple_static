package com.maple.growth.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.maple.growth.config.AppProperties;
import com.maple.growth.config.CollectionProperties;
import com.maple.growth.domain.CollectionRetryJobStatus;
import com.maple.growth.entity.CharacterEntity;
import com.maple.growth.entity.CollectionRetryJobEntity;
import com.maple.growth.entity.CollectionRunEntity;
import com.maple.growth.domain.CollectionRunTrigger;
import com.maple.growth.repository.CollectionRetryJobRepository;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CollectionRetryQueueServiceTest {

    private final Clock fixedClock = Clock.fixed(Instant.parse("2026-08-02T00:00:00Z"), ZoneOffset.UTC);
    private final AppProperties appProperties = new AppProperties("Asia/Seoul", "0 0 4 * * *", "http://localhost:3000", "https://open.api.nexon.com", 10, 300);
    private final KstClock kstClock = new KstClock(fixedClock, appProperties);
    private final CollectionProperties collectionProperties = new CollectionProperties("0 */15 * * * *", 20, 3, 60, 900);

    @Test
    void enqueueCoalescesAnExistingPendingJob() {
        CollectionRetryJobRepository repository = mock(CollectionRetryJobRepository.class);
        CollectionRetryQueueService service = service(repository);
        CharacterEntity character = character();
        CollectionRunEntity firstRun = new CollectionRunEntity(CollectionRunTrigger.SCHEDULED, 1, kstClock.now());
        firstRun.setId(UUID.randomUUID());
        CollectionRunEntity secondRun = new CollectionRunEntity(CollectionRunTrigger.SCHEDULED, 1, kstClock.now());
        secondRun.setId(UUID.randomUUID());
        CollectionRetryJobEntity existing = new CollectionRetryJobEntity(character, firstRun, kstClock.now().plusSeconds(600));
        existing.setId(10L);
        when(repository.findFirstByCharacterIdAndStatusIn(any(UUID.class), any())).thenReturn(Optional.of(existing));
        when(repository.save(any(CollectionRetryJobEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.enqueue(character, secondRun, new NexonApiException(com.maple.growth.dto.api.ApiErrorCode.RATE_LIMITED, "too many", true));

        assertThat(existing.getStatus()).isEqualTo(CollectionRetryJobStatus.PENDING);
        assertThat(existing.getSourceRun()).isSameAs(secondRun);
        assertThat(existing.getLastErrorCode()).isEqualTo("RATE_LIMITED");
        assertThat(existing.getNextAttemptAt()).isEqualTo(kstClock.now().plusSeconds(60));
    }

    @Test
    void retryableFailureReschedulesUntilMaximumAttemptsThenDeadLetters() {
        CollectionRetryJobRepository repository = mock(CollectionRetryJobRepository.class);
        CollectionRetryQueueService service = service(repository);
        CollectionRetryJobEntity job = pendingJob();
        when(repository.findById(10L)).thenReturn(Optional.of(job));
        when(repository.save(any(CollectionRetryJobEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        NexonApiException failure = new NexonApiException(com.maple.growth.dto.api.ApiErrorCode.RATE_LIMITED, "too many", true);

        UUID claimToken = UUID.randomUUID();
        job.setStatus(CollectionRetryJobStatus.CLAIMED);
        job.setClaimToken(claimToken);
        assertThat(service.markFailed(10L, claimToken, failure)).isTrue();
        assertThat(job.getAttemptCount()).isEqualTo(1);
        assertThat(job.getStatus()).isEqualTo(CollectionRetryJobStatus.PENDING);

        job.setStatus(CollectionRetryJobStatus.CLAIMED);
        job.setClaimToken(claimToken);
        assertThat(service.markFailed(10L, claimToken, failure)).isTrue();
        assertThat(job.getAttemptCount()).isEqualTo(2);
        job.setStatus(CollectionRetryJobStatus.CLAIMED);
        job.setClaimToken(claimToken);
        assertThat(service.markFailed(10L, claimToken, failure)).isFalse();
        assertThat(job.getAttemptCount()).isEqualTo(3);
        assertThat(job.getStatus()).isEqualTo(CollectionRetryJobStatus.DEAD_LETTERED);
    }

    @Test
    void claimUsesConditionalUpdateAndReturnsCharacterNameOnlyAfterClaim() {
        CollectionRetryJobRepository repository = mock(CollectionRetryJobRepository.class);
        CollectionRetryQueueService service = service(repository);
        CollectionRetryJobEntity candidate = pendingJob();
        when(repository.findByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAscIdAsc(any(), any(), any()))
                .thenReturn(List.of(candidate));
        when(repository.claimPending(anyLong(), any(), any(), any(), any(), any(UUID.class))).thenReturn(1);
        when(repository.findById(10L)).thenReturn(Optional.of(candidate));

        List<CollectionRetryQueueService.ClaimedRetryJob> claimed = service.claimDueJobs();

        assertThat(claimed).singleElement().extracting(CollectionRetryQueueService.ClaimedRetryJob::characterName)
                .isEqualTo("Aries92");
        verify(repository).claimPending(
                anyLong(), any(CollectionRetryJobStatus.class), any(CollectionRetryJobStatus.class), any(), any(), any(UUID.class)
        );
    }

    private CollectionRetryQueueService service(CollectionRetryJobRepository repository) {
        return new CollectionRetryQueueService(repository, collectionProperties, kstClock, transactionManager());
    }

    private CollectionRetryJobEntity pendingJob() {
        CharacterEntity character = character();
        CollectionRetryJobEntity job = new CollectionRetryJobEntity(character, null, kstClock.now());
        job.setId(10L);
        return job;
    }

    private CharacterEntity character() {
        CharacterEntity character = new CharacterEntity("ocid-1", "Aries92", "루나", "나이트로드", "male", "img");
        character.setId(UUID.randomUUID());
        return character;
    }

    private PlatformTransactionManager transactionManager() {
        return new AbstractPlatformTransactionManager() {
            @Override
            protected Object doGetTransaction() {
                return new Object();
            }

            @Override
            protected void doBegin(Object transaction, TransactionDefinition definition) {
            }

            @Override
            protected void doCommit(DefaultTransactionStatus status) {
            }

            @Override
            protected void doRollback(DefaultTransactionStatus status) {
            }
        };
    }
}
