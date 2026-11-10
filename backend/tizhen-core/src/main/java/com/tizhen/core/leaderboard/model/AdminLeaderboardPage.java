package com.tizhen.core.leaderboard.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * 后台榜单响应。榜单为实时派生，不保存快照。
 */
public record AdminLeaderboardPage(
        String period,
        LocalDate weekStartDate,
        Instant generatedAt,
        List<LeaderboardItem> items,
        int page,
        int pageSize,
        long total,
        long totalPages
) {
}
