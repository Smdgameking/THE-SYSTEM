package com.thesystem.common.response;

public record ApiResponse<T>(
        Integer status,
        String message,
        T data,
        Long timestamp
) {
    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(200, message, data, System.currentTimeMillis());
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(200, null, data, System.currentTimeMillis());
    }

    public static <T> ApiResponse<T> created(T data, String message) {
        return new ApiResponse<>(201, message, data, System.currentTimeMillis());
    }
}
