package com.maple.growth.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "characters")
public class CharacterEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String ocid;

    @Column(name = "character_name", nullable = false, unique = true, length = 50)
    private String characterName;

    @Column(name = "world_name", nullable = false, length = 50)
    private String worldName;

    @Column(name = "job_name", nullable = false, length = 50)
    private String jobName;

    @Column(name = "character_gender", length = 10)
    private String characterGender;

    @Column(name = "character_image_url")
    private String characterImageUrl;

    @Column(name = "is_auto_track", nullable = false)
    private boolean isAutoTrack = true;

    @Column(name = "last_fetched_at")
    private OffsetDateTime lastFetchedAt;

    @Column(name = "last_sync_attempted_at")
    private OffsetDateTime lastSyncAttemptedAt;

    @Column(name = "last_sync_error_code", length = 50)
    private String lastSyncErrorCode;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public CharacterEntity(String ocid, String characterName, String worldName, String jobName, String characterGender, String characterImageUrl) {
        this.ocid = ocid;
        this.characterName = characterName;
        this.worldName = worldName;
        this.jobName = jobName;
        this.characterGender = characterGender;
        this.characterImageUrl = characterImageUrl;
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
