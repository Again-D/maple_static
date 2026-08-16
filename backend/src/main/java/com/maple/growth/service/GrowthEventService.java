package com.maple.growth.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
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

    private static final Comparator<ActiveEquipmentSlotKey> ACTIVE_EQUIPMENT_SLOT_KEY_COMPARATOR = Comparator
            .comparing(ActiveEquipmentSlotKey::part)
            .thenComparing(ActiveEquipmentSlotKey::slot);
    private static final Comparator<ItemReplacementChange> ITEM_REPLACEMENT_CHANGE_COMPARATOR = Comparator
            .comparing(ItemReplacementChange::part)
            .thenComparing(ItemReplacementChange::slot)
            .thenComparing(ItemReplacementChange::previousItemName)
            .thenComparing(ItemReplacementChange::currentItemName);

    private final GrowthEventLogRepository growthEventLogRepository;
    private final ObjectMapper objectMapper;

    static Map<ActiveEquipmentSlotKey, ActiveEquipmentRecord> normalizeActiveEquipment(JsonNode rawEquipmentJson) {
        if (rawEquipmentJson == null || !rawEquipmentJson.isObject()) {
            return Map.of();
        }

        JsonNode itemEquipment = rawEquipmentJson.get("item_equipment");
        if (itemEquipment == null || !itemEquipment.isArray()) {
            return Map.of();
        }

        Map<ActiveEquipmentSlotKey, ActiveEquipmentRecord> normalized = new java.util.TreeMap<>(ACTIVE_EQUIPMENT_SLOT_KEY_COMPARATOR);
        for (JsonNode row : itemEquipment) {
            if (row == null || !row.isObject()) {
                continue;
            }
            String part = normalizeRequiredText(row.get("item_equipment_part"));
            String slot = normalizeRequiredText(row.get("item_equipment_slot"));
            String itemName = normalizeRequiredText(row.get("item_name"));
            if (part == null || slot == null || itemName == null) {
                continue;
            }
            ActiveEquipmentSlotKey key = new ActiveEquipmentSlotKey(part, slot);
            normalized.put(key, new ActiveEquipmentRecord(part, slot, itemName));
        }

        if (normalized.isEmpty()) {
            return Map.of();
        }
        return new LinkedHashMap<>(normalized);
    }

    static List<ComparableActiveEquipmentPair> buildComparableActiveEquipmentPairs(JsonNode previousRawEquipmentJson, JsonNode currentRawEquipmentJson) {
        Map<ActiveEquipmentSlotKey, ActiveEquipmentRecord> previous = normalizeActiveEquipment(previousRawEquipmentJson);
        Map<ActiveEquipmentSlotKey, ActiveEquipmentRecord> current = normalizeActiveEquipment(currentRawEquipmentJson);
        if (previous.isEmpty() || current.isEmpty()) {
            return List.of();
        }

        List<ComparableActiveEquipmentPair> pairs = new ArrayList<>();
        for (Map.Entry<ActiveEquipmentSlotKey, ActiveEquipmentRecord> entry : previous.entrySet()) {
            ActiveEquipmentRecord previousRecord = entry.getValue();
            ActiveEquipmentRecord currentRecord = current.get(entry.getKey());
            if (currentRecord == null) {
                continue;
            }
            pairs.add(new ComparableActiveEquipmentPair(
                    entry.getKey().part(),
                    entry.getKey().slot(),
                    previousRecord.itemName(),
                    currentRecord.itemName()
            ));
        }
        return pairs;
    }

    static boolean hasComparableActiveEquipment(JsonNode rawEquipmentJson) {
        return !normalizeActiveEquipment(rawEquipmentJson).isEmpty();
    }

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
        addGroupedItemReplacedEvent(result, current, previous);

        return result;
    }

    private void addGroupedItemReplacedEvent(List<GrowthEventLogEntity> result, DailySnapshotEntity current, DailySnapshotEntity previous) {
        List<ComparableActiveEquipmentPair> comparablePairs = buildComparableActiveEquipmentPairs(
                previous.getRawEquipmentJson(),
                current.getRawEquipmentJson()
        );
        if (comparablePairs.isEmpty()) {
            return;
        }

        List<ItemReplacementChange> changes = comparablePairs.stream()
                .filter(pair -> !pair.previousItemName().equals(pair.currentItemName()))
                .map(pair -> new ItemReplacementChange(pair.part(), pair.slot(), pair.previousItemName(), pair.currentItemName()))
                .sorted(ITEM_REPLACEMENT_CHANGE_COMPARATOR)
                .toList();
        if (changes.isEmpty()) {
            return;
        }

        String eventKey = buildItemReplacementEventKey(changes);
        ObjectNode detailJson = objectMapper.createObjectNode();
        detailJson.put("changeCount", changes.size());
        addCombatPowerContext(detailJson, current, previous);
        detailJson.set("changes", objectMapper.valueToTree(changes.stream()
                .map(change -> comparisonNode(change, previous.getRawEquipmentJson(), current.getRawEquipmentJson()))
                .toList()));
        result.add(buildEvent(
                current,
                previous,
                GrowthEventType.ITEM_REPLACED,
                "장비 교체 %d건".formatted(changes.size()),
                eventKey,
                2,
                detailJson,
                "대표 스냅샷 기준 장착 장비 변경 %d건".formatted(changes.size())
        ));
    }

    private void addCombatPowerContext(ObjectNode detail, DailySnapshotEntity current, DailySnapshotEntity previous) {
        ObjectNode combatPower = objectMapper.createObjectNode();
        Long from = previous.getCombatPower();
        Long to = current.getCombatPower();
        if (from == null || to == null) {
            combatPower.put("status", "unavailable");
            combatPower.put("message", "전투력 변화를 확인할 수 없음");
        } else {
            long delta = to - from;
            combatPower.put("from", from);
            combatPower.put("to", to);
            combatPower.put("delta", delta);
            if (delta == 0) {
                combatPower.put("status", "unchanged");
                combatPower.put("message", "전투력 변화 없음");
            } else if (sameOtherGrowthMetrics(current, previous)) {
                combatPower.put("status", "estimated");
                combatPower.put("estimatedEquipmentContribution", delta);
                combatPower.put("message", "다른 대표 성장 지표 변화가 없어 장비 변경과 함께 기록된 전투력 변화로 추정");
            } else {
                combatPower.put("status", "accompanied");
                combatPower.put("message", "장비 변경과 함께 기록된 전투력 변화");
            }
        }
        detail.set("combatPower", combatPower);
    }

    private boolean sameOtherGrowthMetrics(DailySnapshotEntity current, DailySnapshotEntity previous) {
        return current.getLevel() == previous.getLevel()
                && java.util.Objects.equals(current.getUnionLevel(), previous.getUnionLevel())
                && java.util.Objects.equals(current.getUnionArtifactLevel(), previous.getUnionArtifactLevel())
                && java.util.Objects.equals(current.getHexaMatrixLevelSum(), previous.getHexaMatrixLevelSum());
    }

    private ObjectNode comparisonNode(ItemReplacementChange change, JsonNode previousRaw, JsonNode currentRaw) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("part", change.part());
        node.put("slot", change.slot());
        node.put("previousItemName", change.previousItemName());
        node.put("currentItemName", change.currentItemName());
        node.set("previous", itemComparison(previousRaw, change.part(), change.slot()));
        node.set("current", itemComparison(currentRaw, change.part(), change.slot()));
        return node;
    }

    private ObjectNode itemComparison(JsonNode raw, String part, String slot) {
        ObjectNode result = objectMapper.createObjectNode();
        JsonNode item = findActiveItem(raw, part, slot);
        if (item == null) {
            result.put("available", false);
            return result;
        }
        result.put("available", true);
        copyIfPresent(result, item, "item_name", "name");
        copyIfPresent(result, item, "item_starforce", "starforce");
        copyIfPresent(result, item, "item_potential_option_grade", "potentialGrade");
        copyIfPresent(result, item, "item_additional_potential_option_grade", "additionalPotentialGrade");
        copyObjectIfPresent(result, item, "item_total_option", "totalOptions");
        copyObjectIfPresent(result, item, "item_base_option", "baseOptions");
        copyObjectIfPresent(result, item, "item_add_option", "additionalOptions");
        copyObjectIfPresent(result, item, "item_etc_option", "etcOptions");
        copyObjectIfPresent(result, item, "item_starforce_option", "starforceOptions");
        copyPotentialOptions(result, item, "item_potential_option_", "potentialOptions");
        copyPotentialOptions(result, item, "item_additional_potential_option_", "additionalPotentialOptions");
        return result;
    }

    private JsonNode findActiveItem(JsonNode raw, String part, String slot) {
        if (raw == null || !raw.isObject() || !raw.path("item_equipment").isArray()) return null;
        for (JsonNode item : raw.path("item_equipment")) {
            if (part.equals(normalizeRequiredText(item.get("item_equipment_part")))
                    && slot.equals(normalizeRequiredText(item.get("item_equipment_slot")))) return item;
        }
        return null;
    }

    private void copyIfPresent(ObjectNode target, JsonNode source, String sourceKey, String targetKey) {
        JsonNode value = source.get(sourceKey);
        if (value != null && !value.isNull() && !value.asText().isBlank()) target.put(targetKey, value.asText());
    }

    private void copyObjectIfPresent(ObjectNode target, JsonNode source, String sourceKey, String targetKey) {
        JsonNode value = source.get(sourceKey);
        if (value != null && value.isObject() && value.size() > 0) target.set(targetKey, value.deepCopy());
    }

    private void copyPotentialOptions(ObjectNode target, JsonNode source, String prefix, String targetKey) {
        var values = new ArrayList<String>();
        for (int index = 1; index <= 3; index++) {
            String value = normalizeRequiredText(source.get(prefix + index));
            if (value != null) values.add(value);
        }
        if (!values.isEmpty()) target.set(targetKey, objectMapper.valueToTree(values));
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

    private String buildItemReplacementEventKey(List<ItemReplacementChange> changes) {
        String canonical = changes.stream()
                .map(change -> "%s\u001f%s\u001f%s\u001f%s".formatted(
                        change.part(),
                        change.slot(),
                        change.previousItemName(),
                        change.currentItemName()
                ))
                .reduce((left, right) -> left + "\u001e" + right)
                .orElse("");
        return "item_replaced:" + sha256Hex(canonical);
    }

    private String sha256Hex(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", exception);
        }
    }

    private static String normalizeRequiredText(JsonNode node) {
        if (node == null || !node.isTextual()) {
            return null;
        }
        String value = node.asText().trim();
        if (value.isEmpty()) {
            return null;
        }
        return value;
    }

    record ActiveEquipmentSlotKey(String part, String slot) {
    }

    record ActiveEquipmentRecord(String part, String slot, String itemName) {
    }

    record ComparableActiveEquipmentPair(String part, String slot, String previousItemName, String currentItemName) {
    }

    record ItemReplacementChange(String part, String slot, String previousItemName, String currentItemName) {
    }
}
