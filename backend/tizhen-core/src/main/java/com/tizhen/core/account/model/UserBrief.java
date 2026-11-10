package com.tizhen.core.account.model;

/**
 * 后台关联展示用的选手简要信息。ID 以字符串输出，避免前端精度丢失。
 */
public record UserBrief(
        String id,
        String username,
        String nickname,
        String avatarUrl,
        String school
) {

    public static UserBrief from(com.tizhen.core.account.entity.AppUser user) {
        return new UserBrief(
                String.valueOf(user.getId()),
                user.getUsername(),
                user.getNickname(),
                user.getAvatarUrl(),
                user.getSchool());
    }
}
