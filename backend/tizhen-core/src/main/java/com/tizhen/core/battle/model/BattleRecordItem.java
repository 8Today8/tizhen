package com.tizhen.core.battle.model;

import com.tizhen.core.account.model.UserBrief;

import java.time.Instant;

/**
 * 我的对战记录列表项。resultForMe 已按当前用户角色映射为胜/负/平。
 */
public record BattleRecordItem(
        String roomId,
        String roomCode,
        UserBrief opponent,
        BattleRecordProblem problem,
        String resultForMe,
        String endReason,
        Instant startedAt,
        Instant endedAt,
        Long durationSeconds
) {
}
