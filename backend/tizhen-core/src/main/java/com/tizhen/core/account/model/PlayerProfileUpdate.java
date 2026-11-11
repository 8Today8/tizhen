package com.tizhen.core.account.model;

import java.util.Set;

/**
 * 选手资料的部分更新命令。presentFields 记录客户端实际提交的字段，
 * 以区分未提交与显式传 null（清空手机号/头像/学校）。
 */
public record PlayerProfileUpdate(
        String nickname,
        String email,
        String phone,
        String avatarUrl,
        String school,
        Set<String> presentFields
) {

    public PlayerProfileUpdate {
        presentFields = presentFields == null ? Set.of() : Set.copyOf(presentFields);
    }

    public boolean hasField(String field) {
        return presentFields.contains(field);
    }
}
