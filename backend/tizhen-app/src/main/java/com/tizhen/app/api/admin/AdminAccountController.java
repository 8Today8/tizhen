package com.tizhen.app.api.admin;

import com.tizhen.app.api.admin.dto.CreateAdminAccountRequest;
import com.tizhen.app.api.admin.dto.ResetAdminPasswordRequest;
import com.tizhen.app.api.admin.dto.UpdateAdminStatusRequest;
import com.tizhen.common.api.ApiResponse;
import com.tizhen.common.api.PageResult;
import com.tizhen.core.account.model.AdminAccountView;
import com.tizhen.core.account.model.AdminPrincipal;
import com.tizhen.core.account.service.AdminAccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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

/**
 * 管理员账号管理，所有端点仅超级管理员可用。
 */
@RestController
@RequestMapping("/api/v1/admin/admin-accounts")
public class AdminAccountController {

    private final AdminAccountService adminAccountService;

    public AdminAccountController(AdminAccountService adminAccountService) {
        this.adminAccountService = adminAccountService;
    }

    @GetMapping
    public ApiResponse<PageResult<AdminAccountView>> list(
            @AuthenticationPrincipal AdminPrincipal principal,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String roleCode,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(adminAccountService.listAccounts(
                principal, keyword, status, roleCode, page, pageSize));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AdminAccountView>> create(
            @AuthenticationPrincipal AdminPrincipal principal,
            @Valid @RequestBody CreateAdminAccountRequest body,
            HttpServletRequest request) {
        AdminAccountView account = adminAccountService.createAccount(
                principal,
                body.username(),
                body.displayName(),
                body.password(),
                body.roleCode(),
                AdminRequestMetadata.of(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(account));
    }

    @GetMapping("/{adminId}")
    public ApiResponse<AdminAccountView> detail(
            @AuthenticationPrincipal AdminPrincipal principal,
            @PathVariable Long adminId) {
        return ApiResponse.ok(adminAccountService.getAccount(principal, adminId));
    }

    @PatchMapping("/{adminId}/status")
    public ApiResponse<AdminAccountView> updateStatus(
            @AuthenticationPrincipal AdminPrincipal principal,
            @PathVariable Long adminId,
            @Valid @RequestBody UpdateAdminStatusRequest body,
            HttpServletRequest request) {
        return ApiResponse.ok(adminAccountService.updateStatus(
                principal, adminId, body.status(), AdminRequestMetadata.of(request)));
    }

    @PostMapping("/{adminId}/reset-password")
    public ResponseEntity<Void> resetPassword(
            @AuthenticationPrincipal AdminPrincipal principal,
            @PathVariable Long adminId,
            @Valid @RequestBody ResetAdminPasswordRequest body,
            HttpServletRequest request) {
        adminAccountService.resetPassword(
                principal, adminId, body.newPassword(), AdminRequestMetadata.of(request));
        return ResponseEntity.noContent().build();
    }
}
