package com.tizhen.core.leaderboard.model;

/**
 * 榜单公开展示的选手信息，不包含邮箱、手机号与外部平台账号。
 */
public record PlayerLeaderboardUser(
        String id,
        String nickname,
        String avatarUrl,
        String school
) {
}
