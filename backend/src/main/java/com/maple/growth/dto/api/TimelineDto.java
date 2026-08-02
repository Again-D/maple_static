package com.maple.growth.dto.api;

import java.util.List;

public record TimelineDto(List<GrowthEventDto> items, boolean hasMore, String nextCursor) {
}
