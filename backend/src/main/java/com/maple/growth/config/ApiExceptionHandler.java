package com.maple.growth.config;

import java.time.OffsetDateTime;

import com.maple.growth.dto.api.ApiErrorCode;
import com.maple.growth.dto.api.ApiResponse;
import com.maple.growth.service.KstClock;
import com.maple.growth.service.NexonApiException;
import com.maple.growth.service.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
@RequiredArgsConstructor
public class ApiExceptionHandler {

    private final KstClock kstClock;

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(ValidationException exception) {
        return buildResponse(ApiErrorCode.INVALID_CHARACTER_NAME, exception.getMessage(), false);
    }

    @ExceptionHandler(NexonApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleNexon(NexonApiException exception) {
        return buildResponse(exception.getErrorCode(), exception.getMessage(), exception.isRetryable());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleBeanValidation(MethodArgumentNotValidException exception) {
        return buildResponse(ApiErrorCode.INVALID_CHARACTER_NAME, "요청 값이 올바르지 않습니다.", false);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException exception) {
        return buildResponse(ApiErrorCode.INVALID_CHARACTER_NAME, "요청 값이 올바르지 않습니다.", false);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleFallback(Exception exception) {
        return buildResponse(ApiErrorCode.INTERNAL_ERROR, "알 수 없는 오류가 발생했습니다.", true);
    }

    private ResponseEntity<ApiResponse<Void>> buildResponse(ApiErrorCode code, String message, boolean retryable) {
        HttpStatus status = code.httpStatus();
        OffsetDateTime serverTime = kstClock.now();
        return ResponseEntity.status(status)
                .body(ApiResponse.failure(code, message, retryable, serverTime, kstClock.zoneId().getId()));
    }
}
