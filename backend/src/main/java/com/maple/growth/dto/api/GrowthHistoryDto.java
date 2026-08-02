package com.maple.growth.dto.api;

import java.util.List;

public record GrowthHistoryDto(int rangeDays, List<ChartPointDto> points) {
}
