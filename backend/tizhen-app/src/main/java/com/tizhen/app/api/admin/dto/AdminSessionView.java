package com.tizhen.app.api.admin.dto;

import com.tizhen.core.account.model.AdminPrincipal;

import java.util.List;

/**
 * 后台当前会话视图，不包含密码哈希等敏感字段。
 */
public record AdminSessionView(
        String id,
        String username,
        String displayName,
        String avatarUrl,
        String roleCode,
        String roleName,
        List<String> permissions,
        String csrfToken
) {

    public static AdminSessionView from(AdminPrincipal principal, String csrfToken) {
        return new AdminSessionView(
                String.valueOf(principal.id()),
                principal.username(),
                principal.displayName(),
                principal.avatarUrl(),
                principal.roleCode(),
                principal.roleName(),
                principal.permissions(),
                csrfToken
        );
    }
}
