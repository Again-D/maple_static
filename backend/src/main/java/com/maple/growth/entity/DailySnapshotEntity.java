package com.maple.growth.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
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
        name = "daily_snapshots",
        uniqueConstraints = @UniqueConstraint(name = "uq_character_snapshot_date", columnNames = {"character_id", "snapshot_date"})
)
public class DailySnapshotEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "character_id", nullable = false)
    private CharacterEntity character;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(nullable = false)
    private int level;

    @Column(nullable = false)
    private long exp;

    @Column(name = "exp_rate", precision = 7, scale = 4)
    private BigDecimal expRate;

    @Column(name = "combat_power")
    private Long combatPower;

    @Column(name = "union_level")
    private Integer unionLevel;

    @Column(name = "union_artifact_level")
    private Integer unionArtifactLevel;

    @Column(name = "hexa_matrix_level_sum")
    private Integer hexaMatrixLevelSum;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_stat_json")
    private JsonNode rawStatJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_equipment_json")
    private JsonNode rawEquipmentJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_hexa_json")
    private JsonNode rawHexaJson;

    @Column(name = "captured_at", nullable = false)
    private OffsetDateTime capturedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public DailySnapshotEntity(CharacterEntity character, LocalDate snapshotDate) {
        this.character = character;
        this.snapshotDate = snapshotDate;
    }

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (capturedAt == null) {
            capturedAt = now;
        }
    }
}
