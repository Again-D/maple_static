package com.maple.growth.controller;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import com.maple.growth.config.OperationsProperties;
import com.maple.growth.dto.api.ApiErrorCode;
import com.maple.growth.dto.api.ApiResponse;
import com.maple.growth.dto.api.CollectionOperationsStatusDto;
import com.maple.growth.service.CollectionOperationsService;
import com.maple.growth.service.KstClock;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/operations")
public class OperationsController {

    private static final String OPERATIONS_TOKEN_HEADER = "X-Operations-Token";

    private final CollectionOperationsService collectionOperationsService;
    private final OperationsProperties operationsProperties;
    private final KstClock kstClock;

    @GetMapping("/collections")
    public ResponseEntity<ApiResponse<CollectionOperationsStatusDto>> getCollectionStatus(
            @RequestHeader(value = OPERATIONS_TOKEN_HEADER, required = false) String token,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit
    ) {
        if (!matchesConfiguredToken(token)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.failure(
                    ApiErrorCode.OPERATIONS_ACCESS_DENIED,
                    "운영 API 접근 권한이 없습니다.",
                    false,
                    kstClock.now(),
                    kstClock.zoneId().getId()
            ));
        }
        return ResponseEntity.ok(ApiResponse.success(
                    collectionOperationsService.status(limit),
                    kstClock.now(),
                    kstClock.zoneId().getId()
        ));
    }

    private boolean matchesConfiguredToken(String token) {
        if (token == null) {
            return false;
        }
        return MessageDigest.isEqual(
                token.getBytes(StandardCharsets.UTF_8),
                operationsProperties.operationsApiToken().getBytes(StandardCharsets.UTF_8)
        );
    }
}
