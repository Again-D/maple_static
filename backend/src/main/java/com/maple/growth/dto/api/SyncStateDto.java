package com.maple.growth.dto.api;

import java.time.OffsetDateTime;

public record SyncStateDto(
        String state,
        OffsetDateTime lastSuccessAt,
        OffsetDateTime lastAttemptAt,
        String message
) {
}
