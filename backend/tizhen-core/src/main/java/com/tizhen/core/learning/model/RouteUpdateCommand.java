package com.tizhen.core.learning.model;

/**
 * 修改学习路线命令。present 标记用于区分“未提交”和“显式清空”。
 */
public record RouteUpdateCommand(
        String name,
        boolean namePresent,
        String description,
        boolean descriptionPresent,
        String coverUrl,
        boolean coverUrlPresent,
        Integer sortOrder,
        boolean sortOrderPresent
) {
}
