package com.maple.growth.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maple.growth.dto.api.BossDamageAnalysisDto;
import com.maple.growth.dto.api.BossMultiplierDto;
import com.maple.growth.entity.DailySnapshotEntity;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class BossDamageAnalysisService {

    private static final String LIMITATIONS = "Nexon 최종 스탯과 버전 고정 방어율 가정으로 계산한 참고용 배율입니다. 스킬, 최종 데미지, 크리티컬, 레벨 보정, 버프, 파티 효과, 페이즈·패턴, 실제 로테이션 DPS는 반영하지 않습니다.";

    private final ObjectMapper objectMapper;
    private final Catalog catalog;

    public BossDamageAnalysisService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.catalog = loadCatalog();
    }

    public BossDamageAnalysisDto analyze(DailySnapshotEntity snapshot) {
        BigDecimal bossDamage = extractPercent(snapshot == null ? null : snapshot.getRawStatJson(), "보스 몬스터 데미지", "보스 공격력", "boss damage");
        BigDecimal ignoreDefense = extractPercent(snapshot == null ? null : snapshot.getRawStatJson(), "방어율 무시", "방어율무시", "ignore defense");
        boolean available = bossDamage != null && ignoreDefense != null;
        List<BossMultiplierDto> bosses = catalog.bosses().stream().map(boss -> {
            if (!available) {
                return new BossMultiplierDto(boss.id(), boss.name(), boss.difficulty(), boss.defenseRate(), null, false, "보스 데미지 또는 방어율 무시 스탯이 없습니다.");
            }
            BigDecimal defenseFactor = BigDecimal.ONE.subtract(
                    boss.defenseRate().divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.ONE.subtract(ignoreDefense.divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP)))
            ).max(BigDecimal.ZERO);
            BigDecimal multiplier = defenseFactor.multiply(BigDecimal.ONE.add(bossDamage.divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP)))
                    .setScale(3, RoundingMode.HALF_UP);
            return new BossMultiplierDto(boss.id(), boss.name(), boss.difficulty(), boss.defenseRate(), multiplier, true, null);
        }).toList();
        return new BossDamageAnalysisDto(catalog.version(), catalog.reviewedAt(), catalog.sourceReference(), bossDamage, ignoreDefense, available, LIMITATIONS, bosses);
    }

    private Catalog loadCatalog() {
        try {
            JsonNode root = objectMapper.readTree(new ClassPathResource("boss-defense-catalog.json").getInputStream());
            String version = requiredText(root, "version");
            String reviewedAt = requiredText(root, "reviewedAt");
            String sourceReference = requiredText(root, "sourceReference");
            JsonNode bossNodes = root.path("bosses");
            validateCatalog(root, bossNodes);
            List<CatalogBoss> bosses = objectMapper.convertValue(bossNodes, objectMapper.getTypeFactory().constructCollectionType(List.class, CatalogBoss.class));
            if (bosses.stream().anyMatch(boss -> boss.id() == null || boss.id().isBlank() || boss.name() == null || boss.name().isBlank() || boss.difficulty() == null || boss.difficulty().isBlank() || boss.defenseRate() == null || boss.defenseRate().compareTo(BigDecimal.ZERO) < 0)) {
                throw new IllegalStateException("보스 방어율 카탈로그 항목이 올바르지 않습니다.");
            }
            return new Catalog(version, reviewedAt, sourceReference, bosses);
        } catch (IOException exception) {
            throw new IllegalStateException("보스 방어율 카탈로그를 읽을 수 없습니다.", exception);
        }
    }

    static void validateCatalog(JsonNode root, JsonNode bossNodes) {
        if (root == null || !root.isObject()) throw new IllegalStateException("보스 방어율 카탈로그 형식이 올바르지 않습니다.");
        if (!bossNodes.isArray() || bossNodes.isEmpty()) throw new IllegalStateException("보스 방어율 카탈로그에 보스가 없습니다.");
        for (JsonNode boss : bossNodes) {
            if (boss.path("id").asText("").isBlank() || boss.path("name").asText("").isBlank() || boss.path("difficulty").asText("").isBlank() || !boss.path("defenseRate").isNumber() || boss.path("defenseRate").decimalValue().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalStateException("보스 방어율 카탈로그 항목이 올바르지 않습니다.");
            }
        }
    }

    private static String requiredText(JsonNode node, String field) {
        String value = node.path(field).asText("");
        if (value.isBlank()) throw new IllegalStateException("보스 방어율 카탈로그 필수 필드가 없습니다: " + field);
        return value;
    }

    private static BigDecimal extractPercent(JsonNode node, String... candidates) {
        if (node == null || !node.path("final_stat").isArray()) return null;
        for (JsonNode item : node.path("final_stat")) {
            String name = item.path("stat_name").asText(item.path("name").asText(""));
            for (String candidate : candidates) {
                if (name.toLowerCase().contains(candidate.toLowerCase())) {
                    String raw = item.path("stat_value").asText(item.path("value").asText(""));
                    try { return new BigDecimal(raw.replace(",", "").replace("%", "")); } catch (NumberFormatException ignored) { return null; }
                }
            }
        }
        return null;
    }

    private record Catalog(String version, String reviewedAt, String sourceReference, List<CatalogBoss> bosses) {}
    private record CatalogBoss(String id, String name, String difficulty, BigDecimal defenseRate) {}
}
