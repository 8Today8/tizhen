package com.tizhen.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tizhen.common.exception.ErrorCode;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 全局统一响应包络。成功与失败共用同一结构，失败时 data 为 null；
 * fieldErrors 仅在参数校验失败时出现。
 */
public record ApiResponse<T>(
        boolean success,
        String code,
        String message,
        T data,
        @JsonInclude(JsonInclude.Include.NON_NULL) List<FieldErrorItem> fieldErrors,
        String requestId,
        Instant timestamp
) {

    private static final String OK_CODE = "OK";
    private static final String OK_MESSAGE = "ok";

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, OK_CODE, OK_MESSAGE, data, null, RequestContext.currentRequestId(), now());
    }

    public static ApiResponse<Void> ok() {
        return ok(null);
    }

    public static <T> ApiResponse<T> failure(ErrorCode errorCode, String message, List<FieldErrorItem> fieldErrors) {
        String resolvedMessage = message == null || message.isBlank()
                ? errorCode.getDefaultMessage()
                : message;
        return new ApiResponse<>(
                false,
                errorCode.getCode(),
                resolvedMessage,
                null,
                fieldErrors,
                RequestContext.currentRequestId(),
                now()
        );
    }

    public static ApiResponse<Void> failure(ErrorCode errorCode) {
        return failure(errorCode, errorCode.getDefaultMessage(), null);
    }

    private static Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }
}
