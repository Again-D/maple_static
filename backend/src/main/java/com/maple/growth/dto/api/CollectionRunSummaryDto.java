package com.maple.growth.dto.api;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CollectionRunSummaryDto(
        UUID id,
        String triggerType,
        String status,
        OffsetDateTime startedAt,
        OffsetDateTime completedAt,
        int targetCount,
        int successCount,
        int failureCount,
        int retryQueuedCount,
        String skipReason
) {
}
