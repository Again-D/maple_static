package com.maple.growth.service;

import com.maple.growth.dto.api.ApiErrorCode;

public class NexonApiException extends RuntimeException {
    private final ApiErrorCode errorCode;
    private final boolean retryable;

    public NexonApiException(ApiErrorCode errorCode, String message, boolean retryable) {
        super(message);
        this.errorCode = errorCode;
        this.retryable = retryable;
    }

    public ApiErrorCode getErrorCode() {
        return errorCode;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
