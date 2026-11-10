package com.tizhen.core.datagen.model;

import com.tizhen.core.account.model.UserBrief;

import java.time.Instant;

/**
 * 后台造数据任务列表行。
 */
public record AdminDataGenTaskListItem(
        String id,
        String title,
        UserBrief user,
        String username,
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
