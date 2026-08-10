package com.maple.growth.service;

import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.maple.growth.config.CollectionProperties;
import com.maple.growth.domain.CollectionRetryJobStatus;
import com.maple.growth.entity.CharacterEntity;
import com.maple.growth.entity.CollectionRetryJobEntity;
import com.maple.growth.entity.CollectionRunEntity;
import com.maple.growth.repository.CollectionRetryJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class CollectionRetryQueueService {

    private static final java.util.Set<CollectionRetryJobStatus> ACTIVE_STATUSES =
            EnumSet.of(CollectionRetryJobStatus.PENDING, CollectionRetryJobStatus.CLAIMED);

    private final CollectionRetryJobRepository retryJobRepository;
    private final CollectionProperties collectionProperties;
    private final KstClock kstClock;
    private final PlatformTransactionManager transactionManager;

    @Transactional
    public CollectionRetryJobEntity enqueue(CharacterEntity character, CollectionRunEntity run, NexonApiException exception) {
        OffsetDateTime now = kstClock.now();
        Optional<CollectionRetryJobEntity> existing = retryJobRepository
                .findFirstByCharacterIdAndStatusIn(character.getId(), ACTIVE_STATUSES);
        CollectionRetryJobEntity job = existing.orElseGet(() -> new CollectionRetryJobEntity(
                character,
                run,
                now.plusSeconds(collectionProperties.retryInitialBackoffSeconds())
        ));
        if (job.getStatus() == CollectionRetryJobStatus.PENDING) {
            job.setNextAttemptAt(earliest(job.getNextAttemptAt(), now.plusSeconds(collectionProperties.retryInitialBackoffSeconds())));
        }
        job.setSourceRun(run);
        job.setLastErrorCode(exception.getErrorCode().name());
        job.setLastErrorMessage(sanitize(exception.getMessage()));
        return retryJobRepository.save(job);
    }

    public int reclaimExpiredClaims() {
        OffsetDateTime now = kstClock.now();
        OffsetDateTime claimedBefore = now.minusSeconds(collectionProperties.retryLeaseSeconds());
        return inTransaction(() -> retryJobRepository.reclaimExpiredClaims(
                CollectionRetryJobStatus.CLAIMED,
                CollectionRetryJobStatus.PENDING,
                claimedBefore,
                now
        ));
    }

    public List<ClaimedRetryJob> claimDueJobs() {
        OffsetDateTime now = kstClock.now();
        List<CollectionRetryJobEntity> candidates = retryJobRepository
                .findByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAscIdAsc(
                        CollectionRetryJobStatus.PENDING,
                        now,
                        PageRequest.of(0, collectionProperties.retryBatchSize())
                );
        return candidates.stream()
                .map(job -> claim(job.getId(), now))
                .flatMap(Optional::stream)
                .toList();
    }

    public void markSucceeded(long jobId, UUID claimToken) {
        inTransaction(() -> {
            CollectionRetryJobEntity job = require(jobId);
            if (!ownsClaim(job, claimToken)) {
                return null;
            }
            job.setStatus(CollectionRetryJobStatus.SUCCEEDED);
            job.setClaimedAt(null);
            job.setClaimToken(null);
            job.setLastAttemptedAt(kstClock.now());
            job.setLastErrorCode(null);
            job.setLastErrorMessage(null);
            retryJobRepository.save(job);
            return null;
        });
    }

    public boolean markFailed(long jobId, UUID claimToken, NexonApiException exception) {
        return inTransaction(() -> {
            CollectionRetryJobEntity job = require(jobId);
            if (!ownsClaim(job, claimToken)) {
                return false;
            }
            int attemptCount = job.getAttemptCount() + 1;
            job.setAttemptCount(attemptCount);
            job.setClaimedAt(null);
            job.setClaimToken(null);
            job.setLastAttemptedAt(kstClock.now());
            job.setLastErrorCode(exception.getErrorCode().name());
            job.setLastErrorMessage(sanitize(exception.getMessage()));
            boolean retryQueued = exception.isRetryable() && attemptCount < collectionProperties.retryMaxAttempts();
            if (retryQueued) {
                job.setStatus(CollectionRetryJobStatus.PENDING);
                long delay = (long) collectionProperties.retryInitialBackoffSeconds() * (1L << Math.min(attemptCount - 1, 10));
                job.setNextAttemptAt(kstClock.now().plusSeconds(delay));
            } else {
                job.setStatus(CollectionRetryJobStatus.DEAD_LETTERED);
            }
            retryJobRepository.save(job);
            return retryQueued;
        });
    }

    public void markUnexpectedFailure(long jobId, UUID claimToken, Exception exception) {
        inTransaction(() -> {
            CollectionRetryJobEntity job = require(jobId);
            if (!ownsClaim(job, claimToken)) {
                return null;
            }
            job.setAttemptCount(job.getAttemptCount() + 1);
            job.setStatus(CollectionRetryJobStatus.DEAD_LETTERED);
            job.setClaimedAt(null);
            job.setClaimToken(null);
            job.setLastAttemptedAt(kstClock.now());
            job.setLastErrorCode("INTERNAL_ERROR");
            job.setLastErrorMessage(sanitize(exception.getMessage()));
            retryJobRepository.save(job);
            return null;
        });
    }

    @Transactional(readOnly = true)
    public long count(CollectionRetryJobStatus status) {
        return retryJobRepository.countByStatus(status);
    }

    private Optional<ClaimedRetryJob> claim(Long jobId, OffsetDateTime now) {
        return inTransaction(() -> {
            UUID claimToken = UUID.randomUUID();
            int claimed = retryJobRepository.claimPending(
                    jobId,
                    CollectionRetryJobStatus.PENDING,
                    CollectionRetryJobStatus.CLAIMED,
                    now,
                    now,
                    claimToken
            );
            if (claimed == 0) {
                return Optional.empty();
            }
            CollectionRetryJobEntity job = require(jobId);
            job.setClaimToken(claimToken);
            return Optional.of(new ClaimedRetryJob(job.getId(), job.getCharacter().getCharacterName(), claimToken));
        });
    }

    private static boolean ownsClaim(CollectionRetryJobEntity job, UUID claimToken) {
        return job.getStatus() == CollectionRetryJobStatus.CLAIMED
                && claimToken.equals(job.getClaimToken());
    }

    private CollectionRetryJobEntity require(long jobId) {
        return retryJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalStateException("Retry job does not exist: " + jobId));
    }

    private <T> T inTransaction(java.util.function.Supplier<T> operation) {
        return new TransactionTemplate(transactionManager).execute(status -> operation.get());
    }

    private static OffsetDateTime earliest(OffsetDateTime first, OffsetDateTime second) {
        return first == null || second.isBefore(first) ? second : first;
    }

    private static String sanitize(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }
        return message.length() <= 255 ? message : message.substring(0, 255);
    }

    public record ClaimedRetryJob(long id, String characterName, UUID claimToken) {
    }
}
