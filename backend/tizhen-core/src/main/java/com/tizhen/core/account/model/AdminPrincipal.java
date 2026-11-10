package com.tizhen.core.account.model;

import java.util.List;

/**
 * 已认证管理员快照。只包含授权与展示所需信息，不包含密码哈希。
 */
public record AdminPrincipal(
        Long id,
        String username,
        String displayName,
        String avatarUrl,
        String roleCode,
        String roleName,
        List<String> permissions
) {

    public AdminPrincipal {
        permissions = permissions == null ? List.of() : List.copyOf(permissions);
    }

    public boolean hasPermission(String permission) {
        return permissions.contains("*") || permissions.contains(permission);
    }
}
