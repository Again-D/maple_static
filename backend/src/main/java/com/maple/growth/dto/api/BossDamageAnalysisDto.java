package com.maple.growth.dto.api;

import java.util.List;

public record BossDamageAnalysisDto(
        String catalogVersion,
        String catalogReviewedAt,
        String catalogSource,
        Integer bossDamagePercent,
        Integer ignoreDefensePercent,
        boolean available,
        String limitations,
        List<BossMultiplierDto> bosses
) {
}
