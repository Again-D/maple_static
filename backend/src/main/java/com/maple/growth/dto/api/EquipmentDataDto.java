package com.maple.growth.dto.api;

import java.util.List;

public record EquipmentDataDto(
        List<EquipmentItemDto> items,
        String snapshotDate,
        String capturedAt,
        boolean available,
        List<EquipmentUpgradeCandidateDto> upgradeCandidates
) {
    public EquipmentDataDto(List<EquipmentItemDto> items, String snapshotDate, String capturedAt, boolean available) {
        this(items, snapshotDate, capturedAt, available, List.of());
    }
}
