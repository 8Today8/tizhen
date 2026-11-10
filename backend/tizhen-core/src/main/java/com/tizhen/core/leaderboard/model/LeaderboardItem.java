package com.tizhen.core.leaderboard.model;

import java.time.Instant;

/**
 * 榜单行。名次使用 RANK()，允许并列后跳号。
 */
public record LeaderboardItem(
        int rank,
        AdminLeaderboardUser user,
        int totalScore,
        int internalSolved,
        int cfSolved,
        int manualSolved,
        String cfVerificationStatus,
        String cfDataSource,
        Instant cfLastSyncAt,
        String manualVerificationStatus,
        Instant manualUpdatedAt
) {
}
