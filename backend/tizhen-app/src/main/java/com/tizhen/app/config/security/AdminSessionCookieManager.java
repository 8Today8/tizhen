package com.tizhen.app.config.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

import java.time.Duration;

/**
 * 后台会话 Cookie 读写。始终 HttpOnly，开发环境可按配置关闭 Secure。
 */
@Component
public class AdminSessionCookieManager {

    private final String cookieName;
    private final boolean secure;

    public AdminSessionCookieManager(
            @Value("${tizhen.security.admin.session.cookie-name:TIZHEN_ADMIN_SID}") String cookieName,
            @Value("${tizhen.security.admin.session.cookie-secure:false}") boolean secure) {
        this.cookieName = cookieName;
        this.secure = secure;
    }

    public String resolve(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, cookieName);
        return cookie == null ? null : cookie.getValue();
    }

    public void write(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from(cookieName, token)
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void clear(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(cookieName, "")
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .sameSite("Lax")
                .maxAge(Duration.ZERO)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
