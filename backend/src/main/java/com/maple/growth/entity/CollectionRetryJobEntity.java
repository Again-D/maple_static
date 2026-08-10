package com.maple.growth.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.maple.growth.domain.CollectionRetryJobStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "collection_retry_jobs")
public class CollectionRetryJobEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "character_id", nullable = false)
    private CharacterEntity character;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_run_id")
    private CollectionRunEntity sourceRun;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CollectionRetryJobStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at", nullable = false)
    private OffsetDateTime nextAttemptAt;

    @Column(name = "claimed_at")
    private OffsetDateTime claimedAt;

    @Column(name = "claim_token")
    private UUID claimToken;

    @Column(name = "last_attempted_at")
    private OffsetDateTime lastAttemptedAt;

    @Column(name = "last_error_code", length = 50)
    private String lastErrorCode;

    @Column(name = "last_error_message", length = 255)
    private String lastErrorMessage;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    public CollectionRetryJobEntity(CharacterEntity character, CollectionRunEntity sourceRun, OffsetDateTime nextAttemptAt) {
        this.character = character;
        this.sourceRun = sourceRun;
        this.status = CollectionRetryJobStatus.PENDING;
        this.nextAttemptAt = nextAttemptAt;
    }

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
