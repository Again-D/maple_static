package com.maple.growth.dto.api;

public record DashboardResponseDto(
        CharacterProfileDto profile,
        SnapshotSummaryDto latestSnapshot,
        SyncStateDto syncState,
        GrowthSummaryDto summary,
        GrowthHistoryDto chart,
        TimelineDto timeline
) {
}
