package com.tizhen.common.exception;

import com.tizhen.common.api.ApiResponse;
import com.tizhen.common.api.FieldErrorItem;
import com.tizhen.common.api.RequestContext;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

/**
 * 全局异常处理：所有错误都转换为统一响应，禁止向客户端泄漏堆栈与内部细节。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
        log.warn("业务异常 code={}, requestId={}, message={}",
                exception.getErrorCode().getCode(),
                RequestContext.currentRequestId(),
                exception.getMessage());
        return build(exception.getErrorCode(), exception.getMessage(), exception.getFieldErrors());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
        return build(ErrorCode.COMMON_VALIDATION_FAILED,
                ErrorCode.COMMON_VALIDATION_FAILED.getDefaultMessage(),
                toFieldErrors(exception.getBindingResult().getFieldErrors()));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Void>> handleBindException(BindException exception) {
        return build(ErrorCode.COMMON_VALIDATION_FAILED,
                ErrorCode.COMMON_VALIDATION_FAILED.getDefaultMessage(),
                toFieldErrors(exception.getBindingResult().getFieldErrors()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException exception) {
        List<FieldErrorItem> fieldErrors = exception.getConstraintViolations().stream()
                .map(violation -> new FieldErrorItem(
                        violation.getPropertyPath().toString(),
                        defaultIfBlank(violation.getMessage(), "参数不合法")))
                .toList();
        return build(ErrorCode.COMMON_VALIDATION_FAILED,
                ErrorCode.COMMON_VALIDATION_FAILED.getDefaultMessage(),
                fieldErrors);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleHandlerMethodValidation(HandlerMethodValidationException exception) {
        List<FieldErrorItem> fieldErrors = exception.getAllErrors().stream()
                .map(this::toFieldError)
                .toList();
        return build(ErrorCode.COMMON_VALIDATION_FAILED,
                ErrorCode.COMMON_VALIDATION_FAILED.getDefaultMessage(),
                fieldErrors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadableMessage(HttpMessageNotReadableException exception) {
        log.warn("请求体解析失败 requestId={}", RequestContext.currentRequestId());
        return build(ErrorCode.COMMON_INVALID_PARAM, "请求体格式错误", null);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException exception) {
        return build(ErrorCode.COMMON_UNSUPPORTED_MEDIA_TYPE,
                ErrorCode.COMMON_UNSUPPORTED_MEDIA_TYPE.getDefaultMessage(),
                null);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSize(MaxUploadSizeExceededException exception) {
        return build(ErrorCode.COMMON_PAYLOAD_TOO_LARGE,
                ErrorCode.COMMON_PAYLOAD_TOO_LARGE.getDefaultMessage(),
                null);
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingPart(MissingServletRequestPartException exception) {
        return build(ErrorCode.COMMON_VALIDATION_FAILED, "请选择要上传的文件", null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResource(NoResourceFoundException exception) {
        return build(ErrorCode.COMMON_NOT_FOUND, ErrorCode.COMMON_NOT_FOUND.getDefaultMessage(), null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException exception) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.failure(ErrorCode.COMMON_INVALID_PARAM, "请求方法不支持", null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception exception) {
        log.error("未处理异常 requestId={}", RequestContext.currentRequestId(), exception);
        return build(ErrorCode.COMMON_INTERNAL_ERROR,
                ErrorCode.COMMON_INTERNAL_ERROR.getDefaultMessage(),
                null);
    }

    private ResponseEntity<ApiResponse<Void>> build(ErrorCode errorCode,
                                                    String message,
                                                    List<FieldErrorItem> fieldErrors) {
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ApiResponse.failure(errorCode, message, fieldErrors));
    }

    private List<FieldErrorItem> toFieldErrors(List<FieldError> errors) {
        return errors.stream()
                .map(error -> new FieldErrorItem(error.getField(), defaultIfBlank(error.getDefaultMessage(), "参数不合法")))
                .toList();
    }

    private FieldErrorItem toFieldError(MessageSourceResolvable resolvable) {
        return new FieldErrorItem("request", defaultIfBlank(resolvable.getDefaultMessage(), "参数不合法"));
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
