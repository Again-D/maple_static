package com.maple.growth.dto.api;

import java.math.BigDecimal;
import java.util.List;

public record BossDamageAnalysisDto(
        String catalogVersion,
        String catalogReviewedAt,
        String catalogSource,
        BigDecimal bossDamagePercent,
        BigDecimal ignoreDefensePercent,
        boolean available,
        String limitations,
        List<BossMultiplierDto> bosses
) {
}
