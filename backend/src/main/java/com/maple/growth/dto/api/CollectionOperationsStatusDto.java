package com.maple.growth.dto.api;

import java.util.List;

public record CollectionOperationsStatusDto(
        List<CollectionRunSummaryDto> recentRuns,
        long pendingRetryCount,
        long claimedRetryCount,
        long succeededRetryCount,
        long deadLetteredRetryCount
) {
}
