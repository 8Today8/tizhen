package com.tizhen.common.exception;

import com.tizhen.common.api.FieldErrorItem;

import java.util.List;

/**
 * 可识别的业务异常，由全局异常处理器转换为统一失败响应。
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final List<FieldErrorItem> fieldErrors;

    public BusinessException(ErrorCode errorCode) {
        this(errorCode, errorCode.getDefaultMessage(), null);
    }

    public BusinessException(ErrorCode errorCode, String message) {
        this(errorCode, message, null);
    }

    public BusinessException(ErrorCode errorCode, List<FieldErrorItem> fieldErrors) {
        this(errorCode, errorCode.getDefaultMessage(), fieldErrors);
    }

    public BusinessException(ErrorCode errorCode, String message, List<FieldErrorItem> fieldErrors) {
        super(message);
        this.errorCode = errorCode;
        this.fieldErrors = fieldErrors == null ? List.of() : List.copyOf(fieldErrors);
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public List<FieldErrorItem> getFieldErrors() {
        return fieldErrors;
    }
}
