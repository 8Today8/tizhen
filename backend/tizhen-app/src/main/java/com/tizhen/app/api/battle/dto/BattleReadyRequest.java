package com.tizhen.app.api.battle.dto;

/**
 * 准备/取消准备请求。
 */
public record BattleReadyRequest(
        boolean ready
) {
}
