package com.tizhen.app.config.security;

import com.tizhen.core.account.model.PlayerPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

/**
 * 选手会话绝对超时。Spring Session 负责空闲超时，本过滤器补充从登录时刻起算的绝对时限。
 */
@Component
public class PlayerSessionTimeoutFilter extends OncePerRequestFilter {

    public static final String SESSION_EXPIRED_ATTRIBUTE = "tizhen.player.sessionExpired";

    private final Duration absoluteTimeout;

    public PlayerSessionTimeoutFilter(
            @Value("${tizhen.security.player.session.absolute-timeout-seconds:43200}")
            long absoluteTimeoutSeconds) {
        this.absoluteTimeout = Duration.ofSeconds(absoluteTimeoutSeconds);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof PlayerPrincipal) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                Object loginAtValue = session.getAttribute(PlayerSessionManager.LOGIN_AT_ATTRIBUTE);
                long loginAt = loginAtValue instanceof Long value ? value : 0L;
                if (loginAt == 0L) {
                    // 兼容升级前已存在的会话，从本次请求开始计算绝对时限。
                    session.setAttribute(
                            PlayerSessionManager.LOGIN_AT_ATTRIBUTE,
                            Instant.now().toEpochMilli());
                } else if (Instant.ofEpochMilli(loginAt).plus(absoluteTimeout).isBefore(Instant.now())) {
                    session.invalidate();
                    SecurityContextHolder.clearContext();
                    request.setAttribute(SESSION_EXPIRED_ATTRIBUTE, true);
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}
