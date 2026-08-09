package com.maple.growth.dto.api;

import org.springframework.http.HttpStatus;

public enum ApiErrorCode {
    INVALID_CHARACTER_NAME(HttpStatus.BAD_REQUEST),
    CHARACTER_NOT_FOUND(HttpStatus.NOT_FOUND),
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS),
    NEXON_API_AUTH_FAILED(HttpStatus.BAD_GATEWAY),
    NEXON_API_FAILED(HttpStatus.BAD_GATEWAY),
    NEXON_API_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE),
    OPERATIONS_ACCESS_DENIED(HttpStatus.FORBIDDEN),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus httpStatus;

    ApiErrorCode(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
    }

    public HttpStatus httpStatus() {
        return httpStatus;
    }
}
