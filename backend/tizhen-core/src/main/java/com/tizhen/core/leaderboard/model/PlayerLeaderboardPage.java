package com.tizhen.core.leaderboard.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * 选手端榜单分页结果。me 在未登录或无成绩时为 null。
 */
public record PlayerLeaderboardPage(
        String period,
        LocalDate weekStartDate,
        Instant generatedAt,
        List<PlayerLeaderboardItem> items,
        PlayerLeaderboardItem me,
        int page,
        int pageSize,
        long total,
        long totalPages
) {

    public PlayerLeaderboardPage {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
