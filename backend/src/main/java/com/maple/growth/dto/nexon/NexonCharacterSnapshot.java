package com.maple.growth.dto.nexon;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;

public record NexonCharacterSnapshot(
        String ocid,
        String name,
        String worldName,
        String jobName,
        String gender,
        String imageUrl,
        int level,
        long exp,
        BigDecimal expRate,
        Long combatPower,
        Integer unionLevel,
        Integer unionArtifactLevel,
        Integer hexaMatrixLevelSum,
        JsonNode rawStatJson,
        JsonNode rawEquipmentJson,
        JsonNode rawHexaJson
) {
}
