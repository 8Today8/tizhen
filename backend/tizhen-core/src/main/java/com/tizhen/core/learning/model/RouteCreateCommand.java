package com.tizhen.core.learning.model;

/**
 * 新增学习路线命令。
 */
public record RouteCreateCommand(
        String name,
        String description,
        String coverUrl,
        Integer sortOrder
) {
}
