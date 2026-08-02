package com.maple.growth.dto.api;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ChartPointDto(
        LocalDate snapshotDate,
        Long combatPower,
        Integer level,
        BigDecimal expRate,
        Integer unionLevel,
        Integer hexaMatrixLevelSum
) {
}
