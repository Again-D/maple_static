package com.maple.growth.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.maple.growth.domain.GrowthEventType;
import com.maple.growth.entity.DailySnapshotEntity;
import com.maple.growth.entity.GrowthEventLogEntity;
import com.maple.growth.repository.GrowthEventLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GrowthEventService {

    private final GrowthEventLogRepository growthEventLogRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public int recomputeEvents(DailySnapshotEntity snapshot, DailySnapshotEntity previousSnapshot) {
        growthEventLogRepository.deleteBySnapshot(snapshot);
        if (previousSnapshot == null || previousSnapshot.getSnapshotDate().equals(snapshot.getSnapshotDate())) {
            return 0;
        }
        List<GrowthEventLogEntity> desired = buildEvents(snapshot, previousSnapshot);
        growthEventLogRepository.saveAll(desired);
        return desired.size();
    }

    public List<GrowthEventLogEntity> buildEvents(DailySnapshotEntity current, DailySnapshotEntity previous) {
        List<GrowthEventLogEntity> result = new ArrayList<>();
        if (previous == null || previous.getSnapshotDate().equals(current.getSnapshotDate())) {
            return result;
        }

        if (current.getLevel() > previous.getLevel()) {
            int delta = current.getLevel() - previous.getLevel();
            result.add(buildEvent(current, previous, GrowthEventType.LEVEL_UP,
                    current.getLevel() == previous.getLevel() + 1
                            ? "Lv.%d -> Lv.%d 레벨업".formatted(previous.getLevel(), current.getLevel())
                            : "Lv.%d -> Lv.%d 레벨 상승".formatted(previous.getLevel(), current.getLevel()),
                    "level_up:%d->%d".formatted(previous.getLevel(), current.getLevel()),
                    3,
                    detailNode(values("fromLevel", previous.getLevel(), "toLevel", current.getLevel(), "delta", delta)),
                    "이전 대표 스냅샷 대비 레벨이 상승했습니다."));
        }

        addCombatPowerEvent(result, current, previous);
        addHexaEvent(result, current, previous);
        addUnionEvent(result, current, previous);

        return result;
    }

    private void addCombatPowerEvent(List<GrowthEventLogEntity> result, DailySnapshotEntity current, DailySnapshotEntity previous) {
        if (current.getCombatPower() == null || previous.getCombatPower() == null) {
            return;
        }
        long from = previous.getCombatPower();
        long to = current.getCombatPower();
        long delta = to - from;
        long absDelta = Math.abs(delta);
        boolean ratioThreshold = from > 0 && meetsRatioThreshold(from, absDelta, new BigDecimal("0.0100"));
        if (absDelta < 100_000 && !ratioThreshold) {
            return;
        }
        int importance = absDelta >= 1_000_000 || (from > 0 && meetsRatioThreshold(from, absDelta, new BigDecimal("0.0500"))) ? 2 : 1;
        String direction = delta >= 0 ? "상승" : "하락";
        result.add(buildEvent(current, previous, GrowthEventType.COMBAT_POWER_CHANGE,
                "전투력 %s %s".formatted(formatNumber(absDelta), direction),
                "combat_power:%d->%d".formatted(from, to),
                importance,
                detailNode(values("from", from, "to", to, "delta", delta, "deltaRate", ratio(from, absDelta), "direction", delta >= 0 ? "up" : "down")),
                "%s -> %s".formatted(formatNumber(from), formatNumber(to))));
    }

    private void addHexaEvent(List<GrowthEventLogEntity> result, DailySnapshotEntity current, DailySnapshotEntity previous) {
        if (current.getHexaMatrixLevelSum() == null || previous.getHexaMatrixLevelSum() == null) {
            return;
        }
        int from = previous.getHexaMatrixLevelSum();
        int to = current.getHexaMatrixLevelSum();
        if (to <= from) {
            return;
        }
        int delta = to - from;
        result.add(buildEvent(current, previous, GrowthEventType.HEXA_UPGRADED,
                "헥사 매트릭스 +%d".formatted(delta),
                "hexa_sum:%d->%d".formatted(from, to),
                2,
                detailNode(values("from", from, "to", to, "delta", delta)),
                "%d -> %d".formatted(from, to)));
    }

    private void addUnionEvent(List<GrowthEventLogEntity> result, DailySnapshotEntity current, DailySnapshotEntity previous) {
        boolean levelUp = current.getUnionLevel() != null && previous.getUnionLevel() != null && current.getUnionLevel() > previous.getUnionLevel();
        boolean artifactUp = current.getUnionArtifactLevel() != null && previous.getUnionArtifactLevel() != null && current.getUnionArtifactLevel() > previous.getUnionArtifactLevel();
        if (!levelUp && !artifactUp) {
            return;
        }
        String key = "union:%s:%s->%s:%s".formatted(
                previous.getUnionLevel(),
                previous.getUnionArtifactLevel(),
                current.getUnionLevel(),
                current.getUnionArtifactLevel()
        );
        result.add(buildEvent(current, previous, GrowthEventType.UNION_UPGRADED,
                levelUp && artifactUp ? "유니온 성장 감지" : levelUp ? "유니온 레벨 +%d".formatted(current.getUnionLevel() - previous.getUnionLevel()) : "유니온 아티팩트 +%d".formatted(current.getUnionArtifactLevel() - previous.getUnionArtifactLevel()),
                key,
                2,
                detailNode(values("fromUnionLevel", previous.getUnionLevel(), "toUnionLevel", current.getUnionLevel(), "fromArtifactLevel", previous.getUnionArtifactLevel(), "toArtifactLevel", current.getUnionArtifactLevel())),
                "%s/%s -> %s/%s".formatted(
                        String.valueOf(previous.getUnionLevel()),
                        String.valueOf(previous.getUnionArtifactLevel()),
                        String.valueOf(current.getUnionLevel()),
                        String.valueOf(current.getUnionArtifactLevel()))));
    }

    private GrowthEventLogEntity buildEvent(
            DailySnapshotEntity current,
            DailySnapshotEntity previous,
            GrowthEventType type,
            String title,
            String key,
            int importance,
            ObjectNode detailJson,
            String description
    ) {
        GrowthEventLogEntity event = new GrowthEventLogEntity(current.getCharacter(), current, current.getSnapshotDate(), type.name(), key);
        event.setTitle(title);
        event.setImportanceLevel(importance);
        event.setDescription(description);
        event.setDetailJson(detailJson);
        return event;
    }

    private ObjectNode detailNode(Map<String, Object> values) {
        ObjectNode node = objectMapper.createObjectNode();
        values.forEach((key, value) -> {
            if (value instanceof Integer integer) {
                node.put(key, integer);
            } else if (value instanceof Long longValue) {
                node.put(key, longValue);
            } else if (value instanceof BigDecimal bigDecimal) {
                node.put(key, bigDecimal);
            } else if (value instanceof Double doubleValue) {
                node.put(key, doubleValue);
            } else if (value instanceof Boolean booleanValue) {
                node.put(key, booleanValue);
            } else if (value != null) {
                node.putPOJO(key, value);
            } else {
                node.putNull(key);
            }
        });
        return node;
    }

    private Map<String, Object> values(Object... entries) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (int i = 0; i < entries.length; i += 2) {
            values.put(String.valueOf(entries[i]), entries[i + 1]);
        }
        return values;
    }

    private BigDecimal ratio(long from, long delta) {
        if (from <= 0) {
            return null;
        }
        return BigDecimal.valueOf(delta)
                .divide(BigDecimal.valueOf(from), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    private boolean meetsRatioThreshold(long from, long absDelta, BigDecimal threshold) {
        return BigDecimal.valueOf(absDelta)
                .compareTo(BigDecimal.valueOf(from).multiply(threshold)) >= 0;
    }

    private String formatNumber(long value) {
        return String.format("%,d", value);
    }
}
