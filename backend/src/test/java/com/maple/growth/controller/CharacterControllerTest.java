package com.maple.growth.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZoneId;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.maple.growth.dto.api.ApiErrorCode;
import com.maple.growth.dto.api.CharacterLookupResponseDto;
import com.maple.growth.dto.api.CharacterProfileDto;
import com.maple.growth.dto.api.ChartPointDto;
import com.maple.growth.dto.api.DashboardResponseDto;
import com.maple.growth.dto.api.EventsResponseDto;
import com.maple.growth.dto.api.GrowthEventDto;
import com.maple.growth.dto.api.GrowthHistoryDto;
import com.maple.growth.dto.api.GrowthSummaryDto;
import com.maple.growth.dto.api.RefreshResponseDto;
import com.maple.growth.dto.api.SnapshotSummaryDto;
import com.maple.growth.dto.api.SyncStateDto;
import com.maple.growth.dto.api.TimelineDto;
import com.maple.growth.service.CharacterLookupService;
import com.maple.growth.service.KstClock;
import com.maple.growth.service.NexonApiException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CharacterController.class)
class CharacterControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    CharacterLookupService characterLookupService;

    @MockBean
    KstClock kstClock;

    @Test
    void characterLookupSuccessIncludesMetaTimezone() throws Exception {
        when(kstClock.now()).thenReturn(OffsetDateTime.ofInstant(Instant.parse("2026-08-02T04:10:00Z"), ZoneOffset.ofHours(9)));
        when(kstClock.zoneId()).thenReturn(ZoneId.of("Asia/Seoul"));
        when(characterLookupService.lookupOrRegister(anyString())).thenReturn(new CharacterLookupResponseDto(
                new CharacterProfileDto(java.util.UUID.randomUUID(), "ocid", "Aries92", "루나", "나이트로드", "male", "img", true),
                new SnapshotSummaryDto(123L, java.time.LocalDate.of(2026, 8, 2), 278, 123L, new BigDecimal("42.1234"), 7420500L, 8500, 42, 135, OffsetDateTime.parse("2026-08-02T04:00:12+09:00")),
                new SyncStateDto("fresh", OffsetDateTime.parse("2026-08-02T04:00:12+09:00"), OffsetDateTime.parse("2026-08-02T04:00:12+09:00"), "오늘 04:00 수집됨")
        ));

        mockMvc.perform(get("/api/v1/characters/Aries92"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.profile.name").value("Aries92"))
                .andExpect(jsonPath("$.meta.timezone").value("Asia/Seoul"))
                .andExpect(jsonPath("$.meta.serverTime").exists());
    }

    @Test
    void dashboardWithInsufficientSnapshotsReturnsHttp200() throws Exception {
        when(kstClock.now()).thenReturn(OffsetDateTime.ofInstant(Instant.parse("2026-08-02T04:10:00Z"), ZoneOffset.ofHours(9)));
        when(kstClock.zoneId()).thenReturn(ZoneId.of("Asia/Seoul"));
        when(characterLookupService.dashboard(anyString())).thenReturn(new DashboardResponseDto(
                new CharacterProfileDto(java.util.UUID.randomUUID(), "ocid", "Aries92", "루나", "나이트로드", "male", "img", true),
                new SnapshotSummaryDto(123L, java.time.LocalDate.of(2026, 8, 2), 278, 123L, new BigDecimal("42.1234"), 7420500L, 8500, 42, 135, OffsetDateTime.parse("2026-08-02T04:00:12+09:00")),
                new SyncStateDto("stale", null, null, "저장된 데이터를 표시합니다."),
                new GrowthSummaryDto(7, false, null, null, null, null, null, null, null, null, 0),
                new GrowthHistoryDto("7d", "combatPower", false, List.of(new ChartPointDto(java.time.LocalDate.of(2026, 8, 2), 7420500L, 278, new BigDecimal("42.1234"), 8500, 135))),
                new TimelineDto(List.of(), false, null)
        ));

        mockMvc.perform(get("/api/v1/characters/Aries92/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.summary.hasEnoughSnapshots").value(false))
                .andExpect(jsonPath("$.data.chart.range").value("7d"))
                .andExpect(jsonPath("$.data.chart.metric").value("combatPower"))
                .andExpect(jsonPath("$.data.chart.hasEnoughSnapshots").value(false));
    }

    @Test
    void dashboardTimelineExposesEventsAndNeverItems() throws Exception {
        when(kstClock.now()).thenReturn(OffsetDateTime.ofInstant(Instant.parse("2026-08-02T04:10:00Z"), ZoneOffset.ofHours(9)));
        when(kstClock.zoneId()).thenReturn(ZoneId.of("Asia/Seoul"));
        Map<String, Object> itemReplacedDetail = Map.of(
                "changeCount", 2,
                "changes", List.of(
                        Map.of("slot", "무기", "previousItemName", "아케인 스태프", "currentItemName", "에테르넬 스태프"),
                        Map.of("slot", "신발", "previousItemName", "아케인 슈즈", "currentItemName", "에테르넬 슈즈")
                )
        );
        when(characterLookupService.dashboard(anyString())).thenReturn(new DashboardResponseDto(
                new CharacterProfileDto(java.util.UUID.randomUUID(), "ocid", "Aries92", "루나", "나이트로드", "male", "img", true),
                new SnapshotSummaryDto(123L, java.time.LocalDate.of(2026, 8, 2), 278, 123L, new BigDecimal("42.1234"), 7420500L, 8500, 42, 135, OffsetDateTime.parse("2026-08-02T04:00:12+09:00")),
                new SyncStateDto("fresh", OffsetDateTime.parse("2026-08-02T04:00:12+09:00"), OffsetDateTime.parse("2026-08-02T04:00:12+09:00"), "오늘 04:00 수집됨"),
                new GrowthSummaryDto(7, true, 120500L, new BigDecimal("1.6300"), 277, 278, new BigDecimal("42.1234"), new BigDecimal("43.0000"), 100, 3, 1),
                new GrowthHistoryDto("7d", "combatPower", true,
                        List.of(new ChartPointDto(java.time.LocalDate.of(2026, 8, 1), 7300000L, 277, new BigDecimal("88.4200"), 8380, 132),
                                new ChartPointDto(java.time.LocalDate.of(2026, 8, 2), 7420500L, 278, new BigDecimal("42.1234"), 8500, 135))),
                new TimelineDto(
                        List.of(new GrowthEventDto(5L, "2026-08-02", "ITEM_REPLACED", 2, "장비 교체 2건", "대표 스냅샷 기준 장착 장비 변경 2건", itemReplacedDetail)),
                        false,
                        null
                )
        ));

        mockMvc.perform(get("/api/v1/characters/Aries92/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.chart.range").value("7d"))
                .andExpect(jsonPath("$.data.chart.metric").value("combatPower"))
                .andExpect(jsonPath("$.data.timeline.events[0].id").value(5))
                .andExpect(jsonPath("$.data.timeline.events[0].eventType").value("ITEM_REPLACED"))
                .andExpect(jsonPath("$.data.timeline.events[0].detail.changeCount").value(2))
                .andExpect(jsonPath("$.data.timeline.events[0].detail.changes[0].slot").value("무기"))
                .andExpect(jsonPath("$.data.timeline.events[0].detail.changes[0].previousItemName").value("아케인 스태프"))
                .andExpect(jsonPath("$.data.timeline.events[0].detail.changes[0].currentItemName").value("에테르넬 스태프"))
                .andExpect(jsonPath("$.data.timeline.items").doesNotExist());
    }

    @Test
    void unsupportedRangeReturns400() throws Exception {
        when(kstClock.now()).thenReturn(OffsetDateTime.ofInstant(Instant.parse("2026-08-02T04:10:00Z"), ZoneOffset.ofHours(9)));
        when(kstClock.zoneId()).thenReturn(ZoneId.of("Asia/Seoul"));

        mockMvc.perform(get("/api/v1/characters/Aries92/growth-history").param("range", "90d"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value(ApiErrorCode.INVALID_CHARACTER_NAME.name()));
    }

    @Test
    void unsupportedMetricReturns400() throws Exception {
        when(kstClock.now()).thenReturn(OffsetDateTime.ofInstant(Instant.parse("2026-08-02T04:10:00Z"), ZoneOffset.ofHours(9)));
        when(kstClock.zoneId()).thenReturn(ZoneId.of("Asia/Seoul"));

        mockMvc.perform(get("/api/v1/characters/Aries92/growth-history").param("metric", "meso"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value(ApiErrorCode.INVALID_CHARACTER_NAME.name()));
    }

    @Test
    void growthHistorySevenDayRangeReturnsExistingPointsOnly() throws Exception {
        when(kstClock.now()).thenReturn(OffsetDateTime.ofInstant(Instant.parse("2026-08-02T04:10:00Z"), ZoneOffset.ofHours(9)));
        when(kstClock.zoneId()).thenReturn(ZoneId.of("Asia/Seoul"));
        when(characterLookupService.growthHistory(any(), eq("7d"), eq("combatPower"), eq(7))).thenReturn(new GrowthHistoryDto(
                "7d",
                "combatPower",
                false,
                List.of(new ChartPointDto(LocalDate.of(2026, 8, 1), 7300000L, 277, new BigDecimal("88.4200"), 8380, 132))
        ));
        when(characterLookupService.requireExisting(anyString())).thenReturn(new com.maple.growth.entity.CharacterEntity("ocid", "Aries92", "루나", "나이트로드", "male", "img"));

        mockMvc.perform(get("/api/v1/characters/Aries92/growth-history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.range").value("7d"))
                .andExpect(jsonPath("$.data.metric").value("combatPower"))
                .andExpect(jsonPath("$.data.hasEnoughSnapshots").value(false))
                .andExpect(jsonPath("$.data.points.length()").value(1))
                .andExpect(jsonPath("$.meta.timezone").value("Asia/Seoul"));
    }

    @Test
    void growthHistorySupportsThirtyDayAndMetricSelection() throws Exception {
        when(kstClock.now()).thenReturn(OffsetDateTime.ofInstant(Instant.parse("2026-08-02T04:10:00Z"), ZoneOffset.ofHours(9)));
        when(kstClock.zoneId()).thenReturn(ZoneId.of("Asia/Seoul"));
        when(characterLookupService.growthHistory(any(), eq("30d"), eq("level"), eq(30))).thenReturn(new GrowthHistoryDto(
                "30d",
                "level",
                true,
                List.of(new ChartPointDto(LocalDate.of(2026, 8, 1), 7300000L, 277, new BigDecimal("88.4200"), 8380, 132),
                        new ChartPointDto(LocalDate.of(2026, 8, 2), 7420500L, 278, new BigDecimal("42.1234"), 8500, 135))
        ));
        when(characterLookupService.requireExisting(anyString())).thenReturn(new com.maple.growth.entity.CharacterEntity("ocid", "Aries92", "루나", "나이트로드", "male", "img"));

        mockMvc.perform(get("/api/v1/characters/Aries92/growth-history").param("range", "30d").param("metric", "level"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.range").value("30d"))
                .andExpect(jsonPath("$.data.metric").value("level"))
                .andExpect(jsonPath("$.data.hasEnoughSnapshots").value(true))
                .andExpect(jsonPath("$.data.points.length()").value(2));
    }

    @Test
    void growthHistorySupportsAllRangeAndHexaMetricSelection() throws Exception {
        when(kstClock.now()).thenReturn(OffsetDateTime.ofInstant(Instant.parse("2026-08-02T04:10:00Z"), ZoneOffset.ofHours(9)));
        when(kstClock.zoneId()).thenReturn(ZoneId.of("Asia/Seoul"));
        when(characterLookupService.growthHistory(any(), eq("all"), eq("hexaMatrixLevelSum"), eq(Integer.MAX_VALUE))).thenReturn(new GrowthHistoryDto(
                "all",
                "hexaMatrixLevelSum",
                true,
                List.of(new ChartPointDto(LocalDate.of(2026, 8, 1), 7300000L, 277, new BigDecimal("88.4200"), 8380, 132),
                        new ChartPointDto(LocalDate.of(2026, 8, 2), 7420500L, 278, new BigDecimal("42.1234"), 8500, 135))
        ));
        when(characterLookupService.requireExisting(anyString())).thenReturn(new com.maple.growth.entity.CharacterEntity("ocid", "Aries92", "루나", "나이트로드", "male", "img"));

        mockMvc.perform(get("/api/v1/characters/Aries92/growth-history").param("range", "all").param("metric", "hexaMatrixLevelSum"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.range").value("all"))
                .andExpect(jsonPath("$.data.metric").value("hexaMatrixLevelSum"))
                .andExpect(jsonPath("$.data.hasEnoughSnapshots").value(true))
                .andExpect(jsonPath("$.data.points.length()").value(2));
    }

    @Test
    void eventsEndpointReturnsLatestFirstAndCursorState() throws Exception {
        when(kstClock.now()).thenReturn(OffsetDateTime.ofInstant(Instant.parse("2026-08-02T04:10:00Z"), ZoneOffset.ofHours(9)));
        when(kstClock.zoneId()).thenReturn(ZoneId.of("Asia/Seoul"));
        when(characterLookupService.events(any(), anyInt())).thenReturn(new EventsResponseDto(
                List.of(
                        new GrowthEventDto(2L, "2026-08-02", "HEXA_UPGRADED", 2, "헥사 매트릭스 +1", null, java.util.Map.of()),
                        new GrowthEventDto(1L, "2026-08-02", "LEVEL_UP", 3, "Lv.277 -> Lv.278 레벨업", null, java.util.Map.of())
                ),
                true,
                "1"
        ));
        when(characterLookupService.requireExisting(anyString())).thenReturn(new com.maple.growth.entity.CharacterEntity("ocid", "Aries92", "루나", "나이트로드", "male", "img"));

        mockMvc.perform(get("/api/v1/characters/Aries92/events").param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.events[0].id").value(2))
                .andExpect(jsonPath("$.data.hasMore").value(true))
                .andExpect(jsonPath("$.data.nextCursor").value("1"));
    }

    @Test
    void itemReplacedEventSerializesGroupedDetailChanges() throws Exception {
        when(kstClock.now()).thenReturn(OffsetDateTime.ofInstant(Instant.parse("2026-08-02T04:10:00Z"), ZoneOffset.ofHours(9)));
        when(kstClock.zoneId()).thenReturn(ZoneId.of("Asia/Seoul"));
        Map<String, Object> itemReplacedDetail = Map.of(
                "changeCount", 2,
                "changes", List.of(
                        Map.of("slot", "무기", "previousItemName", "아케인 스태프", "currentItemName", "에테르넬 스태프"),
                        Map.of("slot", "신발", "previousItemName", "아케인 슈즈", "currentItemName", "에테르넬 슈즈")
                )
        );
        when(characterLookupService.events(any(), anyInt())).thenReturn(new EventsResponseDto(
                List.of(new GrowthEventDto(5L, "2026-08-02", "ITEM_REPLACED", 2, "장비 교체 2건", "대표 스냅샷 기준 장착 장비 변경 2건", itemReplacedDetail)),
                false,
                null
        ));
        when(characterLookupService.requireExisting(anyString())).thenReturn(new com.maple.growth.entity.CharacterEntity("ocid", "Aries92", "루나", "나이트로드", "male", "img"));

        mockMvc.perform(get("/api/v1/characters/Aries92/events").param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.events[0].eventType").value("ITEM_REPLACED"))
                .andExpect(jsonPath("$.data.events[0].importanceLevel").value(2))
                .andExpect(jsonPath("$.data.events[0].detail.changeCount").value(2))
                .andExpect(jsonPath("$.data.events[0].detail.changes.length()").value(2))
                .andExpect(jsonPath("$.data.events[0].detail.changes[0].slot").value("무기"))
                .andExpect(jsonPath("$.data.events[0].detail.changes[0].previousItemName").value("아케인 스태프"))
                .andExpect(jsonPath("$.data.events[0].detail.changes[0].currentItemName").value("에테르넬 스태프"))
                .andExpect(jsonPath("$.data.events[0].detail.changes[1].slot").value("신발"))
                .andExpect(jsonPath("$.data.events[0].detail.changes[1].previousItemName").value("아케인 슈즈"))
                .andExpect(jsonPath("$.data.events[0].detail.changes[1].currentItemName").value("에테르넬 슈즈"))
                .andExpect(jsonPath("$.data.items").doesNotExist());
    }

    @Test
    void invalidLimitReturns400() throws Exception {
        when(kstClock.now()).thenReturn(OffsetDateTime.ofInstant(Instant.parse("2026-08-02T04:10:00Z"), ZoneOffset.ofHours(9)));
        when(kstClock.zoneId()).thenReturn(ZoneId.of("Asia/Seoul"));

        mockMvc.perform(get("/api/v1/characters/Aries92/events").param("limit", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value(ApiErrorCode.INVALID_CHARACTER_NAME.name()));
    }

    @Test
    void notFoundMapsTo404() throws Exception {
        when(kstClock.now()).thenReturn(OffsetDateTime.ofInstant(Instant.parse("2026-08-02T04:10:00Z"), ZoneOffset.ofHours(9)));
        when(kstClock.zoneId()).thenReturn(ZoneId.of("Asia/Seoul"));
        when(characterLookupService.lookupOrRegister(anyString())).thenThrow(new NexonApiException(ApiErrorCode.CHARACTER_NOT_FOUND, "캐릭터를 찾을 수 없습니다.", false));

        mockMvc.perform(get("/api/v1/characters/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value(ApiErrorCode.CHARACTER_NOT_FOUND.name()))
                .andExpect(jsonPath("$.error.retryable").value(false));
    }

    @Test
    void refreshSuccessReturnsSnapshotFlags() throws Exception {
        when(kstClock.now()).thenReturn(OffsetDateTime.ofInstant(Instant.parse("2026-08-02T04:10:00Z"), ZoneOffset.ofHours(9)));
        when(kstClock.zoneId()).thenReturn(ZoneId.of("Asia/Seoul"));
        when(characterLookupService.refresh(anyString())).thenReturn(new RefreshResponseDto(
                new CharacterProfileDto(java.util.UUID.randomUUID(), "ocid", "Aries92", "루나", "나이트로드", "male", "img", true),
                new SnapshotSummaryDto(123L, java.time.LocalDate.of(2026, 8, 2), 278, 123L, new BigDecimal("42.1234"), 7420500L, 8500, 42, 135, OffsetDateTime.parse("2026-08-02T04:00:12+09:00")),
                new SyncStateDto("fresh", OffsetDateTime.parse("2026-08-02T04:00:12+09:00"), OffsetDateTime.parse("2026-08-02T04:00:12+09:00"), "오늘 04:00 수집됨"),
                true,
                false,
                1
        ));

        mockMvc.perform(post("/api/v1/characters/Aries92/refresh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.snapshotCreated").value(true))
                .andExpect(jsonPath("$.data.createdEventCount").value(1));
    }
}
