package com.maple.growth.dto.api;

import java.util.Map;

public record GrowthEventDto(
        Long id,
        String eventDate,
        String eventType,
        int importanceLevel,
        String title,
        String description,
        Map<String, Object> detail
) {
}
