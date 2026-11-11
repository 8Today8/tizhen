package com.tizhen.core.battle.model;

import java.util.List;

/**
 * 对战题目摘要。完整题面由选手在开始后通过题目详情接口读取。
 */
public record BattleProblemView(
        String id,
        String type,
        String title,
        String difficulty,
        List<String> tags
) {
}
