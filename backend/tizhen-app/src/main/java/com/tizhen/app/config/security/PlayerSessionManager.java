package com.tizhen.app.config.security;

import com.tizhen.core.account.model.PlayerPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * 选手会话生命周期：登录时轮换会话 ID、写入安全上下文并重建 CSRF Token，
 * 退出时同时清理会话、Cookie 与安全上下文。
 */
@Component
public class PlayerSessionManager {

    public static final String LOGIN_AT_ATTRIBUTE = "tizhen.player.loginAt";

    private final SecurityContextRepository securityContextRepository;
    private final CookieCsrfTokenRepository csrfTokenRepository;
    private final String sessionCookieName;
    private final boolean sessionCookieSecure;

    public PlayerSessionManager(
            SecurityContextRepository securityContextRepository,
            @Qualifier("playerCsrfTokenRepository") CookieCsrfTokenRepository csrfTokenRepository,
            @Value("${server.servlet.session.cookie.name:TIZHEN_SID}") String sessionCookieName,
            @Value("${server.servlet.session.cookie.secure:false}") boolean sessionCookieSecure) {
        this.securityContextRepository = securityContextRepository;
        this.csrfTokenRepository = csrfTokenRepository;
        this.sessionCookieName = sessionCookieName;
        this.sessionCookieSecure = sessionCookieSecure;
    }

    public void establish(HttpServletRequest request,
                          HttpServletResponse response,
                          PlayerPrincipal principal) {
        HttpSession session = request.getSession(true);
        // 防止会话固定攻击；登录后旧会话 ID 立即失效。
        request.changeSessionId();

        UsernamePasswordAuthenticationToken authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        principal,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_PLAYER")));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        session.setAttribute(
                FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME,
                principal.username());
        session.setAttribute(LOGIN_AT_ATTRIBUTE, Instant.now().toEpochMilli());
        securityContextRepository.saveContext(context, request, response);
    }

    public String rotateCsrfToken(HttpServletRequest request, HttpServletResponse response) {
        csrfTokenRepository.saveToken(null, request, response);
        CsrfToken newToken = csrfTokenRepository.generateToken(request);
        csrfTokenRepository.saveToken(newToken, request, response);
        return newToken.getToken();
    }

    public void clear(HttpServletRequest request, HttpServletResponse response) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        csrfTokenRepository.saveToken(null, request, response);

        ResponseCookie expiredCookie = ResponseCookie.from(sessionCookieName, "")
                .path("/")
                .maxAge(Duration.ZERO)
                .httpOnly(true)
                .secure(sessionCookieSecure)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, expiredCookie.toString());
    }
}
