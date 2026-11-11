package com.tizhen.core.learning.model;

/**
 * 修改学习阶段命令。present 标记用于区分“未提交”和“显式清空”。
 */
public record StageUpdateCommand(
        String name,
        boolean namePresent,
        String content,
        boolean contentPresent,
        Integer sortOrder,
        boolean sortOrderPresent
) {
}
