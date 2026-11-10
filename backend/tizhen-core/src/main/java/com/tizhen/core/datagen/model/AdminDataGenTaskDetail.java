package com.tizhen.core.datagen.model;

import com.tizhen.core.account.model.UserBrief;

import java.time.Instant;
import java.util.List;

/**
 * 后台造数据任务详情。不包含 generatorCode。
 */
public record AdminDataGenTaskDetail(
        String id,
        String title,
        UserBrief user,
        String username,
        String statement,
        String inputFormat,
        String outputFormat,
        String stdCode,
        String stdLanguage,
        Object dataRange,
        int groupCount,
        String generationMode,
        String specialRequirements,
        String llmProvider,
        String llmModel,
        String generatorStatus,
        String llmDataStatus,
        String status,
        String failureStage,
        String failureReason,
        Object resultSummary,
        int timeLimitMs,
        int memoryLimitMb,
        Instant startedAt,
        Instant finishedAt,
        Instant filesExpireAt,
        Instant filesCleanedAt,
        List<DataGenFileView> files
) {
}
