package com.maple.growth.dto.api;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.LocalDate;

public record SnapshotSummaryDto(
        Long snapshotId,
        LocalDate snapshotDate,
        Integer level,
        Long exp,
        BigDecimal expRate,
        Long combatPower,
        Integer unionLevel,
        Integer unionArtifactLevel,
        Integer hexaMatrixLevelSum,
        OffsetDateTime capturedAt
) {
}
