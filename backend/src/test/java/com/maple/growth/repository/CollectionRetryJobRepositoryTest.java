package com.maple.growth.repository;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import com.maple.growth.domain.CollectionRetryJobStatus;
import com.maple.growth.domain.CollectionRunTrigger;
import com.maple.growth.entity.CharacterEntity;
import com.maple.growth.entity.CollectionRetryJobEntity;
import com.maple.growth.entity.CollectionRunEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:collection_retry_repository;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
class CollectionRetryJobRepositoryTest {

    @Autowired
    CollectionRetryJobRepository retryJobRepository;

    @Autowired
    CharacterRepository characterRepository;

    @Autowired
    CollectionRunRepository collectionRunRepository;

    @Test
    void claimPendingChangesOnlyOneDueJobToClaimed() {
        CollectionRetryJobEntity job = saveJob(OffsetDateTime.now(ZoneOffset.UTC));
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        int claimed = retryJobRepository.claimPending(
                job.getId(),
                CollectionRetryJobStatus.PENDING,
                CollectionRetryJobStatus.CLAIMED,
                now,
                now,
                UUID.randomUUID()
        );

        assertThat(claimed).isEqualTo(1);
        assertThat(retryJobRepository.findById(job.getId()).orElseThrow().getStatus())
                .isEqualTo(CollectionRetryJobStatus.CLAIMED);
        assertThat(retryJobRepository.claimPending(
                job.getId(), CollectionRetryJobStatus.PENDING, CollectionRetryJobStatus.CLAIMED, now, now, UUID.randomUUID()
        )).isZero();
    }

    @Test
    void reclaimExpiredClaimReturnsJobToPending() {
        CollectionRetryJobEntity job = saveJob(OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(20));
        OffsetDateTime claimedAt = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(20);
        job.setStatus(CollectionRetryJobStatus.CLAIMED);
        job.setClaimedAt(claimedAt);
        retryJobRepository.saveAndFlush(job);

        int reclaimed = retryJobRepository.reclaimExpiredClaims(
                CollectionRetryJobStatus.CLAIMED,
                CollectionRetryJobStatus.PENDING,
                OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(10),
                OffsetDateTime.now(ZoneOffset.UTC)
        );

        assertThat(reclaimed).isEqualTo(1);
        CollectionRetryJobEntity reclaimedJob = retryJobRepository.findById(job.getId()).orElseThrow();
        assertThat(reclaimedJob.getStatus()).isEqualTo(CollectionRetryJobStatus.PENDING);
        assertThat(reclaimedJob.getClaimedAt()).isNull();
    }

    private CollectionRetryJobEntity saveJob(OffsetDateTime nextAttemptAt) {
        CharacterEntity character = characterRepository.save(new CharacterEntity(
                "ocid-" + nextAttemptAt.toEpochSecond(),
                "Aries" + nextAttemptAt.toEpochSecond(),
                "루나",
                "나이트로드",
                "male",
                "img"
        ));
        CollectionRunEntity run = collectionRunRepository.save(new CollectionRunEntity(
                CollectionRunTrigger.SCHEDULED,
                1,
                nextAttemptAt
        ));
        return retryJobRepository.saveAndFlush(new CollectionRetryJobEntity(character, run, nextAttemptAt));
    }
}
