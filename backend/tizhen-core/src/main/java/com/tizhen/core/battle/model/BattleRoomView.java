package com.tizhen.core.battle.model;

import java.time.Instant;

/**
 * 房间快照。resultForMe 相对当前请求用户计算；cancelled 房间没有胜负。
 */
public record BattleRoomView(
        String id,
        String roomCode,
        String status,
        BattlePlayerView creator,
        BattlePlayerView opponent,
        String filterDifficulty,
        String filterTag,
        boolean creatorReady,
        boolean opponentReady,
        BattleProblemView problem,
        Instant startedAt,
        Instant endsAt,
        Instant endedAt,
        String resultForMe,
        String endReason,
        Instant createdAt
) {
}
