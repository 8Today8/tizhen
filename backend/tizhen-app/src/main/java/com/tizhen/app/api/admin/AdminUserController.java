package com.tizhen.app.api.admin;

import com.tizhen.app.api.admin.dto.BatchDeleteUsersRequest;
import com.tizhen.app.api.admin.dto.BatchUpdateUserStatusRequest;
import com.tizhen.app.api.admin.dto.CreateUserRequest;
import com.tizhen.app.api.admin.dto.ResetUserPasswordRequest;
import com.tizhen.app.api.admin.dto.UpdateUserStatusRequest;
import com.tizhen.common.api.ApiResponse;
import com.tizhen.common.api.PageResult;
import com.tizhen.core.account.model.AdminPrincipal;
import com.tizhen.core.account.model.AdminUserDetail;
import com.tizhen.core.account.model.AdminUserSummary;
import com.tizhen.core.account.service.AdminUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * M01 后台用户管理接口。接口契约见 docs/design/04.API接口设计.md §6.1.3。
 */
@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public ApiResponse<PageResult<AdminUserSummary>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(adminUserService.list(keyword, status, page, pageSize));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AdminUserSummary>> create(
            @AuthenticationPrincipal AdminPrincipal principal,
            @Valid @RequestBody CreateUserRequest body,
            HttpServletRequest request) {
        AdminUserSummary user = adminUserService.createUser(
                principal,
                body.username(),
                body.nickname(),
                body.password(),
                body.email(),
                body.phone(),
                body.school(),
                AdminRequestMetadata.of(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(user));
    }

    @GetMapping("/{userId}")
    public ApiResponse<AdminUserDetail> detail(@PathVariable Long userId) {
        return ApiResponse.ok(adminUserService.detail(userId));
    }

    @PatchMapping("/batch/status")
    public ResponseEntity<Void> batchUpdateStatus(
            @AuthenticationPrincipal AdminPrincipal principal,
            @Valid @RequestBody BatchUpdateUserStatusRequest body,
            HttpServletRequest request) {
        adminUserService.updateStatuses(
                principal, body.userIds(), body.status(), AdminRequestMetadata.of(request));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> batchDelete(
            @AuthenticationPrincipal AdminPrincipal principal,
            @Valid @RequestBody BatchDeleteUsersRequest body,
            HttpServletRequest request) {
        adminUserService.batchDelete(
                principal, body.userIds(), AdminRequestMetadata.of(request));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{userId}/reset-password")
    public ResponseEntity<Void> resetPassword(
            @AuthenticationPrincipal AdminPrincipal principal,
            @PathVariable Long userId,
            @Valid @RequestBody ResetUserPasswordRequest body,
            HttpServletRequest request) {
        adminUserService.resetPassword(
                principal, userId, body.newPassword(), AdminRequestMetadata.of(request));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{userId}/status")
    public ApiResponse<AdminUserSummary> updateStatus(
            @AuthenticationPrincipal AdminPrincipal principal,
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserStatusRequest body,
            HttpServletRequest request) {
        return ApiResponse.ok(adminUserService.updateStatus(
                principal, userId, body.status(), AdminRequestMetadata.of(request)));
    }
}
