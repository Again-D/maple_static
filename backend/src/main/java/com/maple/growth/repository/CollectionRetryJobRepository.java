package com.maple.growth.repository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.maple.growth.domain.CollectionRetryJobStatus;
import com.maple.growth.entity.CollectionRetryJobEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CollectionRetryJobRepository extends JpaRepository<CollectionRetryJobEntity, Long> {
    Optional<CollectionRetryJobEntity> findFirstByCharacterIdAndStatusIn(UUID characterId, Collection<CollectionRetryJobStatus> statuses);

    List<CollectionRetryJobEntity> findByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAscIdAsc(
            CollectionRetryJobStatus status,
            OffsetDateTime now,
            Pageable pageable
    );

    long countByStatus(CollectionRetryJobStatus status);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update CollectionRetryJobEntity j set j.status = :claimed, j.claimedAt = :claimedAt, j.claimToken = :claimToken, j.updatedAt = :claimedAt, j.version = j.version + 1 "
            + "where j.id = :id and j.status = :pending and j.nextAttemptAt <= :now")
    int claimPending(
            @Param("id") Long id,
            @Param("pending") CollectionRetryJobStatus pending,
            @Param("claimed") CollectionRetryJobStatus claimed,
            @Param("now") OffsetDateTime now,
            @Param("claimedAt") OffsetDateTime claimedAt,
            @Param("claimToken") UUID claimToken
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update CollectionRetryJobEntity j set j.status = :pending, j.claimedAt = null, j.claimToken = null, j.nextAttemptAt = :now, j.updatedAt = :now, j.version = j.version + 1 "
            + "where j.status = :claimed and j.claimedAt < :claimedBefore")
    int reclaimExpiredClaims(
            @Param("claimed") CollectionRetryJobStatus claimed,
            @Param("pending") CollectionRetryJobStatus pending,
            @Param("claimedBefore") OffsetDateTime claimedBefore,
            @Param("now") OffsetDateTime now
    );
}
