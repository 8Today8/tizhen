package com.tizhen.app.api.battle.dto;

/**
 * 对战中的 OJ 代码提交。
 */
public record BattleSubmissionRequest(
        String language,
        String code
) {
}
