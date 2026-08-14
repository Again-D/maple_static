package com.maple.growth.dto.api;

import java.util.List;

public record EventsResponseDto(List<GrowthEventDto> events, boolean hasMore, String nextCursor) {
}
