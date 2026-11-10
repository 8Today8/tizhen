package com.tizhen.core.account.model;

import java.util.List;

/**
 * 固定角色与权限。只读。
 */
public record RoleView(
        String id,
        String code,
        String name,
        List<String> permissions
) {
}
