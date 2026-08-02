package com.maple.growth.dto.api;

import java.util.List;

public record EventsResponseDto(List<GrowthEventDto> items, boolean hasMore, String nextCursor) {
}
