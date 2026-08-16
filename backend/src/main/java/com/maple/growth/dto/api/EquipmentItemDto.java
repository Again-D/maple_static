package com.maple.growth.dto.api;

import java.util.List;
import java.util.Map;

public record EquipmentItemDto(
        String id,
        String part,
        String slot,
        String name,
        String iconUrl,
        String shapeIconUrl,
        String description,
        String gender,
        String equipmentLevel,
        String starforce,
        String potentialGrade,
        String additionalPotentialGrade,
        Map<String, String> totalOptions,
        Map<String, String> baseOptions,
        Map<String, String> additionalOptions,
        Map<String, String> etcOptions,
        Map<String, String> starforceOptions,
        List<String> potentialOptions,
        List<String> additionalPotentialOptions
) {
}
