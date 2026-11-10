package com.tizhen.app.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tizhen.common.api.ApiResponse;
import com.tizhen.common.exception.ErrorCode;
import com.tizhen.core.account.model.AdminPrincipal;
import com.tizhen.core.account.service.AdminAuthService;
import com.tizhen.core.account.service.AdminSessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * 从后台独立 Cookie 恢复管理员身份。认证状态只存在于当前请求上下文，
 * 不写入 Servlet 会话，避免与选手会话互相覆盖。
 */
@Component
public class AdminSessionAuthenticationFilter extends OncePerRequestFilter {

    public static final String SESSION_EXPIRED_ATTRIBUTE = "tizhen.admin.sessionExpired";

    private final AdminSessionCookieManager cookieManager;
    private final AdminSessionService sessionService;
    private final AdminAuthService adminAuthService;
    private final ObjectMapper objectMapper;

    public AdminSessionAuthenticationFilter(
            AdminSessionCookieManager cookieManager,
            AdminSessionService sessionService,
            AdminAuthService adminAuthService,
            ObjectMapper objectMapper) {
        this.cookieManager = cookieManager;
        this.sessionService = sessionService;
        this.adminAuthService = adminAuthService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = cookieManager.resolve(request);
            if (token != null) {
                Optional<Long> adminId = sessionService.resolve(token);
                if (adminId.isEmpty()) {
                    request.setAttribute(SESSION_EXPIRED_ATTRIBUTE, true);
                    cookieManager.clear(response);
                } else {
                    Optional<AdminPrincipal> principal = adminAuthService.loadPrincipal(adminId.get());
                    if (principal.isEmpty()) {
                        sessionService.delete(token);
                        cookieManager.clear(response);
                        writeAccountDisabled(response);
                        return;
                    }
                    authenticate(principal.get());
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(AdminPrincipal principal) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + principal.roleCode().toUpperCase(Locale.ROOT)));
        principal.permissions().stream()
                .map(SimpleGrantedAuthority::new)
                .forEach(authorities::add);

        UsernamePasswordAuthenticationToken authentication =
                UsernamePasswordAuthenticationToken.authenticated(principal, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void writeAccountDisabled(HttpServletResponse response) throws IOException {
        response.setStatus(ErrorCode.ADMIN_ACCOUNT_DISABLED.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(
                response.getOutputStream(),
                ApiResponse.failure(ErrorCode.ADMIN_ACCOUNT_DISABLED,
                        ErrorCode.ADMIN_ACCOUNT_DISABLED.getDefaultMessage(),
                        null)
        );
    }
}
