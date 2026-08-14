package com.maple.growth.dto.api;

import java.util.List;

public record TimelineDto(List<GrowthEventDto> events, boolean hasMore, String nextCursor) {
}
