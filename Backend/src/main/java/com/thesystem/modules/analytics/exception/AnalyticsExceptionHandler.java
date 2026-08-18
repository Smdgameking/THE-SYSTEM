package com.thesystem.modules.analytics.exception;

import com.thesystem.common.constants.ErrorCodes;
import com.thesystem.common.exception.BusinessException;
import com.thesystem.common.response.ApiResponse;
import com.thesystem.modules.analytics.dto.AnalyticsOverviewResponse;
import com.thesystem.modules.analytics.dto.AnalyticsTrendsResponse;
import com.thesystem.modules.analytics.dto.AiUsageAnalyticsResponse;
import com.thesystem.modules.analytics.dto.MemoryUsageAnalyticsResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.UUID;

@Slf4j
@RestControllerAdvice(basePackages = "com.thesystem.modules.analytics")
public class AnalyticsExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusinessException(BusinessException ex) {
        String requestId = UUID.randomUUID().toString();
        HttpStatus status = switch (ex.getErrorCode()) {
            case ErrorCodes.NOT_FOUND -> HttpStatus.NOT_FOUND;
            case ErrorCodes.CONFLICT -> HttpStatus.CONFLICT;
            case ErrorCodes.UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case ErrorCodes.FORBIDDEN -> HttpStatus.FORBIDDEN;
            default -> HttpStatus.BAD_REQUEST;
        };
        return ResponseEntity.status(status)
                .body(ApiResponse.error(ex.getErrorCode(), ex.getMessage(), requestId));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGenericException(Exception ex) {
        String requestId = UUID.randomUUID().toString();
        log.error("Unhandled exception in analytics module", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("INTERNAL_ERROR", "An unexpected error occurred", requestId));
    }
}