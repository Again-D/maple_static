package com.maple.growth.dto.api;

public record EquipmentUpgradeCandidateDto(
        String itemId,
        String itemName,
        String part,
        String slot,
        String category,
        String reason
) {
}
