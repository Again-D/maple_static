package com.maple.growth.dto.api;

public record ApiError(String code, String message, boolean retryable) {
}
