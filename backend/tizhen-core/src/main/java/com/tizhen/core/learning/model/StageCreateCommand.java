package com.tizhen.core.learning.model;

/**
 * 新增学习阶段命令。
 */
public record StageCreateCommand(
        Long routeId,
        String name,
        String content,
        Integer sortOrder
) {
}
