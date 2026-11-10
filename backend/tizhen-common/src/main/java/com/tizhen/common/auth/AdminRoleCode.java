package com.tizhen.common.auth;

import java.util.Arrays;
import java.util.Optional;

/**
 * 后台固定角色编码，与 admin_role.code 及权限矩阵保持一致。
 */
public enum AdminRoleCode {

    SUPER_ADMIN("super_admin", "超级管理员"),
    CONTENT_OPERATOR("content_operator", "内容运营");

    private final String code;
    private final String displayName;

    AdminRoleCode(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static Optional<AdminRoleCode> fromCode(String code) {
        return Arrays.stream(values())
                .filter(role -> role.code.equals(code))
                .findFirst();
    }
}
