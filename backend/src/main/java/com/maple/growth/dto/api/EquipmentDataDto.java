package com.maple.growth.dto.api;

import java.util.List;

public record EquipmentDataDto(
        List<EquipmentItemDto> items,
        String snapshotDate,
        String capturedAt,
        boolean available
) {
}
