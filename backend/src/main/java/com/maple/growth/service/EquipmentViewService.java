package com.maple.growth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.maple.growth.dto.api.EquipmentDataDto;
import com.maple.growth.dto.api.EquipmentItemDto;
import com.maple.growth.entity.DailySnapshotEntity;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class EquipmentViewService {

    private static final List<String> OPTION_GROUPS = List.of(
            "item_total_option",
            "item_base_option",
            "item_add_option",
            "item_etc_option",
            "item_starforce_option"
    );

    public EquipmentDataDto fromSnapshot(DailySnapshotEntity snapshot) {
        if (snapshot == null) {
            return new EquipmentDataDto(List.of(), null, null, false);
        }

        JsonNode root = snapshot.getRawEquipmentJson();
        JsonNode activeItems = root == null ? null : root.get("item_equipment");
        if (activeItems == null || !activeItems.isArray()) {
            return new EquipmentDataDto(List.of(), snapshot.getSnapshotDate().toString(), snapshot.getCapturedAt().toString(), false);
        }

        Map<String, EquipmentItemDto> itemsById = new LinkedHashMap<>();
        for (JsonNode item : activeItems) {
            EquipmentItemDto normalized = normalize(item);
            if (normalized != null) {
                itemsById.putIfAbsent(normalized.id(), normalized);
            }
        }
        List<EquipmentItemDto> items = new ArrayList<>(itemsById.values());
        items.sort(Comparator.comparing(EquipmentItemDto::part).thenComparing(EquipmentItemDto::slot));
        return new EquipmentDataDto(items, snapshot.getSnapshotDate().toString(), snapshot.getCapturedAt().toString(), !items.isEmpty());
    }

    private EquipmentItemDto normalize(JsonNode item) {
        if (item == null || !item.isObject()) {
            return null;
        }
        String part = text(item, "item_equipment_part");
        String slot = text(item, "item_equipment_slot");
        String name = text(item, "item_name");
        if (part == null || slot == null || name == null) {
            return null;
        }

        String id = part + ":" + slot;
        return new EquipmentItemDto(
                id,
                part,
                slot,
                name,
                text(item, "item_icon"),
                text(item, "item_shape_icon"),
                text(item, "item_description"),
                text(item, "item_gender"),
                text(item, "item_equipment_level"),
                text(item, "item_starforce"),
                text(item, "item_potential_option_grade"),
                text(item, "item_additional_potential_option_grade"),
                optionMap(item, OPTION_GROUPS.get(0)),
                optionMap(item, OPTION_GROUPS.get(1)),
                optionMap(item, OPTION_GROUPS.get(2)),
                optionMap(item, OPTION_GROUPS.get(3)),
                optionMap(item, OPTION_GROUPS.get(4)),
                optionList(item, "item_potential_option_"),
                optionList(item, "item_additional_potential_option_")
        );
    }

    private static Map<String, String> optionMap(JsonNode item, String field) {
        JsonNode options = item.get(field);
        if (options == null || !options.isObject()) {
            return Map.of();
        }
        Map<String, String> values = new LinkedHashMap<>();
        options.fields().forEachRemaining(entry -> {
            if (entry.getValue() != null && !entry.getValue().isNull()) {
                values.put(entry.getKey(), entry.getValue().asText());
            }
        });
        return Collections.unmodifiableMap(values);
    }

    private static List<String> optionList(JsonNode item, String prefix) {
        List<String> values = new ArrayList<>();
        for (int index = 1; index <= 3; index++) {
            String value = text(item, prefix + index);
            if (value != null) {
                values.add(value);
            }
        }
        return List.copyOf(values);
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || value.asText().isBlank()) {
            return null;
        }
        return value.asText();
    }
}
