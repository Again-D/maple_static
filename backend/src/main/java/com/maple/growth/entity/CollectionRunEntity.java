package com.maple.growth.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.maple.growth.domain.CollectionRunStatus;
import com.maple.growth.domain.CollectionRunTrigger;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "collection_runs")
public class CollectionRunEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false, length = 30)
    private CollectionRunTrigger triggerType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CollectionRunStatus status;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "target_count", nullable = false)
    private int targetCount;

    @Column(name = "success_count", nullable = false)
    private int successCount;

    @Column(name = "failure_count", nullable = false)
    private int failureCount;

    @Column(name = "retry_queued_count", nullable = false)
    private int retryQueuedCount;

    @Column(name = "skip_reason", length = 100)
    private String skipReason;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public CollectionRunEntity(CollectionRunTrigger triggerType, int targetCount, OffsetDateTime startedAt) {
        this.triggerType = triggerType;
        this.status = CollectionRunStatus.RUNNING;
        this.targetCount = targetCount;
        this.startedAt = startedAt;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = startedAt != null ? startedAt : OffsetDateTime.now();
        }
    }
}
