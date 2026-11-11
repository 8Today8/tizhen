package com.tizhen.core.battle.model;

import com.tizhen.core.account.model.UserBrief;

/**
 * 对战房间中的一方。只暴露结果标记，不暴露代码。
 */
public record BattlePlayerView(
        UserBrief user,
        boolean ready,
        boolean online,
        int submissionCount,
        String lastVerdict
) {
}
