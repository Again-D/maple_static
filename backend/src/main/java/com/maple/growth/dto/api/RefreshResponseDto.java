package com.maple.growth.dto.api;

public record RefreshResponseDto(
        CharacterProfileDto profile,
        SnapshotSummaryDto latestSnapshot,
        SyncStateDto syncState,
        boolean snapshotCreated,
        boolean snapshotUpdated,
        int createdEventCount
) {
}
