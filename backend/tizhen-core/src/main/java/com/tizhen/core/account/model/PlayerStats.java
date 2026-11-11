package com.tizhen.core.account.model;

/**
 * 个人中心与学习记录页共用的当前选手统计。
 */
public record PlayerStats(
        LearningStats learning,
        SubmissionStats submissions,
        WrongBookStats wrongBook,
        BattleStats battles,
        LeaderboardStats leaderboard
) {

    public record LearningStats(long completedStages, long totalStages) {
    }

    public record SubmissionStats(long total,
                                  long ojAcCount,
                                  long objectiveAnswered,
                                  long shortAnswerAnswered) {
    }

    public record WrongBookStats(long activeCount) {
    }

    public record BattleStats(long total, long wins, long losses, long draws) {
    }

    public record LeaderboardStats(LeaderboardPeriod week, LeaderboardPeriod allTime) {
    }

    /**
     * 无成绩时 rank 为 null，score 固定返回 0。
     */
    public record LeaderboardPeriod(Integer rank, long score) {
    }
}
