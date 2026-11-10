package com.tizhen.app.api.admin;

import com.tizhen.common.api.ApiResponse;
import com.tizhen.core.account.model.AdminPrincipal;
import com.tizhen.core.account.model.RoleView;
import com.tizhen.core.account.service.AdminAccountService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 固定角色与权限查看，仅超级管理员可用。
 */
@RestController
@RequestMapping("/api/v1/admin/roles")
public class AdminRoleController {

    private final AdminAccountService adminAccountService;

    public AdminRoleController(AdminAccountService adminAccountService) {
        this.adminAccountService = adminAccountService;
    }

    @GetMapping
    public ApiResponse<Map<String, List<RoleView>>> list(
            @AuthenticationPrincipal AdminPrincipal principal) {
        return ApiResponse.ok(Map.of("items", adminAccountService.listRoles(principal)));
    }
}
