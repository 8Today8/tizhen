package com.tizhen.app.api.admin;

import com.tizhen.app.api.admin.dto.AdminLoginRequest;
import com.tizhen.app.api.admin.dto.AdminSessionView;
import com.tizhen.app.api.admin.dto.CsrfTokenView;
import com.tizhen.app.config.security.AdminSessionCookieManager;
import com.tizhen.common.api.ApiResponse;
import com.tizhen.common.api.RequestContext;
import com.tizhen.core.account.model.AdminPrincipal;
import com.tizhen.core.account.model.AuthenticatedAdmin;
import com.tizhen.core.account.model.LoginMetadata;
import com.tizhen.core.account.service.AdminAuthService;
import com.tizhen.core.account.service.AdminSessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * M09 后台认证接口。接口契约见 docs/design/04.API接口设计.md §6.9。
 */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminAuthController {

    private final AdminAuthService adminAuthService;
    private final AdminSessionService adminSessionService;
    private final AdminSessionCookieManager cookieManager;
    private final CookieCsrfTokenRepository csrfTokenRepository;
    private final String csrfCookieName;
    private final String csrfHeaderName;

    public AdminAuthController(
            AdminAuthService adminAuthService,
            AdminSessionService adminSessionService,
            AdminSessionCookieManager cookieManager,
            @Qualifier("adminCsrfTokenRepository") CookieCsrfTokenRepository csrfTokenRepository,
            @Value("${tizhen.security.admin.csrf.cookie-name:ADMIN-XSRF-TOKEN}") String csrfCookieName,
            @Value("${tizhen.security.admin.csrf.header-name:X-XSRF-TOKEN}") String csrfHeaderName) {
        this.adminAuthService = adminAuthService;
        this.adminSessionService = adminSessionService;
        this.cookieManager = cookieManager;
        this.csrfTokenRepository = csrfTokenRepository;
        this.csrfCookieName = csrfCookieName;
        this.csrfHeaderName = csrfHeaderName;
    }

    @GetMapping("/auth/csrf")
    public ApiResponse<CsrfTokenView> csrf(HttpServletRequest request, HttpServletResponse response) {
        CsrfToken token = csrfTokenRepository.generateToken(request);
        csrfTokenRepository.saveToken(token, request, response);
        return ApiResponse.ok(new CsrfTokenView(token.getToken(), csrfHeaderName, csrfCookieName));
    }

    @PostMapping("/auth/login")
    public ApiResponse<AdminSessionView> login(@Valid @RequestBody AdminLoginRequest body,
                                               HttpServletRequest request,
                                               HttpServletResponse response) {
        String previousToken = cookieManager.resolve(request);
        AuthenticatedAdmin authenticated = adminAuthService.login(
                body.username(),
                body.password(),
                metadataOf(request)
        );
        // 登录成功后轮换会话，旧令牌立即失效，避免同一浏览器残留可用的历史会话。
        adminSessionService.delete(previousToken);
        cookieManager.write(response, authenticated.sessionToken());
        String csrfToken = rotateCsrfToken(request, response);
        return ApiResponse.ok(AdminSessionView.from(authenticated.principal(), csrfToken));
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        adminSessionService.delete(cookieManager.resolve(request));
        cookieManager.clear(response);
        csrfTokenRepository.saveToken(null, request, response);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ApiResponse<AdminSessionView> me(@AuthenticationPrincipal AdminPrincipal principal,
                                            CsrfToken csrfToken) {
        return ApiResponse.ok(AdminSessionView.from(principal, csrfToken.getToken()));
    }

    private String rotateCsrfToken(HttpServletRequest request, HttpServletResponse response) {
        csrfTokenRepository.saveToken(null, request, response);
        CsrfToken newToken = csrfTokenRepository.generateToken(request);
        csrfTokenRepository.saveToken(newToken, request, response);
        return newToken.getToken();
    }

    private LoginMetadata metadataOf(HttpServletRequest request) {
        return new LoginMetadata(
                request.getRemoteAddr(),
                request.getHeader(HttpHeaders.USER_AGENT),
                RequestContext.currentRequestId()
        );
    }
}
