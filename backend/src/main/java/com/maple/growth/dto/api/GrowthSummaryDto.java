package com.maple.growth.dto.api;

import java.math.BigDecimal;

public record GrowthSummaryDto(
        int rangeDays,
        boolean hasEnoughSnapshots,
        Long combatPowerDelta,
        BigDecimal combatPowerDeltaRate,
        Integer levelFrom,
        Integer levelTo,
        BigDecimal expRateFrom,
        BigDecimal expRateTo,
        Integer unionLevelDelta,
        Integer hexaMatrixLevelDelta,
        int eventCount
) {
}
