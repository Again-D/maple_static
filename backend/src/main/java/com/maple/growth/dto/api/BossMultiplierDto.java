package com.maple.growth.dto.api;

import java.math.BigDecimal;

public record BossMultiplierDto(
        String bossId,
        String bossName,
        String difficulty,
        BigDecimal defenseRate,
        BigDecimal effectiveDamageMultiplier,
        boolean available,
        String unavailableReason
) {
}
