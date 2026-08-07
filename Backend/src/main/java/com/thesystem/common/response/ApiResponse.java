package com.thesystem.common.response;

import java.time.Instant;

public record ApiResponse<T>(
        Boolean success,
        String message,
        T data,
        Instant timestamp,
        String requestId,
        ErrorInfo error
) {
    public static <T> ApiResponse<T> ok(T data, String message, String requestId) {
        return new ApiResponse<>(true, message, data, Instant.now(), requestId, null);
    }

    public static <T> ApiResponse<T> ok(T data, String requestId) {
        return new ApiResponse<>(true, null, data, Instant.now(), requestId, null);
    }

    public static <T> ApiResponse<T> error(String code, String message, String requestId) {
        return new ApiResponse<>(false, null, null, Instant.now(), requestId, new ErrorInfo(code, message));
    }

    public static <T> ApiResponse<T> created(T data, String message, String requestId) {
        return new ApiResponse<>(true, message, data, Instant.now(), requestId, null);
    }

    public record ErrorInfo(String code, String message) {
    }
}
