package com.tizhen.app.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tizhen.common.api.ApiResponse;
import com.tizhen.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 未认证请求统一返回 401 JSON，避免 Spring Security 默认的 HTML/重定向行为。
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authenticationException) throws IOException {
        boolean sessionExpired = Boolean.TRUE.equals(
                request.getAttribute(AdminSessionAuthenticationFilter.SESSION_EXPIRED_ATTRIBUTE))
                || Boolean.TRUE.equals(
                request.getAttribute(PlayerSessionTimeoutFilter.SESSION_EXPIRED_ATTRIBUTE))
                || (request.getRequestedSessionId() != null && !request.isRequestedSessionIdValid());
        ErrorCode errorCode = sessionExpired
                ? ErrorCode.AUTH_SESSION_EXPIRED
                : ErrorCode.COMMON_UNAUTHENTICATED;

        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(
                response.getOutputStream(),
                ApiResponse.failure(errorCode, errorCode.getDefaultMessage(), null)
        );
    }
}
