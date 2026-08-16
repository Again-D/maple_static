package com.maple.growth.dto.api;

public record DashboardResponseDto(
        CharacterProfileDto profile,
        SnapshotSummaryDto latestSnapshot,
        SyncStateDto syncState,
        GrowthSummaryDto summary,
        GrowthHistoryDto chart,
        TimelineDto timeline,
        EquipmentDataDto equipment
) {
    public DashboardResponseDto(
            CharacterProfileDto profile,
            SnapshotSummaryDto latestSnapshot,
            SyncStateDto syncState,
            GrowthSummaryDto summary,
            GrowthHistoryDto chart,
            TimelineDto timeline
    ) {
        this(profile, latestSnapshot, syncState, summary, chart, timeline, new EquipmentDataDto(java.util.List.of(), null, null, false));
    }
}
