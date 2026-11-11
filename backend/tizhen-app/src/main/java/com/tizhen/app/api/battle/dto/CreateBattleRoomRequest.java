package com.tizhen.app.api.battle.dto;

/**
 * 创建对战房间的抽题筛选条件，均为可选。
 */
public record CreateBattleRoomRequest(
        String filterDifficulty,
        String filterTag
) {
}
