package com.tizhen.app.api.admin;

import com.tizhen.app.api.admin.dto.ReviewManualDeclarationRequest;
import com.tizhen.common.api.ApiResponse;
import com.tizhen.common.api.PageResult;
import com.tizhen.core.account.model.AdminPrincipal;
import com.tizhen.core.external.model.AdminManualDeclarationItem;
import com.tizhen.core.external.model.CfSyncAccepted;
import com.tizhen.core.external.service.AdminManualDeclarationService;
import com.tizhen.core.external.service.CfSyncService;
import com.tizhen.core.leaderboard.model.AdminLeaderboardPage;
import com.tizhen.core.leaderboard.service.AdminLeaderboardService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * M05 后台榜单、手动申报记录与 CF 模拟刷新接口。
 */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminLeaderboardController {

    private final AdminLeaderboardService leaderboardService;
    private final AdminManualDeclarationService manualDeclarationService;
    private final CfSyncService cfSyncService;

    public AdminLeaderboardController(
            AdminLeaderboardService leaderboardService,
            AdminManualDeclarationService manualDeclarationService,
            CfSyncService cfSyncService) {
        this.leaderboardService = leaderboardService;
        this.manualDeclarationService = manualDeclarationService;
        this.cfSyncService = cfSyncService;
    }

    @GetMapping("/leaderboard")
    public ApiResponse<AdminLeaderboardPage> leaderboard(
            @RequestParam(required = false) String period,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(leaderboardService.getLeaderboard(period, page, pageSize));
    }

    @GetMapping("/manual-declarations")
    public ApiResponse<PageResult<AdminManualDeclarationItem>> manualDeclarations(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStartDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(manualDeclarationService.list(
                userId, platform, weekStartDate, page, pageSize));
    }

    @PatchMapping("/manual-declarations/{declarationId}/review")
    public ApiResponse<AdminManualDeclarationItem> reviewManualDeclaration(
            @AuthenticationPrincipal AdminPrincipal principal,
            @PathVariable Long declarationId,
            @Valid @RequestBody ReviewManualDeclarationRequest body,
            HttpServletRequest request) {
        return ApiResponse.ok(manualDeclarationService.review(
                principal, declarationId, body.status(), body.note(),
                AdminRequestMetadata.of(request)));
    }

    @PostMapping("/external-accounts/sync-cf")
    public ResponseEntity<ApiResponse<CfSyncAccepted>> syncCf(
            @AuthenticationPrincipal AdminPrincipal principal,
            HttpServletRequest request) {
        CfSyncAccepted accepted = cfSyncService.sync(principal, AdminRequestMetadata.of(request));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.ok(accepted));
    }
}
