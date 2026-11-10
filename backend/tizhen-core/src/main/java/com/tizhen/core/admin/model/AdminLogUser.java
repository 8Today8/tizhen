package com.tizhen.core.admin.model;

/**
 * 操作日志中的操作人快照。
 */
public record AdminLogUser(
        String id,
        String username,
        String displayName,
        String roleCode
) {
}
