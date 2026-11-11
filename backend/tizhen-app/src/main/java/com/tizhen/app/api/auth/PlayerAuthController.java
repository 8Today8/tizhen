package com.tizhen.app.api.auth;

import com.tizhen.app.api.auth.dto.PlayerCsrfTokenView;
import com.tizhen.app.api.auth.dto.PlayerLoginRequest;
import com.tizhen.app.api.auth.dto.PlayerRegisterRequest;
import com.tizhen.app.api.auth.dto.UserSessionView;
import com.tizhen.app.config.security.PlayerSessionManager;
import com.tizhen.common.api.ApiResponse;
import com.tizhen.core.account.model.PlayerPrincipal;
import com.tizhen.core.account.model.PlayerProfile;
import com.tizhen.core.account.service.PlayerAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * M01 选手认证接口。接口契约见 docs/design/04.API接口设计.md §6.1.1。
 */
@RestController
@RequestMapping("/api/v1/auth")
public class PlayerAuthController {

    private final PlayerAuthService playerAuthService;
    private final PlayerSessionManager sessionManager;
    private final CookieCsrfTokenRepository csrfTokenRepository;
    private final String csrfCookieName;
    private final String csrfHeaderName;

    public PlayerAuthController(
            PlayerAuthService playerAuthService,
            PlayerSessionManager sessionManager,
            @Qualifier("playerCsrfTokenRepository") CookieCsrfTokenRepository csrfTokenRepository,
            @Value("${tizhen.security.player.csrf.cookie-name:XSRF-TOKEN}") String csrfCookieName,
            @Value("${tizhen.security.player.csrf.header-name:X-XSRF-TOKEN}") String csrfHeaderName) {
        this.playerAuthService = playerAuthService;
        this.sessionManager = sessionManager;
        this.csrfTokenRepository = csrfTokenRepository;
        this.csrfCookieName = csrfCookieName;
        this.csrfHeaderName = csrfHeaderName;
    }

    @GetMapping("/csrf")
    public ApiResponse<PlayerCsrfTokenView> csrf(HttpServletRequest request, HttpServletResponse response) {
        CsrfToken token = csrfTokenRepository.generateToken(request);
        csrfTokenRepository.saveToken(token, request, response);
        return ApiResponse.ok(new PlayerCsrfTokenView(token.getToken(), csrfHeaderName, csrfCookieName));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserSessionView>> register(
            @Valid @RequestBody PlayerRegisterRequest body,
            HttpServletRequest request,
            HttpServletResponse response) {
        PlayerProfile profile = playerAuthService.register(
                body.username(),
                body.nickname(),
                body.password(),
                body.confirmPassword());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(establishSession(profile, request, response)));
    }

    @PostMapping("/login")
    public ApiResponse<UserSessionView> login(
            @Valid @RequestBody PlayerLoginRequest body,
            HttpServletRequest request,
            HttpServletResponse response) {
        PlayerProfile profile = playerAuthService.login(
                body.username(),
                body.password(),
                request.getRemoteAddr());
        return ApiResponse.ok(establishSession(profile, request, response));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        sessionManager.clear(request, response);
        return ResponseEntity.noContent().build();
    }

    private UserSessionView establishSession(PlayerProfile profile,
                                             HttpServletRequest request,
                                             HttpServletResponse response) {
        PlayerPrincipal principal = PlayerPrincipal.from(profile);
        sessionManager.establish(request, response, principal);
        String csrfToken = sessionManager.rotateCsrfToken(request, response);
        return new UserSessionView(profile, csrfToken);
    }
}
