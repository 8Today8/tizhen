package com.tizhen.core.account.service;

import com.tizhen.core.account.mapper.PlayerStatsMapper;
import com.tizhen.core.account.mapper.UserStatisticsMapper;
import com.tizhen.core.account.model.BattleStatsRow;
import com.tizhen.core.account.model.PlayerStats;
import com.tizhen.core.leaderboard.model.PlayerLeaderboardItem;
import com.tizhen.core.leaderboard.service.PlayerLeaderboardService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 当前选手的个人统计。榜单名次与榜单接口使用同一实时派生口径。
 */
@Service
public class PlayerStatsService {

    private static final String TYPE_OBJECTIVE = "objective";
    private static final String TYPE_SHORT_ANSWER = "short_answer";
    private static final String PERIOD_WEEK = "week";
    private static final String PERIOD_ALL = "all";

    private final UserStatisticsMapper userStatisticsMapper;
    private final PlayerStatsMapper playerStatsMapper;
    private final PlayerLeaderboardService playerLeaderboardService;

    public PlayerStatsService(UserStatisticsMapper userStatisticsMapper,
                              PlayerStatsMapper playerStatsMapper,
                              PlayerLeaderboardService playerLeaderboardService) {
        this.userStatisticsMapper = userStatisticsMapper;
        this.playerStatsMapper = playerStatsMapper;
        this.playerLeaderboardService = playerLeaderboardService;
    }

    @Transactional(readOnly = true)
    public PlayerStats getStats(Long userId) {
        BattleStatsRow battleRow = playerStatsMapper.selectBattleStats(userId);
        PlayerLeaderboardItem weekRank = playerLeaderboardService.getMyRank(userId, PERIOD_WEEK);
        PlayerLeaderboardItem allTimeRank = playerLeaderboardService.getMyRank(userId, PERIOD_ALL);

        return new PlayerStats(
                new PlayerStats.LearningStats(
                        playerStatsMapper.countCompletedPublishedStages(userId),
                        playerStatsMapper.countPublishedStages()),
                new PlayerStats.SubmissionStats(
                        userStatisticsMapper.countSubmissions(userId),
                        userStatisticsMapper.countFirstAc(userId),
                        playerStatsMapper.countSubmissionsByType(userId, TYPE_OBJECTIVE),
                        playerStatsMapper.countSubmissionsByType(userId, TYPE_SHORT_ANSWER)),
                new PlayerStats.WrongBookStats(userStatisticsMapper.countWrongBook(userId)),
                new PlayerStats.BattleStats(
                        valueOrZero(battleRow == null ? null : battleRow.getTotal()),
                        valueOrZero(battleRow == null ? null : battleRow.getWins()),
                        valueOrZero(battleRow == null ? null : battleRow.getLosses()),
                        valueOrZero(battleRow == null ? null : battleRow.getDraws())),
                new PlayerStats.LeaderboardStats(
                        toPeriod(weekRank),
                        toPeriod(allTimeRank)));
    }

    private PlayerStats.LeaderboardPeriod toPeriod(PlayerLeaderboardItem item) {
        if (item == null) {
            return new PlayerStats.LeaderboardPeriod(null, 0);
        }
        return new PlayerStats.LeaderboardPeriod(item.rank(), item.totalScore());
    }

    private long valueOrZero(Long value) {
        return value == null ? 0 : value;
    }
}
