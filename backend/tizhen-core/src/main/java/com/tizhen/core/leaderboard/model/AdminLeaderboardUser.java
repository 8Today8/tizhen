package com.tizhen.core.leaderboard.model;

/**
 * 后台榜单中的选手信息，比选手端多出用户名、邮箱与 CF Handle。
 */
public record AdminLeaderboardUser(
        String id,
        String username,
        String nickname,
        String avatarUrl,
        String school,
        String email,
        String cfHandle
) {
}
