package com.tizhen.core.battle.model;

import com.tizhen.core.account.model.UserBrief;

import java.time.Instant;

/**
 * 邀请码预览。不返回题目，避免等待阶段提前泄题。
 */
public record BattleRoomPreview(
        String roomCode,
        String status,
        UserBrief creator,
        UserBrief opponent,
        boolean canJoin,
        Instant createdAt
) {
}
