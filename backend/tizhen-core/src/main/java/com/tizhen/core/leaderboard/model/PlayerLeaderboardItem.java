package com.tizhen.core.leaderboard.model;

import java.time.Instant;

/**
 * 选手端榜单行。分数来源与校验状态用于页面标注，不暴露 CF Handle。
 */
public record PlayerLeaderboardItem(
        int rank,
        PlayerLeaderboardUser user,
        int totalScore,
        int internalSolved,
        int cfSolved,
        int manualSolved,
        String cfVerificationStatus,
        String cfDataSource,
        Instant cfLastSyncAt,
        String manualVerificationStatus,
        Instant manualUpdatedAt,
        boolean isMe
) {
}
