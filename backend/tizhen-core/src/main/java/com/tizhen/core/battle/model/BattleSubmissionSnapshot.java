package com.tizhen.core.battle.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 对战实时推送使用的提交状态快照，不包含代码。
 */
public record BattleSubmissionSnapshot(
        Long id,
        Long userId,
        String status,
        String verdict,
        BigDecimal score,
        LocalDateTime judgedAt,
        LocalDateTime updatedAt
) {
}
