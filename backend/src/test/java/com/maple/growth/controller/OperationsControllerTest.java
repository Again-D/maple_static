package com.maple.growth.controller;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import com.maple.growth.config.OperationsProperties;
import com.maple.growth.config.ApiExceptionHandler;
import com.maple.growth.dto.api.CollectionOperationsStatusDto;
import com.maple.growth.dto.api.CollectionRunSummaryDto;
import com.maple.growth.service.CollectionOperationsService;
import com.maple.growth.service.KstClock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OperationsController.class)
@Import(ApiExceptionHandler.class)
class OperationsControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    CollectionOperationsService collectionOperationsService;

    @MockBean
    OperationsProperties operationsProperties;

    @MockBean
    KstClock kstClock;

    @Test
    void missingTokenDoesNotExposeOperationsData() throws Exception {
        when(kstClock.now()).thenReturn(OffsetDateTime.parse("2026-08-02T04:10:00+09:00"));
        when(kstClock.zoneId()).thenReturn(ZoneId.of("Asia/Seoul"));

        mockMvc.perform(get("/api/v1/operations/collections"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("OPERATIONS_ACCESS_DENIED"));
    }

    @Test
    void validTokenReturnsRecentRunsAndRetryCounts() throws Exception {
        OffsetDateTime now = OffsetDateTime.ofInstant(Instant.parse("2026-08-02T04:10:00Z"), ZoneOffset.ofHours(9));
        when(operationsProperties.operationsApiToken()).thenReturn("operations-secret");
        when(kstClock.now()).thenReturn(now);
        when(kstClock.zoneId()).thenReturn(ZoneId.of("Asia/Seoul"));
        when(collectionOperationsService.status(anyInt())).thenReturn(new CollectionOperationsStatusDto(
                List.of(new CollectionRunSummaryDto(
                        UUID.randomUUID(), "SCHEDULED", "PARTIALLY_FAILED", now.minusMinutes(5), now,
                        3, 2, 1, 1, null
                )),
                1,
                0,
                2,
                1
        ));

        mockMvc.perform(get("/api/v1/operations/collections")
                        .header("X-Operations-Token", "operations-secret")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.recentRuns[0].status").value("PARTIALLY_FAILED"))
                .andExpect(jsonPath("$.data.pendingRetryCount").value(1))
                .andExpect(jsonPath("$.data.deadLetteredRetryCount").value(1))
                .andExpect(jsonPath("$.meta.timezone").value("Asia/Seoul"));
    }

}
