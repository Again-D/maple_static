package com.maple.growth.entity;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "growth_event_logs",
        uniqueConstraints = @UniqueConstraint(name = "uq_growth_event_snapshot_type_key", columnNames = {"snapshot_id", "event_type", "event_key"})
)
public class GrowthEventLogEntity {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "character_id", nullable = false)
    private CharacterEntity character;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "snapshot_id", nullable = false)
    private DailySnapshotEntity snapshot;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "event_key", nullable = false, length = 255)
    private String eventKey;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "detail_json")
    private JsonNode detailJson;

    @Column(name = "importance_level", nullable = false)
    private int importanceLevel;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public GrowthEventLogEntity(CharacterEntity character, DailySnapshotEntity snapshot, LocalDate eventDate, String eventType, String eventKey) {
        this.character = character;
        this.snapshot = snapshot;
        this.eventDate = eventDate;
        this.eventType = eventType;
        this.eventKey = eventKey;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }
}
