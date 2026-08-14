package com.maple.growth.dto.api;

import java.util.List;

public record GrowthHistoryDto(String range, String metric, boolean hasEnoughSnapshots, List<ChartPointDto> points) {
}
