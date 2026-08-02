package com.maple.growth.dto.api;

public record CharacterLookupResponseDto(
        CharacterProfileDto profile,
        SnapshotSummaryDto latestSnapshot,
        SyncStateDto syncState
) {
}
