package com.tizhen.core.datagen.model;

import java.time.Instant;

/**
 * 选手造数据任务列表行。不包含题面、std、文件路径与失败详情。
 */
public record DataGenTaskListItem(
        String id,
        String title,
        String generationMode,
        String stdLanguage,
        int groupCount,
        String status,
        String llmProvider,
        String llmModel,
        Instant createdAt,
        Instant startedAt,
        Instant finishedAt,
        Instant filesExpireAt,
        Instant filesCleanedAt
) {
}
