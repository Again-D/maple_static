package com.maple.growth.dto.api;

import java.time.OffsetDateTime;

public record ApiResponse<T>(boolean success, T data, ApiError error, Meta meta) {

    public static <T> ApiResponse<T> success(T data, OffsetDateTime serverTime, String timezone) {
        return new ApiResponse<>(true, data, null, new Meta(serverTime, timezone));
    }

    public static <T> ApiResponse<T> failure(ApiErrorCode code, String message, boolean retryable, OffsetDateTime serverTime, String timezone) {
        return new ApiResponse<>(false, null, new ApiError(code.name(), message, retryable), new Meta(serverTime, timezone));
    }

    public record Meta(OffsetDateTime serverTime, String timezone) {
    }
}
