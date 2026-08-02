package com.maple.growth.dto.api;

import java.util.UUID;

public record CharacterProfileDto(
        UUID id,
        String ocid,
        String name,
        String worldName,
        String jobName,
        String gender,
        String imageUrl,
        boolean isAutoTrack
) {
}
