package com.tizhen.core.battle.model;

import com.tizhen.core.account.model.UserBrief;
import com.tizhen.core.judge.model.SubmissionListItem;

import java.time.Instant;
import java.util.List;

/**
 * 对战记录详情。双方提交只展示次数、结果与时间，不返回代码。
 */
public record BattleRecordDetail(
        String roomId,
        String roomCode,
        UserBrief opponent,
        BattleRecordProblem problem,
        String resultForMe,
        String endReason,
        Instant startedAt,
        Instant endedAt,
        Long durationSeconds,
        List<SubmissionListItem> mySubmissions,
        List<SubmissionListItem> opponentSubmissions
) {
}
